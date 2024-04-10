import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.util.Properties

object ProjectProperties {
    fun readProperties(file: String): Properties {
        val properties = Properties()
        val localProperties = File(file)
        if (localProperties.isFile) {
            InputStreamReader(FileInputStream(localProperties), Charsets.UTF_8).use { reader
                ->
                properties.load(reader)
            }
        } else  println("\u001B[34mFILE_NOT_FOUND_EXCEPTION: File $file not found\u001B[0m")

        return properties
    }
}