/*
 * Copyright 2021 Ona Systems, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.smartregister.fhircore.engine.configuration

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import org.hl7.fhir.r4.model.Binary
import org.hl7.fhir.r4.model.Parameters
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.app.AppConfigClassification
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.decodeJson
import org.smartregister.fhircore.engine.util.extension.decodeResourceFromString
import org.smartregister.fhircore.engine.util.extension.extractId
import org.smartregister.fhircore.engine.util.extension.showToast
import timber.log.Timber

/**
 * A configuration store used to store all the application configurations. Application
 * configurations are to be downloaded and synced from the server. This registry provides a map with
 * different [Configuration] implementations. The ensures that all the application configurations
 * are accessible from one place. If no configurations are retrieved from the server, then the
 * defaults are used.
 */
@Singleton
class ConfigurationRegistry
@Inject
constructor(
  @ApplicationContext val context: Context,
  val sharedPreferencesHelper: SharedPreferencesHelper,
  val repository: DefaultRepository
) {

  val configurationsMap = mutableMapOf<String, Configuration>()

  val workflowPointsMap = mutableMapOf<String, WorkflowPoint>()

  lateinit var appId: String

  /**
   * Retrieve configuration for the provided [ConfigClassification]. Populate the map when the
   * config is loaded for the first time. File name containing configs MUST start with the workflow
   * resource in snake_case
   *
   * E.g. for a workflow resource RegisterViewConfiguration, the name of the file containing configs
   * becomes register_view_configurations.json
   */
  inline fun <reified T : Configuration> retrieveConfiguration(
    configClassification: ConfigClassification,
    jsonSerializer: Json? = null
  ): T =
    workflowPointName(configClassification.classification).let { workflowName ->
      val workflowPoint = workflowPointsMap[workflowName]
      if (workflowPoint == null) {
        Timber.w("No configuration found for $workflowName. Initializing default instance")
        return T::class.java.newInstance()
      }
      configurationsMap.getOrPut(workflowName) {
        // Binary content could be either a Configuration or a FHIR Resource
        (workflowPoint.resource as Binary).content.decodeToString().let {
          if (T::class.java.isAssignableFrom(FhirConfiguration::class.java))
            FhirConfiguration(appId, workflowPoint.classification, it.decodeResourceFromString())
          else it.decodeJson<T>(jsonSerializer)
        }
      } as
        T
    }

  private fun retrieveConfiguration(id: String, classification: ConfigClassification) =
    retrieveConfiguration<FhirConfiguration<Parameters>>(classification)
      .resource
      .parameter
      .firstOrNull { it.name.contentEquals(id, ignoreCase = true) }
      .also {
        if (it == null)
          with(context.getString(R.string.health_module_filters_not_configured)) {
            Timber.e(this)
            context.showToast(this)
          }
      }

  fun retrieveRegisterDataFilterConfiguration(id: String) =
    retrieveConfiguration(id, AppConfigClassification.REGISTER_DATA_FILTERS)

  fun retrieveRegisterDataMapperConfiguration(id: String) =
    retrieveConfiguration(id, AppConfigClassification.REGISTER_DATA_MAPPERS)

  fun retrieveProfileDataFilterConfiguration(id: String) =
    retrieveConfiguration(id, AppConfigClassification.PROFILE_DATA_FILTERS)

  fun retrieveProfileDataMapperConfiguration(id: String) =
    retrieveConfiguration(id, AppConfigClassification.PROFILE_DATA_MAPPERS)

  suspend fun loadConfigurations(appId: String, configsLoadedCallback: (Boolean) -> Unit) {
    this.appId = appId

    // appId is identifier of Composition
    repository
      .searchCompositionByIdentifier(appId)
      .also { if (it == null) configsLoadedCallback(false) }
      ?.section
      ?.forEach {
        // each section in composition represents workflow
        // { "title": "register configuration",
        //   "mode": "working",
        //   "focus": { "reference": "Binary/11111", "identifier: { "value": "registration" } }
        // }

        // A workflow point would be mapped like
        //   "workflowPoint": "registration",
        //   "resource": "RegisterViewConfiguration",
        //   "classification": "patient_register",
        //   "description": "register configuration"

        val workflowPointName = workflowPointName(it.focus.identifier.value)
        val workflowPoint =
          WorkflowPoint(
            classification = it.focus.identifier.value,
            description = it.title,
            resource = repository.getBinary(it.focus.extractId()),
            workflowPoint = it.focus.identifier.value
          )
        workflowPointsMap[workflowPointName] = workflowPoint
      }
      ?.also { configsLoadedCallback(true) }
  }

  fun workflowPointName(key: String) = "$appId|$key"

  fun isAppIdInitialized() = this::appId.isInitialized

  companion object {
    const val ORGANIZATION = "organization"
    const val PUBLISHER = "publisher"
    const val ID = "_id"
    const val COUNT = "count"
    const val DEFAULT_COUNT = "100"
  }
}
