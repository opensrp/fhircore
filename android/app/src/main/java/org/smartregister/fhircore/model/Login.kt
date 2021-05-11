package org.smartregister.fhircore.model

data class LoginUser (
    var username: String = "",
    var password: String = "",
    var rememberMe: Boolean = false,
    var forgotPassword: Boolean? = null,
) {
}
