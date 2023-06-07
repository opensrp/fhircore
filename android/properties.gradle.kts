import java.util.Properties
import java.io.FileInputStream
import java.io.InputStreamReader

fun Project.readProperties(file: String): Properties {
  val properties = Properties()
  val localProperties = File(file)
  if (localProperties.isFile) {
    InputStreamReader(FileInputStream(localProperties), Charsets.UTF_8).use { reader
      ->
      properties.load(reader)
    }
  } else println("FILE_NOT_FOUND_EXCEPTION: File $file not found")

  return properties
}

// Set required FHIR core properties
val requiredFhirProperties =
  listOf(
    "URL",
    "FHIR_BASE_URL",
    "OAUTH_BASE_URL",
    "OAUTH_CIENT_ID",
    "OAUTH_CLIENT_SECRET",
    "OAUTH_SCOPE",
    "MAPBOX_SDK_TOKEN",
    "SENTRY_DSN"
  )

val localProperties = readProperties("local.properties")

requiredFhirProperties.forEach { property ->
  project.extra.set(property, localProperties.getProperty(property)?: (if(property == "URL") "https://sample.url/fhir/" else "sample_" + property))
}

// Set required keystore properties
val requiredKeystoreProperties = listOf("KEYSTORE_ALIAS", "KEY_PASSWORD", "KEYSTORE_PASSWORD")
val keystoreProperties = readProperties("keystore.properties")

requiredKeystoreProperties.forEach { property ->
  project.extra.set(property, keystoreProperties.getProperty(property)?: "sample_" + property)
}
