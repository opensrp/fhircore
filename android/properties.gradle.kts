import java.util.Properties

fun Project.readProperties(file: String): Properties {
  val properties = Properties()
  val localProperties = File(file)
  if (localProperties.isFile) {
    java.io.InputStreamReader(java.io.FileInputStream(localProperties), Charsets.UTF_8).use { reader
      ->
      properties.load(reader)
    }
  } else println("FILE_NOT_FOUND_EXEPTION: File $file not found")

  return properties
}

// Set required FHIR core properties
val requiredFhirProperties =
  listOf(
    "FHIR_BASE_URL",
    "OAUTH_BASE_URL",
    "OAUTH_CIENT_ID",
    "OAUTH_CLIENT_SECRET",
    "OAUTH_SCOPE",
    "MAPBOX_SDK_TOKEN"
  )

val localProperties = readProperties("local.properties")

requiredFhirProperties.forEach { property ->
  project.extra.set(property, localProperties.getProperty(property))
}

// Set required keystore properties
val requiredKeystoreProperties = listOf("KEYSTORE_ALIAS", "KEY_PASSWORD", "KEYSTORE_PASSWORD")
val keystoreProperties = readProperties("keystore.properties")

requiredKeystoreProperties.forEach { property ->
  project.extra.set(property, keystoreProperties.getProperty(property))
}
