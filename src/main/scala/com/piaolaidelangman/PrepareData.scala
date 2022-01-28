package xgboostsparksgx

import ml.dmlc.xgboost4j.scala.spark.TrackerConf

// import org.apache.spark.ml.feature.{StringIndexer, VectorAssembler}
import org.apache.spark.sql.{SparkSession, Row}
// import org.apache.spark.SparkContext
// import org.apache.spark.sql.types.{IntegerType, DoubleType, StringType, StructField, StructType, BinaryType, ArrayType, FloatType, LongType, ByteType, DataTypes}
// import org.apache.spark.sql.functions.{col, udf}


object PrepareData {

  def main(args: Array[String]): Unit = {
    if (args.length < 4) {
      println("Usage: program input_path num_threads num_round modelsave_path")
      sys.exit(1)
    }
    val spark = SparkSession.builder().getOrCreate()
    import spark.implicits._

    val task = new Task()

    val input_path = args(0) // path to iris.data
    val output_path = args(1) // save to this path

    var df = spark.read.option("header", "false").option("inferSchema", "true").option("delimiter", "\t").csv(input_path)
    df.show()

    df.rdd.map(task.rowToLibsvm).saveAsTextFile(output_path)
    
    spark.stop()
  }
}
