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

package org.smartregister.fhircore.quest.ui.tracing.profile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.fhir.sync.SyncJobStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.appfeature.model.HealthModule
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.AppConfigClassification
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.data.local.register.AppRegisterRepository
import org.smartregister.fhircore.engine.domain.model.ProfileData
import org.smartregister.fhircore.engine.sync.OnSyncListener
import org.smartregister.fhircore.engine.sync.SyncBroadcaster
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireType
import org.smartregister.fhircore.engine.util.extension.asReference
import org.smartregister.fhircore.engine.util.extension.showToast
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.navigation.MainNavigationScreen
import org.smartregister.fhircore.quest.navigation.NavigationArg
import org.smartregister.fhircore.quest.navigation.OverflowMenuFactory
import org.smartregister.fhircore.quest.navigation.OverflowMenuHost
import org.smartregister.fhircore.quest.ui.shared.models.ProfileViewData
import org.smartregister.fhircore.quest.util.mappers.ProfileViewDataMapper
import org.smartregister.fhircore.quest.util.mappers.RegisterViewDataMapper

@HiltViewModel
class TracingProfileViewModel
@Inject
constructor(
  savedStateHandle: SavedStateHandle,
  private val syncBroadcaster: SyncBroadcaster,
  private val overflowMenuFactory: OverflowMenuFactory,
  val registerRepository: AppRegisterRepository,
  val configurationRegistry: ConfigurationRegistry,
  val profileViewDataMapper: ProfileViewDataMapper,
  val registerViewDataMapper: RegisterViewDataMapper,
) : ViewModel() {

  val appFeatureName = savedStateHandle.get<String>(NavigationArg.FEATURE)
  val healthModule =
    savedStateHandle.get<HealthModule>(NavigationArg.HEALTH_MODULE) ?: HealthModule.DEFAULT
  val patientId = savedStateHandle.get<String>(NavigationArg.PATIENT_ID) ?: ""
  val familyId = savedStateHandle.get<String>(NavigationArg.FAMILY_ID)

  var patientTracingProfileUiState: MutableState<TracingProfileUiState> =
    mutableStateOf(
      TracingProfileUiState(
        overflowMenuFactory.retrieveOverflowMenuItems(OverflowMenuHost.TRACING_PROFILE)
      )
    )

  private val _patientProfileViewDataFlow = MutableStateFlow(ProfileViewData.TracingProfileData())
  val patientProfileViewData: StateFlow<ProfileViewData.TracingProfileData>
    get() = _patientProfileViewDataFlow.asStateFlow()

  var patientProfileData: ProfileData? = null

  val applicationConfiguration: ApplicationConfiguration
    get() = configurationRegistry.retrieveConfiguration(AppConfigClassification.APPLICATION)

  private val _showTracingOutcomes = MutableLiveData<Boolean>()
  val showTracingOutcomes: LiveData<Boolean>
    get() = _showTracingOutcomes

  val isSyncing = mutableStateOf(false)

  init {
    syncBroadcaster.registerSyncListener(
      object : OnSyncListener {
        override fun onSync(state: SyncJobStatus) {
          when (state) {
            is SyncJobStatus.Finished, is SyncJobStatus.Failed -> {
              isSyncing.value = false
              fetchTracingData()
            }
            is SyncJobStatus.Started -> {
              isSyncing.value = true
            }
            else -> {
              isSyncing.value = false
            }
          }
        }
      },
      viewModelScope
    )

    fetchTracingData()
  }

  fun fetchTracingData() {
    viewModelScope.launch {
      registerRepository.loadPatientProfileData(appFeatureName, healthModule, patientId)?.let {
        patientProfileData = it
        _patientProfileViewDataFlow.value =
          profileViewDataMapper.transformInputToOutputModel(it) as
            ProfileViewData.TracingProfileData
      }
    }
  }

  fun onEvent(event: TracingProfileEvent) {
    val profile = patientProfileViewData.value

    when (event) {
      is TracingProfileEvent.LoadQuestionnaire ->
        QuestionnaireActivity.launchQuestionnaire(
          event.context,
          event.questionnaireId,
          clientIdentifier = patientId,
          populationResources = profile.populationResources
        )
      is TracingProfileEvent.OverflowMenuClick -> {
        when (event.menuId) {
          R.id.edit_profile ->
            QuestionnaireActivity.launchQuestionnaire(
              event.context,
              questionnaireId = EDIT_PROFILE_FORM,
              clientIdentifier = patientId,
              questionnaireType = QuestionnaireType.EDIT
            )
          R.id.tracing_history -> {
            val urlParams = NavigationArg.bindArgumentsOf(Pair(NavigationArg.PATIENT_ID, patientId))
            event.navController.navigate(
              route = MainNavigationScreen.TracingHistory.route + urlParams
            )
          }
          else -> {
            event.context.showToast("//todo Tracing action here")
          }
        }
      }
      is TracingProfileEvent.OpenTaskForm ->
        QuestionnaireActivity.launchQuestionnaireForResult(
          event.context as Activity,
          questionnaireId = event.taskFormId,
          clientIdentifier = patientId,
          backReference = event.taskId.asReference(ResourceType.Task).reference,
          populationResources = profile.populationResources
        )
      is TracingProfileEvent.LoadOutComesForm -> {
        profile.isHomeTracing?.let { isHomeTracing ->
          QuestionnaireActivity.launchQuestionnaire(
            event.context,
            // TODO: Replace with actual tracing outcomes url
            if (isHomeTracing) "tests/home_outcome.json" else "tests/phone_outcome.json",
            clientIdentifier = patientId,
            questionnaireType = QuestionnaireType.EDIT,
            populationResources = profile.populationResources
          )
        }
      }
      is TracingProfileEvent.OpenTracingOutcomeScreen -> {
        val urlParams =
          NavigationArg.bindArgumentsOf(
            Pair(NavigationArg.PATIENT_ID, patientId),
            Pair(NavigationArg.TRACING_ID, event.historyId)
          )
        event.navController.navigate(route = MainNavigationScreen.TracingOutcomes.route + urlParams)
      }
      is TracingProfileEvent.CallPhoneNumber -> {
        if (event.phoneNumber.isNotBlank()) {
          event.context.startActivity(
            Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:${event.phoneNumber}") }
          )
        }
      }
    }
  }

  fun reSync() {
    syncBroadcaster.runSync()
  }

  companion object {
    const val EDIT_PROFILE_FORM = "edit-patient-profile"
  }
}
