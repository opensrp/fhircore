package org.smartregister.fhircore.model

data class LoginUser (
    var username: String = "",
    var password: CharArray = charArrayOf(),
    var rememberMe: Boolean = false,
    var forgotPassword: Boolean = false,
)
