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

package org.smartregister.fhircore.quest.ui.appsetting

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.net.UnknownHostException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import okhttp3.RequestBody.Companion.toRequestBody
import org.hl7.fhir.r4.model.Binary
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Composition
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.jetbrains.annotations.VisibleForTesting
import org.smartregister.fhircore.engine.BuildConfig
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry.Companion.DEBUG_SUFFIX
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.configuration.customsearch.SearchParametersConfigService
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.di.NetworkModule
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.decodeResourceFromString
import org.smartregister.fhircore.engine.util.extension.encodeResourceToString
import org.smartregister.fhircore.engine.util.extension.extractId
import org.smartregister.fhircore.engine.util.extension.getActivity
import org.smartregister.fhircore.engine.util.extension.launchActivityWithNoBackStackHistory
import org.smartregister.fhircore.engine.util.extension.retrieveCompositionSectionsExcludingCustomSearchParameters
import org.smartregister.fhircore.engine.util.extension.retrieveCustomSearchParametersSection
import org.smartregister.fhircore.engine.util.extension.retrieveImplementationGuideDefinitionResources
import org.smartregister.fhircore.engine.util.extension.sectionDataReference
import org.smartregister.fhircore.quest.ui.login.LoginActivity
import retrofit2.HttpException
import timber.log.Timber

typealias QuestBuildConfig = org.smartregister.fhircore.quest.BuildConfig

@HiltViewModel
class AppSettingViewModel
@Inject
constructor(
  val fhirResourceDataSource: FhirResourceDataSource,
  val defaultRepository: DefaultRepository,
  val sharedPreferencesHelper: SharedPreferencesHelper,
  val configService: ConfigService,
  val configurationRegistry: ConfigurationRegistry,
  val dispatcherProvider: DispatcherProvider,
  val customSearchParameterService: SearchParametersConfigService,
) : ViewModel() {

  private var _isNonProxy = BuildConfig.IS_NON_PROXY_APK
  private val exceptionHandler = CoroutineExceptionHandler { _, exception -> Timber.e(exception) }

  val showProgressBar = MutableLiveData(false)

  private val _appId = MutableLiveData("")
  val appId
    get() = _appId

  private val _error = MutableLiveData("")
  val error: LiveData<String>
    get() = _error

  fun onApplicationIdChanged(appId: String) {
    _appId.value = appId
    _error.value = ""
  }

  /**
   * Fetch the [Composition] resource whose identifier matches the provided [appId]. Save the
   * composition resource and all the nested resources referenced in the
   * [Composition.SectionComponent].
   */
  fun fetchConfigurations(context: Context) {
    showProgressBar.postValue(true)
    val appId = appId.value?.trim()
    if (!appId.isNullOrEmpty()) {
      when {
        hasDebugSuffix() -> loadConfigurations(context)
        else -> fetchRemoteConfigurations(appId, context)
      }
    }
  }

  private fun fetchRemoteConfigurations(appId: String?, context: Context) {
    viewModelScope.launch(exceptionHandler) {
      try {
        showProgressBar.postValue(true)

        Timber.i(
          "Fetching configs for app $appId with highest context-quantity ${QuestBuildConfig.VERSION_CODE}",
        )

        val compositionResource: Composition?

        val implementationGuideResource =
          configurationRegistry.fetchRemoteImplementationGuideByAppId(
            appId,
            QuestBuildConfig.VERSION_CODE,
          )

        compositionResource =
          if (implementationGuideResource != null) {
            configurationRegistry.addOrUpdate(implementationGuideResource)

            val compositionReference =
              implementationGuideResource
                .retrieveImplementationGuideDefinitionResources()[0]
                .reference
                .reference

            val compositionIdWithHistory = compositionReference?.substringAfter('/')
            val compositionId = compositionIdWithHistory?.substringBefore('/')
            val compositionVersion = compositionIdWithHistory?.substringAfterLast('/', "")

            configurationRegistry.fetchRemoteCompositionById(compositionId, compositionVersion)
          } else {
            configurationRegistry.fetchRemoteCompositionByAppId(appId)
          }

        if (compositionResource == null) {
          showProgressBar.postValue(false)
          _error.postValue(context.getString(R.string.application_not_supported, appId?.trim()))
          return@launch
        }

        // Save composition
        defaultRepository.createRemote(false, compositionResource)

        val appConfigSectionReferences =
          compositionResource
            .retrieveCompositionSectionsExcludingCustomSearchParameters()
            .asSequence()
            .flatMap { it.sectionDataReference() }
            .groupBy(::referenceResourceTypeString)
            .filterKeys(::isReferenceToAppConfig)
        appConfigSectionReferences.download { resultBundle ->
          val resources = resultBundle.entry.mapNotNull { it.resource }
          defaultRepository.createRemote(false, *resources.toTypedArray())
        }

        fetchSearchParameterBundle(compositionResource.retrieveCustomSearchParametersSection())

        Timber.d("Done fetching application configurations remotely")
        loadConfigurations(context)
      } catch (unknownHostException: UnknownHostException) {
        _error.postValue(context.getString(R.string.error_loading_config_no_internet))
        showProgressBar.postValue(false)
      } catch (httpException: HttpException) {
        if ((400..503).contains(httpException.response()!!.code())) {
          _error.postValue(context.getString(R.string.error_loading_config_general))
        } else {
          _error.postValue(context.getString(R.string.error_loading_config_http_error))
        }
        showProgressBar.postValue(false)
      }
    }
  }

  suspend fun fetchComposition(urlPath: String, context: Context): Composition? {
    return fhirResourceDataSource.getResource(urlPath).entryFirstRep.let {
      if (!it.hasResource()) {
        Timber.w("No response for composition resource on path $urlPath")
        showProgressBar.postValue(false)
        _error.postValue(context.getString(R.string.application_not_supported, appId.value?.trim()))
        return null
      }

      it.resource as Composition
    }
  }

  fun loadConfigurations(context: Context) {
    appId.value?.trim()?.let { thisAppId ->
      viewModelScope.launch(dispatcherProvider.io()) {
        configurationRegistry.loadConfigurations(thisAppId, context) { loadConfigSuccessful ->
          showProgressBar.postValue(false)
          if (loadConfigSuccessful) {
            sharedPreferencesHelper.write(SharedPreferenceKey.APP_ID.name, thisAppId)
            context.getActivity()?.launchActivityWithNoBackStackHistory<LoginActivity>()
          } else {
            _error.postValue(context.getString(R.string.application_not_supported, thisAppId))
          }
        }
      }
    }
  }

  fun hasDebugSuffix(): Boolean =
    appId.value?.trim()?.endsWith(DEBUG_SUFFIX, ignoreCase = true) == true && isDebugVariant()

  @VisibleForTesting fun isDebugVariant() = BuildConfig.DEBUG

  private suspend fun fetchSearchParameterBundle(
    searchParameterCompositionSection: List<Composition.SectionComponent>,
  ): Bundle? {
    val searchParameterBinary =
      searchParameterCompositionSection
        .flatMap { it.sectionDataReference() }
        .groupBy(::referenceResourceTypeString)
        .download()
        .flatMap { bundle -> bundle.entry.mapNotNull { it.resource as? Binary } }

    val searchParameterBundle =
      searchParameterBinary
        .map { it.content.decodeToString() }
        .map { it.decodeResourceFromString<Bundle>() }
        .filter { it.entry.isNotEmpty() }
        .reduceOrNull { acc, bundle -> acc.apply { entry = entry + bundle.entry } }

    return searchParameterBundle?.also { customSearchParameterService.saveBundle(it) }
  }

  private suspend inline fun <V : Iterable<Reference>> Map<String, V>.download(
    onSuccess: (Bundle) -> Unit = {},
  ): List<Bundle> {
    return entries
      .flatMap { (key, value) ->
        value.chunked(ConfigurationRegistry.MANIFEST_PROCESSOR_BATCH_SIZE) { key to it }
      }
      .map { (resourceType, references) ->
        downloadConfigResourceReferencesWithRetry(resourceType, references)
      }
      .onEach(onSuccess)
  }

  private suspend fun downloadConfigResourceReferencesWithRetry(
    resourceType: String,
    references: Iterable<Reference>,
  ): Bundle {
    val downloadResources = suspend { downloadConfigResourceReferences(resourceType, references) }
    return kotlin
      .runCatching { downloadResources() }
      .getOrElse {
        // retry
        downloadResources()
      }
  }

  private suspend fun downloadConfigResourceReferences(
    resourceType: String,
    references: Iterable<Reference>,
  ): Bundle {
    return if (isNonProxy()) {
      fhirResourceDataSourceGetBundle(
        resourceType,
        references.map { it.extractId() },
      )
    } else {
      fhirResourceDataSource.post(
        requestBody =
          generateRequestBundle(resourceType, references.map { it.extractId() })
            .encodeResourceToString()
            .toRequestBody(NetworkModule.JSON_MEDIA_TYPE),
      )
    }
  }

  private fun referenceResourceTypeString(reference: Reference) =
    reference.reference.substringBefore(
      ConfigurationRegistry.TYPE_REFERENCE_DELIMITER,
      missingDelimiterValue = "",
    )

  private fun isReferenceToAppConfig(reference: String) =
    reference == ResourceType.Binary.name || reference == ResourceType.Parameters.name

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
      val responseBundle =
        fhirResourceDataSource.getResource("$resourceType?${Resource.SP_RES_ID}=$it")
      responseBundle.let {
        bundleEntryComponents.add(
          Bundle.BundleEntryComponent().apply {
            resource = responseBundle.entry?.firstOrNull()?.resource
          },
        )
      }
    }
    return Bundle().apply {
      type = Bundle.BundleType.COLLECTION
      entry = bundleEntryComponents
    }
  }

  @VisibleForTesting fun isNonProxy(): Boolean = _isNonProxy

  @VisibleForTesting
  fun setNonProxy(nonProxy: Boolean) {
    _isNonProxy = nonProxy
  }
}
