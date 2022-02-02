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
    // val sc = new SparkContext()
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

    // var schema =  new StructType(for{
    //   i <- 0 to 39
    // } yield {
    //   // if(i<14)
    //   StructField("_c" + i.toString, if(i<14) IntegerType else LongType, true)
    //   // else
    //   //   StructField("_c" + i.toString, LongType, true)
    // })
    var structFieldArray = new Array[StructField](39)
    for(i <- 0 to 39){
      structFieldArray[i] = StructField("_c" + i.toString, if(i<14) IntegerType else LongType, true)
    }
    var schema =  new StructType(
      // 0 until 40 flatMap {
      //   case i => StructField("_c" + i.toString, if(i<14) IntegerType else LongType, true)
      // }
      stringArray
    )
    // val schema = new StructType(Array(
    //   StructField("_c0", IntegerType, true),
    //   StructField("_c1", IntegerType, true),
    //   StructField("_c2", IntegerType, true),
    //   StructField("_c3", IntegerType, true),
    //   StructField("_c4", IntegerType, true),
    //   StructField("_c5", IntegerType, true),
    //   StructField("_c6", IntegerType, true),
    //   StructField("_c7", IntegerType, true),
    //   StructField("_c8", IntegerType, true),
    //   StructField("_c9", IntegerType, true),
    //   StructField("_c10", IntegerType, true),
    //   StructField("_c11", IntegerType, true),
    //   StructField("_c12", IntegerType, true),
    //   StructField("_c13", IntegerType, true),
    //   StructField("_c14", LongType, true),
    //   StructField("_c15", LongType, true),
    //   StructField("_c16", LongType, true),
    //   StructField("_c17", LongType, true),
    //   StructField("_c18", LongType, true),
    //   StructField("_c19", LongType, true),
    //   StructField("_c20", LongType, true),
    //   StructField("_c21", LongType, true),
    //   StructField("_c22", LongType, true),
    //   StructField("_c23", LongType, true),
    //   StructField("_c24", LongType, true),
    //   StructField("_c25", LongType, true),
    //   StructField("_c26", LongType, true),
    //   StructField("_c27", LongType, true),
    //   StructField("_c28", LongType, true),
    //   StructField("_c29", LongType, true),
    //   StructField("_c30", LongType, true),
    //   StructField("_c31", LongType, true),
    //   StructField("_c32", LongType, true),
    //   StructField("_c33", LongType, true),
    //   StructField("_c34", LongType, true),
    //   StructField("_c35", LongType, true),
    //   StructField("_c36", LongType, true),
    //   StructField("_c37", LongType, true),
    //   StructField("_c38", LongType, true),
    //   StructField("_c39", LongType, true)
    // ))

    // var df = spark.read.option("header", "false").option("delimiter", " ").schema(schema).csv(input_path+"/*.csv")
    // var df = spark.read.option("header", "false").option("inferSchema", "true").option("delimiter", " ").csv(input_path)
    val decryptionRDD = decryption.flatMap(_.split("\n"))
    decryptionRDD.take(2).foreach(println)
    // val schemaString = decryptionRDD.first()
    // val fields = schemaString.split(",")
    // .map(fieldName => StructField(fieldName, StringType, nullable = true))
    // val schema = StructType(fields)

    // val rowRDD = decryptionRDD.map(_.split(" ")).map(stringArray => Row.fromSeq(stringArray))
    val rowRDD = decryptionRDD.map(_.split(" ")).map(row => Row(row(0).toInt, row(1).toInt, row(2).toInt, row(3).toInt, row(4).toInt, row(5).toInt, row(6).toInt, row(7).toInt, row(8).toInt, row(9).toInt, row(10).toInt, row(11).toInt, row(12).toInt, row(13).toInt, 
    row(14).toLong, row(15).toLong, row(16).toLong, row(17).toLong, row(18).toLong, row(19).toLong, row(20).toLong, row(21).toLong, row(22).toLong, row(23).toLong, row(24).toLong, row(25).toLong, row(26).toLong, row(27).toLong, row(28).toLong, row(29).toLong, row(30).toLong, row(31).toLong, 
    row(32).toLong, row(33).toLong, row(34).toLong, row(35).toLong, row(36).toLong, row(37).toLong, row(38).toLong, row(39).toLong))


    // val rowRDD = decryptionRDD.map(_.split(" ")).map(row =>
    //   0 until row.length flatMap {
    //   // case 0 => Some(row(0).toString)
    //   // case i if row(i) == null => None
    //   case i => Some( if (i < 14) row(i).toInt else row(i).toLong )
    // })
    rowRDD.take(2).foreach(println)

    // val sqlString = schemaString.split(",")(0) + " != '" + schemaString.split(",")(0) +"'"
    // val df = spark.createDataFrame(rowRDD,schema).filter("ds_rk != 'ds_rk'")
    val df = spark.createDataFrame(rowRDD,schema)
    df.show()

//##################

    val stringIndexer = new StringIndexer()
      .setInputCol("_c0")
      .setOutputCol("classIndex")
      .fit(df)
    val labelTransformed = stringIndexer.transform(df).drop("_c0")

    // val vectorAssembler = new VectorAssembler().
    // setInputCols(Array("integer feature 1", "iF 2", "iF 3", "iF 4", "iF 5", "iF 6", "iF 7", "iF 8", "iF 9", "iF 10", "iF 11", "iF 12", "iF 13", "categorical feature 1", "cf 2",
    //   "cf 3", "cf 4", "cf 5", "cf 6", "cf 7", "cf 8", "cf 9", "cf 10", "cf 11", "cf 12", "cf 13", "cf 14", "cf 15", "cf 16", "cf 17", "cf 18", "cf 19", "cf 20", "cf 21", "cf 22", "cf 23", "cf 24", "cf 25", "cf 26")).
    // setOutputCol("features")
    // .setHandleInvalid("keep")
    val vectorAssembler = new VectorAssembler().
      setInputCols(Array( "_c1", "_c2", "_c3", "_c4", "_c5", "_c6", "_c7", "_c8", "_c9", "_c10", "_c11", "_c12", "_c13", "_c14", "_c15", "_c16", "_c17", "_c18", "_c19",
      "_c20", "_c21", "_c22", "_c23", "_c24", "_c25", "_c26", "_c27", "_c28", "_c29", "_c30", "_c31", "_c32", "_c33", "_c34", "_c35", "_c36", "_c37", "_c38", "_c39" )).
      setOutputCol("features")
      .setHandleInvalid("skip")


    val xgbInput = vectorAssembler.transform(labelTransformed).select("features","classIndex")

    val Array(train, eval1, eval2, test) = xgbInput.randomSplit(Array(0.6, 0.2, 0.1, 0.1))

    val xgbParam = Map("tracker_conf" -> TrackerConf(0L, "scala"),
      "eval_sets" -> Map("eval1" -> eval1, "eval2" -> eval2),
      "missing" -> -999
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

    // compose all feature columns as vector
    // val vectorAssembler = new VectorAssembler().
    //   setInputCols(Array( "_c1", "_c2", "_c3", "_c4", "_c5", "_c6", "_c7", "_c8", "_c9", "_c10", "_c11", "_c12", "_c13", "_c14", "_c15", "_c16", "_c17", "_c18", "_c19",
    //    "_c20", "_c21", "_c22", "_c23", "_c24", "_c25", "_c26", "_c27", "_c28", "_c29", "_c30", "_c31", "_c32", "_c33", "_c34", "_c35", "_c36", "_c37", "_c38", "_c39")).
    //   setOutputCol("features")