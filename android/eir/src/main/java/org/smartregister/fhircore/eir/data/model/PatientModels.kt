package org.smartregister.fhircore.eir.data.model

import androidx.compose.runtime.Stable

@Stable
data class PatientItem(
  val patientIdentifier: String = "",
  val name: String = "",
  val gender: String = "",
  val age: String = "",
  val demographics: String = "",
  val lastSeen: String = "",
  val vaccineStatus: PatientVaccineStatus = PatientVaccineStatus(VaccineStatus.NOT_VACCINATED, ""),
  val atRisk: String = ""
)

enum class VaccineStatus {
  VACCINATED,
  PARTIAL,
  OVERDUE,
  DUE,
  NOT_VACCINATED
}

data class PatientVaccineStatus(val status: VaccineStatus, val date: String)
