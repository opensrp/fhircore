package org.smartregister.fhircore.auth.account

data class Credentials(
    val username: String, val password: CharArray
)
