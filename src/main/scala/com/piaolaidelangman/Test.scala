package xgboostsparksgx

import ml.dmlc.xgboost4j.scala.spark.TrackerConf

import org.apache.spark.SparkContext
import org.apache.spark.ml.feature.{StringIndexer, VectorAssembler}
import org.apache.spark.sql.{SparkSession, Row}
import org.apache.spark.sql.types.{IntegerType, StructField, StructType, LongType}
/**
 * @author diankun.an
 */
object Test {

  def main(args: Array[String]): Unit = {

    val spark = SparkSession.builder().getOrCreate()
    import spark.implicits._


    val input_path = args(0) // path to iris.data
    val df = spark.read.format("csv").option("header",true).load(input_path)
    df.show()
    df.printSchema()

    spark.stop()
  }
}
