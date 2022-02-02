package xgboostsparksgx

import org.apache.spark.SparkContext

import java.util.Base64
// import java.nio.file.{Files, Paths}

/**
 * @author diankun.an
 */
object SparkEncryptFiles {
    def main(args: Array[String]): Unit = {

        val inputPath = args(0) // path to a txt which contains files' pwd to be encrypted
        val outputPath = args(1)
        val secret = args(2)

        val decoder = Base64.getDecoder()
        val encoder = Base64.getEncoder()
        val key = decoder.decode(decoder.decode(encoder.encodeToString(secret.getBytes)))

        val sc = new SparkContext()
        val task = new Task()

        if(Files.exists(Paths.get(outputPath)) == false){
          Files.createDirectory(Paths.get(outputPath))
        }

        val rdd = sc.binaryFiles(inputPath)
        .map{ case (name, bytesData) => {
            val tmpOutputPath = Paths.get(outputPath, name.split("/").last)
            Files.write(tmpOutputPath, task.encryptBytesWithJavaAESCBC(bytesData.toArray, key))
            tmpOutputPath.toString + " AES-CBC encrypt successfully saved!"
            // new String(task.encryptBytesWithJavaAESCBC(bytesData.toArray, key))
        }}
        rdd.foreach(println)

        sc.stop()
    }
}


