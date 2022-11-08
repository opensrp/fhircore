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

package org.smartregister.fhircore.quest.ui.patient.register

import android.content.Context
import android.content.Intent
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
import com.google.android.fhir.sync.State
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
import kotlinx.coroutines.launch
import org.smartregister.fhircore.engine.appfeature.AppFeature
import org.smartregister.fhircore.engine.appfeature.AppFeatureManager
import org.smartregister.fhircore.engine.appfeature.model.HealthModule
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.AppConfigClassification
import org.smartregister.fhircore.engine.configuration.view.RegisterViewConfiguration
import org.smartregister.fhircore.engine.data.local.register.AppRegisterRepository
import org.smartregister.fhircore.engine.sync.OnSyncListener
import org.smartregister.fhircore.engine.sync.SyncBroadcaster
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireType
import org.smartregister.fhircore.engine.util.LAST_SYNC_TIMESTAMP
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.data.patient.model.PatientPagingSourceState
import org.smartregister.fhircore.quest.data.register.RegisterPagingSource
import org.smartregister.fhircore.quest.data.register.RegisterPagingSource.Companion.DEFAULT_INITIAL_LOAD_SIZE
import org.smartregister.fhircore.quest.data.register.RegisterPagingSource.Companion.DEFAULT_PAGE_SIZE
import org.smartregister.fhircore.quest.navigation.MainNavigationScreen
import org.smartregister.fhircore.quest.navigation.NavigationArg
import org.smartregister.fhircore.quest.ui.shared.models.RegisterViewData
import org.smartregister.fhircore.quest.util.REGISTER_FORM_ID_KEY
import org.smartregister.fhircore.quest.util.mappers.RegisterViewDataMapper

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class PatientRegisterViewModel
@Inject
constructor(
  savedStateHandle: SavedStateHandle,
  syncBroadcaster: SyncBroadcaster,
  val registerRepository: AppRegisterRepository,
  val configurationRegistry: ConfigurationRegistry,
  val registerViewDataMapper: RegisterViewDataMapper,
  val appFeatureManager: AppFeatureManager,
  val sharedPreferencesHelper: SharedPreferencesHelper
) : ViewModel() {

  private val appFeatureName = savedStateHandle.get<String>(NavigationArg.FEATURE)
  private val healthModule =
    savedStateHandle.get<HealthModule>(NavigationArg.HEALTH_MODULE) ?: HealthModule.DEFAULT

  private val _isRefreshing = MutableStateFlow(false)

  val isRefreshing: StateFlow<Boolean>
    get() = _isRefreshing.asStateFlow()

  private val _currentPage = MutableLiveData(0)
  val currentPage
    get() = _currentPage

  private val _searchText = MutableStateFlow("")
  val searchText: StateFlow<String>
    get() = _searchText.asStateFlow()

  private val _refreshCounter = MutableStateFlow(0)
  val refreshCounter: StateFlow<Int>
    get() = _refreshCounter.asStateFlow()

  private val _firstTimeSyncState = MutableStateFlow(isFirstTimeSync())
  val firstTimeSyncState: StateFlow<Boolean>
    get() = _firstTimeSyncState.asStateFlow()

  private val _totalRecordsCount = MutableLiveData(1L)

  val paginatedRegisterData: MutableStateFlow<Flow<PagingData<RegisterViewData>>> =
    MutableStateFlow(emptyFlow())

  val paginatedRegisterDataForSearch: MutableStateFlow<Flow<PagingData<RegisterViewData>>> =
    MutableStateFlow(emptyFlow())

  var registerViewConfiguration: RegisterViewConfiguration
    private set

  init {
    registerViewConfiguration =
      configurationRegistry.retrieveConfiguration(AppConfigClassification.PATIENT_REGISTER)

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
        override fun onSync(state: State) {
          val isStateCompleted = state is State.Failed || state is State.Finished
          if (isStateCompleted) {
            refresh()
            _firstTimeSyncState.value = false
          } else _firstTimeSyncState.value = isFirstTimeSync()
        }
      }
    syncBroadcaster.registerSyncListener(syncStateListener, viewModelScope)
  }

  fun refresh() {
    _isRefreshing.value = true
    _refreshCounter.value += 1
  }

  fun isAppFeatureHousehold() =
    appFeatureName.equals(AppFeature.HouseholdManagement.name, ignoreCase = true)

  fun paginateRegisterDataFlow(page: Int) =
    getPager(appFeatureName, healthModule, loadAll = false, page = page).flow

  fun filterRegisterDataFlow(text: String) =
    getPager(
        appFeatureName = appFeatureName,
        healthModule = healthModule,
        loadAll = false,
        searchFilter = text
      )
      .flow
      .cachedIn(viewModelScope)

  fun paginateRegisterDataForSearch() {
    paginatedRegisterDataForSearch.value =
      getPager(appFeatureName, healthModule, false).flow.cachedIn(viewModelScope)
  }

  private fun getPager(
    appFeatureName: String?,
    healthModule: HealthModule,
    loadAll: Boolean = false,
    page: Int = 0,
    searchFilter: String? = null
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
              searchFilter = searchFilter
            )
          )
        }
      }
    )

  fun countPages() =
    _totalRecordsCount.map { it.toDouble().div(DEFAULT_PAGE_SIZE) }.map { ceil(it).toInt() }

  fun patientRegisterQuestionnaireIntent(context: Context) =
    Intent(context, QuestionnaireActivity::class.java)
      .putExtras(
        QuestionnaireActivity.intentArgs(
          formName = registerViewConfiguration.registrationForm,
          questionnaireType = QuestionnaireType.DEFAULT
        )
      )

  fun onEvent(event: PatientRegisterEvent) {
    when (event) {
      // Search using name or patient logicalId or identifier. Modify to add more search params
      is PatientRegisterEvent.SearchRegister -> {
        _searchText.value = event.searchText
      }
      is PatientRegisterEvent.MoveToNextPage -> {
        this._currentPage.value = this._currentPage.value?.plus(1)
      }
      is PatientRegisterEvent.MoveToPreviousPage -> {
        this._currentPage.value?.let { if (it > 0) _currentPage.value = it.minus(1) }
      }
      is PatientRegisterEvent.RegisterNewClient -> {
        //        event.context.launchQuestionnaire<QuestionnaireActivity>(
        //          registerViewConfiguration.registrationForm
        //        )
      }
      is PatientRegisterEvent.OpenProfile -> {
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
            route = MainNavigationScreen.PatientProfile.route + urlParams
          )
      }
    }
  }

  fun isRegisterFormViaSettingExists(): Boolean {
    return appFeatureManager.appFeatureHasSetting(REGISTER_FORM_ID_KEY)
  }

  fun isFirstTimeSync() = sharedPreferencesHelper.read(LAST_SYNC_TIMESTAMP, null).isNullOrBlank()

  fun progressMessage() =
    if (searchText.value.isEmpty()) {
      ""
    } else {
      configurationRegistry.context.resources.getString(R.string.search_progress_message)
    }
}
