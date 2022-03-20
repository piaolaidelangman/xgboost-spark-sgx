package xgboostsparksgx

import scala.io.Source

import java.nio.file.{Files, Paths}
import java.util.concurrent.locks.{Lock, ReentrantLock}
/**
 * @author diankun.an
 */
object SplitAndEncryptForXgboost {

  def main(args: Array[String]): Unit = {
    if (args.length < 4) {
      println("Usage: program inputFile secret outputPath splitNum")
      sys.exit(1)
    }

    val task = new Task()

    val inputFile = args(0) // path to iris.data
    val secret = args(1)
    val outputPath = args(2) // encrypted files output path
    val splitNum = args(3).toInt // the num of split

    val encryptTask = new Task()

    val filePrefix: String = inputFile.split("/").last.split("\\.")(0)
    if(Files.exists(Paths.get(outputPath, filePrefix)) == false){
      Files.createDirectory(Paths.get(outputPath, filePrefix))
    }

    val stream = Files.lines(Paths.get(inputFile))
    val numLines: Long = stream.count  // Number of file's lines

    val source = Source.fromFile(inputFile)
    val content = source.getLines

    val linesPerFile: Long = numLines / splitNum  // lines per split file

    var splitArray = new Array[Long](splitNum) // split-point
    for(i <- 0 to splitNum-1) {
      splitArray(i) = linesPerFile
    }
    splitArray(splitNum-1) += numLines % linesPerFile // for last part

    var currentSplitNum: Int = 0
    val rtl: Lock = new ReentrantLock()
    val begin = System.currentTimeMillis
    splitArray.par.map{
      num => {
        var splitContentString = ""
        var splitFileName = ""
        rtl.lock()
        try { // get split content
            splitContentString = content.take(num.toInt).map(_.split("\t")).map{
                row=>{
                    0 until row.length flatMap {
                    case 0 => Some(row(0).toString)
                    case i if row(i) == "" => Some("-999")
                    case i => Some( (if (i < 14) row(i) else java.lang.Long.parseLong(row(i).toString, 16)).toString )
                    } mkString ","
                }
            }.mkString("\n")
            splitFileName = "split_" + currentSplitNum.toString + ".csv"
            currentSplitNum += 1
        } finally {
            rtl.unlock()
        }
        Files.write(Paths.get(outputPath, filePrefix, splitFileName), encryptTask.encryptBytesWithJavaAESCBC((splitContentString + "\n").getBytes, secret))
        println("Successfully encrypted " + splitFileName)
      }
    }
    val end = System.currentTimeMillis
    val cost = (end - begin)
    println(s"Encrypt time elapsed $cost ms.")
  }
}
