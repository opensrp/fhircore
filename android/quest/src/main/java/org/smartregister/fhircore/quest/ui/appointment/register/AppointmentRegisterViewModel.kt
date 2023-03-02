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
import java.util.Date
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
import kotlinx.coroutines.launch
import org.smartregister.fhircore.engine.appfeature.model.HealthModule
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.AppConfigClassification
import org.smartregister.fhircore.engine.configuration.view.RegisterViewConfiguration
import org.smartregister.fhircore.engine.data.local.register.AppRegisterRepository
import org.smartregister.fhircore.engine.sync.OnSyncListener
import org.smartregister.fhircore.engine.sync.SyncBroadcaster
import org.smartregister.fhircore.engine.util.extension.capitalizeFirstLetter
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.data.patient.model.PatientPagingSourceState
import org.smartregister.fhircore.quest.data.register.RegisterPagingSource
import org.smartregister.fhircore.quest.navigation.NavigationArg
import org.smartregister.fhircore.quest.ui.StandardRegisterEvent
import org.smartregister.fhircore.quest.ui.StandardRegisterViewModel
import org.smartregister.fhircore.quest.ui.shared.models.RegisterViewData
import org.smartregister.fhircore.quest.util.mappers.RegisterViewDataMapper

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class AppointmentRegisterViewModel
@Inject
constructor(
  savedStateHandle: SavedStateHandle,
  syncBroadcaster: SyncBroadcaster,
  val registerRepository: AppRegisterRepository,
  val configurationRegistry: ConfigurationRegistry,
  private val registerViewDataMapper: RegisterViewDataMapper
) : ViewModel(), StandardRegisterViewModel {
  private val appFeatureName = savedStateHandle.get<String>(NavigationArg.FEATURE)
  private val healthModule =
    savedStateHandle.get<HealthModule>(NavigationArg.HEALTH_MODULE) ?: HealthModule.APPOINTMENT

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

  override var registerViewConfiguration: RegisterViewConfiguration =
    configurationRegistry.retrieveConfiguration(AppConfigClassification.PATIENT_REGISTER)

  init {
    val searchFlow = _searchText.debounce(500)
    val pageFlow = _currentPage.asFlow().debounce(200)
    val refreshCounterFlow = _refreshCounter
    viewModelScope.launch {
      combine(searchFlow, pageFlow, refreshCounterFlow) { s, p, r -> Triple(s, p, r) }
        .mapLatest {
          val pagingFlow =
            if (it.first.isNotBlank()) {
              filterRegisterDataFlow(text = it.first)
            } else {
              paginateRegisterDataFlow(page = it.second)
            }

          return@mapLatest pagingFlow.onEach { _isRefreshing.emit(false) }
        }
        .collect { value -> paginatedRegisterData.emit(value.cachedIn(viewModelScope)) }
    }
    viewModelScope.launch {
      combine(searchFlow, pageFlow, refreshCounterFlow) { s, p, r -> Triple(s, p, r) }
        .filter { it.first.isBlank() }
        .mapLatest { registerRepository.countRegisterData(appFeatureName, healthModule) }
        .collect { _totalRecordsCount.postValue(it) }
    }

    viewModelScope.launch { paginateRegisterDataForSearch() }

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

  override fun refresh() {
    _isRefreshing.value = true
    _refreshCounter.value += 1
  }

  private fun paginateRegisterDataFlow(page: Int) =
    getPager(appFeatureName, loadAll = false, page = page).flow

  private fun filterRegisterDataFlow(text: String) =
    paginatedRegisterDataForSearch.value.map { pagingData: PagingData<RegisterViewData> ->
      pagingData.filter {
        it.title.contains(text, ignoreCase = true) ||
          it.identifier.contains(text, ignoreCase = true)
      }
    }

  private fun paginateRegisterDataForSearch() {
    paginatedRegisterDataForSearch.value =
      getPager(appFeatureName, true).flow.cachedIn(viewModelScope)
  }

  private fun getPager(
    appFeatureName: String?,
    loadAll: Boolean = false,
    page: Int = 0
  ): Pager<Int, RegisterViewData> =
    Pager(
      config =
        PagingConfig(
          pageSize = RegisterPagingSource.DEFAULT_PAGE_SIZE,
          initialLoadSize = RegisterPagingSource.DEFAULT_INITIAL_LOAD_SIZE,
          enablePlaceholders = false
        ),
      pagingSourceFactory = {
        RegisterPagingSource(registerRepository, registerViewDataMapper).apply {
          setPatientPagingSourceState(
            PatientPagingSourceState(
              appFeatureName = appFeatureName,
              healthModule = healthModule,
              loadAll = loadAll,
              currentPage = if (loadAll) 0 else page
            )
          )
        }
      }
    )

  override fun countPages() =
    _totalRecordsCount.map { it.toDouble().div(RegisterPagingSource.DEFAULT_PAGE_SIZE) }.map {
      ceil(it).toInt()
    }

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
        // No-op
      }
    }
  }

  override fun progressMessage() =
    if (searchText.value.isEmpty()) {
      ""
    } else {
      configurationRegistry.context.resources.getString(R.string.search_progress_message)
    }
}

enum class PatientsFilter {
  ALL_PATIENTS,
  MY_PATIENTS;

  override fun toString(): String =
    super.toString().split("_").joinToString { it.capitalizeFirstLetter() }
}

enum class PatientCategory {
  ALL_PATIENT_CATEGORIES,
  ART_CLIENT,
  EXPOSED_INFANT,
  CHILD_CONTACT,
  PERSON_WHO_IS_REACTIVE_AT_THE_COMMUNITY,
  SEXUAL_CONTACT;

  override fun toString(): String =
    super.toString().split("_").joinToString { it.capitalizeFirstLetter() }
}

enum class AppointmentReason(val patientCategory: Array<PatientCategory>) {
  ALL_REASONS(PatientCategory.values()),
  CERVICAL_CANCER_SCREENING(arrayOf(PatientCategory.ART_CLIENT)),
  DBS_POSITIVE(arrayOf(PatientCategory.EXPOSED_INFANT)),
  HIV_TEST(
    arrayOf(
      PatientCategory.CHILD_CONTACT,
      PatientCategory.SEXUAL_CONTACT,
      PatientCategory.PERSON_WHO_IS_REACTIVE_AT_THE_COMMUNITY
    )
  ),
  INDEX_CASE_TESTING(arrayOf(PatientCategory.ART_CLIENT)),
  MILESTONE_HIV_TEST(arrayOf(PatientCategory.EXPOSED_INFANT)),
  LINKAGE(arrayOf(PatientCategory.ART_CLIENT)),
  REFILL(arrayOf(PatientCategory.ART_CLIENT)),
  ROUTINE_VISIT(arrayOf(PatientCategory.EXPOSED_INFANT)),
  VIRAL_LOAD_COLLECTION(arrayOf(PatientCategory.ART_CLIENT)),
  WELCOME_SERVICE(arrayOf(PatientCategory.ART_CLIENT)),
  WELCOME_SERVICE_FOLLOW_UP(arrayOf(PatientCategory.ART_CLIENT));

  override fun toString(): String =
    super.toString().split("_").joinToString { it.capitalizeFirstLetter() }
}

data class AppointmentsFilterObject(
  val date: Date,
  val patients: PatientsFilter,
  val patientCategory: PatientCategory,
  val reason: AppointmentReason
)
