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

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import com.google.android.fhir.search.search
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.configuration.profile.ProfileConfiguration
import org.smartregister.fhircore.engine.configuration.workflow.ApplicationWorkflow
import org.smartregister.fhircore.engine.data.local.register.RegisterRepository
import org.smartregister.fhircore.engine.domain.model.FhirResourceConfig
import org.smartregister.fhircore.engine.domain.model.SnackBarMessageConfig
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.extension.extractLogicalIdUuid
import org.smartregister.fhircore.engine.util.fhirpath.FhirPathDataExtractor
import org.smartregister.fhircore.quest.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.quest.ui.shared.QuestionnaireHandler
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

  private val parser = FhirContext.forCached(FhirVersionEnum.R4).newJsonParser()

  val launchQuestionnaireLiveData = MutableLiveData(false)
  val profileUiState = mutableStateOf(ProfileUiState())
  val applicationConfiguration: ApplicationConfiguration by lazy {
    configurationRegistry.retrieveConfiguration(ConfigType.Application)
  }
  private val _snackBarStateFlow = MutableSharedFlow<SnackBarMessageConfig>()
  val snackBarStateFlow: SharedFlow<SnackBarMessageConfig> = _snackBarStateFlow.asSharedFlow()

  private lateinit var profileConfiguration: ProfileConfiguration

  suspend fun retrieveProfileUiState(
    profileId: String,
    resourceId: String,
    fhirResourceConfig: FhirResourceConfig? = null
  ) {
    if (resourceId.isNotEmpty()) {
      val resourceData =
        registerRepository.loadProfileData(profileId, resourceId, fhirResourceConfig)
      profileUiState.value =
        ProfileUiState(
          resourceData = resourceData,
          profileConfiguration = retrieveProfileConfiguration(profileId),
          snackBarTheme = applicationConfiguration.snackBarTheme
        )
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
                if (event.navController.context is QuestionnaireHandler) {
                  viewModelScope.launch {
                    var questionnaireResponse: String? = null

                    if (event.resourceData != null) {
                      questionnaireResponse =
                        searchQuestionnaireResponses(
                          subjectId = event.resourceData.baseResourceId.extractLogicalIdUuid(),
                          subjectType = event.resourceData.baseResourceType,
                          questionnaireId = questionnaireConfig.id
                        )
                          .maxByOrNull { it.authored } // Get latest version
                          ?.let { parser.encodeResourceToString(it) }
                    }

                    val intentBundle =
                      actionConfig.paramsBundle(event.resourceData?.computedValuesMap ?: emptyMap())
                        .apply {
                          putString(
                            QuestionnaireActivity.QUESTIONNAIRE_RESPONSE,
                            questionnaireResponse
                          )
                        }

                    (event.navController.context as QuestionnaireHandler).launchQuestionnaire<Any>(
                      context = event.navController.context,
                      intentBundle = intentBundle,
                      questionnaireConfig = questionnaireConfig,
                      computedValuesMap = event.resourceData?.computedValuesMap
                    )
                  }
                }
              }
            }
            ApplicationWorkflow.CHANGE_MANAGING_ENTITY -> {
              if (event.managingEntity == null) return@forEach
              if (event.resourceData?.baseResourceType != ResourceType.Group) {
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
    // TODO Refactor implementation to use ResourceData
    /*
    val resourceTypeToFilter = event.managingEntity?.fhirPathResource?.resourceType
    val eligibleManagingEntityList =
       event.ResourceData
         ?.relatedResourcesMap
         ?.get(resourceTypeToFilter)
         ?.filter {
           fhirPathDataExtractor
             .extractValue(it, event.managingEntity?.fhirPathResource?.fhirPathExpression ?: "")
             .toBoolean()
         }
         ?.map {
           EligibleManagingEntity(
             groupId = event.ResourceData.baseResource.logicalId,
             logicalId = it.logicalId,
             memberInfo =
               fhirPathDataExtractor.extractValue(
                 it,
                 event.managingEntity?.infoFhirPathExpression ?: ""
               )
           )
         }
     (event.navController.context.getActivity())?.let { activity ->
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
     }*/
  }

  suspend fun emitSnackBarState(snackBarMessageConfig: SnackBarMessageConfig) {
    _snackBarStateFlow.emit(snackBarMessageConfig)
  }

  private suspend fun searchQuestionnaireResponses(
    subjectId: String,
    subjectType: ResourceType,
    questionnaireId: String
  ): List<QuestionnaireResponse> =
    withContext(dispatcherProvider.io()) {
      registerRepository.fhirEngine.search {
        filter(QuestionnaireResponse.SUBJECT, { value = "${subjectType.name}/$subjectId" })
        filter(
          QuestionnaireResponse.QUESTIONNAIRE,
          { value = "${ResourceType.Questionnaire.name}/$questionnaireId" }
        )
      }
    }
}
