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

package org.smartregister.fhircore.engine.ui.appsetting

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.fhir.knowledge.FhirNpmPackage
import com.google.android.fhir.knowledge.KnowledgeManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.context.SimpleWorkerContext
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.data.remote.config.ConfigRepository
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.di.CoreModule
import org.smartregister.fhircore.engine.domain.util.DataLoadState
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import retrofit2.HttpException
import timber.log.Timber
import java.net.UnknownHostException
import javax.inject.Inject

@HiltViewModel
class AppSettingViewModel
@Inject
constructor(
    val fhirResourceDataSource: FhirResourceDataSource,
    val defaultRepository: DefaultRepository,
    val sharedPreferencesHelper: SharedPreferencesHelper,
    val configurationRegistry: ConfigurationRegistry,
    val dispatcherProvider: DispatcherProvider,
    private val configRepository: ConfigRepository,
    private val knowledgeManager: KnowledgeManager,
    private val simpleWorkerContext: SimpleWorkerContext
) : ViewModel() {

    private val _loadState = MutableLiveData<DataLoadState<Boolean>?>()
    val loadState = _loadState

    fun loadConfigurations() {
        viewModelScope.launch (Dispatchers.IO) {
            try {
                val loaded = configurationRegistry.loadConfigurations()
                if (loaded) {
                    installPackages()
                    loadPackages()
                    _loadState.postValue(DataLoadState.Success(data = true))
                } else {
                    fetchRemoteConfigurations()
                }
            } catch (e: Exception) {
                Timber.e(e)
                _loadState.postValue(DataLoadState.Error(ConfigurationErrorException()))
            }
        }
    }
private suspend fun loadPackages() {
   val npmPackages = knowledgeManager.getNpmPackageManagers()
    for (npmPackage in npmPackages) {
        simpleWorkerContext.loadFromPackage(npmPackage, null)
    }
}
   private suspend fun installPackages() {
       try {
           val npmPackage = FhirNpmPackage(CoreModule.HL7_FHIR_PACKAGE, CoreModule.HL7_FHIR_PACKAGE_VERSION)
           if (knowledgeManager.isPackageInstalled(npmPackage)) {
               return
           }
           _loadState.postValue(DataLoadState.Loading)
           knowledgeManager.install(npmPackage)
       } catch (e: Exception) {
           Timber.e(e)
           _loadState.postValue(DataLoadState.Error(ConfigurationErrorException()))
       }
    }

    fun fetchRemoteConfigurations() {
        viewModelScope.launch {
            try {
                _loadState.postValue(DataLoadState.Loading)
                configRepository.fetchConfigFromRemote()
                loadConfigurations()
            } catch (unknownHostException: UnknownHostException) {
                _loadState.postValue(DataLoadState.Error(InternetConnectionException()))
            } catch (httpException: HttpException) {
                if ((400..503).contains(httpException.response()!!.code())) {
                    _loadState.postValue(DataLoadState.Error(ServerException()))
                } else {
                    _loadState.postValue(DataLoadState.Error(ConfigurationErrorException()))
                }
            } catch (e: Exception) {
                _loadState.postValue(DataLoadState.Error(ConfigurationErrorException()))
            }
        }
    }
}
