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

package org.smartregister.fhircore.quest.ui.patient.profile.childcontact

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import org.hl7.fhir.r4.model.Resource
import org.smartregister.fhircore.engine.appfeature.model.HealthModule
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.AppConfigClassification
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.data.local.register.PatientRegisterRepository
import org.smartregister.fhircore.engine.domain.model.ProfileData
import org.smartregister.fhircore.quest.data.patient.PatientRegisterPagingSource
import org.smartregister.fhircore.quest.data.patient.model.PatientPagingSourceState
import org.smartregister.fhircore.quest.navigation.OverflowMenuFactory
import org.smartregister.fhircore.quest.navigation.OverflowMenuHost
import org.smartregister.fhircore.quest.ui.patient.profile.PatientProfileUiState
import org.smartregister.fhircore.quest.ui.shared.models.ProfileViewData
import org.smartregister.fhircore.quest.ui.shared.models.RegisterViewData
import org.smartregister.fhircore.quest.util.mappers.ProfileViewDataMapper
import org.smartregister.fhircore.quest.util.mappers.RegisterViewDataMapper

@HiltViewModel
class ChildContactsViewModel
@Inject
constructor(
  val overflowMenuFactory: OverflowMenuFactory,
  val patientRegisterRepository: PatientRegisterRepository,
  val configurationRegistry: ConfigurationRegistry,
  val profileViewDataMapper: ProfileViewDataMapper,
  val registerViewDataMapper: RegisterViewDataMapper
) : ViewModel() {

  var patientProfileUiState: MutableState<PatientProfileUiState> =
    mutableStateOf(
      PatientProfileUiState(
        overflowMenuFactory.retrieveOverflowMenuItems(OverflowMenuHost.PATIENT_PROFILE)
      )
    )

  val patientProfileViewData: MutableState<ProfileViewData.PatientProfileViewData> =
    mutableStateOf(ProfileViewData.PatientProfileViewData())

  var patientProfileData: ProfileData? = null

  val applicationConfiguration: ApplicationConfiguration
    get() = configurationRegistry.retrieveConfiguration(AppConfigClassification.APPLICATION)

  var childContactResources = emptyList<Resource>()

  val paginatedRegisterData: MutableStateFlow<Flow<PagingData<RegisterViewData>>> =
    MutableStateFlow(emptyFlow())

  private fun getPager(
    appFeatureName: String?,
    healthModule: HealthModule,
    loadAll: Boolean = true
  ): Pager<Int, RegisterViewData> =
    Pager(
      config =
        PagingConfig(
          pageSize = PatientRegisterPagingSource.DEFAULT_PAGE_SIZE,
          initialLoadSize = PatientRegisterPagingSource.DEFAULT_INITIAL_LOAD_SIZE
        ),
      pagingSourceFactory = {
        ChildContactPagingSource(
            patientProfileViewData.value.otherPatients,
            patientRegisterRepository,
            registerViewDataMapper
          )
          .apply {
            setPatientPagingSourceState(
              PatientPagingSourceState(
                appFeatureName = appFeatureName,
                healthModule = healthModule,
                loadAll = loadAll,
                currentPage = 0
              )
            )
          }
      }
    )

  fun paginateRegisterData(
    appFeatureName: String?,
    healthModule: HealthModule,
    loadAll: Boolean = true
  ) {
    paginatedRegisterData.value = getPager(appFeatureName, healthModule, loadAll).flow
  }

  fun onEvent(event: ChildContactEvent) =
    when (event) {
      is ChildContactEvent.OpenProfile -> {}
      //        event.context.launchQuestionnaireForResult<QuestionnaireActivity>(
      //          questionnaireId = event.taskFormId,
      //          clientIdentifier = event.patientId,
      //          backReference = event.taskId.asReference(ResourceType.Task).reference,
      //          populationResources = event.getActivePopulationResources()
      //        )
      else -> {}
    }

  //    fun initializeRegisterDataViewModel():
  //            RegisterDataViewModel<PatientTask, PatientTaskItem> {
  //        val registerDataViewModel = ChildContactsViewModel(overflowMenuFactory,
  // patientRegisterRepository, configurationRegistry, profileViewDataMapper,
  // registerViewDataMapper)
  //
  //        return ViewModelProvider(viewModelStore, registerDataViewModel.createFactory())[
  //                registerDataViewModel::class.java]
  //    }
}
