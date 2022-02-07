package xgboostsparksgx

import ml.dmlc.xgboost4j.scala.spark.TrackerConf

import org.apache.spark.SparkContext
import org.apache.spark.ml.feature.{StringIndexer, VectorAssembler}
import org.apache.spark.sql.{SparkSession, Row}
import org.apache.spark.SparkContext
import org.apache.spark.sql.types.{IntegerType, DoubleType, StringType, StructField, StructType, BinaryType, ArrayType, FloatType, LongType, ByteType, DataTypes}
import org.apache.spark.sql.functions.{col, udf}

import java.util.Base64


object xgbClassifierTrainingExample {

  def main(args: Array[String]): Unit = {
    // if (args.length < 4) {
    //   println("Usage: program input_path num_threads num_round modelsave_path")
    //   sys.exit(1)
    // }
    val sc = new SparkContext()
    val spark = SparkSession.builder().getOrCreate()
    import spark.implicits._

    val task = new Task()

    val input_path = args(0) // path to iris.data
    val num_threads = args(1).toInt
    val num_workers = args(2).toInt
    val modelsave_path = args(3) // save model to this path
    val secret = args(4)

    val decoder = Base64.getDecoder()
    val encoder = Base64.getEncoder()
    val key = decoder.decode(decoder.decode(encoder.encodeToString(secret.getBytes)))

    var decryption = sc.binaryFiles(input_path)
      .map{ case (name, bytesData) => {
        task.decryptBytesWithJavaAESCBC(bytesData.toArray, key)
      }}

    var structFieldArray = new Array[StructField](40)
    for(i <- 0 to 39){
      // structFieldArray(i) = StructField("_c" + i.toString, if(i<14) IntegerType else LongType, true)
      structFieldArray(i) = StructField("_c" + i.toString, LongType, true)

    }
    var schema =  new StructType(structFieldArray)
  
    val decryptionRDD = decryption.flatMap(_.split("\n"))

    val rowRDD = decryptionRDD.map(_.split(" ")).map(row => Row.fromSeq(
      for{
        i <- 0 to 39
      } yield {
        // if(i<14) row(i).toInt else row(i).toLong
        row(i).toLong
      }
    ))

    val df = spark.createDataFrame(rowRDD,schema)

    val stringIndexer = new StringIndexer()
      .setInputCol("_c0")
      .setOutputCol("classIndex")
      .fit(df)
    val labelTransformed = stringIndexer.transform(df).drop("_c0")

    var inputCols = new Array[String](39)
    for(i <- 0 to 38){
      inputCols(i) = "_c" + (i+1).toString
    }

    val vectorAssembler = new VectorAssembler().
      setInputCols(inputCols).
      setOutputCol("features")

    val xgbInput = vectorAssembler.transform(labelTransformed).select("features","classIndex")

    val Array(train, eval1, eval2, test) = xgbInput.randomSplit(Array(0.6, 0.2, 0.1, 0.1))

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
    xgbClassifier.setNumRound(100)
    xgbClassifier.setTreeMethod("auto")
    xgbClassifier.setObjective("multi:softprob")
    xgbClassifier.setTimeoutRequestWorkers(180000L)

    val xgbClassificationModel = xgbClassifier.fit(train)
    xgbClassificationModel.save(modelsave_path)

    sc.stop()
    spark.stop()
  }
}