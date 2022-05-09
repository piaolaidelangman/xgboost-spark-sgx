package xgboostsparksgx

import org.slf4j.LoggerFactory

trait Supportive {
  def timing[T](name: String)(f: => T): T = {
    val begin = System.currentTimeMillis
    val result = f
    val end = System.currentTimeMillis
    val cost = (end - begin)
    Supportive.logger.info(s"SUCCESS SUCCESS $name time elapsed $cost ms.")
    result
  }
}

object Supportive {
  val logger = LoggerFactory.getLogger(getClass)
}