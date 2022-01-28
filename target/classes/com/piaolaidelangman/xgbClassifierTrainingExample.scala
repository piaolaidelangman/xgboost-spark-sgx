package xgboostsparksgx

import ml.dmlc.xgboost4j.scala.spark.TrackerConf

import org.apache.spark.ml.feature.{StringIndexer, VectorAssembler}
import org.apache.spark.sql.{SparkSession, Row}
import org.apache.spark.SparkContext
import org.apache.spark.sql.types.{IntegerType, DoubleType, StringType, StructField, StructType, BinaryType, ArrayType, FloatType, LongType, ByteType, DataTypes}
import org.apache.spark.sql.functions.{col, udf}




object xgbClassifierTrainingExample {

  def main(args: Array[String]): Unit = {
    if (args.length < 4) {
      println("Usage: program input_path num_threads num_round modelsave_path")
      sys.exit(1)
    }
    val sc = new SparkContext()
    val spark = SparkSession.builder().getOrCreate()
    import spark.implicits._

    val task = new Task()

    val input_path = args(0) // path to iris.data
    val num_threads = args(1).toInt
    val num_round = args(2).toInt
    val modelsave_path = args(3) // save model to this path
    val num_classes = args(4).toInt

    // val DecimalType = DataTypes.createDecimalType(32, 0)
    val schema = new StructType(Array(
      StructField("label", StringType, false),
      StructField("integer feature 1", IntegerType, false),
      StructField("iF 2", IntegerType, false),
      StructField("iF 3", IntegerType, false),
      StructField("iF 4", IntegerType, false),
      StructField("iF 5", IntegerType, false),
      StructField("iF 6", IntegerType, false),
      StructField("iF 7", IntegerType, false),
      StructField("iF 8", IntegerType, false),
      StructField("iF 9", IntegerType, false),
      StructField("iF 10", IntegerType, false),
      StructField("iF 11", IntegerType, false),
      StructField("iF 12", IntegerType, false),
      StructField("iF 13", IntegerType, false),
      StructField("categorical feature 1", LongType, false),
      StructField("cf 2", LongType, false),
      StructField("cf 3", LongType, false),
      StructField("cf 4", LongType, false),
      StructField("cf 5", LongType, false),
      StructField("cf 6", LongType, false),
      StructField("cf 7", LongType, false),
      StructField("cf 8", LongType, false),
      StructField("cf 9", LongType, false),
      StructField("cf 10", LongType, false),
      StructField("cf 11", LongType, false),
      StructField("cf 12", LongType, false),
      StructField("cf 13", LongType, false),
      StructField("cf 14", LongType, false),
      StructField("cf 15", LongType, false),
      StructField("cf 16", LongType, false),
      StructField("cf 17", LongType, false),
      StructField("cf 18", LongType, false),
      StructField("cf 19", LongType, false),
      StructField("cf 20", LongType, false),
      StructField("cf 21", LongType, false),
      StructField("cf 22", LongType, false),
      StructField("cf 23", LongType, false),
      StructField("cf 24", LongType, false),
      StructField("cf 25", LongType, false),
      StructField("cf 26", LongType, false)
    ))

    var df = spark.read.option("header", "false").option("delimiter", " ").schema(schema).csv(input_path)
    // var df = spark.read.option("header", "false").option("inferSchema", "true").option("delimiter", " ").csv(input_path)
    df.show()

//##################

    val stringIndexer = new StringIndexer()
      .setInputCol("label")
      .setOutputCol("classIndex")
      .fit(df)
    val labelTransformed = stringIndexer.transform(df).drop("label")
    // compose all feature columns as vector
    // val vectorAssembler = new VectorAssembler().
    //   setInputCols(Array( "_c1", "_c2", "_c3", "_c4", "_c5", "_c6", "_c7", "_c8", "_c9", "_c10", "_c11", "_c12", "_c13", "_c14", "_c15", "_c16", "_c17", "_c18", "_c19",
    //    "_c20", "_c21", "_c22", "_c23", "_c24", "_c25", "_c26", "_c27", "_c28", "_c29", "_c30", "_c31", "_c32", "_c33", "_c34", "_c35", "_c36", "_c37", "_c38", "_c39")).
    //   setOutputCol("features")
    val vectorAssembler = new VectorAssembler().
    setInputCols(Array("integer feature 1", "iF 2", "iF 3", "iF 4", "iF 5", "iF 6", "iF 7", "iF 8", "iF 9", "iF 10", "iF 11", "iF 12", "iF 13", "categorical feature 1", "cf 2",
      "cf 3", "cf 4", "cf 5", "cf 6", "cf 7", "cf 8", "cf 9", "cf 10", "cf 11", "cf 12", "cf 13", "cf 14", "cf 15", "cf 16", "cf 17", "cf 18", "cf 19", "cf 20", "cf 21", "cf 22", "cf 23", "cf 24", "cf 25", "cf 26")).
    setOutputCol("features")

    val xgbInput = vectorAssembler.transform(labelTransformed).select("features","classIndex")

    val Array(train, eval1, eval2, test) = xgbInput.randomSplit(Array(0.6, 0.2, 0.1, 0.1))

    val xgbParam = Map("tracker_conf" -> TrackerConf(60*60, "scala"),
      "eval_sets" -> Map("eval1" -> eval1, "eval2" -> eval2))

    val xgbClassifier = new XGBClassifier(xgbParam)
    xgbClassifier.setFeaturesCol("features")
    xgbClassifier.setLabelCol("classIndex")
    xgbClassifier.setNumClass(num_classes)
    xgbClassifier.setMaxDepth(2)
    xgbClassifier.setNumWorkers(1)
    xgbClassifier.setNthread(num_threads)
    xgbClassifier.setNumRound(num_round)
    xgbClassifier.setTreeMethod("auto")
    xgbClassifier.setObjective("multi:softprob")
    xgbClassifier.setTimeoutRequestWorkers(180000L)

    val xgbClassificationModel = xgbClassifier.fit(train)
    xgbClassificationModel.save(modelsave_path)

    sc.stop()
  }
}
