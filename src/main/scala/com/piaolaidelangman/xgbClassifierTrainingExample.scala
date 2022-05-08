package xgboostsparksgx

import ml.dmlc.xgboost4j.scala.spark.TrackerConf

import org.apache.spark.ml.feature.{StringIndexer, VectorAssembler}
import org.apache.spark.sql.{SparkSession, Row, DataFrame}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.types.{IntegerType, StructField, StructType, LongType}
/**
 * @author diankun.an
 */
object xgbClassifierTrainingExample extends Supportive{
 def rddToDf(rdd: RDD[String], spark: SparkSession): DataFrame = {
   val decryptionRDD = rdd.flatMap(_.split("\n"))
   val columns = 40

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
   spark.createDataFrame(rowRDD,schema)
 }
  def main(args: Array[String]): Unit = {

    val spark = SparkSession.builder().getOrCreate()
    import spark.implicits._

    val task = new Task()
    val input_path = args(0) // path to iris.data
    val num_threads = args(1).toInt
    val secret = args(2)
    val num_workers = args(3).toInt
    val val_data_path = args(4)

    val columns = 40

    val tmpData = spark.sparkContext.binaryFiles(input_path)
    val decryption = timing("Spark decrypt data") {
      tmpData
        .map{ case (name, bytesData) => {
          task.decryptBytesWithJavaAESCBC(bytesData.toArray, secret)
        }}
    }
    val df = rddToDf(decryption, spark)
    df.show()

    val stringIndexer = new StringIndexer()
      .setInputCol("_c0")
      .setOutputCol("classIndex")
      .fit(df)
    val labelTransformed = stringIndexer.transform(df).drop("_c0")

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
    xgbClassifier.setMaxDepth(4)
    xgbClassifier.setNthread(num_threads)
    xgbClassifier.setNumRound(10)
    xgbClassifier.setTreeMethod("auto")
    xgbClassifier.setObjective("multi:soft-prob")
    xgbClassifier.setTimeoutRequestWorkers(180000L)

    val xgbClassificationModel = timing("XGBoost Training ") {
      xgbClassifier.fit(train)
    }

////////////////// Predict
    val valDecryption = spark.sparkContext.binaryFiles(val_data_path)
      .map{ case (name, bytesData) => {
        task.decryptBytesWithJavaAESCBC(bytesData.toArray, secret)
      }}

    val valDf = rddToDf(valDecryption, spark)
    val featureArray = new Array[String](columns-1)
    for(i <- 1 to columns-1){
      featureArray(i-1) = "_c" + i.toString
    }
    xgbClassificationModel.setFeaturesCol(featureArray)
    val result = xgbClassificationModel.transform(valDf)
    result.show()
/////////////
    spark.stop()
  }
}
