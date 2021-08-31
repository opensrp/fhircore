package org.smartregister.fhircore.engine.data.local.repository.patient.model

import androidx.compose.runtime.Stable

@Stable
data class AncItem(
  val patientIdentifier: String = "",
  val name: String = "",
  val gender: String = "",
  val age: String = "",
  val demographics: String = "",
  val atRisk: String = ""
)