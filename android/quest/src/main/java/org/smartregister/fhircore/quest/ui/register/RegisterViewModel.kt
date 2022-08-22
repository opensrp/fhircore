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

package org.smartregister.fhircore.quest.ui.register

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.ceil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.register.RegisterConfiguration
import org.smartregister.fhircore.engine.data.local.register.RegisterRepository
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.util.LAST_SYNC_TIMESTAMP
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.quest.data.register.RegisterPagingSource
import org.smartregister.fhircore.quest.data.register.RegisterPagingSource.Companion.DEFAULT_PAGE_SIZE
import org.smartregister.fhircore.quest.data.register.model.RegisterPagingSourceState
import org.smartregister.fhircore.quest.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.quest.util.extensions.launchQuestionnaire

@HiltViewModel
class RegisterViewModel
@Inject
constructor(
  val registerRepository: RegisterRepository,
  val configurationRegistry: ConfigurationRegistry,
  val sharedPreferencesHelper: SharedPreferencesHelper,
) : ViewModel() {

  private val _currentPage = MutableLiveData(0)
  val currentPage
    get() = _currentPage

  private val _searchText = mutableStateOf("")
  val searchText: androidx.compose.runtime.State<String>
    get() = _searchText

  private val _totalRecordsCount = MutableLiveData(1L)

  private lateinit var registerConfiguration: RegisterConfiguration

  private lateinit var allPatientRegisterData: Flow<PagingData<ResourceData>>

  val paginatedRegisterData: MutableStateFlow<Flow<PagingData<ResourceData>>> =
    MutableStateFlow(emptyFlow())

  fun paginateRegisterData(registerId: String, loadAll: Boolean = false) {
    paginatedRegisterData.value = getPager(registerId, loadAll).flow.cachedIn(viewModelScope)
  }

  private fun getPager(registerId: String, loadAll: Boolean = false): Pager<Int, ResourceData> =
    Pager(
      config = PagingConfig(pageSize = DEFAULT_PAGE_SIZE, enablePlaceholders = false),
      pagingSourceFactory = {
        RegisterPagingSource(registerRepository).apply {
          setPatientPagingSourceState(
            RegisterPagingSourceState(
              registerId = registerId,
              loadAll = loadAll,
              currentPage = if (loadAll) 0 else currentPage.value!!
            )
          )
        }
      }
    )

  fun setTotalRecordsCount(registerId: String) {
    viewModelScope.launch {
      _totalRecordsCount.postValue(registerRepository.countRegisterData(registerId))
    }
  }

  fun retrieveRegisterConfiguration(registerId: String): RegisterConfiguration {
    // Ensures register configuration is initialized once
    if (!::registerConfiguration.isInitialized) {
      registerConfiguration =
        configurationRegistry.retrieveConfiguration(ConfigType.Register, registerId)
    }
    return registerConfiguration
  }

  private fun retrieveAllPatientRegisterData(registerId: String): Flow<PagingData<ResourceData>> {
    // Ensure that we only initialize this flow once
    if (!::allPatientRegisterData.isInitialized) {
      allPatientRegisterData = getPager(registerId, true).flow.cachedIn(viewModelScope)
    }
    return allPatientRegisterData
  }

  fun countPages(): Int =
    _totalRecordsCount.value?.toDouble()?.div(DEFAULT_PAGE_SIZE.toLong())?.let { ceil(it).toInt() }
      ?: 1

  fun onEvent(event: RegisterEvent) =
    when (event) {
      // Search using name or patient logicalId or identifier. Modify to add more search params
      is RegisterEvent.SearchRegister -> {
        _searchText.value = event.searchText
        if (event.searchText.isEmpty()) paginateRegisterData(event.registerId)
        else filterRegisterData(event)
      }
      is RegisterEvent.MoveToNextPage -> {
        this._currentPage.value = this._currentPage.value?.plus(1)
        paginateRegisterData(event.registerId)
      }
      is RegisterEvent.MoveToPreviousPage -> {
        this._currentPage.value?.let { if (it > 0) _currentPage.value = it.minus(1) }
        paginateRegisterData(event.registerId)
      }
      is RegisterEvent.RegisterNewClient ->
        event.context.launchQuestionnaire<QuestionnaireActivity>(
          // TODO use appropriate property from the register configuration
          "provide-questionnaire-id"
        )
      is RegisterEvent.OnViewComponentEvent ->
        event.viewComponentEvent.handleEvent(event.navController)
    }

  private fun filterRegisterData(event: RegisterEvent.SearchRegister) {
    val searchBar = retrieveRegisterConfiguration(event.registerId).searchBar
    // computedRules (names of pre-computed rules) must be provided for search to work.
    if (searchBar?.computedRules != null) {
      paginatedRegisterData.value =
        retrieveAllPatientRegisterData(event.registerId).map { pagingData: PagingData<ResourceData>
          ->
          pagingData.filter { resourceData: ResourceData ->
            searchBar.computedRules!!.any { ruleName ->
              // if ruleName not found in map return {-1}; check always return false hence no data
              val value = resourceData.computedValuesMap[ruleName]?.toString() ?: "{-1}"
              value.contains(other = event.searchText, ignoreCase = true)
            }
          }
        }
    }
  }

  // TODO this setting should be removed after refactor
  fun isRegisterFormViaSettingExists(): Boolean {
    return false
  }

  fun isFirstTimeSync() = sharedPreferencesHelper.read(LAST_SYNC_TIMESTAMP, null).isNullOrEmpty()
}
