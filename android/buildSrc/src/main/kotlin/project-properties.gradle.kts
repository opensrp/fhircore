import org.gradle.kotlin.dsl.extra
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

// Set required FHIR core properties
val requiredFhirProperties =
  listOf(
    "URL",
    "FHIR_BASE_URL",
    "OAUTH_BASE_URL",
    "OAUTH_CLIENT_ID",
    "OAUTH_SCOPE",
    "MAPBOX_SDK_TOKEN",
    "SENTRY_DSN",
    "OPENSRP_APP_ID"
  )

val localProperties = readProperties((project.properties["localPropertiesFile"] ?: "${rootProject.projectDir}/local.properties").toString())

requiredFhirProperties.forEach { property ->
  project.extra.set(property, localProperties.getProperty(property, when {
    property.contains("URL") -> "https://fhir.aicoe.triveous.tech/fhir/"
    property.equals("OPENSRP_APP_ID") -> "app"
    else -> "sample_" + property
  }
  ))
}

// Set required keystore properties
val requiredKeystoreProperties = listOf("KEYSTORE_ALIAS", "KEY_PASSWORD", "KEYSTORE_PASSWORD")
val keystoreProperties = readProperties((project.properties["keystorePropertiesFile"] ?: "${rootProject.projectDir}/keystore.properties").toString())

requiredKeystoreProperties.forEach { property ->
  project.extra.set(property, keystoreProperties.getProperty(property,"sample_" + property))
}
