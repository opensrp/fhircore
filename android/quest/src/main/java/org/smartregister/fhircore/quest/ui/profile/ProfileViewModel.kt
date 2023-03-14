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
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.uhn.fhir.parser.IParser
import com.google.android.fhir.db.ResourceNotFoundException
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.search
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.LinkedList
import javax.inject.Inject
import kotlin.system.measureTimeMillis
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
import org.smartregister.fhircore.engine.configuration.profile.ProfileConfiguration
import org.smartregister.fhircore.engine.configuration.workflow.ApplicationWorkflow
import org.smartregister.fhircore.engine.data.local.register.RegisterRepository
import org.smartregister.fhircore.engine.domain.model.ActionParameter
import org.smartregister.fhircore.engine.domain.model.FhirResourceConfig
import org.smartregister.fhircore.engine.domain.model.RepositoryResourceData
import org.smartregister.fhircore.engine.domain.model.RepositoryResourceData2
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.domain.model.ResourceData2
import org.smartregister.fhircore.engine.domain.model.SnackBarMessageConfig
import org.smartregister.fhircore.engine.rulesengine.RulesExecutor
import org.smartregister.fhircore.engine.rulesengine.retrieveListProperties
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.extension.extractId
import org.smartregister.fhircore.engine.util.extension.extractLogicalIdUuid
import org.smartregister.fhircore.engine.util.extension.getActivity
import org.smartregister.fhircore.engine.util.extension.interpolate
import org.smartregister.fhircore.engine.util.extension.resourceClassType
import org.smartregister.fhircore.engine.util.fhirpath.FhirPathDataExtractor
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.ui.profile.bottomSheet.ProfileBottomSheetFragment
import org.smartregister.fhircore.quest.ui.profile.model.EligibleManagingEntity
import org.smartregister.fhircore.quest.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.quest.ui.shared.QuestionnaireHandler
import org.smartregister.fhircore.quest.util.convertActionParameterArrayToMap
import timber.log.Timber

@HiltViewModel
class ProfileViewModel
@Inject
constructor(
  val registerRepository: RegisterRepository,
  val configurationRegistry: ConfigurationRegistry,
  val dispatcherProvider: DispatcherProvider,
  val fhirPathDataExtractor: FhirPathDataExtractor,
  val parser: IParser,
  val rulesExecutor: RulesExecutor
) : ViewModel() {

  val profileUiState = mutableStateOf(ProfileUiState())
  val resourceDataState = MutableLiveData<ResourceData?>(null)
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
    paramsList: Array<ActionParameter>?
  ) {
    if (resourceId.isNotEmpty()) {
      val paramsMap: Map<String, String> = convertActionParameterArrayToMap(paramsList)
      val profileConfigs = retrieveProfileConfiguration(profileId, paramsMap)

      // Load the base resource and fire the rules
      val repoResourceData =
        registerRepository.loadProfileData(profileId, resourceId, fhirResourceConfig, paramsList)

      val resourceData =
        rulesExecutor
          .processResourceData(
            baseResource = repoResourceData.resource,
            relatedRepositoryResourceData = LinkedList(repoResourceData.relatedResources),
            ruleConfigs = profileConfigs.rules,
            ruleConfigsKey = profileConfigs::class.java.canonicalName,
            paramsMap
          )
          .copy(listResourceDataMap = listResourceDataMapState)

      resourceDataState.postValue(resourceData)

      profileUiState.value =
        ProfileUiState(
          resourceData = resourceData,
          profileConfiguration = profileConfigs,
          snackBarTheme = applicationConfiguration.snackBarTheme,
          showDataLoadProgressIndicator = false
        )

      val resourceConfig = fhirResourceConfig ?: profileConfiguration.fhirResource
      val baseResourceConfig = resourceConfig.baseResource
      val baseResourceClass = baseResourceConfig.resource.resourceClassType()
      val baseResourceType = baseResourceClass.newInstance().resourceType

      val relatedResources = MutableLiveData<LinkedList<RepositoryResourceData>>()

      val repositoryResourceData = RepositoryResourceData2(
        configId = baseResourceConfig.id ?: baseResourceType.name,
        resource = repoResourceData.resource,
        relatedResources = relatedResources
      )

      registerRepository.loadOtherProfileData(repoResourceData.resource,
        profileId, resourceId, fhirResourceConfig, paramsList, repositoryResourceData)

      var nextPos = 0

      repositoryResourceData.relatedResources.observeForever {
        Timber.e("Received repository resource data $it with nextPos $nextPos")

        if (it.size <= nextPos) {
          return@observeForever
        }

        val repoDataList = LinkedList<RepositoryResourceData>().apply {
          add(it[nextPos])
        }

        nextPos++

        viewModelScope.launch(dispatcherProvider.io()) {
          val resourceDataSingle = rulesExecutor.processResourceData(
            repositoryResourceData.resource,
            repoDataList,
            ruleConfigs = profileConfigs.rules,
            ruleConfigsKey = profileConfigs::class.java.canonicalName,
            paramsMap
          )

          profileConfigs.views.retrieveListProperties().forEach {
            val listResourceData = mutableListOf<ResourceData>()
            listResourceDataMapState[it.id] = listResourceData
            //val listResourceData =
            rulesExecutor.processListResourceData(
              listProperties = it,
              listResourceData,
              listResourceDataMapState,
              relatedRepositoryResourceData = repoDataList,
              computedValuesMap =
              resourceDataSingle.computedValuesMap.toMutableMap().plus(paramsMap).toMap()
            )
            //listResourceDataMapState[it.id] = listResourceData
          }

          Timber.e("Finished loading a related resource item list $repoDataList")

          resourceData.listResourceDataMap.putAll(resourceDataSingle.listResourceDataMap)

          resourceDataState.postValue(resourceData)
        }
      }

      /*val resourceData2 =
        rulesExecutor
          .processResourceData(
            baseResource = repoResourceData2.resource,
            relatedRepositoryResourceData = LinkedList(repoResourceData2.relatedResources),
            ruleConfigs = profileConfigs.rules,ss
            ruleConfigsKey = profileConfigs::class.java.canonicalName,
            paramsMap
          )
          .copy(listResourceDataMap = listResourceDataMapState)*/

      /*profileUiState.value =
        ProfileUiState(
          resourceData = resourceData2,
          profileConfiguration = profileConfigs,
          snackBarTheme = applicationConfiguration.snackBarTheme,
          showDataLoadProgressIndicator = false
        )*/



      // Working before
      /*val timeToFireRules = measureTimeMillis {
        profileConfigs.views.retrieveListProperties().forEach {
          val listResourceData = mutableListOf<ResourceData>()
          listResourceDataMapState[it.id] = listResourceData
          //val listResourceData =
            rulesExecutor.processListResourceData(
              listProperties = it,
              listResourceData,
              listResourceDataMapState,
              relatedRepositoryResourceData = LinkedList(repoResourceData2.relatedResources),
              computedValuesMap =
                resourceData2.computedValuesMap.toMutableMap().plus(paramsMap).toMap()
            )
          //listResourceDataMapState[it.id] = listResourceData
        }
      }*/

      Timber.e("About to invoke the profileUiState again")
      //resourceDataState.postValue(resourceData2)
      Timber.e("Computed values map ${resourceData.computedValuesMap}")
      Timber.e("List resource data map ${resourceData.listResourceDataMap}")


      // Update other computed values slowly
      // Once done update the ProfileUIState
      /*registerRepository.loadOtherProfileData(profileId, resourceId, fhirResourceConfig, resourceData.baseResource!!, resourceData)

      Timber.e("Computed values map ${resourceData.computedValuesMap}")
      Timber.e("List resource data map ${resourceData.listResourceDataMap}")
      profileUiState.value = ProfileUiState(
        resourceData = ResourceData(
          baseResourceId = resourceData.baseResourceId,
          baseResourceType = resourceData.baseResourceType,
          computedValuesMap = resourceData.computedValuesMap,
          listResourceDataMap = resourceData.listResourceDataMap,
          baseResource = resourceData.baseResource
        ),
        profileConfiguration = retrieveProfileConfiguration(profileId, paramsMap),
        snackBarTheme = applicationConfiguration.snackBarTheme
      )
      resourceDataState.value = ResourceData(
        baseResourceId = resourceData.baseResourceId,
        baseResourceType = resourceData.baseResourceType,
        computedValuesMap = resourceData.computedValuesMap,
        listResourceDataMap = resourceData.listResourceDataMap,
        baseResource = resourceData.baseResource
      )
      //profileUiState.value = ProfileUiState()*/
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
        event.overflowMenuItemConfig?.actions?.forEach { actionConfig ->
          when (actionConfig.workflow) {
            ApplicationWorkflow.LAUNCH_QUESTIONNAIRE -> {
              actionConfig.questionnaire?.let { questionnaireConfig ->
                if (event.navController.context is QuestionnaireHandler) {
                  viewModelScope.launch {
                    var questionnaireResponse: String? = null

                    val questionnaireConfigInterpolated =
                      questionnaireConfig.interpolate(
                        event.resourceData?.computedValuesMap ?: emptyMap()
                      )
                    val params =
                      actionConfig
                        .params
                        .map {
                          ActionParameter(
                            key = it.key,
                            paramType = it.paramType,
                            dataType = it.dataType,
                            linkId = it.linkId,
                            value =
                              it.value.interpolate(
                                event.resourceData?.computedValuesMap ?: emptyMap()
                              )
                          )
                        }
                        .toTypedArray()

                    if (event.resourceData != null) {
                      questionnaireResponse =
                        searchQuestionnaireResponses(
                          subjectId = event.resourceData.baseResourceId.extractLogicalIdUuid(),
                          subjectType = event.resourceData.baseResourceType,
                          questionnaireId = questionnaireConfigInterpolated.id
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
                      questionnaireConfig = questionnaireConfigInterpolated,
                      actionParams = params.toList()
                    )
                  }
                }
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
          ?.mapNotNull {
            try {
              registerRepository.loadResource(
                it.entity.extractId(),
                event.managingEntity.resourceType!!
              )
            } catch (resourceNotFoundException: ResourceNotFoundException) {
              null
            }
          }
          ?.filter { managingEntityResource ->
            fhirPathDataExtractor
              .extractValue(
                base = managingEntityResource,
                expression = event.managingEntity.eligibilityCriteriaFhirPathExpression!!
              )
              .toBoolean()
          }
          ?.map {
            EligibleManagingEntity(
              groupId = event.resourceData.baseResourceId,
              logicalId = it.logicalId.extractLogicalIdUuid(),
              memberInfo =
                fhirPathDataExtractor.extractValue(
                  it,
                  event.managingEntity.nameFhirPathExpression!!
                )
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
        filter(
          QuestionnaireResponse.STATUS,
          { value = of(QuestionnaireResponse.QuestionnaireResponseStatus.INPROGRESS.name) }
        )
      }
    }
}
