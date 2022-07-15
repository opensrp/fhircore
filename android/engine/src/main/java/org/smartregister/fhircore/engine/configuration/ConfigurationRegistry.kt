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
import java.util.LinkedList
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.Composition
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.util.APP_ID_KEY
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.decodeJson
import org.smartregister.fhircore.engine.util.extension.decodeResourceFromString
import org.smartregister.fhircore.engine.util.extension.extractId
import org.smartregister.fhircore.engine.util.extension.retrieveCompositionSections
import timber.log.Timber

/** A configuration store for application configurations */
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

  val configsJsonMap = mutableMapOf<String, String>()

  /**
   * Retrieve configuration for the provided [ConfigType]. The JSON retrieved from [configsJsonMap]
   * can be directly converted to a FHIR resource or hard coded custom model.
   */
  // TODO optimize to use a map to avoid decoding configuration everytime a config is retrieved
  inline fun <reified T : Configuration> retrieveConfiguration(
    configType: ConfigType,
    configId: String? = null
  ): T {
    val configKey = if (configType.multiConfig && configId != null) configId else configType.name
    return if (configType.parseAsResource)
      configsJsonMap.getValue(configKey).decodeResourceFromString()
    else configsJsonMap.getValue(configKey).decodeJson()
  }

  /**
   * Retrieve configuration for the provided [ConfigType]. The JSON retrieved from [configsJsonMap]
   * can be directly converted to a FHIR resource or hard coded custom model.
   */
  inline fun <reified T : Base> retrieveResourceConfiguration(configType: ConfigType): T {
    require(configType.parseAsResource) { "Configuration MUST be a supported FHIR Resource" }
    return configsJsonMap.getValue(configType.name).decodeResourceFromString()
  }

  /**
   * Populate application's configurations from the composition resource. Only Binary and Parameter
   * Resources are used to represent application configurations.
   *
   * Sections in Composition with Binary or Parameter represents a valid application configuration.
   * Example below is represents an application configuration uniquely identified by the
   * [ConfigType]'application'. Sections can be nested like in the registers case.
   *
   * ```
   *  {
   *    "title": "Application configuration",
   *    "mode": "working",
   *    "focus": {
   *      "reference": "Binary/11111",
   *      "identifier: {
   *      "value": "application"
   *      }
   *    }
   *  }
   * ```
   *
   * Nested section example
   *
   * ```
   *  {
   *     "title": "Register configurations",
   *     "mode": "working",
   *     "section": [
   *        {
   *          "title": "Household register configuration",
   *          "focus": {
   *             "reference": "Binary/11111115",
   *             "identifier": {
   *                "value": "all_household_register_config"
   *              }
   *          }
   *        }
   *     ]
   * }
   * ```
   *
   * [appId] is a unique identifier for the application. Typically written in human readable form
   * [configsLoadedCallback] is a callback function called once configs have been loaded.
   */
  suspend fun loadConfigurations(appId: String, configsLoadedCallback: (Boolean) -> Unit = {}) {
    // For appId that ends with suffix /debug e.g. app/debug, we load configurations from assets
    // extract appId by removing the suffix e.g. app from above example
    val loadFromAssets = appId.endsWith(DEBUG_SUFFIX, ignoreCase = true)
    if (loadFromAssets) {
      val parsedAppId = appId.substringBefore("/").trim()
      context
        .assets
        .open(String.format(COMPOSITION_CONFIG_PATH, parsedAppId))
        .bufferedReader()
        .readText()
        .decodeResourceFromString<Composition>()
        .run {
          populateConfigurationsMap(
            composition = this,
            loadFromAssets = loadFromAssets,
            appId = parsedAppId,
            configsLoadedCallback = configsLoadedCallback
          )
        }
    } else {
      repository.searchCompositionByIdentifier(appId)?.run {
        populateConfigurationsMap(this, loadFromAssets, appId, configsLoadedCallback)
      }
    }
  }

  private suspend fun populateConfigurationsMap(
    composition: Composition,
    loadFromAssets: Boolean,
    appId: String,
    configsLoadedCallback: (Boolean) -> Unit
  ) {
    if (loadFromAssets) {
      retrieveAssetConfigs(appId).forEach { fileName ->
        // Create binary config from asset and add to map, skip composition resource
        // Use file name as the key. Conventionally navigation configs MUST end with "_config.json"
        // File names in asset should match the configType/id (MUST be unique) in the config JSON
        if (!fileName.equals(String.format(COMPOSITION_CONFIG_PATH, appId), ignoreCase = true)) {
          val configKey = fileName.substringAfterLast("/").removeSuffix(CONFIG_SUFFIX)
          val configJson = context.assets.open(fileName).bufferedReader().readText()
          configsJsonMap[configKey] = configJson
        }
      }
    } else {
      composition.retrieveCompositionSections().forEach {
        if (it.hasFocus() && it.focus.hasReferenceElement() && it.focus.hasIdentifier()) {
          val configKey = it.focus.identifier.value
          val referenceResourceType = it.focus.reference.substringBeforeLast("/")
          if (isAppConfig(referenceResourceType)) {
            val configBinary = repository.getBinary(it.focus.extractId())
            configsJsonMap[configKey] = configBinary.content.decodeToString()
          }
        }
      }
    }
    configsLoadedCallback(true)
  }

  private fun isAppConfig(referenceResourceType: String) =
    referenceResourceType in arrayOf(ResourceType.Binary.name, ResourceType.Parameters.name)

  private fun retrieveAssetConfigs(appId: String): MutableList<String> {
    // Reads .json configurations in asset/config/* directory recursively.
    // Populates all sub directory in a queue then reads all the nested files for each sub
    // directory until queue is empty
    val filesQueue = LinkedList<String>()
    val configFiles = mutableListOf<String>()
    context.assets.list(String.format(BASE_CONFIG_PATH, appId))?.onEach {
      if (!it.endsWith(JSON_EXTENSION))
        filesQueue.addLast(String.format(BASE_CONFIG_PATH, appId) + it)
      else configFiles.add(String.format(BASE_CONFIG_PATH, appId) + it)
    }
    while (filesQueue.isNotEmpty()) {
      val currentPath = filesQueue.removeFirst()
      context.assets.list(currentPath)?.onEach {
        if (!it.endsWith(JSON_EXTENSION)) filesQueue.addLast("$currentPath/$it")
        else configFiles.add("$currentPath/$it")
      }
    }
    return configFiles
  }

  /**
   * Fetch non-patient Resources for the application that are not application configurations
   * resources such as [ResourceType.Questionnaire] and [ResourceType.StructureMap]. (
   * [ResourceType.Binary] and [ResourceType.Parameters] are currently the only FHIR HL7 resources
   * used to represent application configurations). These non-patients resource identifiers are also
   * set in the section components of the [Composition] resource.
   *
   * This function retrieves the composition based on the appId and groups the non-patient resources
   * ( [ResourceType.Questionnaire] or [ResourceType.Questionnaire]) based on their type.
   *
   * Searching is done using the _id search parameter of these not patient resources; the
   * composition section components are grouped by resource type ,then the ids concatenated (as
   * comma separated values), thus generating a search query like the following 'Resource
   * Type'?_id='comma,separated,list,of,ids'
   */
  fun fetchNonWorkflowConfigResources() {
    // TODO load these type of configs from assets too
    CoroutineScope(dispatcherProvider.io()).launch {
      try {
        sharedPreferencesHelper.read(APP_ID_KEY, null)?.let { appId: String ->
          repository.searchCompositionByIdentifier(appId)?.let { composition ->
            composition
              .retrieveCompositionSections()
              .groupBy { it.focus.reference?.split(TYPE_REFERENCE_DELIMITER)?.firstOrNull() ?: "" }
              .filterNot { isAppConfig(it.key) }
              .forEach { resourceGroup ->
                val resourceIds =
                  resourceGroup.value.joinToString(",") { sectionComponent ->
                    sectionComponent.focus.extractId()
                  }
                val searchPath = resourceGroup.key + "?${Composition.SP_RES_ID}=$resourceIds"
                fhirResourceDataSource.loadData(searchPath).entry.forEach {
                  repository.addOrUpdate(it.resource)
                }
              }
          }
        }
      } catch (exception: Exception) {
        Timber.e(exception)
      }
    }
  }

  /**
   * Application configurations are represented with only [ResourceType.Binary] and
   * [ResourceType.Parameters]
   */
  fun Composition.SectionComponent.isApplicationConfig(): Boolean {
    this.focus.reference?.split(TYPE_REFERENCE_DELIMITER)?.first().let { resourceType ->
      return resourceType in arrayOf(ResourceType.Parameters.name, ResourceType.Binary.name)
    }
  }

  companion object {
    const val BASE_CONFIG_PATH = "configs/%s/"
    const val COMPOSITION_CONFIG_PATH = "configs/%s/composition_config.json"
    const val DEBUG_SUFFIX = "/debug"
    const val ORGANIZATION = "organization"
    const val PUBLISHER = "publisher"
    const val ID = "_id"
    const val COUNT = "count"
    const val TYPE_REFERENCE_DELIMITER = "/"
    const val JSON_EXTENSION = ".json"
    const val CONFIG_SUFFIX = "_config.json"
  }
}
