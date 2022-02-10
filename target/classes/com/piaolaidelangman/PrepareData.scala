package xgboostsparksgx

import org.apache.spark.sql.{SparkSession, Row}


object PrepareData {

  def main(args: Array[String]): Unit = {
    if (args.length < 2) {
      println("Usage: program input_path output_path")
      sys.exit(1)
    }
    val spark = SparkSession.builder().getOrCreate()
    import spark.implicits._

    val task = new Task()

    val input_path = args(0) // path to iris.data
    val output_path = args(1) // save to this path
    val num_repartions = args(2).toInt

    var df = spark.read.option("header", "false").option("inferSchema", "true").option("delimiter", "\t").csv(input_path).repartition(num_repartions)

    // df.rdd.map(task.rowToLibsvm).saveAsTextFile(output_path)
    // df.coalesce(num_repartions).map(task.rowToLibsvm).write.mode("overwrite").option("header",false).csv(output_path)
    df.map(task.rowToLibsvm).write.mode("overwrite").option("header",false).csv(output_path)
    // df.map(task.rowToLibsvm).write.mode("overwrite").option("header",false).option("maxRecordsPerFile", num_repartions).csv(output_path)

    spark.stop()
  }
}
