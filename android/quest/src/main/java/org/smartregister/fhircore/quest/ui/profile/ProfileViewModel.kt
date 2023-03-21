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

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.fhir.datacapture.mapping.ResourceMapper
import com.google.android.fhir.datacapture.validation.QuestionnaireResponseValidator
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.search
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.Group
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.RelatedPerson
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.configuration.interpolate
import org.smartregister.fhircore.engine.configuration.profile.ProfileConfiguration
import org.smartregister.fhircore.engine.configuration.workflow.ApplicationWorkflow
import org.smartregister.fhircore.engine.data.local.register.RegisterRepository
import org.smartregister.fhircore.engine.domain.model.FhirResourceConfig
import org.smartregister.fhircore.engine.domain.model.SnackBarMessageConfig
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.extension.encodeResourceToString
import org.smartregister.fhircore.engine.util.extension.extractId
import org.smartregister.fhircore.engine.util.extension.extractLogicalIdUuid
import org.smartregister.fhircore.engine.util.extension.generateMissingItems
import org.smartregister.fhircore.engine.util.extension.getActivity
import org.smartregister.fhircore.engine.util.extension.interpolate
import org.smartregister.fhircore.engine.util.extension.prepareQuestionsForReadingOrEditing
import org.smartregister.fhircore.engine.util.extension.referenceValue
import org.smartregister.fhircore.engine.util.fhirpath.FhirPathDataExtractor
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.ui.profile.bottomSheet.ProfileBottomSheetFragment
import org.smartregister.fhircore.quest.ui.profile.model.EligibleManagingEntity
import org.smartregister.fhircore.quest.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_RESPONSE
import org.smartregister.fhircore.quest.ui.shared.QuestionnaireHandler
import timber.log.Timber

@HiltViewModel
class ProfileViewModel
@Inject
constructor(
  val registerRepository: RegisterRepository,
  val configurationRegistry: ConfigurationRegistry,
  val dispatcherProvider: DispatcherProvider,
  val fhirPathDataExtractor: FhirPathDataExtractor,
) : ViewModel() {

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

  fun emitSnackBarState(snackBarMessageConfig: SnackBarMessageConfig) {
    viewModelScope.launch {
      _snackBarStateFlow.emit(snackBarMessageConfig)
    }
  }

  fun onEvent(event: ProfileEvent) {
    when (event) {
      is ProfileEvent.OverflowMenuClick -> {
        event.overflowMenuItemConfig?.actions?.forEach { actionConfig ->
          when (actionConfig.workflow) {
            ApplicationWorkflow.LAUNCH_QUESTIONNAIRE -> {
              val context = event.navController.context
              val computedValuesMap = event.resourceData?.computedValuesMap ?: emptyMap()
              val questionnaireConfig = actionConfig.questionnaire?.interpolate(computedValuesMap)

              if (questionnaireConfig == null) {
                emitSnackBarState(SnackBarMessageConfig(context.getString(R.string.error_msg_questionnaire_config_is_not_found)))
                Timber.tag("ProfileViewModel.onEvent.LAUNCH_QUESTIONNAIRE").d(context.getString(R.string.error_msg_questionnaire_config_is_not_found))
                return
              }

              if (context !is QuestionnaireHandler) {
                emitSnackBarState(SnackBarMessageConfig(context.getString(R.string.error_msg_navigation_controller_context_is_not_questionnaire_handler)))
                Timber.tag("ProfileViewModel.onEvent.LAUNCH_QUESTIONNAIRE").d(context.getString(R.string.error_msg_navigation_controller_context_is_not_questionnaire_handler))
                return
              }

              viewModelScope.launch {
                var questionnaire: Questionnaire? = null
                var questionnaireResponse: QuestionnaireResponse? = null
                val paramsBundle = actionConfig.paramsBundle(computedValuesMap)
                val intentBundle = bundleOf().apply { putAll(paramsBundle) }
                val actionParams =
                  actionConfig.params.map {
                    it.copy(value = it.value.interpolate(computedValuesMap))
                  }

                questionnaire = loadQuestionnaire(questionnaireConfig.id)

                if (questionnaire == null) {
                  emitSnackBarState(SnackBarMessageConfig(context.getString(R.string.error_msg_questionnaire_is_not_found_in_database)))
                  Timber.tag("ProfileViewModel.onEvent.LAUNCH_QUESTIONNAIRE").d(context.getString(R.string.error_msg_questionnaire_is_not_found_in_database))
                  return@launch
                }

                questionnaire.apply {
                  this.url = this.url ?: this.referenceValue()
                  if (questionnaireConfig.type.isReadOnly() || questionnaireConfig.type.isEditMode()) {
                    item.prepareQuestionsForReadingOrEditing("QuestionnaireResponse.item", questionnaireConfig.type.isReadOnly())
                  }
                }

                if (event.resourceData != null) {
                  questionnaireResponse = getQuestionnaireResponseFromDbOrPopulation(
                    questionnaire = questionnaire,
                    subjectId = event.resourceData.baseResourceId.extractLogicalIdUuid(),
                    subjectType = event.resourceData.baseResourceType
                  )
                  questionnaireResponse.apply { generateMissingItems(questionnaire) }
                }

                if (questionnaireResponse != null) {
                  val isQuestionnaireResponseValid = isQuestionnaireResponseValid(questionnaire, questionnaireResponse, context)
                  if (!isQuestionnaireResponseValid) {
                    emitSnackBarState(SnackBarMessageConfig(context.getString(R.string.error_msg_questionnaire_response_is_broken)))
                    return@launch
                  }
                  intentBundle.apply {
                    putString(QUESTIONNAIRE_RESPONSE, questionnaireResponse.encodeResourceToString())
                  }
                }

                context.launchQuestionnaire<Any>(
                  context = context,
                  intentBundle = intentBundle,
                  questionnaireConfig = questionnaireConfig,
                  actionParams = actionParams
                )
              }
            }
            ApplicationWorkflow.CHANGE_MANAGING_ENTITY -> changeManagingEntity(event = event)
            else -> {}
          }
        }
      }
      is ProfileEvent.OnChangeManagingEntity -> {
        viewModelScope.launch(dispatcherProvider.io()) {
          registerRepository.changeManagingEntity(
            event.eligibleManagingEntity.logicalId,
            event.eligibleManagingEntity.groupId
          )
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

  /**
   * Validates the given Questionnaire Response using the SDK [QuestionnaireResponseValidator].
   * */
  private fun isQuestionnaireResponseValid(
    questionnaire: Questionnaire,
    questionnaireResponse: QuestionnaireResponse,
    context: Context
  ): Boolean {
    return try {
      QuestionnaireResponseValidator.checkQuestionnaireResponse(
        questionnaire,
        questionnaireResponse
      )
      QuestionnaireResponseValidator.validateQuestionnaireResponse(
        questionnaire,
        questionnaireResponse,
        context
      )
      true
    } catch (e: IllegalArgumentException) {
      Timber.tag("ProfileViewModel.isQuestionnaireResponseValid").d(e)
      false
    }
  }

  /**
   * Gets a Questionnaire Response from the database if it exists. Generates Questionnaire Response from population, otherwise.
   * @param questionnaire Questionnaire as the basis for how the resources are to be populated
   * @param subjectId ID of the resource that submitted the Questionnaire Response, and related with the population resources
   * @param subjectType resource type of the resource that submitted the Questionnaire Response
   * */
  private suspend fun getQuestionnaireResponseFromDbOrPopulation(
    questionnaire: Questionnaire,
    subjectId: String,
    subjectType: ResourceType,
  ): QuestionnaireResponse {
    var questionnaireResponse =
      loadQuestionnaireResponse(subjectId, subjectType, questionnaire.logicalId)

    if (questionnaireResponse == null) {
      val populationResources = loadPopulationResources(subjectId, subjectType)
      questionnaireResponse = populateQuestionnaireResponse(questionnaire, populationResources)
    }

    return questionnaireResponse
  }

  /**
   * Generates a Questionnaire Response by populating the given resources.
   * @param questionnaire Questionnaire as the basis for how the resources are to be populated
   * @param populationResources resources to be populated
   * */
  private suspend fun populateQuestionnaireResponse(
    questionnaire: Questionnaire,
    populationResources: ArrayList<Resource>
  ): QuestionnaireResponse {
    return ResourceMapper.populate(questionnaire, *populationResources.toTypedArray())
      .also { questionnaireResponse ->
        if (!questionnaireResponse.hasItem()) {
          Timber.tag("ProfileViewModel.populateQuestionnaireResponse").d("Questionnaire response has no populated answers")
        }
      }
  }

  /**
   * Loads the latest Questionnaire Response resource that is associated with the given subject ID and Questionnaire ID.
   * @param subjectId ID of the resource that submitted the Questionnaire Response
   * @param subjectType resource type of the resource that submitted the Questionnaire Response
   * @param questionnaireId ID of the Questionnaire that owns the Questionnaire Response
   * */
  private suspend fun loadQuestionnaireResponse(
    subjectId: String,
    subjectType: ResourceType,
    questionnaireId: String
  ): QuestionnaireResponse? {
    return searchQuestionnaireResponses(
      subjectId = subjectId,
      subjectType = subjectType,
      questionnaireId = questionnaireId
    )
      .maxByOrNull { it.meta.lastUpdated }
      .also { questionnaireResponse ->
        if (questionnaireResponse == null) {
          Timber.tag("ProfileViewModel.loadQuestionnaireResponse").d("Questionnaire response is not found in database")
        }
      }
  }

  /**
   * Search Questionnaire Response resources that are associated with the given subject ID and Questionnaire ID.
   * @param subjectId ID of the resource that submitted the Questionnaire Response
   * @param subjectType resource type of the resource that submitted the Questionnaire Response
   * @param questionnaireId ID of the Questionnaire that owns the Questionnaire Response
   * */
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

  /** Loads a Questionnaire resource with the given ID. */
  private suspend fun loadQuestionnaire(questionnaireId: String): Questionnaire? {
    return registerRepository.loadResource(questionnaireId)
  }

  /**
   * Loads resources to be populated into a Questionnaire Response.
   * @param subjectId can be Patient ID or Group ID
   * @param subjectType resource type of the ID
   */
  private suspend fun loadPopulationResources(
    subjectId: String,
    subjectType: ResourceType
  ): ArrayList<Resource> {
    val populationResources = arrayListOf<Resource>()
    when (subjectType) {
      ResourceType.Patient -> {
        loadPatient(subjectId)?.run { populationResources.add(this) }
        loadRelatedPerson(subjectId)?.run { populationResources.add(this) }
      }
      ResourceType.Group -> {
        loadGroup(subjectId)?.run { populationResources.add(this) }
      }
      else -> {
        Timber.tag("ProfileViewModel.loadPopulationResources")
          .d("$subjectType resource type is not supported to load populated resources!")
      }
    }
    return populationResources
  }

  /** Loads a Patient resource with the given ID. */
  private suspend fun loadPatient(patientId: String): Patient? {
    return registerRepository.loadResource(patientId)
  }

  /** Loads a Group resource with the given ID. */
  private suspend fun loadGroup(groupId: String): Group? {
    return registerRepository.loadResource(groupId)
  }

  /** Loads a RelatedPerson resource that belongs to the given Patient ID. */
  private suspend fun loadRelatedPerson(patientId: String): RelatedPerson? {
    return registerRepository
      .searchResourceFor<RelatedPerson>(
        subjectType = ResourceType.Patient,
        subjectId = patientId,
        subjectParam = RelatedPerson.PATIENT,
      )
      .singleOrNull()
  }

  /**
   * This function launches a configurable dialog for selecting new managing entity from the list of
   * [Group] resource members. This function only works when [Group] resource is the used as the
   * main resource.
   */
  private fun changeManagingEntity(event: ProfileEvent.OverflowMenuClick) {
    if (event.managingEntity == null || event.resourceData?.baseResourceType != ResourceType.Group
    ) {
      Timber.w("ManagingEntityConfig required. Base resource should be Group")
      return
    }
    viewModelScope.launch {
      val group = registerRepository.loadResource<Group>(event.resourceData.baseResourceId)
      val eligibleManagingEntities: List<EligibleManagingEntity> =
        group
          ?.member
          ?.map {
            registerRepository.loadResource(
              it.entity.extractId(),
              event.managingEntity.resourceType
            )
          }
          ?.filter { managingEntityResource ->
            fhirPathDataExtractor
              .extractValue(
                base = managingEntityResource,
                expression = event.managingEntity.eligibilityCriteriaFhirPathExpression
              )
              .toBoolean()
          }
          ?.map {
            EligibleManagingEntity(
              groupId = event.resourceData.baseResourceId,
              logicalId = it.logicalId.extractLogicalIdUuid(),
              memberInfo =
                fhirPathDataExtractor.extractValue(it, event.managingEntity.nameFhirPathExpression)
            )
          }
          ?: emptyList()

      // Show error message when no group members are found
      if (eligibleManagingEntities.isEmpty()) {
        emitSnackBarState(
          SnackBarMessageConfig(message = event.managingEntity.noMembersErrorMessage)
        )
      } else {
        (event.navController.context.getActivity())?.let { activity ->
          ProfileBottomSheetFragment(
              eligibleManagingEntities = eligibleManagingEntities,
              onSaveClick = {
                onEvent(
                  ProfileEvent.OnChangeManagingEntity(
                    context = activity,
                    eligibleManagingEntity = it,
                    managingEntityConfig = event.managingEntity
                  )
                )
              },
              managingEntity = event.managingEntity
            )
            .run { show(activity.supportFragmentManager, ProfileBottomSheetFragment.TAG) }
        }
      }
    }
  }
}
