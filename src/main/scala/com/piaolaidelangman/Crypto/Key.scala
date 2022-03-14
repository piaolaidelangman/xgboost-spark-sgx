package xgboostsparksgx

import java.security.SecureRandom
import java.util.Base64.getEncoder

/**
 * @author diankun.an
*/

class Key extends Serializable{

    def generateKey(): String = {
        val random = new SecureRandom()
        val key: Array[Byte] = new Array[Byte](32)
        random.nextBytes(key)
        getEncoder().encodeToString(key)
    }

}

object GenerateKey{

    def main(args: Array[String]): Unit = {
        val key: Key = new Key()
        println("Successfully generate key:")
        println(key.generateKey())
    }
}
