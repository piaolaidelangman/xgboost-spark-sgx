package xgboostsparksgx

import org.apache.spark.SparkContext
import java.nio.file.{Files, Paths}
/**
 * @author diankun.an
 */
object SparkEncryptFiles {
    def main(args: Array[String]): Unit = {

        val inputPath = args(0) // path to a txt which contains files' pwd to be encrypted
        val outputPath = args(1)
        val secret = args(2)

        val sc = new SparkContext()
        val task = new Task()

        if(Files.exists(Paths.get(outputPath)) == false){
          Files.createDirectory(Paths.get(outputPath))
        }

        val rdd = sc.binaryFiles(inputPath)
        .map{ case (name, bytesData) => {
            val tmpOutputPath = Paths.get(outputPath, name.split("/").last)
            Files.write(tmpOutputPath, task.encryptBytesWithJavaAESCBC(bytesData.toArray, secret))
            println(tmpOutputPath.toString + " AES-CBC encrypt successfully saved!")
        }}

        sc.stop()
    }
}


