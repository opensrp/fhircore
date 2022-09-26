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
import org.smartregister.fhircore.engine.appfeature.AppFeature
import org.smartregister.fhircore.engine.appfeature.AppFeatureManager
import org.smartregister.fhircore.engine.appfeature.model.HealthModule
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.AppConfigClassification
import org.smartregister.fhircore.engine.configuration.view.RegisterViewConfiguration
import org.smartregister.fhircore.engine.data.local.register.PatientRegisterRepository
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireType
import org.smartregister.fhircore.engine.util.LAST_SYNC_TIMESTAMP
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.data.patient.PatientRegisterPagingSource
import org.smartregister.fhircore.quest.data.patient.PatientRegisterPagingSource.Companion.DEFAULT_INITIAL_LOAD_SIZE
import org.smartregister.fhircore.quest.data.patient.PatientRegisterPagingSource.Companion.DEFAULT_PAGE_SIZE
import org.smartregister.fhircore.quest.data.patient.model.PatientPagingSourceState
import org.smartregister.fhircore.quest.navigation.MainNavigationScreen
import org.smartregister.fhircore.quest.navigation.NavigationArg
import org.smartregister.fhircore.quest.ui.shared.models.RegisterViewData
import org.smartregister.fhircore.quest.util.REGISTER_FORM_ID_KEY
import org.smartregister.fhircore.quest.util.mappers.RegisterViewDataMapper

@HiltViewModel
class PatientRegisterViewModel
@Inject
constructor(
  val patientRegisterRepository: PatientRegisterRepository,
  val configurationRegistry: ConfigurationRegistry,
  val registerViewDataMapper: RegisterViewDataMapper,
  val appFeatureManager: AppFeatureManager,
  val sharedPreferencesHelper: SharedPreferencesHelper
) : ViewModel() {

  private val _currentPage = MutableLiveData(0)
  val currentPage
    get() = _currentPage

  private val _searchText = mutableStateOf("")
  val searchText: androidx.compose.runtime.State<String>
    get() = _searchText

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
  }

  fun paginateRegisterData(
    appFeatureName: String?,
    healthModule: HealthModule,
    loadAll: Boolean = false
  ) {
    paginatedRegisterData.value = getPager(appFeatureName, healthModule, loadAll).flow
  }

  fun paginateRegisterDataForSearch(
    appFeatureName: String?,
    healthModule: HealthModule,
    loadAll: Boolean = true
  ) {
    paginatedRegisterDataForSearch.value =
      getPager(appFeatureName, healthModule, loadAll).flow.cachedIn(viewModelScope)
  }

  private fun getPager(
    appFeatureName: String?,
    healthModule: HealthModule,
    loadAll: Boolean = false
  ): Pager<Int, RegisterViewData> =
    Pager(
      config =
        PagingConfig(pageSize = DEFAULT_PAGE_SIZE, initialLoadSize = DEFAULT_INITIAL_LOAD_SIZE),
      pagingSourceFactory = {
        PatientRegisterPagingSource(patientRegisterRepository, registerViewDataMapper).apply {
          setPatientPagingSourceState(
            PatientPagingSourceState(
              appFeatureName = appFeatureName,
              healthModule = healthModule,
              loadAll = loadAll,
              currentPage = if (loadAll) 0 else currentPage.value!!
            )
          )
        }
      }
    )

  fun setTotalRecordsCount(appFeatureName: String?, healthModule: HealthModule) {
    viewModelScope.launch {
      _totalRecordsCount.postValue(
        patientRegisterRepository.countRegisterData(appFeatureName, healthModule)
      )
    }
  }

  fun countPages(): Int =
    _totalRecordsCount.value?.toDouble()?.div(DEFAULT_PAGE_SIZE.toLong())?.let { ceil(it).toInt() }
      ?: 1

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
        if (event.searchText.isEmpty())
          paginateRegisterData(event.appFeatureName, event.healthModule)
        else filterRegisterData(event)
      }
      is PatientRegisterEvent.MoveToNextPage -> {
        this._currentPage.value = this._currentPage.value?.plus(1)
        paginateRegisterData(event.appFeatureName, event.healthModule)
      }
      is PatientRegisterEvent.MoveToPreviousPage -> {
        this._currentPage.value?.let { if (it > 0) _currentPage.value = it.minus(1) }
        paginateRegisterData(event.appFeatureName, event.healthModule)
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
            Pair(NavigationArg.HEALTH_MODULE, event.healthModule.name),
            Pair(NavigationArg.PATIENT_ID, event.patientId)
          )
        if (event.healthModule == HealthModule.FAMILY)
          event.navController.navigate(route = MainNavigationScreen.FamilyProfile.route + urlParams)
        else
          event.navController.navigate(
            route = MainNavigationScreen.PatientProfile.route + urlParams
          )
      }
    }
  }

  private fun filterRegisterData(event: PatientRegisterEvent.SearchRegister) {
    paginatedRegisterData.value =
      paginatedRegisterDataForSearch.value.map {
        //         getPager(event.appFeatureName, event.healthModule, true).flow.map {
        pagingData: PagingData<RegisterViewData> ->
        pagingData.filter {
          it.title.contains(event.searchText, ignoreCase = true) ||
            it.identifier.contains(event.searchText, ignoreCase = true)
        }
      }
  }

  fun isRegisterFormViaSettingExists(): Boolean {
    return appFeatureManager.appFeatureHasSetting(REGISTER_FORM_ID_KEY)
  }

  fun isFirstTimeSync() = sharedPreferencesHelper.read(LAST_SYNC_TIMESTAMP, null).isNullOrEmpty()

  fun progressMessage() =
    if (searchText.value.isEmpty()) {
      ""
    } else {
      configurationRegistry.context.resources.getString(R.string.search_progress_message)
    }
}
