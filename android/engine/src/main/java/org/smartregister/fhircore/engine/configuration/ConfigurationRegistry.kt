/*
 * Copyright 2021-2024 Ona Systems, Inc
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
import android.database.SQLException
import ca.uhn.fhir.context.FhirContext
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.db.ResourceNotFoundException
import com.google.android.fhir.get
import com.google.android.fhir.knowledge.KnowledgeManager
import com.google.android.fhir.sync.CurrentSyncJobStatus
import com.google.android.fhir.sync.Sync
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.RequestBody.Companion.toRequestBody
import okio.ByteString.Companion.decodeBase64
import org.apache.commons.lang3.StringUtils
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.Binary
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Composition
import org.hl7.fhir.r4.model.ImplementationGuide
import org.hl7.fhir.r4.model.ListResource
import org.hl7.fhir.r4.model.MetadataResource
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.jetbrains.annotations.VisibleForTesting
import org.json.JSONObject
import org.smartregister.fhircore.engine.BuildConfig
import org.smartregister.fhircore.engine.OpenSrpApplication
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.configuration.profile.ProfileConfiguration
import org.smartregister.fhircore.engine.configuration.register.RegisterConfiguration
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.di.NetworkModule
import org.smartregister.fhircore.engine.domain.model.FhirResourceConfig
import org.smartregister.fhircore.engine.domain.model.ResourceConfig
import org.smartregister.fhircore.engine.sync.CompListItemSyncWorker
import org.smartregister.fhircore.engine.sync.CompositionConfigSyncWorker
import org.smartregister.fhircore.engine.sync.CompositionListSyncWorker
import org.smartregister.fhircore.engine.sync.CompositionManifestSyncWorker
import org.smartregister.fhircore.engine.sync.CompositionSyncWorker
import org.smartregister.fhircore.engine.sync.ResTypeId
import org.smartregister.fhircore.engine.sync.SyncParamSource
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.camelCase
import org.smartregister.fhircore.engine.util.extension.decodeJson
import org.smartregister.fhircore.engine.util.extension.decodeResourceFromString
import org.smartregister.fhircore.engine.util.extension.encodeResourceToString
import org.smartregister.fhircore.engine.util.extension.extractId
import org.smartregister.fhircore.engine.util.extension.extractLogicalIdUuid
import org.smartregister.fhircore.engine.util.extension.fileExtension
import org.smartregister.fhircore.engine.util.extension.generateMissingId
import org.smartregister.fhircore.engine.util.extension.interpolate
import org.smartregister.fhircore.engine.util.extension.referenceValue
import org.smartregister.fhircore.engine.util.extension.retrieveCompositionSections
import org.smartregister.fhircore.engine.util.extension.searchCompositionByIdentifier
import org.smartregister.fhircore.engine.util.extension.tryDecodeJson
import org.smartregister.fhircore.engine.util.extension.updateLastUpdated
import org.smartregister.fhircore.engine.util.helper.LocalizationHelper
import retrofit2.HttpException
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.net.UnknownHostException
import java.nio.charset.StandardCharsets
import java.util.LinkedList
import java.util.Locale
import java.util.PropertyResourceBundle
import java.util.ResourceBundle
import javax.inject.Inject
import javax.inject.Singleton

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
  @ApplicationContext val context: Context,
  private var openSrpApplication: OpenSrpApplication?,
//  val syncListenerManager: SyncListenerManager,
  val syncParamSource: SyncParamSource
) {

  val configsJsonMap = mutableMapOf<String, String>()
  val configCacheMap = mutableMapOf<String, Configuration>()
  val localizationHelper: LocalizationHelper by lazy { LocalizationHelper(this) }
  private val supportedFileExtensions = listOf("json", "properties")
  private var _isNonProxy = BuildConfig.IS_NON_PROXY_APK
  private val fhirContext = FhirContext.forR4Cached()

  @Inject lateinit var knowledgeManager: KnowledgeManager

  private val jsonParser = fhirContext.newJsonParser()

  /**
   * Retrieve configuration for the provided [ConfigType]. The JSON retrieved from [configsJsonMap]
   * can be directly converted to a FHIR resource or hard coded custom model. The filtering assumes
   * you are passing data across screens, then later using it in DataQueries and to retrieve
   * registerConfiguration. It is necessary to check that [paramsMap] is empty to confirm that the
   * params used in the DataQuery are passed when retrieving the configurations.
   *
   * @throws NoSuchElementException when the [configsJsonMap] doesn't contain a value for the
   *   specified key.
   */
  inline fun <reified T : Configuration> retrieveConfiguration(
    configType: ConfigType,
    configId: String? = null,
    paramsMap: Map<String, String>? = emptyMap(),
  ): T {
    require(!configType.parseAsResource) { "Configuration MUST be a template" }
    val configKey = if (configType.multiConfig && configId != null) configId else configType.name
    if (configCacheMap.contains(configKey) && paramsMap?.isEmpty() == true) {
//      if(configType == ConfigType.Sync)
//        syncListenerManager.linkSyncConfig( configCacheMap[configKey] as Parameters)
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
//    if(configType == ConfigType.Sync)
//      syncListenerManager.linkSyncConfig( configCacheMap[configKey] as Parameters)
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
          .decodeJson()
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
//    syncListenerManager.syncConfig = configsJsonMap.getValue(configType.name).decodeResourceFromString()
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
   * Resources are used to represent application configurations. The [configCacheMap] is reset on
   * every configs load.
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
  ) = coroutineScope {
    // Reset configurations before loading new ones

    val coroutineScope = this
    configCacheMap.clear()

    // For appId that ends with suffix /debug e.g. app/debug, we load configurations from assets
    // extract appId by removing the suffix e.g. app from above example
    val loadFromAssets = appId.endsWith(DEBUG_SUFFIX, ignoreCase = true)
    val parsedAppId = appId.substringBefore(TYPE_REFERENCE_DELIMITER).trim()
    if (loadFromAssets) {
      try {
        val localCompositionResource =
          context.assets
            .open(String.format(COMPOSITION_CONFIG_PATH, parsedAppId))
            .bufferedReader()
            .readText()
            .decodeResourceFromString<Composition>()

        addOrUpdate(localCompositionResource)

        localCompositionResource.run {
          val iconConfigs =
            retrieveCompositionSections().filter {
              it.focus.hasIdentifier() && isIconConfig(it.focus.identifier.value)
            }
          if (iconConfigs.isNotEmpty()) {
            val ids = iconConfigs.joinToString(DEFAULT_STRING_SEPARATOR) { it.focus.extractId() }

            // fetch binaries ~ BinarySyncWork would work
            // Sync.oneTimeSync<ConfigSyncWorker>(context).handleConfigSyncJobStatus(coroutineScope)
            // retrofit call
            fhirResourceDataSource
              .getResource(
                "${ResourceType.Binary.name}?$ID=$ids&_count=$DEFAULT_COUNT",
              )
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
      fhirEngine.searchCompositionByIdentifier(parsedAppId)?.run {
        populateConfigurationsMap(context, this, false, parsedAppId, configsLoadedCallback)
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
            val extractedId = it.focus.extractId()
            try {
              val configBinary = fhirEngine.get<Binary>(extractedId)
              configsJsonMap[configIdentifier] = configBinary.content.decodeToString()
            } catch (resourceNotFoundException: ResourceNotFoundException) {
              Timber.e("Missing Binary file with ID :$extractedId")
              withContext(dispatcherProvider.main()) { configsLoadedCallback(false) }
            }
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
  suspend fun fetchNonWorkflowConfigResources(isInitialLogin: Boolean = true) = coroutineScope {
    Timber.d("fetchNonWorkflow Config Sync")
    fetchCompositionConfig(this)
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
  suspend fun fetchNonWorkflowConfigResourcesMain(isInitialLogin: Boolean = true) {
    // Reset configurations before loading new ones
    configCacheMap.clear()
    sharedPreferencesHelper.read(SharedPreferenceKey.APP_ID.name, null)?.let { appId ->
      val parsedAppId = appId.substringBefore(TYPE_REFERENCE_DELIMITER).trim()
      val patientRelatedResourceTypes = mutableListOf<ResourceType>()
      val compositionResource = fetchRemoteCompositionByAppId(parsedAppId)
      compositionResource?.let { composition ->
        composition
          .retrieveCompositionSections()
          .asSequence()
          .filter {
            it.hasFocus() && it.focus.hasReferenceElement()
          } // is focus.identifier a necessary check
          .groupBy { section ->
            section.focus.reference.substringBefore(
              TYPE_REFERENCE_DELIMITER,
              missingDelimiterValue = "",
            )
          }
          .filter { entry -> entry.key in FILTER_RESOURCE_LIST }
          .forEach { entry: Map.Entry<String, List<Composition.SectionComponent>> ->
            if (entry.key == ResourceType.List.name) {
              processCompositionListResources(
                entry,
                patientRelatedResourceTypes = patientRelatedResourceTypes,
              )
            } else {
              val chunkedResourceIdList = entry.value.chunked(MANIFEST_PROCESSOR_BATCH_SIZE)

              chunkedResourceIdList.forEach { parentIt ->
                Timber.d(
                  "Fetching config resource ${entry.key}: with ids ${StringUtils.join(parentIt,",")}",
                )
                processCompositionManifestResources(
                  entry.key,
                  parentIt.map { sectionComponent -> sectionComponent.focus.extractId() },
                  patientRelatedResourceTypes,
                )
              }
            }
          }

        saveSyncSharedPreferences(patientRelatedResourceTypes.toList())

        // Save composition after fetching all the referenced section resources
        addOrUpdate(compositionResource)

        Timber.d("Done fetching application configurations remotely")
      }
    }
  }

    suspend fun fetchRemoteImplementationGuideByAppId(
        appId: String?,
        appVersionCode: Int?,
    ): ImplementationGuide? {
        Timber.i("Fetching ImplementationGuide config for app $appId version $appVersionCode")

        val urlPath =
            "ImplementationGuide?&name=$appId&context-quantity=le$appVersionCode&_sort=-context-quantity&_count=1"
        return fhirResourceDataSource.getResource(urlPath).entryFirstRep.let {
            if (!it.hasResource()) {
                Timber.w("No response for ImplementationGuide resource on path $urlPath")
                return null
            }

            it.resource as ImplementationGuide
        }
    }


    suspend fun fetchRemoteCompositionById(
        id: String?,
        version: String?,
    ): Composition? {
        Timber.i("Fetching Composition config id $id version $version")
        val urlPath = "Composition/$id/_history/$version"
        return fhirResourceDataSource.getResource(urlPath).entryFirstRep.let {
            if (!it.hasResource()) {
                Timber.w("No response for composition resource on path $urlPath")
                return null
            }

            it.resource as Composition
        }
    }

    suspend fun fetchRemoteCompositionByAppId(appId: String?): Composition? {
        Timber.i("Fetching Composition config for app $appId")
        val urlPath = "Composition?identifier=$appId&_count=$DEFAULT_COUNT"
        return fhirResourceDataSource.getResource(urlPath).entryFirstRep.let {
            if (!it.hasResource()) {
                Timber.w("No response for composition resource on path $urlPath")
                return null
            }

            it.resource as Composition
        }
    }


//to change
  suspend fun fetchRemoteComposition(appId: String?): Composition? {
    Timber.i("Fetching configs for app $appId")
    val urlPath =
      "${ResourceType.Composition.name}?${Composition.SP_IDENTIFIER}=$appId&_count=$DEFAULT_COUNT"

    val testComp =  fhirEngine.get(ResourceType.Composition, appId!!)

    return fhirResourceDataSource.getResource(urlPath).entryFirstRep.let {
      if (!it.hasResource()) {
        Timber.w("No response for composition resource on path $urlPath")
        return null
      }

      it.resource as Composition
    }
  }

  // change Retrofit
  private suspend fun processCompositionManifestResources(
    resourceType: String,
    resourceIdList: List<String>,
    patientRelatedResourceTypes: MutableList<ResourceType>,
  ): Bundle {
    val resultBundle =
      if (isNonProxy()) {
        fhirResourceDataSourceGetBundle(resourceType, resourceIdList)
      } else
        fhirResourceDataSource.post(
          requestBody =
          generateRequestBundle(resourceType, resourceIdList)
            .encodeResourceToString()
            .toRequestBody(NetworkModule.JSON_MEDIA_TYPE),
        )

    processResultBundleEntries(resultBundle.entry, patientRelatedResourceTypes)

    return resultBundle
  }

  private suspend fun processCompositionManifestResources(
    gatewayModeHeaderValue: String? = null,
    searchPath: String,
    patientRelatedResourceTypes: MutableList<ResourceType>,
  ) {
    val resultBundle = fetchResourceBundle(gatewayModeHeaderValue, searchPath)
    val nextPageUrl = resultBundle.getLink(PAGINATION_NEXT)?.url ?: ""

    processResultBundleEntries(resultBundle.entry, patientRelatedResourceTypes)

    if (nextPageUrl.isNotEmpty()) {
      processCompositionManifestResources(
        gatewayModeHeaderValue,
        nextPageUrl,
        patientRelatedResourceTypes,
      )
    }
  }

  private suspend fun fetchResourceBundle(
    gatewayModeHeaderValue: String?,
    searchPath: String,
  ): Bundle {
    return if (gatewayModeHeaderValue.isNullOrEmpty()) {
      fhirResourceDataSource.getResource(searchPath)
    } else {
      fhirResourceDataSource.getResourceWithGatewayModeHeader(gatewayModeHeaderValue, searchPath)
    }
  }

  private suspend fun processResultBundleEntries(
    resultBundleEntries: List<Bundle.BundleEntryComponent>,
    patientRelatedResourceTypes: MutableList<ResourceType>,
  ) {
    resultBundleEntries.forEach { bundleEntryComponent ->
      when (bundleEntryComponent.resource) {
        is Bundle -> {
          val bundle = bundleEntryComponent.resource as Bundle
          bundle.entry.forEach { entryComponent ->
            when (entryComponent.resource) {
              is Bundle -> {
                val thisBundle = entryComponent.resource as Bundle
                addOrUpdate(thisBundle)
                thisBundle.entry.forEach { innerEntryComponent ->
                  saveListEntryResource(innerEntryComponent)
                }
              }
              else -> saveListEntryResource(entryComponent)
            }
          }
        }
        is Binary -> {
          val binary = bundleEntryComponent.resource as Binary
          processResultBundleBinaries(binary, patientRelatedResourceTypes)
          addOrUpdate(bundleEntryComponent.resource)
        }
        else -> {
          if (bundleEntryComponent.resource != null) {
            addOrUpdate(bundleEntryComponent.resource)
            Timber.d(
              "Fetched and processed resources ${bundleEntryComponent.resource.resourceType}/${bundleEntryComponent.resource.id}",
            )
          }
        }
      }
    }
  }

  private suspend fun saveListEntryResource(entryComponent: Bundle.BundleEntryComponent) {
    addOrUpdate(entryComponent.resource)
    Timber.d(
      "Fetched and processed List reference ${entryComponent.resource.resourceType}/${entryComponent.resource.id}",
    )
  }

  /**
   * Update this stored resources with the passed resource, or create it if not found. If the
   * resource is a Metadata Resource save it in the Knowledge Manager
   *
   * Note
   */
  suspend fun <R : Resource> addOrUpdate(resource: R) {
    withContext(dispatcherProvider.io()) {
      try {
        createOrUpdateRemote(resource)
      } catch (sqlException: SQLException) {
        Timber.e(sqlException)
      }

      /**
       * Knowledge manager [MetadataResource]s install Here we install all resources types of
       * [MetadataResource] as per FHIR Spec.This supports future use cases as well
       */
      try {
        if (resource is MetadataResource && resource.name != null) {
          knowledgeManager.install(
            writeToFile(resource.overwriteCanonicalURL()),
          )
        }
      } catch (exception: Exception) {
        Timber.e(exception)
      }
    }
  }

  private fun MetadataResource.overwriteCanonicalURL() =
    this.apply {
      url =
        url
          ?: "${openSrpApplication?.getFhirServerHost().toString()?.trimEnd { it == '/' }}/${this.referenceValue()}"
    }

  fun writeToFile(resource: Resource): File {
    val fileName =
      if (resource is MetadataResource && resource.name != null) {
        resource.name
      } else {
        resource.idElement.idPart
      }

    return File(context.filesDir, "$fileName.json").apply {
      writeText(jsonParser.encodeResourceToString(resource))
    }
  }

  /**
   * Using this [FhirEngine] and [DispatcherProvider], for all passed resources, make sure they all
   * have IDs or generate if they don't, then pass them to create.
   *
   * Note: The backing db API for fhirEngine.create(..,isLocalOnly) performs an UPSERT
   *
   * @param resources vararg of resources
   */
  suspend fun createOrUpdateRemote(vararg resources: Resource) {
    return withContext(dispatcherProvider.io()) {
      resources.onEach {
        it.updateLastUpdated()
        it.generateMissingId()
      }
      fhirEngine.create(*resources, isLocalOnly = true)
    }
  }

  @VisibleForTesting fun isNonProxy(): Boolean = _isNonProxy

  @VisibleForTesting
  fun setNonProxy(nonProxy: Boolean) {
    _isNonProxy = nonProxy
  }

  private fun generateRequestBundle(resourceType: String, idList: List<String>): Bundle {
    val bundleEntryComponents = mutableListOf<Bundle.BundleEntryComponent>()

    idList.forEach {
      bundleEntryComponents.add(
        Bundle.BundleEntryComponent().apply {
          request =
            Bundle.BundleEntryRequestComponent().apply {
              url = "$resourceType/$it"
              method = Bundle.HTTPVerb.GET
            }
        },
      )
    }

    return Bundle().apply {
      type = Bundle.BundleType.BATCH
      entry = bundleEntryComponents
    }
  }

  private suspend fun fhirResourceDataSourceGetBundle(
    resourceType: String,
    resourceIds: List<String>,
  ): Bundle {
    val bundleEntryComponents = mutableListOf<Bundle.BundleEntryComponent>()

    resourceIds.forEach {
      // retrofit call
      val responseBundle =
        fhirResourceDataSource.getResource("$resourceType?${Composition.SP_RES_ID}=$it")
      responseBundle.let {
        bundleEntryComponents.add(
          Bundle.BundleEntryComponent().apply { resource = it.entry?.firstOrNull()?.resource },
        )
      }
    }
    return Bundle().apply {
      type = Bundle.BundleType.COLLECTION
      entry = bundleEntryComponents
    }
  }

  fun clearConfigsCache() = configCacheMap.clear()

  private suspend fun processCompositionListResources(
    resourceGroup:
    Map.Entry<
            String,
            List<Composition.SectionComponent>,
            >,
    patientRelatedResourceTypes: MutableList<ResourceType>,
  ) {
    if (isNonProxy()) {
      val chunkedResourceIdList = resourceGroup.value.chunked(MANIFEST_PROCESSOR_BATCH_SIZE)
      chunkedResourceIdList.forEach {
        processCompositionManifestResources(
          resourceType = resourceGroup.key,
          resourceIdList = it.map { sectionComponent -> sectionComponent.focus.extractId() },
          patientRelatedResourceTypes = patientRelatedResourceTypes,
        )
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
                  val resourceId = listEntryComponent.item.reference.extractLogicalIdUuid()
                  val listResourceUrlPath = "$resourceKey?$ID=$resourceId&_count=$DEFAULT_COUNT"
                  // retrofit call
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
          gatewayModeHeaderValue = FHIR_GATEWAY_MODE_HEADER_VALUE,
          searchPath =
          "${resourceGroup.key}?$ID=${it.focus.extractId()}&_page=1&_count=$DEFAULT_COUNT",
          patientRelatedResourceTypes = patientRelatedResourceTypes,
        )
      }
    }
  }

  private fun FhirResourceConfig.dependentResourceTypes(target: MutableList<ResourceType>) {
    this.baseResource.dependentResourceTypes(target)
    this.relatedResources.forEach { it.dependentResourceTypes(target) }
  }

  private fun ResourceConfig.dependentResourceTypes(target: MutableList<ResourceType>) {
    target.add(resource)
    relatedResources.forEach { it.dependentResourceTypes(target) }
  }

  fun processResultBundleBinaries(
    binary: Binary,
    patientRelatedResourceTypes: MutableList<ResourceType>,
  ) {
    binary.data.decodeToString().decodeBase64()?.string(StandardCharsets.UTF_8)?.let {
      val config =
        it.tryDecodeJson<RegisterConfiguration>() ?: it.tryDecodeJson<ProfileConfiguration>()

      when (config) {
        is RegisterConfiguration ->
          config.fhirResource.dependentResourceTypes(
            patientRelatedResourceTypes,
          )
        is ProfileConfiguration ->
          config.fhirResource.dependentResourceTypes(
            patientRelatedResourceTypes,
          )
      }
    }
  }

  fun saveSyncSharedPreferences(resourceTypes: List<ResourceType>) =
    sharedPreferencesHelper.write(
      SharedPreferenceKey.REMOTE_SYNC_RESOURCES.name,
      resourceTypes.distinctBy { it.name },
    )

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
    const val MANIFEST_PROCESSOR_BATCH_SIZE = 20
    const val ORGANIZATION = "organization"
    const val TYPE_REFERENCE_DELIMITER = "/"
    const val DEFAULT_COUNT = 200
    const val PAGINATION_NEXT = "next"

    /**
     * The list of resources whose types can be synced down as part of the Composition configs.
     * These are hardcoded as they are not meant to be easily configurable to avoid config vs data
     * sync issues
     */
    val FILTER_RESOURCE_LIST =
      listOf(
        ResourceType.Questionnaire.name,
        ResourceType.StructureMap.name,
        ResourceType.List.name,
        ResourceType.PlanDefinition.name,
        ResourceType.Library.name,
        ResourceType.Measure.name,
        ResourceType.Basic.name,
        ResourceType.Binary.name,
        ResourceType.Parameters,
      )
  }

  private suspend fun fetchCompositionConfig(coroutineScope: CoroutineScope) {
    val compConfigFlow1 = Sync.oneTimeSync<CompositionSyncWorker>(context)
    compConfigFlow1.handleCompositionSyncJobStatus(coroutineScope)
    compConfigFlow1.collect { compConfigFlowJobStatus ->
      when (compConfigFlowJobStatus) {
        is CurrentSyncJobStatus.Succeeded -> {
          Timber.d("Composition Config Sync FlowJobStatus succeeded")
          fetchCompositionContent(coroutineScope)
        } else -> {
          // do nothing
        }
      }
    }
  }

  private fun Flow<CurrentSyncJobStatus>.handleCompositionSyncJobStatus(
    coroutineScope: CoroutineScope,
  ) {
    Timber.d("configSync handleCompositionSyncJobStatus")
  }

  private suspend fun fetchCompositionContent(coroutineScope: CoroutineScope){
    run {

      sharedPreferencesHelper.read(SharedPreferenceKey.APP_ID.name, null)?.let { appId ->
        val parsedAppId = appId.substringBefore(TYPE_REFERENCE_DELIMITER).trim()
        val patientRelatedResourceTypes = mutableListOf<ResourceType>()
        val compositionResource = fhirEngine.searchCompositionByIdentifier(parsedAppId)

        compositionResource?.let { composition ->
          Timber.d("Composition = ${jsonParser.encodeResourceToString(composition)}")
          composition
            .retrieveCompositionSections()
            .asSequence()
            .filter {
              it.hasFocus() && it.focus.hasReferenceElement()
            } // is focus.identifier a necessary check
            .groupBy { section ->
              section.focus.reference.substringBefore(
                TYPE_REFERENCE_DELIMITER,
                missingDelimiterValue = "",
              )
            }
            .filter { entry -> entry.key in FILTER_RESOURCE_LIST }
            .forEach { entry: Map.Entry<String, List<Composition.SectionComponent>> ->
              Timber.d("configSync Composition Content entry.key - ${entry.key}")
              if (entry.key == ResourceType.List.name) {
                if (isNonProxy()) {
                  val chunkedResourceIdList = entry.value.chunked(MANIFEST_PROCESSOR_BATCH_SIZE)
                  chunkedResourceIdList.forEach {
                    val resourceIds: List<String> =
                      it.map { sectionComponent -> sectionComponent.focus.extractId() }
                    val resourceType = entry.key
                    val compositionListRequestParamPairs = mutableListOf<Pair<ResourceType, Map<String, String>>>()
                    if (isNonProxy()) {
                      resourceIds.forEach {
                        Timber.d("configSync composition List entry.key ${entry.key} and id= ${it}")
                        compositionListRequestParamPairs.add(
                          Pair(
                            ResourceType.fromCode(resourceType),
                            mapOf(Composition.SP_RES_ID to it),
                          ),
                        )
                        syncParamSource.compListRequestQue.push(mapOf(*compositionListRequestParamPairs.toTypedArray()))
                        syncParamSource.compListRequestParamForItems.add(ResTypeId(ResourceType.fromCode(resourceType), it))
                      }
                    } else {
                      resourceIds.forEach {
                        Timber.d("configSync composition List entry.key ${entry.key} and id= ${it}")
                        compositionListRequestParamPairs.add(
                          Pair(
                            ResourceType.fromCode(resourceType),
                            mapOf(Composition.SP_RES_ID to it),
                          ),
                        )
                        syncParamSource.compListRequestQue.push(mapOf(*compositionListRequestParamPairs.toTypedArray()))
                        syncParamSource.compListRequestParamForItems.add(ResTypeId(ResourceType.fromCode(resourceType), it))
                      }
                    }
                  }

                  Timber.d("configSync compListRequestQue size ${syncParamSource.compListRequestQue.size}")
                  fetchCompositionListRequest(coroutineScope)

                } else {

                  entry.value.forEach {
                    val compositionManifestRequestParamPairs = mutableListOf<Pair<ResourceType, Map<String, String>>>()
                    Timber.d("configSync composition Manifest entry.key ${entry.key} and id= ${it.focus.extractId()}")
                    compositionManifestRequestParamPairs.add(
                      Pair(
                        ResourceType.fromCode(entry.key),
                        mapOf(Composition.SP_RES_ID to it.focus.extractId()),
                      ),
                    )
                    syncParamSource.compManifestRequestQue.push( mapOf(*compositionManifestRequestParamPairs.toTypedArray()))
                  }

                  Timber.d("configSync composition Manifest RequestQue size - ${syncParamSource.compManifestRequestQue.size}")
                  fetchCompositionManifestRequest(coroutineScope)

                }
              } else {
                Timber.d("configSync process compositionContent if Not LIST block")
                val chunkedResourceIdList = entry.value.chunked(MANIFEST_PROCESSOR_BATCH_SIZE)
                chunkedResourceIdList.forEach { parentIt ->
                  val resourceIds: List<String> =
                    parentIt.map { sectionComponent -> sectionComponent.focus.extractId() }
                  val resourceType = entry.key

                  resourceIds.forEach {
                    Timber.d("configSync composition Config adding resourceType - ${resourceType} and id= ${it}")
                    val compConfigParamPairsReq = mutableListOf<Pair<ResourceType, Map<String, String>>>()
                    compConfigParamPairsReq.add(
                      Pair(
                        ResourceType.fromCode(resourceType),
                        mapOf(Composition.SP_RES_ID to it),
                      ),
                    )
                    syncParamSource.compConfigRequestQue.push(mapOf(*compConfigParamPairsReq.toTypedArray()))
                  }

                  Timber.d("configSync composition Config RequestQue size - ${syncParamSource.compConfigRequestQue.size}")

                  fetchCompositionConfigRequest(coroutineScope)
                }
              }
            }
            saveSyncSharedPreferences(patientRelatedResourceTypes.toList())
        }
      }
    }
  }

  private suspend fun fetchCompositionManifestRequest(coroutineScope: CoroutineScope) {

    val compositionManifestFlow4 = Sync.oneTimeSync<CompositionManifestSyncWorker>(context)
    compositionManifestFlow4.handleCompositionManifestSyncJobStatus(coroutineScope)
    compositionManifestFlow4.collect{ compManifestJobStatus ->
      when(compManifestJobStatus) {
        is CurrentSyncJobStatus.Succeeded -> {
          Timber.d("configSync composition Manifest FlowJobStatus succeeded")
        } else -> {
        // do nothing Timber.d("#### compositionManifestFlow4 JobStatus other than succeeded")
        }
      }
    }
  }

  private suspend fun fetchCompositionListRequest(coroutineScope: CoroutineScope) {
    val compListFlow2 = Sync.oneTimeSync<CompositionListSyncWorker>(context)
    compListFlow2.handleCompositionListSyncJobStatus(coroutineScope)
    compListFlow2.collect{ compListJobStatus ->
      when(compListJobStatus) {
        is CurrentSyncJobStatus.Succeeded -> {
          Timber.d("configSync composition List FlowJobStatus succeeded")
          if(syncParamSource.compListRequestQue.isNotEmpty()){
            fetchCompositionListRequest(coroutineScope)
          } else {
            processCompositionListResult(coroutineScope)
          }
        } else -> {
        // do nothing Timber.d("#### compListJobStatus other than succeeded ")
        }
      }
    }
  }

  private suspend fun fetchCompositionConfigRequest(coroutineScope: CoroutineScope) {
    val compositionConfigFlow3 = Sync.oneTimeSync<CompositionConfigSyncWorker>(context)
    compositionConfigFlow3.handleCompositionConfigSyncJobStatus(coroutineScope)
    compositionConfigFlow3.collect{ compListJobStatus ->
      when(compListJobStatus) {
        is CurrentSyncJobStatus.Succeeded -> {
          Timber.d("configSync composition Config FlowJobStatus succeeded")
          if(syncParamSource.compConfigRequestQue.isNotEmpty()){
            fetchCompositionConfigRequest(coroutineScope)
          }
        } else -> {
        // do nothing Timber.d("#### compConfigFlow3 JobStatus other than succeeded // do nothing")
        }
      }
    }
  }

  private suspend fun processCompositionListResult(coroutineScope: CoroutineScope){
    Timber.e("configSync process result of compositionList for next resources call")
    val compositionListItemRequestParamPairs = mutableListOf<Pair<ResourceType, Map<String, String>>>()
    for (item in syncParamSource.compListRequestParamForItems) {
      val dbResource = fhirEngine.get(item.type, item.id)
      when(dbResource){
        is ListResource -> {
          dbResource.entry.forEach { listEntryComponent ->
            val resourceKey =
              listEntryComponent.item.reference.substringBefore(
                TYPE_REFERENCE_DELIMITER,
              )
            val resourceId = listEntryComponent.item.reference.extractLogicalIdUuid()
            val listResourceUrlPath = "$resourceKey?$ID=$resourceId&_count=$DEFAULT_COUNT"

            compositionListItemRequestParamPairs.add(
              Pair(
                ResourceType.fromCode(resourceKey),
                mapOf(ID to resourceId),
              ),
            )
            compositionListItemRequestParamPairs.add(
              Pair(
                ResourceType.fromCode(resourceKey),
                mapOf("_count" to "200"),
              ),
            )
            syncParamSource.compListItemRequestQue.push(mapOf(*compositionListItemRequestParamPairs.toTypedArray()))
          }
        } else -> {
          // do nothing
        }
      }
      Timber.d("configSync composition ListItemRequestQue size - ${syncParamSource.compConfigRequestQue.size}")
      fetchCompListItemRequest(coroutineScope)
    }
  }


  private suspend fun fetchCompListItemRequest(coroutineScope: CoroutineScope) {
    val compListFlow2 = Sync.oneTimeSync<CompListItemSyncWorker>(context)
    compListFlow2.handleCompListItemSyncJobStatus(coroutineScope)
    compListFlow2.collect{ compListJobStatus ->
      when(compListJobStatus) {
        is CurrentSyncJobStatus.Succeeded -> {
          Timber.d("configSync composition ListItemJobStatus succeeded")
          if(syncParamSource.compListRequestQue.isNotEmpty()){
            fetchCompListItemRequest(coroutineScope)
          } else {
            processCompListItemResult()
          }
        } else -> {
        // do nothing Timber.d("#### compListItemJobStatus other than succeeded ")
      }
      }
    }
  }

  private fun Flow<CurrentSyncJobStatus>.handleCompositionListSyncJobStatus(
    coroutineScope: CoroutineScope,
  ) {
    Timber.d("confRegistry handleCompositionListSyncJobStatus")
  }

  private fun Flow<CurrentSyncJobStatus>.handleCompositionConfigSyncJobStatus(
    coroutineScope: CoroutineScope,
  ) {
    Timber.d("confRegistry handleCompositionConfigSyncJobStatus")
  }

  private fun Flow<CurrentSyncJobStatus>.handleCompositionManifestSyncJobStatus(
    coroutineScope: CoroutineScope,
  ) {
    Timber.d("confRegistry handleCompositionManifestSyncJobStatus")
  }

  private fun Flow<CurrentSyncJobStatus>.handleCompListItemSyncJobStatus(
    coroutineScope: CoroutineScope,
  ) {
    Timber.d("confRegistry handleCompListItemSyncJobStatus")
  }

  private suspend fun processCompListItemResult(){
    Timber.e("confRegistry process result of composition List Items")
  }

}
