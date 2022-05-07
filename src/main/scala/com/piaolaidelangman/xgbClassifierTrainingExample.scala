package xgboostsparksgx

import ml.dmlc.xgboost4j.scala.spark.TrackerConf

import org.apache.spark.SparkContext
import org.apache.spark.ml.feature.{StringIndexer, VectorAssembler}
import org.apache.spark.sql.{SparkSession, Row}
import org.apache.spark.sql.types.{IntegerType, StructField, StructType, LongType}
import org.slf4j.LoggerFactory
/**
 * @author diankun.an
 */
object xgbClassifierTrainingExample {

  def main(args: Array[String]): Unit = {

    val logger = LoggerFactory.getLogger(getClass)
    val spark = SparkSession.builder().getOrCreate()
    import spark.implicits._

    val task = new Task()
    val input_path = args(0) // path to iris.data
    val num_threads = args(1).toInt
    val secret = args(2)
    val num_workers = args(3).toInt
    val val_data_path = args(4)

    val tmpData = spark.sparkContext.binaryFiles(input_path)
    var begin = System.currentTimeMillis
    val decryption = tmpData
      .map{ case (name, bytesData) => {
        task.decryptBytesWithJavaAESCBC(bytesData.toArray, secret)
      }}
    var end = System.currentTimeMillis
    var cost = (end - begin)
    logger.info(s"SUCCESS Spark decrypt data time elapsed $cost ms.")

    val decryptionRDD = decryption.flatMap(_.split("\n"))
    // decryptionRDD.foreach(println)
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
//    df.cache()
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
    xgbClassifier.setMaxDepth(2)
    xgbClassifier.setNthread(num_threads)
    xgbClassifier.setNumRound(10)
    xgbClassifier.setTreeMethod("auto")
    xgbClassifier.setObjective("multi:softprob")
    xgbClassifier.setTimeoutRequestWorkers(180000L)

    begin = System.currentTimeMillis
    val xgbClassificationModel = xgbClassifier.fit(train)
    end = System.currentTimeMillis
    cost = (end - begin)
    logger.info(s"Spark XGBoost training time elapsed $cost ms.")

////////////////// Predict
    val valDecryption = spark.sparkContext.binaryFiles(val_data_path)
      .map{ case (name, bytesData) => {
        task.decryptBytesWithJavaAESCBC(bytesData.toArray, secret)
      }}

    val valDecryptionRDD = valDecryption.flatMap(_.split("\n"))
    // decryptionRDD.foreach(println)
    val valColumns = valDecryptionRDD.first.split(",").length

    var valStructFieldArray = new Array[StructField](valColumns)
    for(i <- 0 to valColumns-1){
      valStructFieldArray(i) = StructField("_c" + i.toString, LongType, true)
    }
    var valSchema =  new StructType(valStructFieldArray)


    val valRowRDD = valDecryptionRDD.map(_.split(",")).map(row => Row.fromSeq(
      for{
        i <- 0 to columns-1
      } yield {
        // if(i<14) row(i).toInt else row(i).toLong
        row(i).toLong
      }
    ))

    val valDf = spark.createDataFrame(valRowRDD,valSchema).repartition(2)
    val featureArray = new Array[String](valColumns-1)
    for(i <- 1 to valColumns-1){
      featureArray(i-1) = "_c" + i.toString
    }
    xgbClassificationModel.setFeaturesCol(featureArray)
    val result = xgbClassificationModel.transform(valDf)
    result.show()
/////////////
    spark.stop()
  }
}
