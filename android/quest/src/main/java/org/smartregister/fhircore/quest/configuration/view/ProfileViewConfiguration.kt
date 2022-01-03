package org.smartregister.fhircore.quest.configuration.view

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import org.smartregister.fhircore.engine.configuration.Configuration

@Stable
@Serializable
data class ProfileViewConfiguration(
  override val appId: String,
  override val classification: String,
  val g6pdStatus: Boolean = false,
  val contentTitle: String = "Responses",
  val contentItemTitle: Boolean = true,
  val contentItemSubTitle: Boolean = true,
) : Configuration

@Stable
fun profileViewConfigurationOf(
  appId: String = "quest",
  classification: String = "patient_details",
  g6pdStatus: Boolean = false,
  contentTitle: String = "Responses",
  contentItemTitle: Boolean = true,
  contentItemSubTitle: Boolean = true,
) =
  ProfileViewConfiguration(
    appId = appId,
    classification = classification,
    g6pdStatus = g6pdStatus,
    contentTitle = contentTitle,
    contentItemTitle = contentItemTitle,
    contentItemSubTitle = contentItemSubTitle,
  )
