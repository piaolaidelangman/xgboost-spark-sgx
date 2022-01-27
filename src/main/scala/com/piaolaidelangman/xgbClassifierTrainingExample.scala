package xgboostsparksgx

import ml.dmlc.xgboost4j.scala.spark.TrackerConf

import org.apache.spark.ml.feature.{StringIndexer, VectorAssembler}
import org.apache.spark.sql.{SparkSession}
import org.apache.spark.SparkContext
import org.apache.spark.sql.types.{IntegerType, DoubleType, StringType, StructField, StructType, BinaryType, ArrayType, DecimalType}
import org.apache.spark.sql.functions.col

object xgbClassifierTrainingExample {
  def main(args: Array[String]): Unit = {
    if (args.length < 4) {
      println("Usage: program input_path num_threads num_round modelsave_path")
      sys.exit(1)
    }
    val sc = new SparkContext()
    val spark = SparkSession.builder().getOrCreate()
    // import spark.implicits._

    val input_path = args(0) // path to iris.data
    val num_threads = args(1).toInt
    val num_round = args(2).toInt
    val modelsave_path = args(3) // save model to this path
    val num_classes = args(4).toInt

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
      StructField("categorical feature 1", DoubleType, false),
      StructField("cf 2", DoubleType, false),
      StructField("cf 3", DoubleType, false),
      StructField("cf 4", DoubleType, false),
      StructField("cf 5", DoubleType, false),
      StructField("cf 6", DoubleType, false),
      StructField("cf 7", DoubleType, false),
      StructField("cf 8", DoubleType, false),
      StructField("cf 9", DoubleType, false),
      StructField("cf 10", DoubleType, false),
      StructField("cf 11", DoubleType, false),
      StructField("cf 12", DoubleType, false),
      StructField("cf 13", DoubleType, false),
      StructField("cf 14", DoubleType, false),
      StructField("cf 15", DoubleType, false),
      StructField("cf 16", DoubleType, false),
      StructField("cf 17", DoubleType, false),
      StructField("cf 18", DoubleType, false),
      StructField("cf 19", DoubleType, false),
      StructField("cf 20", DoubleType, false),
      StructField("cf 21", DoubleType, false),
      StructField("cf 22", DoubleType, false),
      StructField("cf 23", DoubleType, false),
      StructField("cf 24", DoubleType, false),
      StructField("cf 25", DoubleType, false),
      StructField("cf 26", DoubleType, false)
    ))
    var df = spark.read.option("delimiter", "\t").schema(schema).csv(input_path)
    df.show()

    val filterCond = df.columns.map(x=>col(x).isNotNull).reduce(_ && _)
    df.filter(filterCond).show()

    // val stringIndexer = new StringIndexer()
    //   .setInputCol("label")
    //   .setOutputCol("classIndex")
    //   .fit(df)
    // val labelTransformed = stringIndexer.transform(df).drop("label")
    // // compose all feature columns as vector
    // val vectorAssembler = new VectorAssembler().
    //   setInputCols(Array("integer feature 1", "iF 2", "iF 3", "iF 4", "iF 5", "iF 6", "iF 7", "iF 8", "iF 9", "iF 10", "iF 11", "iF 12", "iF 13", "categorical feature 1", "cf 2",
    //    "cf 3", "cf 4", "cf 5", "cf 6", "cf 7", "cf 8", "cf 9", "cf 10", "cf 11", "cf 12", "cf 13", "cf 14", "cf 15", "cf 16", "cf 17", "cf 18", "cf 19", "cf 20", "cf 21", "cf 22", "cf 23", "cf 24", "cf 25", "cf 26")).
    //   setOutputCol("features")
    // val xgbInput = vectorAssembler.transform(labelTransformed).select("features","classIndex")

    // val Array(train, eval1, eval2, test) = xgbInput.randomSplit(Array(0.6, 0.2, 0.1, 0.1))

    // val xgbParam = Map("tracker_conf" -> TrackerConf(60*60, "scala"),
    //   "eval_sets" -> Map("eval1" -> eval1, "eval2" -> eval2))

    // val xgbClassifier = new XGBClassifier(xgbParam)
    // xgbClassifier.setFeaturesCol("features")
    // xgbClassifier.setLabelCol("classIndex")
    // xgbClassifier.setNumClass(num_classes)
    // xgbClassifier.setMaxDepth(2)
    // xgbClassifier.setNumWorkers(1)
    // xgbClassifier.setNthread(num_threads)
    // xgbClassifier.setNumRound(num_round)
    // xgbClassifier.setTreeMethod("auto")
    // xgbClassifier.setObjective("multi:softprob")
    // xgbClassifier.setTimeoutRequestWorkers(180000L)

    // val xgbClassificationModel = xgbClassifier.fit(train)
    // xgbClassificationModel.save(modelsave_path)

    sc.stop()
  }
}
