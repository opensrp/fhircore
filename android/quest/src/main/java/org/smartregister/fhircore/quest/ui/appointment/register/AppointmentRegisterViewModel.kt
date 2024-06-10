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

package org.smartregister.fhircore.quest.ui.appointment.register

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import androidx.paging.map
import com.google.android.fhir.sync.SyncJobStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import org.apache.commons.lang3.time.DateUtils
import org.smartregister.fhircore.engine.appfeature.AppFeature
import org.smartregister.fhircore.engine.appfeature.model.HealthModule
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.data.local.AppointmentRegisterFilter
import org.smartregister.fhircore.engine.data.local.register.AppRegisterRepository
import org.smartregister.fhircore.engine.domain.util.PaginationConstant
import org.smartregister.fhircore.engine.sync.OnSyncListener
import org.smartregister.fhircore.engine.sync.SyncBroadcaster
import org.smartregister.fhircore.engine.ui.filter.DateFilterOption
import org.smartregister.fhircore.engine.ui.filter.FilterOption
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.data.patient.model.PatientPagingSourceState
import org.smartregister.fhircore.quest.data.register.RegisterPagingSource
import org.smartregister.fhircore.quest.navigation.MainNavigationScreen
import org.smartregister.fhircore.quest.navigation.NavigationArg
import org.smartregister.fhircore.quest.ui.StandardRegisterEvent
import org.smartregister.fhircore.quest.ui.StandardRegisterViewModel
import org.smartregister.fhircore.quest.ui.shared.models.RegisterViewData
import org.smartregister.fhircore.quest.util.mappers.RegisterViewDataMapper
import org.smartregister.fhircore.quest.util.mappers.transformAppointmentUiReasonToCode
import org.smartregister.fhircore.quest.util.mappers.transformPatientCategoryToHealthStatus

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class AppointmentRegisterViewModel
@Inject
constructor(
  savedStateHandle: SavedStateHandle,
  syncBroadcaster: SyncBroadcaster,
  val registerRepository: AppRegisterRepository,
  val configurationRegistry: ConfigurationRegistry,
  val dispatcherProvider: DispatcherProvider,
  private val registerViewDataMapper: RegisterViewDataMapper,
) : ViewModel(), StandardRegisterViewModel {
  private val appFeatureName = savedStateHandle.get<String>(NavigationArg.FEATURE)
  private val healthModule =
    savedStateHandle.get<HealthModule>(NavigationArg.HEALTH_MODULE) ?: HealthModule.APPOINTMENT

  private val _isRefreshing = MutableStateFlow(false)

  override val isRefreshing: StateFlow<Boolean>
    get() = _isRefreshing.asStateFlow()

  private val _currentPage = MutableStateFlow(0)
  override val currentPage: StateFlow<Int>
    get() = _currentPage

  private val _searchText = MutableStateFlow("")
  override val searchText: StateFlow<String>
    get() = _searchText.asStateFlow()

  private val _refreshCounter = MutableStateFlow(0)
  val refreshCounter: StateFlow<Int>
    get() = _refreshCounter.asStateFlow()

  private val _paginatedRegisterData: MutableStateFlow<Flow<PagingData<RegisterViewData>>> =
    MutableStateFlow(emptyFlow())

  override val pageRegisterListItemData:
    StateFlow<Flow<PagingData<RegisterViewData.ListItemView>>> =
    _paginatedRegisterData
      .map { pagingDataFlow ->
        pagingDataFlow.map { pagingData ->
          pagingData
            .filter { it is RegisterViewData.ListItemView }
            .map { it as RegisterViewData.ListItemView }
        }
      }
      .stateIn(
        viewModelScope.plus(dispatcherProvider.io()),
        SharingStarted.Lazily,
        initialValue = emptyFlow(),
      )

  override val pageNavigationItemViewData:
    StateFlow<Flow<PagingData<RegisterViewData.PageNavigationItemView>>> =
    _paginatedRegisterData
      .map { pagingDataFlow ->
        pagingDataFlow.map { pagingData ->
          pagingData
            .filter { it is RegisterViewData.PageNavigationItemView }
            .map { it as RegisterViewData.PageNavigationItemView }
        }
      }
      .stateIn(
        viewModelScope.plus(dispatcherProvider.io()),
        SharingStarted.Lazily,
        initialValue = emptyFlow(),
      )

  private val paginatedRegisterDataForSearch: MutableStateFlow<Flow<PagingData<RegisterViewData>>> =
    MutableStateFlow(emptyFlow())

  private val _filtersMutableStateFlow: MutableStateFlow<AppointmentFilterState> =
    MutableStateFlow(AppointmentFilterState.default())
  val filtersStateFlow: StateFlow<AppointmentFilterState> = _filtersMutableStateFlow.asStateFlow()

  init {
    val searchFlow = _searchText.debounce(500)
    val registerFilterFlow =
      _filtersMutableStateFlow
        .map {
          val categories = transformPatientCategoryToHealthStatus(it.patientCategory.selected)
          val reason = transformAppointmentUiReasonToCode(it.reason.selected)
          AppointmentRegisterFilter(
            dateOfAppointment = it.date.value,
            myPatients = it.patients.selected == PatientAssignment.MY_PATIENTS,
            patientCategory = categories,
            reasonCode = reason,
          )
        }
        .onEach { resetPage() }
    val refreshCounterFlow = refreshCounter

    viewModelScope.launch(dispatcherProvider.io()) {
      combine(searchFlow, _currentPage, registerFilterFlow, refreshCounterFlow) { s, p, f, _ ->
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
        .collect { value -> _paginatedRegisterData.emit(value.cachedIn(viewModelScope)) }
    }

    viewModelScope.launch(dispatcherProvider.io()) {
      registerFilterFlow.collect { paginateRegisterDataForSearch(it) }
    }

    val syncStateListener = OnSyncListener { state ->
      val isStateCompleted = state is SyncJobStatus.Failed || state is SyncJobStatus.Succeeded
      if (isStateCompleted) {
        refresh()
      }
    }
    syncBroadcaster.registerSyncListener(syncStateListener, viewModelScope)
  }

  override fun refresh() {
    _isRefreshing.value = true
    _refreshCounter.value += 1
  }

  private fun resetPage() {
    _currentPage.value = 0
  }

  private fun paginateRegisterDataFlow(filters: AppointmentRegisterFilter, page: Int) =
    getPager(appFeatureName, loadAll = false, page = page, registerFilters = filters).flow

  private fun filterRegisterDataFlow(text: String) =
    paginatedRegisterDataForSearch.value.map { pagingData: PagingData<RegisterViewData> ->
      pagingData
        .filter { it is RegisterViewData.ListItemView }
        .filter {
          it as RegisterViewData.ListItemView
          it.title.contains(text, ignoreCase = true) ||
            it.identifier.contains(text, ignoreCase = true)
        }
    }

  private fun paginateRegisterDataForSearch(filters: AppointmentRegisterFilter) {
    paginatedRegisterDataForSearch.value =
      getPager(appFeatureName, true, registerFilters = filters).flow.cachedIn(viewModelScope)
  }

  private fun getPager(
    appFeatureName: String?,
    loadAll: Boolean = false,
    page: Int = 0,
    registerFilters: AppointmentRegisterFilter,
  ): Pager<Int, RegisterViewData> =
    Pager(
      config =
        PagingConfig(
          pageSize = PaginationConstant.DEFAULT_PAGE_SIZE,
          initialLoadSize = PaginationConstant.DEFAULT_INITIAL_LOAD_SIZE,
          enablePlaceholders = false,
        ),
      pagingSourceFactory = {
        RegisterPagingSource(registerRepository, registerViewDataMapper, dispatcherProvider).apply {
          setPatientPagingSourceState(
            PatientPagingSourceState(
              appFeatureName = appFeatureName,
              healthModule = healthModule,
              loadAll = loadAll,
              currentPage = if (loadAll) 0 else page,
              filters = registerFilters,
            ),
          )
        }
      },
    )

  override fun onEvent(event: StandardRegisterEvent) {
    when (event) {
      // Search using name or patient logicalId or identifier. Modify to add more search params
      is StandardRegisterEvent.SearchRegister -> {
        _searchText.value = event.searchText
      }
      is StandardRegisterEvent.MoveToNextPage -> {
        this._currentPage.update { it + 1 }
      }
      is StandardRegisterEvent.MoveToPreviousPage -> {
        this._currentPage.update { if (it > 0) it - 1 else it }
      }
      is StandardRegisterEvent.ApplyFilter<*> -> {
        val newFilterState = event.filterState as AppointmentFilterState
        _filtersMutableStateFlow.update { newFilterState }
      }
      is StandardRegisterEvent.OpenProfile -> {
        val urlParams =
          NavigationArg.bindArgumentsOf(
            Pair(NavigationArg.FEATURE, AppFeature.PatientManagement.name),
            Pair(NavigationArg.HEALTH_MODULE, HealthModule.HIV),
            Pair(NavigationArg.PATIENT_ID, event.patientId),
          )
        event.navController.navigate(
          route = MainNavigationScreen.PatientProfile.route + urlParams,
        )
      }
    }
  }

  override fun progressMessage() =
    if (searchText.value.isEmpty()) {
      ""
    } else {
      configurationRegistry.context.resources.getString(R.string.search_progress_message)
    }

  fun clearFilters() {
    _filtersMutableStateFlow.update { AppointmentFilterState.default() }
  }
}

data class AppointmentFilterState(
  val date: DateFilterOption,
  val patients: AppointmentFilter<PatientAssignment>,
  val patientCategory: AppointmentFilter<PatientCategory>,
  val reason: AppointmentFilter<Reason>,
) {
  fun toFilterList(): List<FilterOption> {
    val activeFilters: MutableList<FilterOption> = mutableListOf()
    val defaultState = default()
    if (patients.selected != defaultState.patients.selected) {
      activeFilters.add(patients.selected)
    }
    if (patientCategory.selected != defaultState.patientCategory.selected) {
      activeFilters.add(patientCategory.selected)
    }
    if (reason.selected != defaultState.reason.selected) {
      activeFilters.add(reason.selected)
    }
    if (!DateUtils.isSameDay(date.value, defaultState.date.value)) {
      activeFilters.add(date)
    }
    return activeFilters
  }

  companion object {
    fun default() =
      AppointmentFilterState(
        date = DateFilterOption(Date()),
        patients =
          AppointmentFilter(PatientAssignment.ALL_PATIENTS, PatientAssignment.values().asList()),
        patientCategory =
          AppointmentFilter(
            PatientCategory.ALL_PATIENT_CATEGORIES,
            PatientCategory.values().asList(),
          ),
        reason = AppointmentFilter(Reason.ALL_REASONS, Reason.values().asList()),
      )
  }
}
