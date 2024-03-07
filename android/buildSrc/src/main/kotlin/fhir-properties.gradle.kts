import org.gradle.api.Project
import org.gradle.kotlin.dsl.extra
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.InputStreamReader
import java.util.Properties

val fhirAuthArray = arrayOf(
    "FHIR_BASE_URL", "OAUTH_BASE_URL", "OAUTH_CIENT_ID", "OAUTH_CLIENT_SECRET", "OAUTH_SCOPE"
)
//KEYSTORE CREDENTIALS
val keystoreAuthArray = arrayOf(
    "KEYSTORE_ALIAS", "KEY_PASSWORD", "KEYSTORE_PASSWORD"
)

val localProperties = readProperties((properties["localPropertiesFile"] ?: "${rootProject.projectDir}/local.properties").toString())

fhirAuthArray.forEach { property ->
    extra.set(property, localProperties.getProperty(property, when {
        property.contains("URL") -> "https://sample.url/fhir/"
        else -> "sample_" + property
    }
    ))
}


val keystoreProperties = readProperties((properties["keystorePropertiesFile"] ?: "${rootProject.projectDir}/keystore.properties").toString())

keystoreAuthArray.forEach { property ->
    extra.set(property, keystoreProperties.getProperty(property, "sample_" + property))
}

fun Project.readProperties(file: String): Properties {
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
