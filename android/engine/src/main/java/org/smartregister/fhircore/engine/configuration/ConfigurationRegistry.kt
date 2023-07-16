/*
 * Copyright 2021-2023 Ona Systems, Inc
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
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.db.ResourceNotFoundException
import com.google.android.fhir.get
import com.google.android.fhir.logicalId
import java.io.FileNotFoundException
import java.net.UnknownHostException
import java.util.LinkedList
import java.util.Locale
import java.util.PropertyResourceBundle
import java.util.ResourceBundle
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.Binary
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent
import org.hl7.fhir.r4.model.Composition
import org.hl7.fhir.r4.model.ListResource
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.jetbrains.annotations.VisibleForTesting
import org.json.JSONObject
import org.smartregister.fhircore.engine.BuildConfig
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.camelCase
import org.smartregister.fhircore.engine.util.extension.decodeJson
import org.smartregister.fhircore.engine.util.extension.decodeResourceFromString
import org.smartregister.fhircore.engine.util.extension.extractId
import org.smartregister.fhircore.engine.util.extension.extractLogicalIdUuid
import org.smartregister.fhircore.engine.util.extension.fileExtension
import org.smartregister.fhircore.engine.util.extension.generateMissingId
import org.smartregister.fhircore.engine.util.extension.interpolate
import org.smartregister.fhircore.engine.util.extension.retrieveCompositionSections
import org.smartregister.fhircore.engine.util.extension.searchCompositionByIdentifier
import org.smartregister.fhircore.engine.util.extension.updateFrom
import org.smartregister.fhircore.engine.util.extension.updateLastUpdated
import org.smartregister.fhircore.engine.util.helper.LocalizationHelper
import retrofit2.HttpException
import timber.log.Timber

@Singleton
class ConfigurationRegistry
@Inject
constructor(
  val fhirEngine: FhirEngine,
  val fhirResourceDataSource: FhirResourceDataSource,
  val sharedPreferencesHelper: SharedPreferencesHelper,
  val dispatcherProvider: DispatcherProvider,
  val configService: ConfigService,
  val json: Json,
) {

  val configsJsonMap = mutableMapOf<String, String>()
  val configCacheMap = mutableMapOf<String, Configuration>()
  val localizationHelper: LocalizationHelper by lazy { LocalizationHelper(this) }
  private val supportedFileExtensions = listOf("json", "properties")
  private var isNonProxy_ = BuildConfig.IS_NON_PROXY_APK

  /**
   * Retrieve configuration for the provided [ConfigType]. The JSON retrieved from [configsJsonMap]
   * can be directly converted to a FHIR resource or hard coded custom model. The filtering assumes
   * you are passing data across screens, then later using it in DataQueries and to retrieve
   * registerConfiguration. It is necessary to check that [paramsMap] is empty to confirm that the
   * params used in the DataQuery are passed when retrieving the configurations.
   */
  inline fun <reified T : Configuration> retrieveConfiguration(
    configType: ConfigType,
    configId: String? = null,
    paramsMap: Map<String, String>? = emptyMap(),
  ): T {
    require(!configType.parseAsResource) { "Configuration MUST be a template" }
    val configKey = if (configType.multiConfig && configId != null) configId else configType.name
    if (configCacheMap.contains(configKey) && paramsMap?.isEmpty() == true) {
      return configCacheMap[configKey] as T
    }
    val decodedConfig =
      localizationHelper
        .parseTemplate(
          bundleName = LocalizationHelper.STRINGS_BASE_BUNDLE_NAME,
          locale = Locale.getDefault(),
          template = getConfigValueWithParam(paramsMap, configKey),
        )
        .decodeJson<T>(jsonInstance = json)
    configCacheMap[configKey] = decodedConfig
    return decodedConfig
  }

  inline fun <reified T : Configuration> retrieveConfigurations(configType: ConfigType): List<T> =
    configsJsonMap.values
      .filter {
        try {
          JSONObject(it).getString(CONFIG_TYPE).equals(configType.name, ignoreCase = true)
        } catch (e: Exception) {
          Timber.w(e.localizedMessage)
          false
        }
      }
      .map {
        localizationHelper
          .parseTemplate(
            bundleName = LocalizationHelper.STRINGS_BASE_BUNDLE_NAME,
            locale = Locale.getDefault(),
            template = it,
          )
          .decodeJson<T>()
      }

  /**
   * This function interpolates the value for the given [configKey] by replacing the string
   * placeholders e.g. {{ placeholder }} with value retrieved from the [paramsMap] using [configKey]
   * as the key. If value is null the placeholder is returned
   */
  fun getConfigValueWithParam(paramsMap: Map<String, String>?, configKey: String) =
    configsJsonMap.getValue(configKey).let { jsonValue ->
      if (paramsMap != null) jsonValue.interpolate(paramsMap) else jsonValue
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
   * Retrieve translation configuration for the provided [bundleName]. The Bundle value is retrieved
   * from [configsJsonMap] can be directly converted to a ResourceBundle.
   */
  fun retrieveResourceBundleConfiguration(bundleName: String): ResourceBundle? {
    val resourceBundle =
      configsJsonMap[bundleName.camelCase()] // Convention for config map keys is camelCase
    if (resourceBundle != null) {
      return PropertyResourceBundle(resourceBundle.byteInputStream())
    }
    if (bundleName.contains("_")) {
      return retrieveResourceBundleConfiguration(
        bundleName.substring(0, bundleName.lastIndexOf('_')),
      )
    }
    return null
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
   *
   * [context] is the targeted Android context
   *
   * [configsLoadedCallback] is a callback function called once configs have been loaded.
   */
  suspend fun loadConfigurations(
    appId: String,
    context: Context,
    configsLoadedCallback: (Boolean) -> Unit = {},
  ) {
    // For appId that ends with suffix /debug e.g. app/debug, we load configurations from assets
    // extract appId by removing the suffix e.g. app from above example
    val loadFromAssets = appId.endsWith(DEBUG_SUFFIX, ignoreCase = true)
    val parsedAppId = appId.substringBefore(TYPE_REFERENCE_DELIMITER).trim()
    if (loadFromAssets) {
      try {
        context.assets
          .open(String.format(COMPOSITION_CONFIG_PATH, parsedAppId))
          .bufferedReader()
          .readText()
          .decodeResourceFromString<Composition>()
          .run {
            val iconConfigs =
              retrieveCompositionSections().filter {
                it.focus.hasIdentifier() && isIconConfig(it.focus.identifier.value)
              }
            if (iconConfigs.isNotEmpty()) {
              val ids = iconConfigs.joinToString(DEFAULT_STRING_SEPARATOR) { it.focus.extractId() }
              fhirResourceDataSource
                .getResource("${ResourceType.Binary.name}?$ID=$ids")
                .entry
                .forEach { addOrUpdate(it.resource) }
            }
            populateConfigurationsMap(
              composition = this,
              loadFromAssets = true,
              appId = parsedAppId,
              configsLoadedCallback = configsLoadedCallback,
              context = context,
            )
          }
      } catch (fileNotFoundException: FileNotFoundException) {
        Timber.e("Missing app configs for app ID: $parsedAppId", fileNotFoundException)
        withContext(dispatcherProvider.main()) { configsLoadedCallback(false) }
      }
    } else {
      fhirEngine.searchCompositionByIdentifier(appId)?.run {
        populateConfigurationsMap(context, this, false, appId, configsLoadedCallback)
      }
    }
  }

  private suspend fun populateConfigurationsMap(
    context: Context,
    composition: Composition,
    loadFromAssets: Boolean,
    appId: String,
    configsLoadedCallback: (Boolean) -> Unit,
  ) {
    if (loadFromAssets) {
      retrieveAssetConfigs(context, appId).forEach { fileName ->
        // Create binary config from asset and add to map, skip composition resource
        // Use file name as the key. Conventionally navigation configs MUST end with
        // "_config.<extension>"
        // File names in asset should match the configType/id (MUST be unique) in the config JSON
        if (!fileName.equals(String.format(COMPOSITION_CONFIG_PATH, appId), ignoreCase = true)) {
          val configKey =
            fileName
              .lowercase(Locale.ENGLISH)
              .substring(
                fileName.indexOfLast { it == '/' }.plus(1),
                fileName.lastIndexOf(CONFIG_SUFFIX),
              )
              .camelCase()

          val configJson = context.assets.open(fileName).bufferedReader().readText()
          configsJsonMap[configKey] = configJson
        }
      }
    } else {
      composition.retrieveCompositionSections().forEach {
        if (it.hasFocus() && it.focus.hasReferenceElement() && it.focus.hasIdentifier()) {
          val configIdentifier = it.focus.identifier.value
          val referenceResourceType = it.focus.reference.substringBefore(TYPE_REFERENCE_DELIMITER)
          if (isAppConfig(referenceResourceType) && !isIconConfig(configIdentifier)) {
            val configBinary = fhirEngine.get<Binary>(it.focus.extractId())
            configsJsonMap[configIdentifier] = configBinary.content.decodeToString()
          }
        }
      }
    }
    configsLoadedCallback(true)
  }

  private fun isAppConfig(referenceResourceType: String) =
    referenceResourceType in arrayOf(ResourceType.Binary.name, ResourceType.Parameters.name)

  private fun isIconConfig(configIdentifier: String) = configIdentifier.startsWith(ICON_PREFIX)

  /**
   * Reads supported files from the asset/config directory recursively, populates all sub directory
   * in a queue, then reads all the nested files for each.
   *
   * @return A list of strings of config files.
   */
  private fun retrieveAssetConfigs(context: Context, appId: String): MutableList<String> {
    val filesQueue = LinkedList<String>()
    val configFiles = mutableListOf<String>()
    context.assets.list(String.format(BASE_CONFIG_PATH, appId))?.onEach {
      if (!supportedFileExtensions.contains(it.fileExtension)) {
        filesQueue.addLast(String.format(BASE_CONFIG_PATH, appId) + "/$it")
      } else configFiles.add(String.format(BASE_CONFIG_PATH, appId) + "/$it")
    }
    while (filesQueue.isNotEmpty()) {
      val currentPath = filesQueue.removeFirst()
      context.assets.list(currentPath)?.onEach {
        if (!supportedFileExtensions.contains(it.fileExtension)) {
          filesQueue.addLast("$currentPath/$it")
        } else configFiles.add("$currentPath/$it")
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
   * ( [ResourceType.Questionnaire] or [ResourceType.StructureMap]) based on their type.
   *
   * Searching is done using the _id search parameter of these not patient resources; the
   * composition section components are grouped by resource type ,then the ids concatenated (as
   * comma separated values), thus generating a search query like the following 'Resource
   * Type'?_id='comma,separated,list,of,ids'
   */
  @Throws(UnknownHostException::class, HttpException::class)
  suspend fun fetchNonWorkflowConfigResources() {
    sharedPreferencesHelper.read(SharedPreferenceKey.APP_ID.name, "app/debug")?.let { appId: String ->
      fhirEngine.searchCompositionByIdentifier(appId)?.let { composition ->
        composition
          .retrieveCompositionSections()
          .groupBy { it.focus.reference?.split(TYPE_REFERENCE_DELIMITER)?.firstOrNull() ?: "" }
          .filter {
            it.key in
              listOf(
                ResourceType.Questionnaire.name,
                ResourceType.StructureMap.name,
                ResourceType.List.name,
                ResourceType.PlanDefinition.name,
                ResourceType.Library.name,
                ResourceType.Measure.name,
              )
          }
          .forEach { resourceGroup ->
            if (resourceGroup.key == ResourceType.List.name) {
              if (isNonProxy()) { // Backward compatibility for NON-PROXY version
                val chunkedResourceIdList =
                  resourceGroup.value.chunked(MANIFEST_PROCESSOR_BATCH_SIZE)
                chunkedResourceIdList.forEach {
                  val resourceIds =
                    it.joinToString(DEFAULT_STRING_SEPARATOR) { sectionComponent ->
                      sectionComponent.focus.extractId()
                    }
                  fhirResourceDataSource
                    .getResource("${resourceGroup.key}?$ID=$resourceIds")
                    .entry
                    .forEach { bundleEntryComponent ->
                      when (bundleEntryComponent.resource) {
                        is ListResource -> {
                          addOrUpdate(bundleEntryComponent.resource)
                          val list = bundleEntryComponent.resource as ListResource
                          list.entry.forEach { listEntryComponent ->
                            val resourceKey =
                              listEntryComponent.item.reference.substringBefore(
                                TYPE_REFERENCE_DELIMITER,
                              )
                            val resourceId =
                              listEntryComponent.item.reference.extractLogicalIdUuid()

                            val listResourceUrlPath = "$resourceKey?$ID=$resourceId"
                            fhirResourceDataSource.getResource(listResourceUrlPath).entry.forEach {
                              listEntryResourceBundle ->
                              addOrUpdate(listEntryResourceBundle.resource)
                              Timber.d("Fetched and processed List reference $listResourceUrlPath")
                            }
                          }
                        }
                      }
                    }
                }
              } else {
                resourceGroup.value.forEach {
                  processCompositionManifestResources(
                    FHIR_GATEWAY_MODE_HEADER_VALUE,
                    "${resourceGroup.key}/${it.focus.extractId()}",
                  )
                }
              }
            } else {
              val chunkedResourceIdList = resourceGroup.value.chunked(MANIFEST_PROCESSOR_BATCH_SIZE)

              chunkedResourceIdList.forEach {
                val resourceIds =
                  it.joinToString(DEFAULT_STRING_SEPARATOR) { sectionComponent ->
                    sectionComponent.focus.extractId()
                  }
                processCompositionManifestResources(
                  searchPath = "${resourceGroup.key}?$ID=$resourceIds",
                )
              }
            }
          }
      }
    }
  }

  private suspend fun processCompositionManifestResources(
    gatewayModeHeaderValue: String? = null,
    searchPath: String,
  ) {
    val resultBundle =
      if (gatewayModeHeaderValue.isNullOrEmpty()) {
        fhirResourceDataSource.getResource(searchPath)
      } else
        fhirResourceDataSource.getResourceWithGatewayModeHeader(gatewayModeHeaderValue, searchPath)

    resultBundle.entry.forEach { bundleEntryComponent ->
      when (bundleEntryComponent.resource) {
        is Bundle -> {
          val bundle = bundleEntryComponent.resource as Bundle
          bundle.entry.forEach { entryComponent ->
            when (entryComponent.resource) {
              is Bundle -> {
                val bundle = entryComponent.resource as Bundle
                addOrUpdate(bundle)
                bundle.entry.forEach { innerEntryComponent ->
                  saveListEntryResource(innerEntryComponent)
                }
              }
              else -> saveListEntryResource(entryComponent)
            }
          }
        }
        else -> {
          addOrUpdate(bundleEntryComponent.resource)
          Timber.d("Fetched and processed resources $searchPath")
        }
      }
    }
  }

  private suspend fun saveListEntryResource(entryComponent: BundleEntryComponent) {
    addOrUpdate(entryComponent.resource)
    Timber.d(
      "Fetched and processed List reference ${entryComponent.resource.resourceType}/${entryComponent.resource.id}",
    )
  }

  /**
   * Using this [FhirEngine] and [DispatcherProvider], update this stored resources with the passed
   * resource, or create it if not found.
   */
  suspend fun <R : Resource> addOrUpdate(resource: R) {
    withContext(dispatcherProvider.io()) {
      resource.updateLastUpdated()
      try {
        fhirEngine.get(resource.resourceType, resource.logicalId).run {
          fhirEngine.update(updateFrom(resource))
        }
      } catch (resourceNotFoundException: ResourceNotFoundException) {
        create(resource)
      }
    }
  }

  /**
   * Using this [FhirEngine] and [DispatcherProvider], for all passed resources, make sure they all
   * have IDs or generate if they don't, then pass them to create.
   *
   * @param resources vararg of resources
   */
  suspend fun create(vararg resources: Resource) {
    return withContext(dispatcherProvider.io()) {
      resources.onEach { it.generateMissingId() }
      fhirEngine.createRemote(*resources)
    }
  }

  @VisibleForTesting fun isNonProxy() = isNonProxy_

  @VisibleForTesting
  fun setNonProxy(nonProxy: Boolean) {
    isNonProxy_ = nonProxy
  }

  companion object {
    const val BASE_CONFIG_PATH = "configs/%s"
    const val COMPOSITION_CONFIG_PATH = "configs/%s/composition_config.json"
    const val CONFIG_SUFFIX = "_config"
    const val CONFIG_TYPE = "configType"
    const val COUNT = "count"
    const val DEBUG_SUFFIX = "/debug"
    const val DEFAULT_STRING_SEPARATOR = ","
    const val FHIR_GATEWAY_MODE_HEADER_VALUE = "list-entries"
    const val ICON_PREFIX = "ic_"
    const val ID = "_id"
    const val MANIFEST_PROCESSOR_BATCH_SIZE = 30
    const val ORGANIZATION = "organization"
    const val TYPE_REFERENCE_DELIMITER = "/"
  }
}
