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

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
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
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.extension.getActivity
import org.smartregister.fhircore.engine.util.fhirpath.FhirPathDataExtractor
import org.smartregister.fhircore.quest.ui.profile.bottomSheet.ProfileBottomSheetFragment
import org.smartregister.fhircore.quest.ui.profile.model.EligibleManagingEntity
import org.smartregister.fhircore.quest.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.quest.util.extensions.launchQuestionnaire
import timber.log.Timber

@HiltViewModel
class ProfileViewModel
@Inject
constructor(
  val registerRepository: RegisterRepository,
  val configurationRegistry: ConfigurationRegistry,
  val dispatcherProvider: DispatcherProvider,
  val fhirPathDataExtractor: FhirPathDataExtractor
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

  fun onEvent(event: ProfileEvent) {
    when (event) {
      is ProfileEvent.OverflowMenuClick -> {
        event.overflowMenuItemConfig?.actions?.forEach { actionConfig ->
          when (actionConfig.workflow) {
            ApplicationWorkflow.LAUNCH_QUESTIONNAIRE -> {
              actionConfig.questionnaire?.let { questionnaireConfig ->
                event.context.launchQuestionnaire<QuestionnaireActivity>(
                  intentBundle =
                    actionConfig.paramsBundle(event.resourceData?.computedValuesMap ?: emptyMap()),
                  questionnaireConfig = questionnaireConfig,
                  computedValuesMap = event.resourceData?.computedValuesMap
                )
              }
            }
            ApplicationWorkflow.CHANGE_MANAGING_ENTITY -> {
              if (event.managingEntity == null) return@forEach
              if (event.resourceData?.baseResource?.resourceType != ResourceType.Group) {
                Timber.w("Wrong resource type. Expecting Group resource")
                return
              }
              changeManagingEntity(event = event)
            }
            else -> {}
          }
        }
      }
      is ProfileEvent.OnChangeManagingEntity -> {
        viewModelScope.launch(dispatcherProvider.io()) {
          registerRepository.changeManagingEntity(event.newManagingEntityId, event.groupId)
        }
      }
    }
  }

  private fun changeManagingEntity(event: ProfileEvent.OverflowMenuClick) {
    val resourceTypeToFilter = event.managingEntity?.fhirPathResource?.resourceType

    val eligibleManagingEntityList =
      event
        .resourceData
        ?.relatedResourcesMap
        ?.get(resourceTypeToFilter)
        ?.filter {
          fhirPathDataExtractor
            .extractValue(it, event.managingEntity?.fhirPathResource?.fhirPathExpression ?: "")
            .toBoolean()
        }
        ?.map {
          EligibleManagingEntity(
            groupId = event.resourceData.baseResource.logicalId,
            logicalId = it.logicalId,
            memberInfo =
              fhirPathDataExtractor.extractValue(
                it,
                event.managingEntity?.infoFhirPathExpression ?: ""
              )
          )
        }
    (event.context.getActivity())?.let { activity ->
      ProfileBottomSheetFragment(
          eligibleManagingEntities = eligibleManagingEntityList!!,
          onSaveClick = {
            onEvent(
              ProfileEvent.OnChangeManagingEntity(
                newManagingEntityId = it.logicalId,
                groupId = it.groupId
              )
            )
          },
          managingEntity = event.managingEntity
        )
        .run { show(activity.supportFragmentManager, ProfileBottomSheetFragment.TAG) }
    }
  }
}
