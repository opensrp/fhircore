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

package org.smartregister.fhircore.quest.ui.patient.profile.guardians

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.engine.appfeature.model.HealthModule
import org.smartregister.fhircore.engine.domain.model.HealthStatus
import org.smartregister.fhircore.engine.domain.model.RegisterData
import org.smartregister.fhircore.engine.util.extension.extractName
import org.smartregister.fhircore.quest.data.patient.HivPatientGuardianRepository
import org.smartregister.fhircore.quest.navigation.MainNavigationScreen
import org.smartregister.fhircore.quest.navigation.NavigationArg
import org.smartregister.fhircore.quest.util.mappers.RegisterViewDataMapper

@HiltViewModel
class GuardianRegisterViewModel
@Inject
constructor(
  savedStateHandle: SavedStateHandle,
  val repository: HivPatientGuardianRepository,
  val registerViewDataMapper: RegisterViewDataMapper
) : ViewModel() {

  // Get your argument from the SavedStateHandle
  private val patientId: String = savedStateHandle[NavigationArg.PATIENT_ID]!!
  private val appFeatureName: String? = savedStateHandle[NavigationArg.FEATURE]
  private val healthModule: HealthModule =
    savedStateHandle[NavigationArg.HEALTH_MODULE] ?: HealthModule.HIV

  private val _guardianUiDetails =
    mutableStateOf(GuardianUiState(patientFirstName = "", registerViewData = emptyList()))

  val guardianUiDetails: State<GuardianUiState> = _guardianUiDetails

  fun loadData() {
    viewModelScope.launch {
      val patient = repository.loadPatient(patientId)
      val guardiansRegisterViewData =
        repository
          .loadGuardianRegisterData(patient!!)
          .filterIsInstance<RegisterData.HivRegisterData>()
          .map {
            GuardianPatientRegisterData(
              viewData = registerViewDataMapper.transformInputToOutputModel(it),
              profileNavRoute = getRoute(it)
            )
          }
      _guardianUiDetails.value =
        GuardianUiState(
          patientFirstName = patient.extractFirstName(),
          registerViewData = guardiansRegisterViewData
        )
    }
  }

  private fun getRoute(registerData: RegisterData.HivRegisterData): String {
    val commonNavArgs =
      listOf(
        Pair(NavigationArg.FEATURE, appFeatureName),
        Pair(NavigationArg.HEALTH_MODULE, healthModule.name)
      )

    val navArgs =
      commonNavArgs +
        Pair(
          NavigationArg.ON_ART,
          (registerData.healthStatus != HealthStatus.NOT_ON_ART).toString()
        )
    val urlParams = NavigationArg.bindArgumentsOf(*navArgs.toTypedArray())

    return "${MainNavigationScreen.GuardianProfile.route}/${registerData.logicalId}$urlParams"
  }

  private fun Patient.extractFirstName() = this.extractName().substringBefore(" ")
}
