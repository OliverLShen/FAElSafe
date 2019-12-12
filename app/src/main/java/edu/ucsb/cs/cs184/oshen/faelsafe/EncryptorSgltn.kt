package edu.ucsb.cs.cs184.oshen.faelsafe

import android.content.Context
import android.util.Log
import java.io.File
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.nio.file.Files
import java.nio.file.Paths
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import java.util.Base64.getEncoder
import java.util.Base64.getDecoder





object EncryptorSgltn {

    private var secretKey: SecretKeySpec? = null
    private var key: ByteArray? = null
    private var fildes: File? = null
    private val keystr = "k.txt"
    private var encdir: File? = null
    private var pathdir: File? = null

    fun init(context: Context) {  //Call this function before using any method
        fildes = context.filesDir
        encdir = File(fildes.toString(), "enc")
        pathdir = File(fildes.toString(), "path")
        val keyfilepath = Paths.get(fildes?.toString(), keystr)
        if (Files.exists(keyfilepath)) { //read from internal storage file to get key
            try {
                val reader = Files.newBufferedReader(keyfilepath)
                setKey(reader.readLine())
                reader.close()
            }
            catch (x : IOException) {
                System.err.format("IOException: %s%n", x);
            }
        }
        else { //create file in internal storage with key, initialize directories
            val keyfile = File(keyfilepath.toString())
            try {
                val id = UUID.randomUUID().toString()
                keyfile.createNewFile()
                val writer = Files.newBufferedWriter(keyfilepath)
                writer.write(id, 0, id.length)
                writer.flush()
                writer.close()
                encdir?.mkdir()
                pathdir?.mkdir()
                setKey(id)
            }
            catch (e: java.lang.Exception) {
                System.err.format("File failed to create");
            }
        }
    }

    fun setKey(myKey: String) {
        var sha: MessageDigest? = null
        try {
            key = myKey.toByteArray(charset("UTF-8"))
            sha = MessageDigest.getInstance("SHA-1")
            key = sha!!.digest(key!!)
            key = Arrays.copyOf(key!!, 16)
            secretKey = SecretKeySpec(key!!, "AES")
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }

    }

    fun encrypt(strToEncrypt: String): String? { //mainly helper for encryptFile
        try {
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            return Base64.getEncoder()
                .encodeToString(cipher.doFinal(strToEncrypt.toByteArray(charset("UTF-8"))))
        } catch (e: Exception) {
            println("Error while encrypting: $e")
        }

        return null
    }

    fun decrypt(strToDecrypt: String): String? { //mainly helper for decryptFile
        try {
            val cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING")
            cipher.init(Cipher.DECRYPT_MODE, secretKey)
            return String(cipher.doFinal(Base64.getDecoder().decode(strToDecrypt)))
        } catch (e: Exception) {
            println("Error while decrypting: $e")
        }

        return null
    }

    fun encryptFile(filepath: String): Boolean {
        try {
            Log.d("DEBUG", filepath)
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            //encode filename
            val split = filepath.split('/')
            val filename = split[split.size - 1]
            //store encoded filepath into path directory
            val pathfile = File(pathdir.toString(), filename)
            Log.d("DEBUG", pathfile.toString())
            pathfile.createNewFile()
            Log.d("DEBUG", "Pathfile created")
            Files.write(Paths.get(pathfile.toString()), encrypt(filepath)!!.toByteArray(charset("UTF-8")))
            Log.d("DEBUG", "Encrypted path stored in pathfile")
            //store encoded file into enc directory
            val encfile = File(encdir.toString(), filename)
            encfile.createNewFile()
            Log.d("DEBUG", "Encfile created")
            Files.write(Paths.get(encfile.toString()), Base64.getEncoder().encode(cipher.doFinal(Files.readAllBytes(Paths.get(filepath)))))
            Log.d("DEBUG", "File encrypted")

            return true
        } catch (e: Exception) {
            println("Error while encrypting: $e")
        }

        return false
    }

    fun decryptFile(encname: String): Boolean {
        try {
            val cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING")
            cipher.init(Cipher.DECRYPT_MODE, secretKey)

            val pathfile = File(pathdir.toString(), encname)
            val encfile = File(encdir.toString(), encname)

            val restoredfile = File(decrypt(String(Files.readAllBytes(Paths.get(pathfile.toString())))))
            restoredfile.createNewFile()
            Files.write(Paths.get(restoredfile.toString()), cipher.doFinal(Base64.getDecoder().decode(Files.readAllBytes(Paths.get(encfile.toString())))))

            pathfile.delete()
            encfile.delete()
        } catch (e: Exception) {
            println("Error while decrypting: $e")
        }

        return false
    }
}
