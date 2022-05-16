package xgboostsparksgx

import ml.dmlc.xgboost4j.scala.spark.TrackerConf
import org.apache.spark.ml.feature.{StringIndexer, VectorAssembler}
import org.apache.spark.sql.{DataFrame, Row, SparkSession}
import org.apache.spark.sql.types.{IntegerType, LongType, StructField, StructType}
/**
 * @author diankun.an
 */
object Autotest extends Supportive {

  def main(args: Array[String]): Unit = {
    val begin = System.currentTimeMillis
    val spark = SparkSession.builder().getOrCreate()
    import spark.implicits._

    val task = new Task()
    val input_path = args(0) // path to iris.data
    val secret = args(1)
    val num_threads = args(2).toInt
    val num_workers = args(3).toInt
    val max_depth = args(4).toInt
    val num_rounds = args(5).toInt

    val tmpData = spark.sparkContext.binaryFiles(input_path)

    val decryption = timing("Spark decrypt data"){
      tmpData.map{ case (name, bytesData) => {
        task.decryptBytesWithJavaAESCBC(bytesData.toArray, secret)
      }}
    }.flatMap(_.split("\n"))

    val decryptionRDD = decryption.flatMap(_.split("\n"))
    val columns = decryptionRDD.first.split(",").length

    val structFieldArray = new Array[StructField](columns)
    for(i <- 0 to columns-1){
      // structFieldArray(i) = StructField("_c" + i.toString, if(i<14) IntegerType else LongType, true)
      structFieldArray(i) = StructField("_c" + i.toString, LongType, true)
    }
    val schema =  new StructType(structFieldArray)


    val rowRDD = decryptionRDD.map(_.split(",")).map(row => Row.fromSeq(
      for{
        i <- 0 to columns-1
      } yield {
        // if(i<14) row(i).toInt else row(i).toLong
        row(i).toLong
      }
    ))

    val df = spark.createDataFrame(rowRDD,schema)
    //val df = spark.read.format("csv").option("inferSchema",true).option("header",false).option("delimiter","\t").load(input_path)
    //    df.show()
    //    df.printSchema()

    val stringIndexer = new StringIndexer()
      .setInputCol("_c0")
      .setOutputCol("classIndex")
      .fit(df)
    val labelTransformed = stringIndexer.transform(df).drop("_c0")

    //val columns=40
    val inputCols = new Array[String](columns-1)
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
      "eval_sets" -> Map("eval1" -> eval1, "eval2" -> eval2)
    )

    val xgbClassifier = new XGBClassifier(xgbParam)
    xgbClassifier.setFeaturesCol("features")
    xgbClassifier.setLabelCol("classIndex")
    xgbClassifier.setNumClass(2)
    xgbClassifier.setNumWorkers(num_workers)
    xgbClassifier.setMaxDepth(max_depth)
    xgbClassifier.setNthread(num_threads)
    xgbClassifier.setNumRound(num_rounds)
    xgbClassifier.setTreeMethod("auto")
    xgbClassifier.setObjective("multi:softprob")
    xgbClassifier.setTimeoutRequestWorkers(180000L)

    val xgbClassificationModel = timing("XGBoost Training"){
      xgbClassifier.fit(train)
    }

    ////////////////// Predict

    val result = xgbClassificationModel.transform(test)
    result.show()


    /////////////
    spark.stop()
    val end = System.currentTimeMillis
    val cost = (end - begin)
    Supportive.logger.info(s"SUCCESS SUCCESS SparkXGBoostRunTime elapsed $cost ms.")
  }
}