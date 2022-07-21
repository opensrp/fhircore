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

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
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
import org.smartregister.fhircore.engine.data.local.register.PatientRegisterRepository
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.rulesengine.RulesFactory
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.util.LAST_SYNC_TIMESTAMP
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.launchQuestionnaire
import org.smartregister.fhircore.engine.util.extension.resourceClassType
import org.smartregister.fhircore.quest.data.patient.PatientRegisterPagingSource
import org.smartregister.fhircore.quest.data.patient.PatientRegisterPagingSource.Companion.DEFAULT_INITIAL_LOAD_SIZE
import org.smartregister.fhircore.quest.data.patient.PatientRegisterPagingSource.Companion.DEFAULT_PAGE_SIZE
import org.smartregister.fhircore.quest.data.patient.model.PatientPagingSourceState
import org.smartregister.fhircore.quest.navigation.NavigationArg
import org.smartregister.fhircore.quest.util.mappers.RegisterViewDataMapper

@HiltViewModel
class PatientRegisterViewModel
@Inject
constructor(
  val patientRegisterRepository: PatientRegisterRepository,
  val configurationRegistry: ConfigurationRegistry,
  val registerViewDataMapper: RegisterViewDataMapper,
  val sharedPreferencesHelper: SharedPreferencesHelper,
  val rulesFactory: RulesFactory
) : ViewModel() {

  private val _currentPage = MutableLiveData(0)
  val currentPage
    get() = _currentPage

  private val _searchText = mutableStateOf("")
  val searchText: androidx.compose.runtime.State<String>
    get() = _searchText

  private val _totalRecordsCount = MutableLiveData(1L)

  private lateinit var registerConfiguration: RegisterConfiguration

  val paginatedRegisterData: MutableStateFlow<Flow<PagingData<ResourceData>>> =
    MutableStateFlow(emptyFlow())

  fun paginateRegisterData(registerId: String, loadAll: Boolean = false) {
    paginatedRegisterData.value = getPager(registerId, loadAll).flow
  }

  private fun getPager(registerId: String, loadAll: Boolean = false): Pager<Int, ResourceData> =
    Pager(
      PagingConfig(pageSize = DEFAULT_PAGE_SIZE, initialLoadSize = DEFAULT_INITIAL_LOAD_SIZE),
      pagingSourceFactory = {
        PatientRegisterPagingSource(patientRegisterRepository).apply {
          setPatientPagingSourceState(
            PatientPagingSourceState(
              registerId = registerId,
              loadAll = loadAll,
              currentPage = if (loadAll) 0 else currentPage.value!!
            )
          )
        }
      }
    )

  fun setTotalRecordsCount(registerId: String) {
    // Get the baseResource from the register configurations. Retrieve it's resource type then count
    val fhirResource = retrieveRegisterConfiguration(registerId).fhirResource

    val baseResourceClass = fhirResource.baseResource.resource.resourceClassType().newInstance()
    viewModelScope.launch {
      _totalRecordsCount.postValue(
        patientRegisterRepository.countRegisterData(registerId)
      )
    }
  }

  fun retrieveRegisterConfiguration(registerId: String): RegisterConfiguration {
    if (!::registerConfiguration.isInitialized) {
      registerConfiguration =
        configurationRegistry.retrieveConfiguration(ConfigType.Register, registerId)
    }
    return registerConfiguration
  }

  fun countPages(): Int =
    _totalRecordsCount.value?.toDouble()?.div(DEFAULT_PAGE_SIZE.toLong())?.let { ceil(it).toInt() }
      ?: 1

  fun onEvent(event: PatientRegisterEvent) {
    when (event) {
      // Search using name or patient logicalId or identifier. Modify to add more search params
      is PatientRegisterEvent.SearchRegister -> {
        _searchText.value = event.searchText
        if (event.searchText.isEmpty()) paginateRegisterData(event.registerId)
        else filterRegisterData(event)
      }
      is PatientRegisterEvent.MoveToNextPage -> {
        this._currentPage.value = this._currentPage.value?.plus(1)
        paginateRegisterData(event.registerId)
      }
      is PatientRegisterEvent.MoveToPreviousPage -> {
        this._currentPage.value?.let { if (it > 0) _currentPage.value = it.minus(1) }
        paginateRegisterData(event.registerId)
      }
      is PatientRegisterEvent.RegisterNewClient ->
        event.context.launchQuestionnaire<QuestionnaireActivity>(
          // TODO use appropriate property from the register configuration
          "provide-questionnaire-id"
        )
      is PatientRegisterEvent.OpenProfile -> {
        val urlParams =
          NavigationArg.bindArgumentsOf(Pair(NavigationArg.PATIENT_ID, event.patientId))
        // TODO conditionally navigate to either family or patient profile
        //        if (event.registerId)
        //          event.navController.navigate(route = MainNavigationScreen.FamilyProfile.route +
        // urlParams)
        //        else
        //          event.navController.navigate(
        //            route = MainNavigationScreen.PatientProfile.route + urlParams
        //          )
      }
    }
  }

  private fun filterRegisterData(event: PatientRegisterEvent.SearchRegister) {
    paginatedRegisterData.value =
      getPager(event.registerId, true).flow.map { pagingData: PagingData<ResourceData> ->
        pagingData.filter {
          // TODO apply relevant filters
          //          it.title.contains(event.searchText, ignoreCase = true) ||
          //            it.logicalId.contentEquals(event.searchText, ignoreCase = true)
          true
        }
      }
  }

  // TODO this setting should be removed after refactor
  fun isRegisterFormViaSettingExists(): Boolean {
    return false
  }

  fun isFirstTimeSync() = sharedPreferencesHelper.read(LAST_SYNC_TIMESTAMP, null).isNullOrEmpty()
}
