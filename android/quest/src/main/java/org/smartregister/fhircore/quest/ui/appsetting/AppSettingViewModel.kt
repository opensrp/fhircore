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

package org.smartregister.fhircore.quest.ui.appsetting

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.net.UnknownHostException
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.RequestBody.Companion.toRequestBody
import okio.ByteString.Companion.decodeBase64
import org.apache.commons.lang3.StringUtils
import org.hl7.fhir.r4.model.Binary
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Composition
import org.hl7.fhir.r4.model.ResourceType
import org.jetbrains.annotations.VisibleForTesting
import org.smartregister.fhircore.engine.BuildConfig
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry.Companion.DEBUG_SUFFIX
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.configuration.profile.ProfileConfiguration
import org.smartregister.fhircore.engine.configuration.register.RegisterConfiguration
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.di.NetworkModule
import org.smartregister.fhircore.engine.domain.model.FhirResourceConfig
import org.smartregister.fhircore.engine.domain.model.ResourceConfig
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.encodeResourceToString
import org.smartregister.fhircore.engine.util.extension.extractId
import org.smartregister.fhircore.engine.util.extension.getActivity
import org.smartregister.fhircore.engine.util.extension.launchActivityWithNoBackStackHistory
import org.smartregister.fhircore.engine.util.extension.retrieveCompositionSections
import org.smartregister.fhircore.engine.util.extension.tryDecodeJson
import org.smartregister.fhircore.quest.ui.login.LoginActivity
import retrofit2.HttpException
import timber.log.Timber

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
) : ViewModel() {

  private var _isNonProxy = BuildConfig.IS_NON_PROXY_APK

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
    val appId = appId.value
    if (!appId.isNullOrEmpty()) {
      when {
        hasDebugSuffix() -> loadConfigurations(context)
        else -> fetchRemoteConfigurations(appId, context)
      }
    }
  }

  private fun fetchRemoteConfigurations(appId: String?, context: Context) {
    viewModelScope.launch {
      try {
        Timber.i("Fetching configs for app $appId")
        val urlPath =
          "${ResourceType.Composition.name}?${Composition.SP_IDENTIFIER}=$appId&_count=${ConfigurationRegistry.DEFAULT_COUNT}"
        val compositionResource =
          withContext(dispatcherProvider.io()) { fetchComposition(urlPath, context) }
            ?: return@launch

        val patientRelatedResourceTypes = mutableListOf<ResourceType>()
        compositionResource
          .retrieveCompositionSections()
          .asSequence()
          .filter { it.hasFocus() && it.focus.hasReferenceElement() && it.focus.hasIdentifier() }
          .groupBy {
            it.focus.reference.substringBefore(ConfigurationRegistry.TYPE_REFERENCE_DELIMITER)
          }
          .filter { it.key == ResourceType.Binary.name || it.key == ResourceType.Parameters.name }
          .forEach { entry: Map.Entry<String, List<Composition.SectionComponent>> ->
            val chunkedResourceIdList =
              entry.value.chunked(ConfigurationRegistry.MANIFEST_PROCESSOR_BATCH_SIZE)
            chunkedResourceIdList.forEach { parentIt ->
              Timber.d(
                "Fetching config resource ${entry.key}: with ids ${StringUtils.join(parentIt,",")}",
              )

              val resultBundle: Bundle =
                if (isNonProxy()) {
                  fhirResourceDataSourceGetBundle(
                    entry.key,
                    parentIt.map { it.focus.extractId() },
                  )
                } else
                  fhirResourceDataSource.post(
                    requestBody =
                      generateRequestBundle(entry.key, parentIt.map { it.focus.extractId() })
                        .encodeResourceToString()
                        .toRequestBody(NetworkModule.JSON_MEDIA_TYPE),
                  )

              resultBundle.entry.forEach { bundleEntryComponent ->
                if (bundleEntryComponent.resource != null) {
                  defaultRepository.createRemote(false, bundleEntryComponent.resource)

                  if (bundleEntryComponent.resource is Binary) {
                    val binary = bundleEntryComponent.resource as Binary
                    binary.data
                      .decodeToString()
                      .decodeBase64()
                      ?.string(StandardCharsets.UTF_8)
                      ?.let {
                        val config =
                          it.tryDecodeJson<RegisterConfiguration>()
                            ?: it.tryDecodeJson<ProfileConfiguration>()

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
                }
              }
            }
          }

        saveSyncSharedPreferences(patientRelatedResourceTypes.toList())

        // Save composition after fetching all the referenced section resources
        defaultRepository.createRemote(false, compositionResource)
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
        _error.postValue(context.getString(R.string.application_not_supported, appId.value))
        return null
      }

      it.resource as Composition
    }
  }

  fun loadConfigurations(context: Context) {
    appId.value?.let { thisAppId ->
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

  fun saveSyncSharedPreferences(resourceTypes: List<ResourceType>) =
    sharedPreferencesHelper.write(
      SharedPreferenceKey.REMOTE_SYNC_RESOURCES.name,
      resourceTypes.distinctBy { it.name },
    )

  private fun FhirResourceConfig.dependentResourceTypes(target: MutableList<ResourceType>) {
    this.baseResource.dependentResourceTypes(target)
    this.relatedResources.forEach { it.dependentResourceTypes(target) }
  }

  private fun ResourceConfig.dependentResourceTypes(target: MutableList<ResourceType>) {
    target.add(resource)
    relatedResources.forEach { it.dependentResourceTypes(target) }
  }

  fun hasDebugSuffix(): Boolean =
    appId.value?.endsWith(DEBUG_SUFFIX, ignoreCase = true) == true && isDebugVariant()

  @VisibleForTesting fun isDebugVariant() = BuildConfig.DEBUG

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
        fhirResourceDataSource.getResource("$resourceType?${Composition.SP_RES_ID}=$it")
      responseBundle?.let {
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
