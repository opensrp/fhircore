package org.smartregister.fhircore.auth.secure

data class Credentials(val username: String, val password: CharArray, val sessionToken: String)
