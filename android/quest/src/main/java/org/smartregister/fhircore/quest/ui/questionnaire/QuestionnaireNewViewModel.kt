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

package org.smartregister.fhircore.quest.ui.questionnaire

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.fhir.datacapture.mapping.ResourceMapper
import com.google.android.fhir.datacapture.mapping.StructureMapExtractionContext
import com.google.android.fhir.datacapture.validation.NotValidated
import com.google.android.fhir.datacapture.validation.QuestionnaireResponseValidator
import com.google.android.fhir.datacapture.validation.Valid
import com.google.android.fhir.db.ResourceNotFoundException
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.context.IWorkerContext
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.ListResource
import org.hl7.fhir.r4.model.ListResource.ListEntryComponent
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.domain.model.ActionParameter
import org.smartregister.fhircore.engine.domain.model.ActionParameterType
import org.smartregister.fhircore.engine.rulesengine.ResourceDataRulesExecutor
import org.smartregister.fhircore.engine.task.FhirCarePlanGenerator
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.DEFAULT_PLACEHOLDER_PREFIX
import org.smartregister.fhircore.engine.util.extension.appendOrganizationInfo
import org.smartregister.fhircore.engine.util.extension.appendPractitionerInfo
import org.smartregister.fhircore.engine.util.extension.asReference
import org.smartregister.fhircore.engine.util.extension.extractByStructureMap
import org.smartregister.fhircore.engine.util.extension.extractLogicalIdUuid
import org.smartregister.fhircore.engine.util.extension.prePopulateInitialValues
import org.smartregister.fhircore.engine.util.extension.prepareQuestionsForReadingOrEditing
import org.smartregister.fhircore.engine.util.extension.resourceClassType
import org.smartregister.fhircore.engine.util.extension.showToast
import org.smartregister.fhircore.engine.util.extension.updateLastUpdated
import org.smartregister.fhircore.engine.util.helper.TransformSupportServices
import org.smartregister.fhircore.quest.R
import timber.log.Timber

@HiltViewModel
class QuestionnaireNewViewModel
@Inject
constructor(
  val defaultRepository: DefaultRepository,
  val dispatcherProvider: DispatcherProvider,
  val fhirCarePlanGenerator: FhirCarePlanGenerator,
  val resourceDataRulesExecutor: ResourceDataRulesExecutor,
  val transformSupportServices: TransformSupportServices,
  val sharedPreferencesHelper: SharedPreferencesHelper,
) : ViewModel() {

  private val authenticatedOrganizationIds by lazy {
    sharedPreferencesHelper.read<List<String>>(ResourceType.Organization.name)
  }

  private val practitionerId: String? by lazy {
    sharedPreferencesHelper
      .read(SharedPreferenceKey.PRACTITIONER_ID.name, null)
      ?.extractLogicalIdUuid()
  }

  /**
   * This function retrieves the [Questionnaire] as configured via the [QuestionnaireConfig]. The
   * retrieved [Questionnaire] can be pre-populated with computed values from the Rules engine.
   */
  suspend fun retrieveQuestionnaire(
    questionnaireConfig: QuestionnaireConfig,
    actionParameters: Array<ActionParameter>?,
  ): Questionnaire? {
    if (questionnaireConfig.id.isEmpty()) return null

    // Compute questionnaire config rules and add extra questionnaire params to action parameters
    val questionnaireComputedValues =
      questionnaireConfig.configRules?.let {
        resourceDataRulesExecutor.computeResourceDataRules(it, null)
      }
        ?: emptyMap()

    val allActionParameters =
      actionParameters?.plus(
        questionnaireConfig.extraParams?.map { it.interpolate(questionnaireComputedValues) }
          ?: emptyList(),
      )

    val questionnaire =
      defaultRepository.loadResource<Questionnaire>(questionnaireConfig.id)?.apply {
        if (questionnaireConfig.type.isReadOnly() || questionnaireConfig.type.isEditMode()) {
          item.prepareQuestionsForReadingOrEditing(
            readOnly = questionnaireConfig.type.isReadOnly(),
            readOnlyLinkIds = questionnaireConfig.readOnlyLinkIds,
          )
        }

        // Pre-populate questionnaire items with configured values
        allActionParameters
          ?.filter { (it.paramType == ActionParameterType.PREPOPULATE && it.value.isNotEmpty()) }
          ?.let { actionParam ->
            item.prePopulateInitialValues(DEFAULT_PLACEHOLDER_PREFIX, actionParam)
          }
      }
    return questionnaire
  }

  /**
   * This function performs data extraction against the [QuestionnaireResponse]. The generated
   * resources are then saved in the database. Optionally, this function will generate CarePlan
   * using the PlanDefinition resource configured in [QuestionnaireConfig.planDefinitions]
   */
  fun handleQuestionnaireSubmission(
    questionnaire: Questionnaire,
    currentQuestionnaireResponse: QuestionnaireResponse,
    questionnaireConfig: QuestionnaireConfig,
    actionParameters: Array<ActionParameter>?,
    context: Context,
  ) {
    val questionnaireResponseValid =
      validateQuestionnaireResponse(
        questionnaire = questionnaire,
        questionnaireResponse = currentQuestionnaireResponse,
        context = context,
      )

    if (!questionnaireResponseValid) {
      Timber.e("Invalid questionnaire response")
      context.showToast(context.getString(R.string.questionnaire_response_invalid))
      return
    }

    currentQuestionnaireResponse.processMetadata(questionnaire)

    viewModelScope.launch {
      val bundle =
        performExtraction(
          extractByStructureMap = questionnaire.extractByStructureMap(),
          questionnaire = questionnaire,
          questionnaireResponse = currentQuestionnaireResponse,
          context = context,
        )

      val containedList = ListResource().apply { id = UUID.randomUUID().toString() }
      bundle?.entry?.forEach { bundleEntryComponent ->
        val bundleEntryResource: Resource? = bundleEntryComponent.resource
        bundleEntryResource?.run {
          applyResourceMetadata()
          defaultRepository.addOrUpdate(resource = this)

          // Track ids for resources in ListResource added to the QuestionnaireResponse
          val listEntryComponent =
            ListEntryComponent().apply {
              deleted = false
              date = Date()
              item = bundleEntryResource.asReference()
            }
          containedList.addEntry(listEntryComponent)
        }
      }

      // Save questionnaire response
      if (bundle != null) {
        currentQuestionnaireResponse.addContained(containedList)
      }
      defaultRepository.addOrUpdate(resource = currentQuestionnaireResponse)

      actionParameters?.let { parameters -> updateResourcesLastUpdatedProperty(parameters) }

      // TODO Handle CarePlan generation and closing of configured resources
    }
  }

  private fun QuestionnaireResponse.processMetadata(questionnaire: Questionnaire) {
    status = QuestionnaireResponse.QuestionnaireResponseStatus.COMPLETED
    authored = Date()
    questionnaire.useContext
      .filter { it.hasValueCodeableConcept() }
      .forEach { it.valueCodeableConcept.coding.forEach { coding -> this.meta.addTag(coding) } }
    applyResourceMetadata()
  }

  private fun Resource?.applyResourceMetadata(): Resource? {
    this?.apply {
      appendOrganizationInfo(authenticatedOrganizationIds)
      appendPractitionerInfo(practitionerId)
      updateLastUpdated()
    }
    return this
  }

  suspend fun performExtraction(
    extractByStructureMap: Boolean,
    questionnaire: Questionnaire,
    questionnaireResponse: QuestionnaireResponse,
    context: Context,
  ): Bundle? =
    kotlin
      .runCatching {
        if (extractByStructureMap) {
          ResourceMapper.extract(
            questionnaire = questionnaire,
            questionnaireResponse = questionnaireResponse,
            StructureMapExtractionContext(
              context = context,
              transformSupportServices = transformSupportServices,
              structureMapProvider = { structureMapUrl: String?, _: IWorkerContext ->
                structureMapUrl?.substringAfterLast("/")?.let { defaultRepository.loadResource(it) }
              },
            ),
          )
        } else {
          ResourceMapper.extract(
            questionnaire = questionnaire,
            questionnaireResponse = questionnaireResponse,
          )
        }
      }
      .onFailure { exception ->
        Timber.e(exception)
        viewModelScope.launch {
          if (exception is NullPointerException && exception.message!!.contains("StructureMap")) {
            context.showToast(
              context.getString(R.string.structure_map_missing_message),
              Toast.LENGTH_LONG,
            )
          } else {
            context.showToast(
              context.getString(R.string.structuremap_failed, questionnaire.name),
              Toast.LENGTH_LONG,
            )
          }
        }
      }
      .getOrDefault(null)

  /**
   * This function saves [QuestionnaireResponse] as draft if any of the [QuestionnaireResponse.item]
   * has an answer.
   */
  fun saveDraftQuestionnaire(questionnaireResponse: QuestionnaireResponse) {
    val questionnaireHasAnswer =
      questionnaireResponse.item.any {
        it.answer.any { answerComponent -> answerComponent.hasValue() }
      }
    if (questionnaireHasAnswer) {
      viewModelScope.launch(dispatcherProvider.io()) {
        questionnaireResponse.status = QuestionnaireResponse.QuestionnaireResponseStatus.INPROGRESS
        defaultRepository.addOrUpdate(addMandatoryTags = true, resource = questionnaireResponse)
      }
    }
  }

  /**
   * This function updates the _lastUpdated property of resources configured by the
   * [ActionParameter.paramType] of [ActionParameterType.UPDATE_DATE_ON_EDIT]. Each time a
   * questionnaire is submitted, the affected resources last modified/updated date will also be
   * updated.
   */
  suspend fun updateResourcesLastUpdatedProperty(actionParameters: Array<ActionParameter>) {
    val updateOnEditParams =
      actionParameters.filter {
        it.paramType == ActionParameterType.UPDATE_DATE_ON_EDIT && it.value.isNotEmpty()
      }

    updateOnEditParams.forEach { param ->
      try {
        val resourceType =
          param.value.substringBefore("/").resourceClassType().newInstance().resourceType
        val resource =
          defaultRepository.loadResource(param.value.extractLogicalIdUuid(), resourceType)
        resource.let { defaultRepository.addOrUpdate(resource = it) }
      } catch (resourceNotFoundException: ResourceNotFoundException) {
        Timber.e("Unable to update resource's _lastUpdated", resourceNotFoundException)
      }
    }
  }

  /**
   * This function validates all [QuestionnaireResponse] and returns true if all the validation
   * result of [QuestionnaireResponseValidator] are [Valid] or [NotValidated] (validation is
   * optional on [Questionnaire] fields)
   */
  fun validateQuestionnaireResponse(
    questionnaire: Questionnaire,
    questionnaireResponse: QuestionnaireResponse,
    context: Context,
  ) =
    QuestionnaireResponseValidator.validateQuestionnaireResponse(
        questionnaire = questionnaire,
        questionnaireResponse = questionnaireResponse,
        context = context,
      )
      .values
      .flatten()
      .all { it is Valid || it is NotValidated }
}
