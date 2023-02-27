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

package org.smartregister.fhircore.quest.ui.register

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.register.RegisterConfiguration
import org.smartregister.fhircore.engine.data.local.register.RegisterRepository
import org.smartregister.fhircore.engine.domain.model.ActionParameter
import org.smartregister.fhircore.engine.domain.model.ActionParameterType
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.domain.model.SnackBarMessageConfig
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.quest.data.register.RegisterPagingSource
import org.smartregister.fhircore.quest.data.register.model.RegisterPagingSourceState

@HiltViewModel
class RegisterViewModel
@Inject
constructor(
  val registerRepository: RegisterRepository,
  val configurationRegistry: ConfigurationRegistry,
  val sharedPreferencesHelper: SharedPreferencesHelper,
  val dispatcherProvider: DispatcherProvider
) : ViewModel() {

  private val _snackBarStateFlow = MutableSharedFlow<SnackBarMessageConfig>()
  val snackBarStateFlow = _snackBarStateFlow.asSharedFlow()

  val registerUiState = mutableStateOf(RegisterUiState())

  val currentPage: MutableState<Int> = mutableStateOf(0)

  val searchText = mutableStateOf("")

  val paginatedRegisterData: MutableStateFlow<Flow<PagingData<ResourceData>>> =
    MutableStateFlow(emptyFlow())

  val pagesDataCache = mutableMapOf<Int, Flow<PagingData<ResourceData>>>()

  private val _totalRecordsCount = mutableStateOf(0L)

  private lateinit var registerConfiguration: RegisterConfiguration

  private var allPatientRegisterData: Flow<PagingData<ResourceData>>? = null

  /**
   * This function paginates the register data. An optional [clearCache] resets the data in the
   * cache (this is necessary after a questionnaire has been submitted to refresh the register with
   * new/updated data).
   */
  fun paginateRegisterData(
    registerId: String,
    loadAll: Boolean = false,
    clearCache: Boolean = false
  ) {
    if (clearCache) {
      pagesDataCache.clear()
      allPatientRegisterData = null
    }
    paginatedRegisterData.value =
      pagesDataCache.getOrPut(currentPage.value) {
        getPager(registerId, loadAll).flow.cachedIn(viewModelScope)
      }
  }

  private fun getPager(registerId: String, loadAll: Boolean = false): Pager<Int, ResourceData> {
    // Get the configured page size from RegisterConfiguration default is 20
    val pageSize = retrieveRegisterConfiguration(registerId).pageSize

    return Pager(
      config = PagingConfig(pageSize = pageSize, enablePlaceholders = false),
      pagingSourceFactory = {
        RegisterPagingSource(registerRepository).apply {
          setPatientPagingSourceState(
            RegisterPagingSourceState(
              registerId = registerId,
              loadAll = loadAll,
              currentPage = if (loadAll) 0 else currentPage.value
            )
          )
        }
      }
    )
  }

  fun retrieveRegisterConfiguration(
    registerId: String,
    paramMap: Map<String, String>? = emptyMap()
  ): RegisterConfiguration {
    // Ensures register configuration is initialized once
    if (!::registerConfiguration.isInitialized) {
      registerConfiguration =
        configurationRegistry.retrieveConfiguration(ConfigType.Register, registerId, paramMap)
    }
    return registerConfiguration
  }

  private fun retrieveAllPatientRegisterData(registerId: String): Flow<PagingData<ResourceData>> {
    // Ensure that we only initialize this flow once
    if (allPatientRegisterData == null) {
      allPatientRegisterData = getPager(registerId, true).flow.cachedIn(viewModelScope)
    }
    return allPatientRegisterData!!
  }

  fun onEvent(event: RegisterEvent) =
    when (event) {
      // Search using name or patient logicalId or identifier. Modify to add more search params
      is RegisterEvent.SearchRegister -> {
        searchText.value = event.searchText
        if (event.searchText.isEmpty()) paginateRegisterData(registerUiState.value.registerId)
        else filterRegisterData(event)
      }
      is RegisterEvent.MoveToNextPage -> {
        currentPage.value = currentPage.value.plus(1)
        paginateRegisterData(registerUiState.value.registerId)
      }
      is RegisterEvent.MoveToPreviousPage -> {
        currentPage.value.let { if (it > 0) currentPage.value = it.minus(1) }
        paginateRegisterData(registerUiState.value.registerId)
      }
    }

  fun filterRegisterData(event: RegisterEvent.SearchRegister) {
    val searchBar = registerUiState.value.registerConfiguration?.searchBar
    // computedRules (names of pre-computed rules) must be provided for search to work.
    if (searchBar?.computedRules != null) {
      paginatedRegisterData.value =
        retrieveAllPatientRegisterData(registerUiState.value.registerId).map {
          pagingData: PagingData<ResourceData> ->
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

  fun retrieveRegisterUiState(
    registerId: String,
    screenTitle: String,
    paramsList: Array<ActionParameter>?
  ) {
    if (registerId.isNotEmpty()) {
      var filterParams: List<ActionParameter>? = null
      if (paramsList != null) {
        filterParams =
          paramsList.filter {
            it.paramType == ActionParameterType.DATAPASS && !it.value.isNullOrEmpty()
          }
      }
      val paramsMap = mapOf<String, String>()
      filterParams?.forEach { paramsMap[it.key] to it.value }

      viewModelScope.launch(dispatcherProvider.io()) {
        val currentRegisterConfiguration = retrieveRegisterConfiguration(registerId, paramsMap)
        // Count register data then paginate the data
        _totalRecordsCount.value = registerRepository.countRegisterData(registerId, paramsMap)
        paginateRegisterData(registerId, loadAll = false)

        registerUiState.value =
          RegisterUiState(
            screenTitle = currentRegisterConfiguration.registerTitle ?: screenTitle,
            isFirstTimeSync =
              sharedPreferencesHelper
                .read(SharedPreferenceKey.LAST_SYNC_TIMESTAMP.name, null)
                .isNullOrEmpty(),
            registerConfiguration = currentRegisterConfiguration,
            registerId = registerId,
            totalRecordsCount = _totalRecordsCount.value,
            pagesCount =
              ceil(
                  _totalRecordsCount
                    .value
                    .toDouble()
                    .div(currentRegisterConfiguration.pageSize.toLong())
                )
                .toInt()
          )
      }
    }
  }

  suspend fun emitSnackBarState(snackBarMessageConfig: SnackBarMessageConfig) {
    _snackBarStateFlow.emit(snackBarMessageConfig)
  }
}
