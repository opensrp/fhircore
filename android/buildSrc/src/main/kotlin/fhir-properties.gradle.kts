val fhirAuthArray = arrayOf(
    "FHIR_BASE_URL", "OAUTH_BASE_URL", "OAUTH_CIENT_ID", "OAUTH_CLIENT_SECRET", "OAUTH_SCOPE", "APP_ID"
)
//KEYSTORE CREDENTIALS
val keystoreAuthArray = arrayOf(
    "KEYSTORE_ALIAS", "KEY_PASSWORD", "KEYSTORE_PASSWORD"
)

val localProperties = ProjectProperties.readProperties((properties["localPropertiesFile"] ?: "${rootProject.projectDir}/local.properties").toString())

fhirAuthArray.forEach { property ->
    extra.set(property, localProperties.getProperty(property, when {
        property.contains("URL") -> "https://sample.url/fhir/"
        else -> "sample_$property"
    }
    ))
}

val keystoreProperties = ProjectProperties.readProperties((properties["keystorePropertiesFile"] ?: "${rootProject.projectDir}/keystore.properties").toString())

keystoreAuthArray.forEach { property ->
    extra.set(property, keystoreProperties.getProperty(property, "sample_$property"))
}
