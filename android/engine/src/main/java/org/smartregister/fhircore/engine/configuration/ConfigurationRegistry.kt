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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.hl7.fhir.r4.model.Binary
import org.hl7.fhir.r4.model.Composition
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.app.AppConfigClassification
import org.smartregister.fhircore.engine.configuration.view.DataFiltersConfiguration
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.decodeJson
import org.smartregister.fhircore.engine.util.extension.decodeResourceFromString
import org.smartregister.fhircore.engine.util.extension.extractId
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
  val fhirResourceDataSource: FhirResourceDataSource,
  val sharedPreferencesHelper: SharedPreferencesHelper,
  val dispatcherProvider: DispatcherProvider,
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

  fun retrieveDataFilterConfiguration(id: String) =
    retrieveConfiguration<DataFiltersConfiguration>(AppConfigClassification.DATA_FILTERS)
      .filters
      .filter { it.id.contentEquals(id, ignoreCase = true) }

  suspend fun loadConfigurations(appId: String, configsLoadedCallback: (Boolean) -> Unit) {
    this.appId = appId

    // appId is identifier of Composition
    repository
      .searchCompositionByIdentifier(appId)
      .also { if (it == null) configsLoadedCallback(false) }
      ?.section
      ?.filter {
        it.focus.reference.split("/")[0].let { resourceType ->
          resourceType == ResourceType.Parameters.name || resourceType == ResourceType.Binary.name
        }
      }
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

  suspend fun loadWorkflowConfigurationsLocally(
    appId: String,
    configsLoadedCallback: (Boolean) -> Unit
  ) {
    val parsedAppId = appId.substringBefore("/$DEBUG_SUFFIX")
    this.appId = parsedAppId

    val baseConfigPath = BASE_CONFIG_PATH.run { replace(DEFAULT_APP_ID, parsedAppId) }

    runCatching {
      context
        .assets
        .open(baseConfigPath.plus(COMPOSITION_CONFIG_PATH))
        .bufferedReader()
        .use { it.readText() }
        .decodeResourceFromString<Composition>()
        .section
        .filter {
          it.focus.reference.split("/")[0].let { resourceType ->
            resourceType == ResourceType.Parameters.name || resourceType == ResourceType.Binary.name
          }
        }
        .forEach { sectionComponent ->
          val binaryConfigPath =
            BINARY_CONFIG_PATH.run {
              replace(DEFAULT_CLASSIFICATION, sectionComponent.focus.identifier.value)
            }

          val localBinaryJsonConfig =
            context.assets.open(baseConfigPath.plus(binaryConfigPath)).bufferedReader().use {
              it.readText()
            }

          val binaryConfig =
            Binary().apply {
              contentType = CONFIG_CONTENT_TYPE
              content = localBinaryJsonConfig.encodeToByteArray()
            }

          val workflowPointName = workflowPointName(sectionComponent.focus.identifier.value)
          val workflowPoint =
            WorkflowPoint(
              classification = sectionComponent.focus.identifier.value,
              description = sectionComponent.title,
              resource = binaryConfig,
              workflowPoint = sectionComponent.focus.identifier.value
            )
          workflowPointsMap[workflowPointName] = workflowPoint
        }
    }
      .getOrNull()
      .also { if (it == null) configsLoadedCallback(false) }
      ?.also { configsLoadedCallback(true) }
  }

  fun fetchNonWorkflowConfigResources() {
    CoroutineScope(dispatcherProvider.io()).launch {
      try {
        Timber.i("Fetching non-workflow resources for app $appId")
        repository
          .searchCompositionByIdentifier(appId)
          ?.section
          ?.groupBy { it.focus.reference.split("/")[0] }
          ?.entries
          ?.filterNot {
            it.key == ResourceType.Binary.name || it.key == ResourceType.Parameters.name
          }
          ?.forEach { entry: Map.Entry<String, List<Composition.SectionComponent>> ->
            val ids = entry.value.joinToString(",") { it.focus.extractId() }
            val rPath = entry.key + "?${Composition.SP_RES_ID}=$ids"
            fhirResourceDataSource.loadData(rPath).entry.forEach {
              repository.addOrUpdate(it.resource)
            }
          }
      } catch (exception: Exception) {
        Timber.e("Error fetching non-workflow resources for app $appId")
        Timber.e(exception)
      }
    }
  }

  fun workflowPointName(key: String) = "$appId|$key"

  fun isAppIdInitialized() = this::appId.isInitialized

  companion object {
    const val DEFAULT_APP_ID = "appId"
    const val BASE_CONFIG_PATH = "configs/$DEFAULT_APP_ID"
    const val COMPOSITION_CONFIG_PATH = "/config_composition.json"
    const val DEFAULT_CLASSIFICATION = "classification"
    const val BINARY_CONFIG_PATH = "/config_$DEFAULT_CLASSIFICATION.json"
    const val CONFIG_CONTENT_TYPE = "application/json"
    const val DEBUG_SUFFIX = "debug"
    const val ORGANIZATION = "organization"
    const val PUBLISHER = "publisher"
    const val ID = "_id"
    const val COUNT = "count"
    const val DEFAULT_COUNT = "100"
  }
}
