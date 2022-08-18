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

package org.smartregister.fhircore.quest.ui.profile

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.fhir.logicalId
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.profile.ProfileConfiguration
import org.smartregister.fhircore.engine.configuration.workflow.ApplicationWorkflow
import org.smartregister.fhircore.engine.data.local.register.RegisterRepository
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireType
import org.smartregister.fhircore.engine.util.extension.asReference
import org.smartregister.fhircore.engine.util.extension.interpolate
import org.smartregister.fhircore.engine.util.extension.launchQuestionnaire
import org.smartregister.fhircore.engine.util.extension.launchQuestionnaireForResult
import org.smartregister.fhircore.engine.util.fhirpath.FhirPathDataExtractor
import org.smartregister.fhircore.quest.ui.profile.bottomSheet.ProfileBottomSheetFragment
import org.smartregister.fhircore.quest.ui.profile.model.EligibleManagingEntity

@HiltViewModel
class ProfileViewModel
@Inject
constructor(
  val registerRepository: RegisterRepository,
  val configurationRegistry: ConfigurationRegistry
) : ViewModel() {

  val profileUiState: MutableState<ProfileUiState> = mutableStateOf(ProfileUiState())

  private lateinit var profileConfiguration: ProfileConfiguration

  fun retrieveProfileUiState(profileId: String, resourceId: String) {
    if (resourceId.isNotEmpty()) {
      viewModelScope.launch {
        profileUiState.value =
          ProfileUiState(
            resourceData = registerRepository.loadProfileData(profileId, resourceId),
            profileConfiguration = retrieveProfileConfiguration(profileId)
          )
      }
    }
  }

  private fun retrieveProfileConfiguration(profileId: String): ProfileConfiguration {
    // Ensures profile configuration is initialized once
    if (!::profileConfiguration.isInitialized) {
      profileConfiguration =
        configurationRegistry.retrieveConfiguration(ConfigType.Profile, profileId)
    }
    return profileConfiguration
  }

  fun onEvent(event: ProfileEvent) =
    when (event) {
      is ProfileEvent.LoadQuestionnaire ->
        event.context.launchQuestionnaire<QuestionnaireActivity>(event.questionnaireId)
      is ProfileEvent.SeeAll -> {
        /* TODO(View all records in this category e.g. all medical history, tasks etc) */
      }
      is ProfileEvent.OverflowMenuClick -> {
        event.overflowMenuItemConfig?.actions?.forEach { actionConfig ->
          when (actionConfig.workflow) {
            ApplicationWorkflow.LAUNCH_QUESTIONNAIRE -> {
              if (actionConfig.questionnaire == null) return@forEach
              var intentBundle: android.os.Bundle = android.os.Bundle.EMPTY
              if (actionConfig.params.isNotEmpty()) {
                val actionParamList: MutableList<Pair<String, String>> =
                  emptyList<Pair<String, String>>().toMutableList()
                actionConfig.params.forEach { actionParameter ->
                  actionParamList.add(
                    Pair(
                      actionParameter.key,
                      actionParameter.value.interpolate(event.resourceData!!.computedValuesMap)
                    )
                  )
                }
                intentBundle = bundleOf(*actionParamList.toTypedArray())
              }
              event.context.launchQuestionnaire<QuestionnaireActivity>(
                questionnaireId = actionConfig.questionnaire!!.id,
                clientIdentifier = event.resourceData?.baseResource?.logicalId,
                questionnaireType = QuestionnaireType.valueOf(actionConfig.questionnaire!!.type),
                intentBundle = intentBundle
              )
            }
            ApplicationWorkflow.CHANGE_MANAGING_ENTITY -> {
              if (event.managingEntity == null) return@forEach
              val resourceTypeToFilter = event.managingEntity.fhirPathResource.resourceType

              val eligibleManagingEntityList =
                event
                  .resourceData
                  ?.relatedResourcesMap
                  ?.get(resourceTypeToFilter)
                  ?.filter {
                    FhirPathDataExtractor.extractValue(
                        it,
                        event.managingEntity.fhirPathResource.fhirPathExpression
                      )
                      .toBoolean()
                  }
                  ?.map {
                    EligibleManagingEntity(
                      groupId = event.resourceData.baseResource.logicalId,
                      logicalId = it.logicalId,
                      memberInfo =
                        FhirPathDataExtractor.extractValue(
                          it,
                          event.managingEntity.infoFhirPathExpression
                        )
                    )
                  }
              (event.context as AppCompatActivity).let { activity ->
                ProfileBottomSheetFragment(
                    eligibleManagingEntities = eligibleManagingEntityList!!,
                    onSaveClick = {}
                  )
                  .run { show(activity.supportFragmentManager, ProfileBottomSheetFragment.TAG) }
              }
            }
            else -> {}
          }
        }
      }
      is ProfileEvent.OpenTaskForm ->
        event.context.launchQuestionnaireForResult<QuestionnaireActivity>(
          questionnaireId = event.taskFormId,
          clientIdentifier = event.patientId,
          backReference = event.taskId?.asReference(ResourceType.Task)?.reference
        )
      else -> {}
    }
}
