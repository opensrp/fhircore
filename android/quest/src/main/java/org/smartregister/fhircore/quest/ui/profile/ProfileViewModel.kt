/*
 * Copyright 2021-2023 Ona Systems, Inc
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

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.fhir.db.ResourceNotFoundException
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.search
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.LinkedList
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.Group
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.configuration.interpolate
import org.smartregister.fhircore.engine.configuration.profile.ManagingEntityConfig
import org.smartregister.fhircore.engine.configuration.profile.ProfileConfiguration
import org.smartregister.fhircore.engine.configuration.workflow.ActionTrigger
import org.smartregister.fhircore.engine.configuration.workflow.ApplicationWorkflow
import org.smartregister.fhircore.engine.data.local.register.RegisterRepository
import org.smartregister.fhircore.engine.domain.model.ActionParameter
import org.smartregister.fhircore.engine.domain.model.FhirResourceConfig
import org.smartregister.fhircore.engine.domain.model.QuestionnaireType
import org.smartregister.fhircore.engine.domain.model.RepositoryResourceData
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.domain.model.SnackBarMessageConfig
import org.smartregister.fhircore.engine.rulesengine.RulesExecutor
import org.smartregister.fhircore.engine.rulesengine.retrieveListProperties
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.extension.extractId
import org.smartregister.fhircore.engine.util.extension.extractLogicalIdUuid
import org.smartregister.fhircore.engine.util.extension.getActivity
import org.smartregister.fhircore.engine.util.fhirpath.FhirPathDataExtractor
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.ui.profile.bottomSheet.ProfileBottomSheetFragment
import org.smartregister.fhircore.quest.ui.profile.model.EligibleManagingEntity
import org.smartregister.fhircore.quest.util.extensions.handleClickEvent
import org.smartregister.fhircore.quest.util.extensions.toParamDataMap
import timber.log.Timber

@HiltViewModel
class ProfileViewModel
@Inject
constructor(
  val registerRepository: RegisterRepository,
  val configurationRegistry: ConfigurationRegistry,
  val dispatcherProvider: DispatcherProvider,
  val fhirPathDataExtractor: FhirPathDataExtractor,
  val rulesExecutor: RulesExecutor
) : ViewModel() {

  val profileUiState = mutableStateOf(ProfileUiState())
  val applicationConfiguration: ApplicationConfiguration by lazy {
    configurationRegistry.retrieveConfiguration(ConfigType.Application)
  }
  private val _snackBarStateFlow = MutableSharedFlow<SnackBarMessageConfig>()
  val snackBarStateFlow: SharedFlow<SnackBarMessageConfig> = _snackBarStateFlow.asSharedFlow()
  private lateinit var profileConfiguration: ProfileConfiguration
  private val listResourceDataMapState = mutableStateMapOf<String, List<ResourceData>>()

  suspend fun retrieveProfileUiState(
    profileId: String,
    resourceId: String,
    fhirResourceConfig: FhirResourceConfig? = null,
    paramsList: Array<ActionParameter>? = emptyArray()
  ) {
    if (resourceId.isNotEmpty()) {
      val repoResourceData =
        registerRepository.loadProfileData(profileId, resourceId, fhirResourceConfig, paramsList)
      val paramsMap: Map<String, String> = paramsList.toParamDataMap<String, String>()
      val profileConfigs = retrieveProfileConfiguration(profileId, paramsMap)
      val queryResult = repoResourceData.queryResult as RepositoryResourceData.QueryResult.Search
      val resourceData =
        rulesExecutor
          .processResourceData(
            baseResource = queryResult.resource,
            relatedRepositoryResourceData = LinkedList(queryResult.relatedResources),
            ruleConfigs = profileConfigs.rules,
            paramsMap
          )
          .copy(listResourceDataMap = listResourceDataMapState)

      profileUiState.value =
        ProfileUiState(
          resourceData = resourceData,
          profileConfiguration = profileConfigs,
          snackBarTheme = applicationConfiguration.snackBarTheme,
          showDataLoadProgressIndicator = false
        )

      profileConfigs.views.retrieveListProperties().forEach {
        val listResourceData =
          rulesExecutor.processListResourceData(
            listProperties = it,
            relatedRepositoryResourceData = LinkedList(queryResult.relatedResources),
            computedValuesMap =
              resourceData.computedValuesMap.toMutableMap().plus(paramsMap).toMap()
          )
        listResourceDataMapState[it.id] = listResourceData
      }
    }
  }

  private fun retrieveProfileConfiguration(
    profileId: String,
    paramsMap: Map<String, String>?
  ): ProfileConfiguration {
    // Ensures profile configuration is initialized once
    if (!::profileConfiguration.isInitialized) {
      profileConfiguration =
        configurationRegistry.retrieveConfiguration(ConfigType.Profile, profileId, paramsMap)
    }
    return profileConfiguration
  }

  fun onEvent(event: ProfileEvent) {
    when (event) {
      is ProfileEvent.OverflowMenuClick -> {
        val actions = event.overflowMenuItemConfig?.actions
        viewModelScope.launch {
          val questionnaireResponse =
            actions
              ?.find {
                it.workflow == ApplicationWorkflow.LAUNCH_QUESTIONNAIRE &&
                  it.trigger == ActionTrigger.ON_CLICK
              }
              ?.questionnaire
              ?.let { questionnaireConfig ->
                val interpolatedConfig =
                  questionnaireConfig.interpolate(
                    event.resourceData?.computedValuesMap ?: emptyMap()
                  )
                if (interpolatedConfig.type != QuestionnaireType.DEFAULT) {
                  searchQuestionnaireResponses(
                    subjectId = event.resourceData?.baseResourceId?.extractLogicalIdUuid(),
                    subjectType = event.resourceData?.baseResourceType,
                    questionnaireId = interpolatedConfig.id
                  )
                    .maxByOrNull { it.authored }
                } else null
              }

          actions?.run {
            find { it.workflow == ApplicationWorkflow.CHANGE_MANAGING_ENTITY }?.let {
              changeManagingEntity(
                event = event,
                managingEntity =
                  it.interpolateManagingEntity(event.resourceData?.computedValuesMap ?: emptyMap())
              )
            }
            handleClickEvent(
              navController = event.navController,
              resourceData = event.resourceData,
              questionnaireResponse = questionnaireResponse
            )
          }
        }
      }
      is ProfileEvent.OnChangeManagingEntity -> {
        viewModelScope.launch(dispatcherProvider.io()) {
          registerRepository.changeManagingEntity(
            event.eligibleManagingEntity.logicalId,
            event.eligibleManagingEntity.groupId
          )
          withContext(dispatcherProvider.main()) {
            emitSnackBarState(
              snackBarMessageConfig =
                SnackBarMessageConfig(
                  message = event.managingEntityConfig?.managingEntityReassignedMessage
                      ?: event.context.getString(R.string.reassigned_managing_entity),
                  actionLabel = event.context.getString(R.string.ok)
                )
            )
          }
        }
      }
    }
  }

  /**
   * This function launches a configurable dialog for selecting new managing entity from the list of
   * [Group] resource members. This function only works when [Group] resource is the used as the
   * main resource.
   */
  private fun changeManagingEntity(
    event: ProfileEvent.OverflowMenuClick,
    managingEntity: ManagingEntityConfig?
  ) {
    if (managingEntity == null || event.resourceData?.baseResourceType != ResourceType.Group) {
      Timber.w("ManagingEntityConfig required. Base resource should be Group")
      return
    }
    viewModelScope.launch {
      val group = registerRepository.loadResource<Group>(event.resourceData.baseResourceId)
      val eligibleManagingEntities: List<EligibleManagingEntity> =
        group
          ?.member
          ?.mapNotNull {
            try {
              registerRepository.loadResource(it.entity.extractId(), managingEntity.resourceType!!)
            } catch (resourceNotFoundException: ResourceNotFoundException) {
              null
            }
          }
          ?.asSequence()
          ?.filter { managingEntityResource ->
            fhirPathDataExtractor
              .extractValue(
                base = managingEntityResource,
                expression = managingEntity.eligibilityCriteriaFhirPathExpression!!
              )
              .toBoolean()
          }
          ?.toList()
          ?.map {
            EligibleManagingEntity(
              groupId = event.resourceData.baseResourceId,
              logicalId = it.logicalId.extractLogicalIdUuid(),
              memberInfo =
                fhirPathDataExtractor.extractValue(
                  base = it,
                  expression = managingEntity.nameFhirPathExpression!!
                )
            )
          }
          ?: emptyList()

      // Show error message when no group members are found
      if (eligibleManagingEntities.isEmpty()) {
        emitSnackBarState(SnackBarMessageConfig(message = managingEntity.noMembersErrorMessage))
      } else {
        (event.navController.context.getActivity())?.let { activity ->
          ProfileBottomSheetFragment(
              eligibleManagingEntities = eligibleManagingEntities,
              onSaveClick = {
                onEvent(
                  ProfileEvent.OnChangeManagingEntity(
                    context = activity,
                    eligibleManagingEntity = it,
                    managingEntityConfig = managingEntity
                  )
                )
              },
              managingEntity = managingEntity
            )
            .run { show(activity.supportFragmentManager, ProfileBottomSheetFragment.TAG) }
        }
      }
    }
  }

  suspend fun emitSnackBarState(snackBarMessageConfig: SnackBarMessageConfig) {
    _snackBarStateFlow.emit(snackBarMessageConfig)
  }

  private suspend fun searchQuestionnaireResponses(
    subjectId: String?,
    subjectType: ResourceType?,
    questionnaireId: String
  ): List<QuestionnaireResponse> =
    if (subjectId.isNullOrEmpty() && subjectType == null) emptyList()
    else
      withContext(dispatcherProvider.io()) {
        registerRepository.fhirEngine.search {
          filter(QuestionnaireResponse.SUBJECT, { value = "${subjectType!!.name}/$subjectId" })
          filter(
            QuestionnaireResponse.QUESTIONNAIRE,
            { value = "${ResourceType.Questionnaire.name}/$questionnaireId" }
          )
        }
      }
}
