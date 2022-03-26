package xgboostsparksgx

import ml.dmlc.xgboost4j.scala.spark.TrackerConf

import org.apache.spark.SparkContext
import org.apache.spark.ml.feature.{StringIndexer, VectorAssembler}
import org.apache.spark.sql.{SparkSession, Row}
import org.apache.spark.sql.types.{IntegerType, StructField, StructType, LongType}
/**
 * @author diankun.an
 */
object xgbClassifierTrainingExample {

  def main(args: Array[String]): Unit = {

    val spark = SparkSession.builder().getOrCreate()
    import spark.implicits._

    val task = new Task()
    val input_path = args(0) // path to iris.data
    val num_threads = args(1).toInt
    val modelsave_path = args(2) // save model to this path
    val secret = args(3)
    val num_workers = args(4).toInt

    var decryption = spark.sparkContext.binaryFiles(input_path)
      .map{ case (name, bytesData) => {
        task.decryptBytesWithJavaAESCBC(bytesData.toArray, secret)
      }}
    val decryptionRDD = decryption.flatMap(_.split("\n"))
    // decryptionRDD.foreach(println)
    val columns = decryptionRDD.first.split(",").length

    var structFieldArray = new Array[StructField](columns)
    for(i <- 0 to columns-1){
      // structFieldArray(i) = StructField("_c" + i.toString, if(i<14) IntegerType else LongType, true)
      structFieldArray(i) = StructField("_c" + i.toString, LongType, true)
    }
    var schema =  new StructType(structFieldArray)
  

    val rowRDD = decryptionRDD.map(_.split(",")).map(row => Row.fromSeq(
      for{
        i <- 0 to columns-1
      } yield {
        // if(i<14) row(i).toInt else row(i).toLong
        row(i).toLong
      }
    ))

    val df = spark.createDataFrame(rowRDD,schema).repartition(8)
    //val df = spark.read.format("csv").option("inferSchema",true).option("header",false).option("delimiter","\t").load(input_path)
    df.show()
    df.printSchema()

    val stringIndexer = new StringIndexer()
      .setInputCol("_c0")
      .setOutputCol("classIndex")
      .fit(df)
    val labelTransformed = stringIndexer.transform(df).drop("_c0")

    //val columns=40
    var inputCols = new Array[String](columns-1)
    for(i <- 0 to columns-2){
      inputCols(i) = "_c" + (i+1).toString
    }

    val vectorAssembler = new VectorAssembler().
      setInputCols(inputCols).
      setOutputCol("features")

    val xgbInput = vectorAssembler.transform(labelTransformed).select("features","classIndex")

    val Array(train, eval1, eval2, test) = xgbInput.randomSplit(Array(0.6, 0.2, 0.1, 0.1))

 /*   val xgbParam = Map("tracker_conf" -> TrackerConf(0L, "scala"),
      "eval_sets" -> Map("eval1" -> eval1, "eval2" -> eval2),
      "missing" -> 0,
      "use_external_memory" -> true,
      "allow_non_zero_for_missing" ->true,
      "cache_training_set" -> true,
      "checkpoint_path" -> "/tmp"
      )*/
   val xgbParam = Map("tracker_conf" -> TrackerConf(0L, "scala"),
     "eval_sets" -> Map("eval1" -> eval1, "eval2" -> eval2),
     "missing" -> 0
   )

    val xgbClassifier = new XGBClassifier(xgbParam)
    xgbClassifier.setFeaturesCol("features")
    xgbClassifier.setLabelCol("classIndex")
    xgbClassifier.setNumClass(2)
    xgbClassifier.setNumWorkers(num_workers)
    xgbClassifier.setMaxDepth(2)
    xgbClassifier.setNthread(num_threads)
    xgbClassifier.setNumRound(10)
    xgbClassifier.setTreeMethod("auto")
    xgbClassifier.setObjective("multi:softprob")
    xgbClassifier.setTimeoutRequestWorkers(180000L)

    val xgbClassificationModel = xgbClassifier.fit(train)
    xgbClassificationModel.save(modelsave_path)

    spark.stop()
  }
}
