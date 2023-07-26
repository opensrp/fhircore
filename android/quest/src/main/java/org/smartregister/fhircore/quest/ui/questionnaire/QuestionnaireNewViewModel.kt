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
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.Search
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.context.IWorkerContext
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Group
import org.hl7.fhir.r4.model.ListResource
import org.hl7.fhir.r4.model.ListResource.ListEntryComponent
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.cql.LibraryEvaluator
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
import org.smartregister.fhircore.engine.util.extension.cqfLibraryIds
import org.smartregister.fhircore.engine.util.extension.extractByStructureMap
import org.smartregister.fhircore.engine.util.extension.extractLogicalIdUuid
import org.smartregister.fhircore.engine.util.extension.isIn
import org.smartregister.fhircore.engine.util.extension.prePopulateInitialValues
import org.smartregister.fhircore.engine.util.extension.prepareQuestionsForReadingOrEditing
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
  val libraryEvaluator: LibraryEvaluator,
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
    actionParameters: List<ActionParameter>?,
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
        if (questionnaireConfig.type.isReadOnly() || questionnaireConfig.type.isEditable()) {
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
   * This function performs data extraction against the [QuestionnaireResponse]. All the resources
   * generated from a successful extraction by StructureMap or definition are stored in the
   * database. The [QuestionnaireResponse] is also stored in the database regardless of the outcome
   * of [ResourceMapper.extract]. This function will optionally generate CarePlan using the
   * PlanDefinition resource configured in [QuestionnaireConfig.planDefinitions]. The
   * [QuestionnaireConfig.eventWorkflows] contains configurations to cascade update the statuses of
   * resources to in-active (close) that are related to the current [QuestionnaireResponse.subject]
   */
  fun handleQuestionnaireSubmission(
    questionnaire: Questionnaire,
    currentQuestionnaireResponse: QuestionnaireResponse,
    questionnaireConfig: QuestionnaireConfig,
    actionParameters: List<ActionParameter>,
    context: Context,
  ) {
    viewModelScope.launch(SupervisorJob()) {
      val questionnaireResponseValid =
        validateQuestionnaireResponse(
          questionnaire = questionnaire,
          questionnaireResponse = currentQuestionnaireResponse,
          context = context,
        )

      if (!questionnaireResponseValid) {
        Timber.e("Invalid questionnaire response")
        context.showToast(context.getString(R.string.questionnaire_response_invalid))
        return@launch
      }

      currentQuestionnaireResponse.processMetadata(questionnaire)

      val bundle =
        performExtraction(
          extractByStructureMap = questionnaire.extractByStructureMap(),
          questionnaire = questionnaire,
          questionnaireResponse = currentQuestionnaireResponse,
          context = context,
        )

      val extractionDate = Date()

      // Create a ListResource to store the references for generated resources
      val listResource =
        ListResource().apply {
          id = UUID.randomUUID().toString()
          status = ListResource.ListStatus.CURRENT
          mode = ListResource.ListMode.WORKING
          title = CONTAINED_LIST_TITLE
          date = extractionDate
        }

      val group =
        questionnaireConfig.groupResource?.groupIdentifier?.let { groupId ->
          defaultRepository.loadResource<Group>(groupId)
        }

      bundle?.entry?.forEach { bundleEntryComponent ->
        val bundleEntryResource: Resource? = bundleEntryComponent.resource
        bundleEntryResource?.run {
          applyResourceMetadata()
          defaultRepository.addOrUpdate(resource = this)

          // Add resource as member of a (configured) Group
          group?.let { bundleEntryResource.addMemberToGroup(it) }

          // Track ids for resources in ListResource added to the QuestionnaireResponse.contained
          val listEntryComponent =
            ListEntryComponent().apply {
              deleted = false
              date = extractionDate
              item = bundleEntryResource.asReference()
            }
          listResource.addEntry(listEntryComponent)
        }
      }

      val subject =
        retrieveSubject(
            questionnaire = questionnaire,
            questionnaireConfig = questionnaireConfig,
            bundle = bundle,
          )
          ?.apply { id = "$resourceType/$logicalId" }

      // Update Group resource to save members
      group?.let { defaultRepository.addOrUpdate(resource = it) }

      // Save questionnaire response
      if (subject != null) {
        defaultRepository.addOrUpdate(
          resource =
            currentQuestionnaireResponse.apply {
              setQuestionnaire("${questionnaire.resourceType}/${questionnaire.logicalId}")
              setSubject(subject.asReference())
              addContained(listResource)
            },
        )
      }

      // Update _lastUpdated for resources configured via ActionParameterType.UPDATE_DATE_ON_EDIT
      updateResourcesLastUpdatedProperty(actionParameters)

      if (subject != null && bundle != null) {
        // Generate CarePlan using configured plan definitions
        val newBundle = bundle.copyBundle(currentQuestionnaireResponse)
        generateCarePlan(
          subject = subject,
          bundle = newBundle,
          questionnaireConfig = questionnaireConfig,
        )

        // Execute CQL
        executeCql(
          subject = subject,
          bundle = newBundle,
          questionnaire = questionnaire,
        )

        // Cascade update statuses of resources closed by this Questionnaire submission
        viewModelScope.launch {
          fhirCarePlanGenerator.conditionallyUpdateResourceStatus(
            questionnaireConfig = questionnaireConfig,
            subject = subject,
            bundle = bundle,
          )
        }
      }

      softDeleteResources(questionnaireConfig)
    }
  }

  private fun Bundle.copyBundle(currentQuestionnaireResponse: QuestionnaireResponse): Bundle =
    this.copy().apply {
      addEntry(Bundle.BundleEntryComponent().apply { resource = currentQuestionnaireResponse })
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

  /**
   * Perform StructureMap or Definition based definition. The result of this function returns a
   * Bundle that contains the resources that were generated via the [ResourceMapper.extract]
   * operation otherwise returns null if an exception is encountered.
   */
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
  suspend fun updateResourcesLastUpdatedProperty(actionParameters: List<ActionParameter>?) {
    val updateOnEditParams =
      actionParameters?.filter {
        it.paramType == ActionParameterType.UPDATE_DATE_ON_EDIT && it.value.isNotEmpty()
      }

    updateOnEditParams?.forEach { param ->
      try {
        val resource = defaultRepository.loadResource(Reference(param.value))
        defaultRepository.addOrUpdate(resource = resource)
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

  suspend fun executeCql(subject: Resource, bundle: Bundle, questionnaire: Questionnaire) {
    questionnaire.cqfLibraryIds().forEach {
      if (subject.resourceType == ResourceType.Patient) {
        libraryEvaluator.runCqlLibrary(it, subject as Patient, bundle)
      }
    }
  }

  /**
   * This function generates CarePlans for the [QuestionnaireResponse.subject] using the configured
   * [QuestionnaireConfig.planDefinitions]
   */
  suspend fun generateCarePlan(
    subject: Resource,
    bundle: Bundle,
    questionnaireConfig: QuestionnaireConfig,
  ) {
    questionnaireConfig.planDefinitions?.forEach { planId ->
      kotlin
        .runCatching {
          fhirCarePlanGenerator.generateOrUpdateCarePlan(
            planDefinitionId = planId,
            subject = subject,
            data = bundle,
          )
        }
        .onFailure { Timber.e(it) }
    }
  }

  /**
   * This retrieves and returns [Resource] as subject if [QuestionnaireConfig.resourceType] and
   * [QuestionnaireConfig.resourceIdentifier] are provided. This will be the typical case for
   * subsequent [Questionnaire] submissions where the [QuestionnaireResponse.subject] already
   * exists. For instances where the [Questionnaire] is submitted the first time (e.g. during
   * registration events), the subject [Resource] will be included and accessed from the extracted
   * resources in the [Bundle.entry]
   */
  suspend fun retrieveSubject(
    questionnaire: Questionnaire,
    questionnaireConfig: QuestionnaireConfig,
    bundle: Bundle?,
  ): Resource? {
    val questionnaireSubjectType = questionnaire.subjectType.firstOrNull()?.code
    val resourceType =
      questionnaireConfig.resourceType ?: questionnaireSubjectType?.let { ResourceType.valueOf(it) }
    val resourceIdentifier = questionnaireConfig.resourceIdentifier

    if (resourceType != null && !resourceIdentifier.isNullOrEmpty()) {
      return loadResource(resourceType, resourceIdentifier)
    }
    questionnaire.subjectType.forEach {
      val resourceType = ResourceType.valueOf(it.code)
      return bundle
        ?.entry
        ?.firstOrNull { entryComponent -> entryComponent.resource.resourceType == resourceType }
        ?.resource
    }
    return null
  }

  /** Adds [Resource] to [Group.member] */
  fun Resource.addMemberToGroup(group: Group) {
    this.run {
      if (
        this.resourceType.isIn(
          ResourceType.CareTeam,
          ResourceType.Device,
          ResourceType.Group,
          ResourceType.HealthcareService,
          ResourceType.Location,
          ResourceType.Organization,
          ResourceType.Patient,
          ResourceType.Practitioner,
          ResourceType.PractitionerRole,
          ResourceType.Specimen,
        )
      ) {
        group.addMember(Group.GroupMemberComponent().apply { entity = asReference() })
      }
    }
  }

  /**
   * This function triggers removal of [Resource] s as per the [QuestionnaireConfig.groupResource]
   * or [QuestionnaireConfig.removeResource] config properties.
   */
  fun softDeleteResources(questionnaireConfig: QuestionnaireConfig) {
    if (questionnaireConfig.groupResource != null) {
      removeGroup(
        groupId = questionnaireConfig.groupResource!!.groupIdentifier,
        removeGroup = questionnaireConfig.groupResource?.removeGroup ?: false,
        deactivateMembers = questionnaireConfig.groupResource!!.deactivateMembers,
      )
      removeGroupMember(
        memberId = questionnaireConfig.resourceIdentifier,
        removeMember = questionnaireConfig.groupResource?.removeMember ?: false,
        groupIdentifier = questionnaireConfig.groupResource!!.groupIdentifier,
        memberResourceType = questionnaireConfig.groupResource!!.memberResourceType,
      )
    }

    if (
      questionnaireConfig.removeResource == true &&
        questionnaireConfig.resourceType != null &&
        !questionnaireConfig.resourceIdentifier.isNullOrEmpty()
    ) {
      viewModelScope.launch {
        defaultRepository.delete(
          resourceType = questionnaireConfig.resourceType!!,
          resourceId = questionnaireConfig.resourceIdentifier!!,
          softDelete = true,
        )
      }
    }
  }

  private fun removeGroup(groupId: String, removeGroup: Boolean, deactivateMembers: Boolean) {
    if (removeGroup) {
      viewModelScope.launch(dispatcherProvider.io()) {
        try {
          defaultRepository.removeGroup(
            groupId = groupId,
            isDeactivateMembers = deactivateMembers,
            configComputedRuleValues = emptyMap(),
          )
        } catch (exception: Exception) {
          Timber.e(exception)
        }
      }
    }
  }

  private fun removeGroupMember(
    memberId: String?,
    groupIdentifier: String?,
    memberResourceType: ResourceType?,
    removeMember: Boolean,
  ) {
    if (removeMember && !memberId.isNullOrEmpty()) {
      viewModelScope.launch(dispatcherProvider.io()) {
        try {
          defaultRepository.removeGroupMember(
            memberId = memberId,
            groupId = groupIdentifier,
            groupMemberResourceType = memberResourceType,
            configComputedRuleValues = emptyMap(),
          )
        } catch (exception: Exception) {
          Timber.e(exception)
        }
      }
    }
  }

  /**
   * This function searches and returns the latest [QuestionnaireResponse] for the given
   * [resourceId] that was extracted from the [Questionnaire] identified as [questionnaireId].
   * Returns null if non is found.
   */
  suspend fun searchLatestQuestionnaireResponse(
    resourceId: String,
    resourceType: ResourceType,
    questionnaireId: String,
  ): QuestionnaireResponse? {
    val search =
      Search(ResourceType.QuestionnaireResponse).apply {
        filter(QuestionnaireResponse.SUBJECT, { value = "$resourceType/$resourceId" })
        filter(
          QuestionnaireResponse.QUESTIONNAIRE,
          { value = "${ResourceType.Questionnaire}/$questionnaireId" },
        )
      }
    val questionnaireResponses: List<QuestionnaireResponse> = defaultRepository.search(search)
    return questionnaireResponses.maxByOrNull { it.meta.lastUpdated }
  }

  /**
   * Return [Resource]s to be used in the launch context of the questionnaire. Launch context allows
   * information to be passed into questionnaire based on the context in which the questionnaire is
   * being evaluated. For example, what patient, what encounter, what user, etc. is "in context" at
   * the time the questionnaire response is being completed:
   * https://build.fhir.org/ig/HL7/sdc/StructureDefinition-sdc-questionnaire-launchContext.html
   */
  suspend fun retrievePopulationResources(actionParameters: List<ActionParameter>): List<Resource> {
    return actionParameters
      .filter {
        it.paramType == ActionParameterType.QUESTIONNAIRE_RESPONSE_POPULATION_RESOURCE &&
          it.resourceType != null &&
          it.value.isNotEmpty()
      }
      .mapNotNull {
        try {
          loadResource(it.resourceType!!, it.value)
        } catch (resourceNotFoundException: ResourceNotFoundException) {
          null
        }
      }
  }

  /** Load [Resource] of type [ResourceType] for the provided [resourceIdentifier] */
  suspend fun loadResource(resourceType: ResourceType, resourceIdentifier: String): Resource? =
    try {
      defaultRepository.loadResource(resourceIdentifier, resourceType)
    } catch (resourceNotFoundException: ResourceNotFoundException) {
      null
    }

  companion object {
    const val CONTAINED_LIST_TITLE = "GeneratedResourcesList"
  }
}
