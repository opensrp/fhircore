package org.smartregister.fhircore.quest.configuration.view

import androidx.compose.runtime.Stable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.smartregister.fhircore.engine.configuration.Configuration

@Serializable
@SerialName("test_details")
data class TestDetailsNavigationAction(val form: String? = null, val readOnly: Boolean? = null) :
  NavigationAction()

@Stable
@Serializable
data class ResultDetailsNavigationConfiguration(
  override val appId: String,
  override val classification: String,
  val navigationOptions: List<NavigationOption>
) : Configuration
