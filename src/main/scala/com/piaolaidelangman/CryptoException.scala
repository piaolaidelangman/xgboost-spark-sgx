package xgboostsparksgx

import java.lang.RuntimeException

/**
 * @author diankun.an
 */

class CryptoException (message: String, cause: Throwable) 
  extends RuntimeException(message) {
    if (cause != null)
      initCause(cause)

    def this(message: String) = this(message, null)  
}