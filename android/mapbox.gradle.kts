import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.InputStreamReader
import java.util.Properties

fun readProperties(file: String): Properties {
    val properties = Properties()
    val localProperties = File(file)
    if (localProperties.isFile) {
        InputStreamReader(FileInputStream(localProperties), Charsets.UTF_8).use { reader
            ->
            properties.load(reader)
        }
    }
    else  throw FileNotFoundException("\u001B[34mFile $file not found\u001B[0m")

    return properties
}

val mapboxSdkToken = System.getenv("MAPBOX_SDK_TOKEN") ?: readProperties((project.properties["localPropertiesFile"] ?: "${rootProject.projectDir}/local.properties").toString()).getProperty("MAPBOX_SDK_TOKEN")

allprojects {
    repositories {

        google()
        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            credentials.username = "mapbox"
            credentials.password = mapboxSdkToken
            authentication.create<BasicAuthentication>("basic")
        }
    }
}