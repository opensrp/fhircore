package org.smartregister.fhircore.quest.configuration.view

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import org.smartregister.fhircore.engine.configuration.Configuration

@Stable
@Serializable
data class PatientDetailsViewConfiguration(
  override val appId: String,
  override val classification: String,
  val clientInfo: String = "Patient name, F, 30",
  val contentTitle: String = "Responses",
  val filters: List<Filter>? = null
) : Configuration

@Stable
fun patientDetailsViewConfigurationOf(
  appId: String = "quest",
  classification: String = "patient_details",
  clientInfo: String = "Patient name, F, 30",
  contentTitle: String = "Responses"
) =
  PatientDetailsViewConfiguration(
    appId = appId,
    classification = classification,
    clientInfo = clientInfo,
    contentTitle = contentTitle
  )
