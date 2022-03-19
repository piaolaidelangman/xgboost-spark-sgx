package xgboostsparksgx

import org.apache.spark.SparkContext
import org.apache.spark.sql.{SparkSession, Row}
import org.apache.spark.sql.types.{StructField, StructType, StringType}
/**
 * @author diankun.an
 */
object SparkDecryptFiles {

  def main(args: Array[String]): Unit = {

    val sc = new SparkContext()
    val spark = SparkSession.builder().getOrCreate()
    import spark.implicits._

    val task = new Task()

    val input_path = args(0) // path to iris.data
    val secret = args(1)

    var decryption = sc.binaryFiles(input_path)
      .map{ case (name, bytesData) => {
        task.decryptBytesWithJavaAESCBC(bytesData.toArray, secret)
      }}
    val decryptionRDD = decryption.flatMap(_.split("\n"))
    val columns = decryptionRDD.first.split(",").length
    
    // RDD to DataFrame
    var structFieldArray = new Array[StructField](columns)
    for(i <- 0 to columns-1){
      structFieldArray(i) = StructField("_c" + i.toString, StringType, true)
    }
    var schema =  new StructType(structFieldArray)


    val rowRDD = decryptionRDD.map(_.split(",")).map(row => Row.fromSeq(
      for{
        i <- 0 to columns-1
      } yield {
        row(i).toString
      }
    ))

    val df = spark.createDataFrame(rowRDD,schema)

    df.printSchema()
    df.show()

    sc.stop()
    spark.stop()
  }
}
