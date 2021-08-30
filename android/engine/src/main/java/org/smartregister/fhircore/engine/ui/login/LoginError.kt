package org.smartregister.fhircore.engine.ui.login

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginError(
  val error: String = "",
  @SerialName("error_description") val errorDescription: String = "Error logging in"
)
