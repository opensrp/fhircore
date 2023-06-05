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

package org.smartregister.fhircore.quest.ui.tracing.register

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import com.google.android.fhir.sync.SyncJobStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.ceil
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.smartregister.fhircore.engine.appfeature.AppFeature
import org.smartregister.fhircore.engine.appfeature.AppFeatureManager
import org.smartregister.fhircore.engine.appfeature.model.HealthModule
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.AppConfigClassification
import org.smartregister.fhircore.engine.configuration.view.RegisterViewConfiguration
import org.smartregister.fhircore.engine.data.local.TracingRegisterFilter
import org.smartregister.fhircore.engine.data.local.register.AppRegisterRepository
import org.smartregister.fhircore.engine.sync.OnSyncListener
import org.smartregister.fhircore.engine.sync.SyncBroadcaster
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.data.patient.model.PatientPagingSourceState
import org.smartregister.fhircore.quest.data.register.RegisterPagingSource
import org.smartregister.fhircore.quest.data.register.RegisterPagingSource.Companion.DEFAULT_INITIAL_LOAD_SIZE
import org.smartregister.fhircore.quest.data.register.RegisterPagingSource.Companion.DEFAULT_PAGE_SIZE
import org.smartregister.fhircore.quest.navigation.MainNavigationScreen
import org.smartregister.fhircore.quest.navigation.NavigationArg
import org.smartregister.fhircore.quest.ui.StandardRegisterEvent
import org.smartregister.fhircore.quest.ui.StandardRegisterViewModel
import org.smartregister.fhircore.quest.ui.shared.models.RegisterViewData
import org.smartregister.fhircore.quest.util.REGISTER_FORM_ID_KEY
import org.smartregister.fhircore.quest.util.mappers.RegisterViewDataMapper
import org.smartregister.fhircore.quest.util.mappers.transformPatientCategoryToHealthStatus
import org.smartregister.fhircore.quest.util.mappers.transformToTracingAgeFilterEnum
import org.smartregister.fhircore.quest.util.mappers.transformTracingUiReasonToCode

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class TracingRegisterViewModel
@Inject
constructor(
  savedStateHandle: SavedStateHandle,
  syncBroadcaster: SyncBroadcaster,
  val registerRepository: AppRegisterRepository,
  val configurationRegistry: ConfigurationRegistry,
  val registerViewDataMapper: RegisterViewDataMapper,
  val appFeatureManager: AppFeatureManager,
  val sharedPreferencesHelper: SharedPreferencesHelper
) : ViewModel(), StandardRegisterViewModel {

  private val appFeatureName = savedStateHandle.get<String>(NavigationArg.FEATURE)
  private val healthModule =
    savedStateHandle.get<HealthModule>(NavigationArg.HEALTH_MODULE) ?: HealthModule.HOME_TRACING

  private val _isRefreshing = MutableStateFlow(false)

  override val isRefreshing: StateFlow<Boolean>
    get() = _isRefreshing.asStateFlow()

  private val _currentPage = MutableLiveData(0)
  override val currentPage: LiveData<Int>
    get() = _currentPage

  private val _searchText = MutableStateFlow("")
  override val searchText: StateFlow<String>
    get() = _searchText.asStateFlow()

  private val _refreshCounter = MutableStateFlow(0)
  val refreshCounter: StateFlow<Int>
    get() = _refreshCounter.asStateFlow()

  private val _totalRecordsCount = MutableLiveData(1L)

  override val paginatedRegisterData: MutableStateFlow<Flow<PagingData<RegisterViewData>>> =
    MutableStateFlow(emptyFlow())

  val paginatedRegisterDataForSearch: MutableStateFlow<Flow<PagingData<RegisterViewData>>> =
    MutableStateFlow(emptyFlow())

  private val _filtersMutableStateFlow: MutableStateFlow<TracingRegisterFilterState> =
    MutableStateFlow(TracingRegisterFilterState.default(healthModule))
  val filtersStateFlow: StateFlow<TracingRegisterFilterState> =
    _filtersMutableStateFlow.asStateFlow()

  override var registerViewConfiguration: RegisterViewConfiguration =
    configurationRegistry.retrieveConfiguration(AppConfigClassification.PATIENT_REGISTER)

  init {
    val searchFlow = _searchText.debounce(500)
    val pageFlow = _currentPage.asFlow().debounce(200)
    val registerFilterFlow =
      _filtersMutableStateFlow
        .map {
          val categories = transformPatientCategoryToHealthStatus(it.patientCategory.selected)
          val ageFilter = transformToTracingAgeFilterEnum(it)
          val reasonCode = transformTracingUiReasonToCode(it.reason.selected)
          TracingRegisterFilter(
            patientCategory = categories,
            isAssignedToMe = it.patientAssignment.selected.assignedToMe(),
            age = ageFilter,
            reasonCode = reasonCode
          )
        }
        .onEach { resetPage() }
    viewModelScope.launch {
      combine(searchFlow, pageFlow, registerFilterFlow, refreshCounter) { s, p, f, _ ->
        Triple(s, p, f)
      }
        .mapLatest {
          val pagingFlow =
            if (it.first.isNotBlank()) {
              filterRegisterDataFlow(text = it.first)
            } else {
              paginateRegisterDataFlow(page = it.second, filters = it.third)
            }

          return@mapLatest pagingFlow.onEach { _isRefreshing.emit(false) }
        }
        .collect { value -> paginatedRegisterData.emit(value.cachedIn(viewModelScope)) }
    }
    viewModelScope.launch {
      combine(searchFlow, pageFlow, registerFilterFlow, refreshCounter) { s, p, f, _ ->
        Triple(s, p, f)
      }
        .filter { it.first.isBlank() }
        .mapLatest {
          registerRepository.countRegisterFiltered(appFeatureName, healthModule, filters = it.third)
        }
        .collect { _totalRecordsCount.postValue(it) }
    }

    viewModelScope.launch { registerFilterFlow.collect { paginateRegisterDataForSearch(it) } }

    val syncStateListener =
      object : OnSyncListener {
        override fun onSync(state: SyncJobStatus) {
          val isStateCompleted = state is SyncJobStatus.Failed || state is SyncJobStatus.Finished
          if (isStateCompleted) {
            refresh()
          }
        }
      }
    syncBroadcaster.registerSyncListener(syncStateListener, viewModelScope)
  }

  private fun resetPage() {
    _currentPage.value = 0
  }

  override fun refresh() {
    _isRefreshing.value = true
    _refreshCounter.value += 1
  }

  fun isAppFeatureHousehold() =
    appFeatureName.equals(AppFeature.HouseholdManagement.name, ignoreCase = true)

  fun paginateRegisterDataFlow(filters: TracingRegisterFilter, page: Int) =
    getPager(appFeatureName, loadAll = false, page = page, registerFilters = filters).flow

  fun filterRegisterDataFlow(text: String) =
    paginatedRegisterDataForSearch.value.map { pagingData: PagingData<RegisterViewData> ->
      pagingData.filter {
        it.title.contains(text, ignoreCase = true) ||
          it.identifier.contains(text, ignoreCase = true)
      }
    }

  fun paginateRegisterDataForSearch(filters: TracingRegisterFilter) {
    paginatedRegisterDataForSearch.value =
      getPager(appFeatureName, true, registerFilters = filters).flow.cachedIn(viewModelScope)
  }

  private fun getPager(
    appFeatureName: String?,
    loadAll: Boolean = false,
    page: Int = 0,
    registerFilters: TracingRegisterFilter
  ): Pager<Int, RegisterViewData> =
    Pager(
      config =
        PagingConfig(
          pageSize = DEFAULT_PAGE_SIZE,
          initialLoadSize = DEFAULT_INITIAL_LOAD_SIZE,
          enablePlaceholders = false
        ),
      pagingSourceFactory = {
        RegisterPagingSource(registerRepository, registerViewDataMapper).apply {
          setPatientPagingSourceState(
            PatientPagingSourceState(
              appFeatureName = appFeatureName,
              healthModule = healthModule,
              loadAll = loadAll,
              currentPage = if (loadAll) 0 else page,
              filters = registerFilters
            )
          )
        }
      }
    )

  override fun countPages() =
    _totalRecordsCount.map { it.toDouble().div(DEFAULT_PAGE_SIZE) }.map { ceil(it).toInt() }

  override fun onEvent(event: StandardRegisterEvent) {
    when (event) {
      // Search using name or patient logicalId or identifier. Modify to add more search params
      is StandardRegisterEvent.SearchRegister -> {
        _searchText.value = event.searchText
      }
      is StandardRegisterEvent.MoveToNextPage -> {
        this._currentPage.value = this._currentPage.value?.plus(1)
      }
      is StandardRegisterEvent.MoveToPreviousPage -> {
        this._currentPage.value?.let { if (it > 0) _currentPage.value = it.minus(1) }
      }
      is StandardRegisterEvent.OpenProfile -> {
        val urlParams =
          NavigationArg.bindArgumentsOf(
            Pair(NavigationArg.FEATURE, AppFeature.PatientManagement.name),
            Pair(NavigationArg.HEALTH_MODULE, healthModule),
            Pair(NavigationArg.PATIENT_ID, event.patientId)
          )
        if (healthModule == HealthModule.FAMILY)
          event.navController.navigate(route = MainNavigationScreen.FamilyProfile.route + urlParams)
        else
          event.navController.navigate(
            route = MainNavigationScreen.TracingProfile.route + urlParams
          )
      }
      is StandardRegisterEvent.ApplyFilter<*> -> {
        val newFilterState = event.filterState as TracingRegisterFilterState
        _filtersMutableStateFlow.update { newFilterState }
      }
    }
  }

  fun isRegisterFormViaSettingExists(): Boolean {
    return appFeatureManager.appFeatureHasSetting(REGISTER_FORM_ID_KEY)
  }

  fun isFirstTimeSync() = sharedPreferencesHelper.read(SharedPreferenceKey.LAST_SYNC_TIMESTAMP.name, null).isNullOrBlank()

  override fun progressMessage() =
    if (searchText.value.isEmpty()) {
      ""
    } else {
      configurationRegistry.context.resources.getString(R.string.search_progress_message)
    }
}

data class TracingRegisterFilterState(
  val patientAssignment: TracingRegisterUiFilter<TracingPatientAssignment>,
  val patientCategory: TracingRegisterUiFilter<TracingPatientCategory>,
  val reason: TracingRegisterUiFilter<TracingReason>,
  val age: TracingRegisterUiFilter<AgeFilter>
) {
  companion object {
    fun default(healthModule: HealthModule) =
      when (healthModule) {
        HealthModule.PHONE_TRACING ->
          TracingRegisterFilterState(
            patientAssignment =
              TracingRegisterUiFilter(
                PhonePatientAssignment.ALL_PHONE,
                PhonePatientAssignment.values().asList()
              ),
            patientCategory =
              TracingRegisterUiFilter(
                TracingPatientCategory.ALL_PATIENT_CATEGORIES,
                TracingPatientCategory.values().asList()
              ),
            reason =
              TracingRegisterUiFilter(TracingReason.ALL_REASONS, TracingReason.values().asList()),
            age = TracingRegisterUiFilter(AgeFilter.ALL_AGES, AgeFilter.values().asList())
          )
        HealthModule.HOME_TRACING ->
          TracingRegisterFilterState(
            patientAssignment =
              TracingRegisterUiFilter(
                HomePatientAssignment.ALL_HOME,
                HomePatientAssignment.values().asList()
              ),
            patientCategory =
              TracingRegisterUiFilter(
                TracingPatientCategory.ALL_PATIENT_CATEGORIES,
                TracingPatientCategory.values().asList()
              ),
            reason =
              TracingRegisterUiFilter(TracingReason.ALL_REASONS, TracingReason.values().asList()),
            age = TracingRegisterUiFilter(AgeFilter.ALL_AGES, AgeFilter.values().asList())
          )
        else -> throw IllegalArgumentException("Not allowed")
      }
  }
}
