package xgboostsparksgx

import org.apache.spark.sql.Row

import java.util.Base64
import java.util.Arrays.copyOfRange
import java.time.Instant
import java.io.{ByteArrayOutputStream, DataOutputStream, ByteArrayInputStream, DataInputStream}
import java.security.SecureRandom
import javax.crypto.{Cipher, SecretKeyFactory, Mac}
import javax.crypto.spec.{GCMParameterSpec, IvParameterSpec, PBEKeySpec, SecretKeySpec}

class Task extends Serializable{
  def rowToLibsvm(row: Row): String = {
    0 until row.length flatMap {
      case 0 => Some(row(0).toString)
      case i if row(i) == null => None
      case i => Some( (if (i < 14) row(i) else java.lang.Long.parseLong(row(i).toString, 16)).toString )
    } mkString " "
  }
  def encryptBytesWithJavaAESCBC(content: Array[Byte], secret: Array[Byte]): Array[Byte] = {
    val encoder = Base64.getUrlEncoder()

    //  get IV
    val random = new SecureRandom()
    val initializationVector: Array[Byte] = new Array[Byte](16)
    random.nextBytes(initializationVector)
    val ivParameterSpec: IvParameterSpec = new IvParameterSpec(initializationVector)

    // key encrypt
    val signingKey: Array[Byte] = copyOfRange(secret, 0, 16)
    val encryptKey: Array[Byte] = copyOfRange(secret, 16, 32)
    val encryptionKeySpec: SecretKeySpec = new SecretKeySpec(encryptKey, "AES")

    val cipher: Cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    cipher.init(Cipher.ENCRYPT_MODE, encryptionKeySpec, ivParameterSpec)

    val cipherText: Array[Byte] = cipher.doFinal(content)
    val timestamp: Instant = Instant.now()

    // sign
    val byteStream: ByteArrayOutputStream = new ByteArrayOutputStream(25 + cipherText.length)
    val dataStream: DataOutputStream = new DataOutputStream(byteStream)

    val version: Byte = (0x80).toByte
    dataStream.writeByte(version)
    dataStream.writeLong(timestamp.getEpochSecond())
    dataStream.write(ivParameterSpec.getIV())
    dataStream.write(cipherText)

    val mac: Mac = Mac.getInstance("HmacSHA256")
    val signingKeySpec = new SecretKeySpec(signingKey, "HmacSHA256")
    mac.init(signingKeySpec)
    val hmac: Array[Byte] = mac.doFinal(byteStream.toByteArray())

    // to bytes
    val outByteStream: ByteArrayOutputStream = new ByteArrayOutputStream(57 + cipherText.length)
    val dataOutStream: DataOutputStream = new DataOutputStream(outByteStream)
    dataOutStream.writeByte(version)
    dataOutStream.writeLong(timestamp.getEpochSecond())
    dataOutStream.write(ivParameterSpec.getIV())
    dataOutStream.write(cipherText)
    dataOutStream.write(hmac)

    // if (timestamp == null) {
    //     throw new CryptoException("Timestamp cannot be null")
    // }
    // if (ivParameterSpec == null || ivParameterSpec.getIV().length != 16) {
    //     throw new CryptoException("Initialization Vector must be 128 bits")
    // }
    // if (cipherText == null || cipherText.length % 16 != 0) {
    //     throw new CryptoException("Ciphertext must be a multiple of 128 bits")
    // }
    // if (hmac == null || hmac.length != 32) {
    //     throw new CryptoException("Hmac must be 256 bits")
    // }

    val resultString = new String(encoder.encodeToString(outByteStream.toByteArray()))
    resultString.getBytes
  }

  def decryptBytesWithJavaAESCBC(content: Array[Byte], secret: Array[Byte]): String = {
    val decoder = Base64.getUrlDecoder()
    val bytes = decoder.decode(new String(content))

    val inputStream: ByteArrayInputStream = new ByteArrayInputStream(bytes)
    val dataStream: DataInputStream = new DataInputStream(inputStream)

    val version: Byte = dataStream.readByte()
    if(version.compare((0x80).toByte) != 0){
      throw new CryptoException("Version error!")
    }
    val encryptKey: Array[Byte] = copyOfRange(secret, 16, 32)

    val timestampSeconds: Long = dataStream.readLong()

    val initializationVector: Array[Byte] = read(dataStream, 16)
    val ivParameterSpec = new IvParameterSpec(initializationVector)

    val cipherText: Array[Byte] = read(dataStream, bytes.length - 57)

    val hmac: Array[Byte] = read(dataStream, 32)
    if(initializationVector.length != 16){
      throw new CryptoException("Initialization Vector must be 128 bits")
    }
    if (cipherText == null || cipherText.length % 16 != 0) {
        throw new CryptoException("Ciphertext must be a multiple of 128 bits")
    }
    if (hmac == null || hmac.length != 32) {
        throw new CryptoException("hmac must be 256 bits")
    }

    val secretKeySpec = new SecretKeySpec(encryptKey, "AES")
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec)

    new String(cipher.doFinal(cipherText))
  }

  private def read(stream: DataInputStream, numBytes: Int): Array[Byte]={
    val retval = new Array[Byte](numBytes)
    val bytesRead: Int = stream.read(retval)
    if (bytesRead < numBytes) {
      throw new CryptoException("Not enough bits to read!")
    }
    retval
  }
}
