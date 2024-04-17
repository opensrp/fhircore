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

package org.smartregister.fhircore.engine.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.WorkerParameters
import ca.uhn.fhir.context.FhirContext
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.knowledge.KnowledgeManager
import com.google.android.fhir.sync.AcceptLocalConflictResolver
import com.google.android.fhir.sync.ConflictResolver
import com.google.android.fhir.sync.DownloadWorkManager
import com.google.android.fhir.sync.FhirSyncWorker
import com.google.android.fhir.sync.upload.UploadStrategy
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.hl7.fhir.r4.model.Composition
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.BuildConfig
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import javax.inject.Inject

@HiltWorker
class ConfigSyncWorker
@AssistedInject
constructor(
  @Assisted appContext: Context,
  @Assisted workerParams: WorkerParameters,
  private val openSrpFhirEngine: FhirEngine,
  private val appTimeStampContext: AppTimeStampContext,
  //private val configCacheMap: MutableMap<String, Configuration> = mutableMapOf<String, Configuration>(),
  val sharedPreferencesHelper: SharedPreferencesHelper,
  //val dispatcherProvider: DispatcherProvider,
  //val fhirEngine: FhirEngine,
  //@ApplicationContext val context: Context,
  //private var openSrpApplication: OpenSrpApplication?,
) : FhirSyncWorker(appContext, workerParams) {

  private val fhirContext = FhirContext.forR4Cached()
  @Inject lateinit var knowledgeManager: KnowledgeManager
  private val jsonParser = fhirContext.newJsonParser()
  private var _isNonProxy = BuildConfig.IS_NON_PROXY_APK

  override fun getConflictResolver(): ConflictResolver = AcceptLocalConflictResolver

  override fun getDownloadWorkManager(): DownloadWorkManager =
    OpenSrpDownloadManager(
      syncParams = loadConfigSyncParams(),
      context = appTimeStampContext,
    )

  override fun getFhirEngine(): FhirEngine = openSrpFhirEngine

  override fun getUploadStrategy(): UploadStrategy = UploadStrategy.AllChangesSquashedBundlePut

  fun loadConfigSyncParams(): Map<ResourceType, Map<String, String>> {

    val pairs = mutableListOf<Pair<ResourceType, Map<String, String>>>()

    pairs.add(
      Pair(
        ResourceType.StructureMap,
        mapOf("_count" to "37"),
      ),
    )

      // val urlPath =
      // "${ResourceType.Composition.name}?${Composition.SP_IDENTIFIER}=$appId&_count=${ConfigurationRegistry.DEFAULT_COUNT}"
    sharedPreferencesHelper.read(SharedPreferenceKey.APP_ID.name, null)?.let { appId ->
      val parsedAppId = appId.substringBefore(ConfigurationRegistry.TYPE_REFERENCE_DELIMITER).trim()
      pairs.add(
        Pair(
          ResourceType.Composition,
          mapOf(Composition.SP_IDENTIFIER to parsedAppId),
        ),
      )
      pairs.add(
        Pair(
          ResourceType.Composition,
          mapOf("_count" to ConfigurationRegistry.DEFAULT_COUNT.toString()),
        ),
      )
    }

    // GET /StructureMap?_count=37
    return mapOf(*pairs.toTypedArray())
  }

//
//  fun loadConfigSyncParamsWithIds(): Map<ResourceType, Map<String, String>> {
//
//    // val pairs = mutableListOf<Pair<ResourceType, Map<String, String>>>()
//
//    // Reset configurations before loading new ones
//    configCacheMap.clear()
//    sharedPreferencesHelper.read(SharedPreferenceKey.APP_ID.name, null)?.let { appId ->
//      val parsedAppId = appId.substringBefore(ConfigurationRegistry.TYPE_REFERENCE_DELIMITER).trim()
//
//      //val urlPath =
//      //        "${ResourceType.Composition.name}?${Composition.SP_IDENTIFIER}=$appId&_count=${ConfigurationRegistry.DEFAULT_COUNT}"
//      pairs.add(
//        Pair(
//          ResourceType.Composition,
//          mapOf(Composition.SP_IDENTIFIER to parsedAppId),
//        ),
//      )
//      pairs.add(
//        Pair(
//          ResourceType.Composition,
//          mapOf("_count" to ConfigurationRegistry.DEFAULT_COUNT.toString()),
//        ),
//      )
//
//      val patientRelatedResourceTypes = mutableListOf<ResourceType>()
//      val compositionResource = fetchRemoteComposition(parsedAppId)
//      compositionResource?.let { composition ->
//        composition
//          .retrieveCompositionSections()
//          .asSequence()
//          .filter {
//            it.hasFocus() && it.focus.hasReferenceElement()
//          } // is focus.identifier a necessary check
//          .groupBy { section ->
//            section.focus.reference.substringBefore(
//              ConfigurationRegistry.TYPE_REFERENCE_DELIMITER,
//              missingDelimiterValue = "",
//            )
//          }
//          .filter { entry -> entry.key in ConfigurationRegistry.FILTER_RESOURCE_LIST }
//          .forEach { entry: Map.Entry<String, List<Composition.SectionComponent>> ->
//            if (entry.key == ResourceType.List.name) {
//              processCompositionListResources(
//                entry,
//                patientRelatedResourceTypes = patientRelatedResourceTypes,
//              )
//            } else {
//              val chunkedResourceIdList =
//                entry.value.chunked(ConfigurationRegistry.MANIFEST_PROCESSOR_BATCH_SIZE)
//
//              chunkedResourceIdList.forEach { parentIt ->
//                Timber.d(
//                  "Fetching config resource ${entry.key}: with ids ${
//                    StringUtils.join(
//                      parentIt,
//                      ","
//                    )
//                  }",
//                )
//                processCompositionManifestResources(
//                  entry.key,
//                  parentIt.map { sectionComponent -> sectionComponent.focus.extractId() },
//                  patientRelatedResourceTypes,
//                )
//              }
//            }
//          }
//
//        saveSyncSharedPreferences(patientRelatedResourceTypes.toList())
//
//        // Save composition after fetching all the referenced section resources
//        addOrUpdate(compositionResource)
//
//        Timber.d("Done fetching application configurations remotely")
//      }
//    }
//
//
//    pairs.add(
//    Pair(
//    ResourceType.StructureMap,
//    mapOf("_count" to "37"),
//    ),
//    )
//
//
//
//    // GET /StructureMap?_count=37
//
//    return mapOf(*pairs.toTypedArray())
//  }
//
//
//  //to change
//  suspend fun fetchRemoteComposition(appId: String?): Composition? {
//    Timber.i("Fetching configs for app $appId")
//    val urlPath =
//      "${ResourceType.Composition.name}?${Composition.SP_IDENTIFIER}=$appId&_count=${ConfigurationRegistry.DEFAULT_COUNT}"
//
//    return fhirResourceDataSource.getResource(urlPath).entryFirstRep.let {
//      if (!it.hasResource()) {
//        Timber.w("No response for composition resource on path $urlPath")
//        return null
//      }
//
//      it.resource as Composition
//    }
//  }
//
//
//  fun saveSyncSharedPreferences(resourceTypes: List<ResourceType>) =
//    sharedPreferencesHelper.write(
//      SharedPreferenceKey.REMOTE_SYNC_RESOURCES.name,
//      resourceTypes.distinctBy { it.name },
//    )
//
//
//  /**
//   * Update this stored resources with the passed resource, or create it if not found. If the
//   * resource is a Metadata Resource save it in the Knowledge Manager
//   *
//   * Note
//   */
//  suspend fun <R : Resource> addOrUpdate(resource: R) {
//    withContext(dispatcherProvider.io()) {
//      try {
//        createOrUpdateRemote(resource)
//      } catch (sqlException: SQLException) {
//        Timber.e(sqlException)
//      }
//
//      /**
//       * Knowledge manager [MetadataResource]s install Here we install all resources types of
//       * [MetadataResource] as per FHIR Spec.This supports future use cases as well
//       */
//
//      /**
//       * Knowledge manager [MetadataResource]s install Here we install all resources types of
//       * [MetadataResource] as per FHIR Spec.This supports future use cases as well
//       */
//      try {
//        if (resource is MetadataResource && resource.name != null) {
//          knowledgeManager.install(
//            writeToFile(resource.overwriteCanonicalURL()),
//          )
//        }
//      } catch (exception: Exception) {
//        Timber.e(exception)
//      }
//    }
//  }
//
//
//  /**
//   * Using this [FhirEngine] and [DispatcherProvider], for all passed resources, make sure they all
//   * have IDs or generate if they don't, then pass them to create.
//   *
//   * Note: The backing db API for fhirEngine.create(..,isLocalOnly) performs an UPSERT
//   *
//   * @param resources vararg of resources
//   */
//  suspend fun createOrUpdateRemote(vararg resources: Resource) {
//    return withContext(dispatcherProvider.io()) {
//      resources.onEach {
//        it.updateLastUpdated()
//        it.generateMissingId()
//      }
//      fhirEngine.create(*resources, isLocalOnly = true)
//    }
//  }
//
//
//  fun writeToFile(resource: Resource): File {
//    val fileName =
//      if (resource is MetadataResource && resource.name != null) {
//        resource.name
//      } else {
//        resource.idElement.idPart
//      }
//
//    return File(context.filesDir, "$fileName.json").apply {
//      writeText(jsonParser.encodeResourceToString(resource))
//    }
//  }
//
//
//  private fun MetadataResource.overwriteCanonicalURL() =
//    this.apply {
//      url =
//        url
//          ?: "${openSrpApplication?.getFhirServerHost().toString()?.trimEnd { it == '/' }}/${this.referenceValue()}"
//    }
//
//
//
//  private suspend fun processCompositionListResources(
//    resourceGroup:
//    Map.Entry<
//            String,
//            List<Composition.SectionComponent>,
//            >,
//    patientRelatedResourceTypes: MutableList<ResourceType>,
//  ) {
//    if (BuildConfig.IS_NON_PROXY_APK) {
//      val chunkedResourceIdList = resourceGroup.value.chunked(ConfigurationRegistry.MANIFEST_PROCESSOR_BATCH_SIZE)
//      chunkedResourceIdList.forEach {
//        processCompositionManifestResources(
//          resourceType = resourceGroup.key,
//          resourceIdList = it.map { sectionComponent -> sectionComponent.focus.extractId() },
//          patientRelatedResourceTypes = patientRelatedResourceTypes,
//        )
//          .entry
//          .forEach { bundleEntryComponent ->
//            when (bundleEntryComponent.resource) {
//              is ListResource -> {
//                addOrUpdate(bundleEntryComponent.resource)
//                val list = bundleEntryComponent.resource as ListResource
//                list.entry.forEach { listEntryComponent ->
//                  val resourceKey =
//                    listEntryComponent.item.reference.substringBefore(
//                      ConfigurationRegistry.TYPE_REFERENCE_DELIMITER,
//                    )
//                  val resourceId = listEntryComponent.item.reference.extractLogicalIdUuid()
//                  val listResourceUrlPath = "$resourceKey?${ConfigurationRegistry.ID}=$resourceId&_count=${ConfigurationRegistry.DEFAULT_COUNT}"
//                  // retrofit call
//                  fhirResourceDataSource.getResource(listResourceUrlPath).entry.forEach {
//                      listEntryResourceBundle ->
//                    addOrUpdate(listEntryResourceBundle.resource)
//                    Timber.d("Fetched and processed List reference $listResourceUrlPath")
//                  }
//                }
//              }
//            }
//          }
//      }
//    } else {
//      resourceGroup.value.forEach {
//        processCompositionManifestResources(
//          gatewayModeHeaderValue = ConfigurationRegistry.FHIR_GATEWAY_MODE_HEADER_VALUE,
//          searchPath = "${resourceGroup.key}/${it.focus.extractId()}",
//          patientRelatedResourceTypes = patientRelatedResourceTypes,
//        )
//      }
//    }
//  }
//
//
//  // change Retrofit
//  private suspend fun processCompositionManifestResources(
//    resourceType: String,
//    resourceIdList: List<String>,
//    patientRelatedResourceTypes: MutableList<ResourceType>,
//  ): Bundle {
//    val resultBundle =
//      if (BuildConfig.IS_NON_PROXY_APK) {
//        fhirResourceDataSourceGetBundle(resourceType, resourceIdList)
//      } else
//        fhirResourceDataSource.post(
//          requestBody =
//          generateRequestBundle(resourceType, resourceIdList)
//            .encodeResourceToString()
//            .toRequestBody(NetworkModule.JSON_MEDIA_TYPE),
//        )
//
//    processResultBundleEntries(resultBundle, patientRelatedResourceTypes)
//
//    return resultBundle
//  }
//
//  private suspend fun processCompositionManifestResources(
//    gatewayModeHeaderValue: String? = null,
//    searchPath: String,
//    patientRelatedResourceTypes: MutableList<ResourceType>,
//  ) {
//    val resultBundle =
//      if (gatewayModeHeaderValue.isNullOrEmpty()) {
//        fhirResourceDataSource.getResource(searchPath)
//      } else
//        fhirResourceDataSource.getResourceWithGatewayModeHeader(
//          gatewayModeHeaderValue,
//          searchPath,
//        )
//
//    processResultBundleEntries(resultBundle, patientRelatedResourceTypes)
//  }
//
//  private suspend fun processResultBundleEntries(
//    resultBundle: Bundle,
//    patientRelatedResourceTypes: MutableList<ResourceType>,
//  ) {
//    resultBundle.entry?.forEach { bundleEntryComponent ->
//      when (bundleEntryComponent.resource) {
//        is Bundle -> {
//          val bundle = bundleEntryComponent.resource as Bundle
//          bundle.entry.forEach { entryComponent ->
//            when (entryComponent.resource) {
//              is Bundle -> {
//                val thisBundle = entryComponent.resource as Bundle
//                addOrUpdate(thisBundle)
//                thisBundle.entry.forEach { innerEntryComponent ->
//                  saveListEntryResource(innerEntryComponent)
//                }
//              }
//              else -> saveListEntryResource(entryComponent)
//            }
//          }
//        }
//        is Binary -> {
//          val binary = bundleEntryComponent.resource as Binary
//          processResultBundleBinaries(binary, patientRelatedResourceTypes)
//          addOrUpdate(bundleEntryComponent.resource)
//        }
//        else -> {
//          if (bundleEntryComponent.resource != null) {
//            addOrUpdate(bundleEntryComponent.resource)
//            Timber.d(
//              "Fetched and processed resources ${bundleEntryComponent.resource.resourceType}/${bundleEntryComponent.resource.id}",
//            )
//          }
//        }
//      }
//    }
//  }
//
//
//  private suspend fun fhirResourceDataSourceGetBundle(
//    resourceType: String,
//    resourceIds: List<String>,
//  ): Bundle {
//    val bundleEntryComponents = mutableListOf<Bundle.BundleEntryComponent>()
//
//    resourceIds.forEach {
//      // retrofit call
//      val responseBundle =
//        fhirResourceDataSource.getResource("$resourceType?${Composition.SP_RES_ID}=$it")
//      responseBundle.let {
//        bundleEntryComponents.add(
//          Bundle.BundleEntryComponent().apply { resource = it.entry?.firstOrNull()?.resource },
//        )
//      }
//    }
//    return Bundle().apply {
//      type = Bundle.BundleType.COLLECTION
//      entry = bundleEntryComponents
//    }
//  }
//

}
