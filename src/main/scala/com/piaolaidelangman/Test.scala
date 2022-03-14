package xgboostsparksgx

import ml.dmlc.xgboost4j.scala.spark.TrackerConf

import org.apache.spark.SparkContext
import org.apache.spark.ml.feature.{StringIndexer, VectorAssembler}
import org.apache.spark.sql.{SparkSession, Row}
import org.apache.spark.sql.types.{IntegerType, StructField, StructType, LongType, DoubleType}
import org.apache.spark.sql.functions.{col, udf}

import java.util.Base64


object Test {

  def main(args: Array[String]): Unit = {

    val featureNum = 4
    val sc = new SparkContext()
    val spark = SparkSession.builder().getOrCreate()
    import spark.implicits._

    val task = new Task()

    val input_path = args(0) // path to iris.data
    val num_threads = args(1).toInt
    val modelsave_path = args(2) // save model to this path
    val secret = args(3)

    var decryption = sc.binaryFiles(input_path)
      .map{ case (name, bytesData) => {
        task.decryptBytesWithJavaAESCBC(bytesData.toArray, secret)
      }}

    var structFieldArray = new Array[StructField](40)
    for(i <- 0 to featureNum){
      // structFieldArray(i) = StructField("_c" + i.toString, if(i<14) IntegerType else LongType, true)
      structFieldArray(i) = StructField("_c" + i.toString, DoubleType, true)
    }
    var schema =  new StructType(structFieldArray)
  
    val decryptionRDD = decryption.flatMap(_.split("\n"))

    val rowRDD = decryptionRDD.map(_.split(" ")).map(row => Row.fromSeq(
      for{
        i <- 0 to featureNum
      } yield {
        row(i).toDouble
      }
    ))

    val df = spark.createDataFrame(rowRDD,schema)

    val stringIndexer = new StringIndexer()
      .setInputCol("_c0")
      .setOutputCol("classIndex")
      .fit(df)
    val labelTransformed = stringIndexer.transform(df).drop("_c0")

    var inputCols = new Array[String](featureNum)
    for(i <- 0 to featureNum-1){
      inputCols(i) = "_c" + (i+1).toString
    }

    val vectorAssembler = new VectorAssembler().
      setInputCols(inputCols).
      setOutputCol("features")

    val xgbInput = vectorAssembler.transform(labelTransformed).select("features","classIndex")

    val Array(train, eval1, eval2, test) = xgbInput.randomSplit(Array(0.6, 0.2, 0.1, 0.1))

    val xgbParam = Map("tracker_conf" -> TrackerConf(0L, "scala"),
      "eval_sets" -> Map("eval1" -> eval1, "eval2" -> eval2),
      "missing" -> 0,
      "use_external_memory" -> true,
      "allow_non_zero_for_missing" ->true,
      "cache_training_set" -> true,
      "checkpoint_path" -> "/tmp",
      )

    val xgbClassifier = new XGBClassifier(xgbParam)
    xgbClassifier.setFeaturesCol("features")
    xgbClassifier.setLabelCol("classIndex")
    xgbClassifier.setNumClass(3)
    xgbClassifier.setNumWorkers(1)
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