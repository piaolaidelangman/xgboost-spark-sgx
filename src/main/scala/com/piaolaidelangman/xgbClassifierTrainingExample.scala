package xgboostsparksgx

import ml.dmlc.xgboost4j.scala.spark.TrackerConf

import org.apache.spark.ml.feature.{StringIndexer, VectorAssembler}
import org.apache.spark.sql.{SparkSession}
import org.apache.spark.SparkContext
import org.apache.spark.sql.types.{DoubleType, StringType, StructField, StructType}

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

    val schema = new StructType(Array(
      StructField("sepal length", DoubleType, true),
      StructField("sepal width", DoubleType, true),
      StructField("petal length", DoubleType, true),
      StructField("petal width", DoubleType, true),
      StructField("class", StringType, true)))
    val df = spark.read.schema(schema).csv(input_path)

    val stringIndexer = new StringIndexer()
      .setInputCol("class")
      .setOutputCol("classIndex")
      .fit(df)
    val labelTransformed = stringIndexer.transform(df).drop("class")
    // compose all feature columns as vector
    val vectorAssembler = new VectorAssembler().
      setInputCols(Array("sepal length", "sepal width", "petal length", "petal width")).
      setOutputCol("features")
    val xgbInput = vectorAssembler.transform(labelTransformed).select("features",
      "classIndex")

    val Array(train, eval1, eval2, test) = xgbInput.randomSplit(Array(0.6, 0.2, 0.1, 0.1))

    val xgbParam = Map("tracker_conf" -> TrackerConf(60*60, "scala"),
      "eval_sets" -> Map("eval1" -> eval1, "eval2" -> eval2))

    val xgbClassifier = new XGBClassifier(xgbParam)
    xgbClassifier.setFeaturesCol("features")
    xgbClassifier.setLabelCol("classIndex")
    xgbClassifier.setNumClass(3)
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
