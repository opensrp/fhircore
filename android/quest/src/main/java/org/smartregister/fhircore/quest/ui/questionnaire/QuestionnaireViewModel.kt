/*
 * Copyright 2021-2024 Ona Systems, Inc
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
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.uhn.fhir.context.FhirContext
import com.google.android.fhir.datacapture.mapping.ResourceMapper
import com.google.android.fhir.datacapture.mapping.StructureMapExtractionContext
import com.google.android.fhir.datacapture.validation.NotValidated
import com.google.android.fhir.datacapture.validation.QuestionnaireResponseValidator
import com.google.android.fhir.datacapture.validation.Valid
import com.google.android.fhir.db.ResourceNotFoundException
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.Search
import com.google.android.fhir.search.filter.TokenParamFilterCriterion
import com.google.android.fhir.search.search
import com.google.android.fhir.workflow.FhirOperator
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.context.IWorkerContext
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.Basic
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Group
import org.hl7.fhir.r4.model.IdType
import org.hl7.fhir.r4.model.Library
import org.hl7.fhir.r4.model.ListResource
import org.hl7.fhir.r4.model.ListResource.ListEntryComponent
import org.hl7.fhir.r4.model.Parameters
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.RelatedPerson
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.StringType
import org.smartregister.fhircore.engine.BuildConfig
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.GroupResourceConfig
import org.smartregister.fhircore.engine.configuration.LinkIdType
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.domain.model.ActionParameter
import org.smartregister.fhircore.engine.domain.model.ActionParameterType
import org.smartregister.fhircore.engine.domain.model.isEditable
import org.smartregister.fhircore.engine.domain.model.isReadOnly
import org.smartregister.fhircore.engine.rulesengine.ResourceDataRulesExecutor
import org.smartregister.fhircore.engine.task.FhirCarePlanGenerator
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.DEFAULT_PLACEHOLDER_PREFIX
import org.smartregister.fhircore.engine.util.extension.appendOrganizationInfo
import org.smartregister.fhircore.engine.util.extension.appendPractitionerInfo
import org.smartregister.fhircore.engine.util.extension.appendRelatedEntityLocation
import org.smartregister.fhircore.engine.util.extension.asReference
import org.smartregister.fhircore.engine.util.extension.cqfLibraryUrls
import org.smartregister.fhircore.engine.util.extension.extractByStructureMap
import org.smartregister.fhircore.engine.util.extension.extractId
import org.smartregister.fhircore.engine.util.extension.extractLogicalIdUuid
import org.smartregister.fhircore.engine.util.extension.find
import org.smartregister.fhircore.engine.util.extension.generateMissingId
import org.smartregister.fhircore.engine.util.extension.isIn
import org.smartregister.fhircore.engine.util.extension.prePopulateInitialValues
import org.smartregister.fhircore.engine.util.extension.prepareQuestionsForReadingOrEditing
import org.smartregister.fhircore.engine.util.extension.showToast
import org.smartregister.fhircore.engine.util.extension.updateLastUpdated
import org.smartregister.fhircore.engine.util.fhirpath.FhirPathDataExtractor
import org.smartregister.fhircore.engine.util.helper.TransformSupportServices
import org.smartregister.fhircore.quest.R
import timber.log.Timber

@HiltViewModel
class QuestionnaireViewModel
@Inject
constructor(
  val defaultRepository: DefaultRepository,
  val dispatcherProvider: DispatcherProvider,
  val fhirCarePlanGenerator: FhirCarePlanGenerator,
  val resourceDataRulesExecutor: ResourceDataRulesExecutor,
  val transformSupportServices: TransformSupportServices,
  val sharedPreferencesHelper: SharedPreferencesHelper,
  val fhirOperator: FhirOperator,
  val fhirPathDataExtractor: FhirPathDataExtractor,
  val configurationRegistry: ConfigurationRegistry,
) : ViewModel() {
  private val parser = FhirContext.forR4Cached().newJsonParser()

  private val authenticatedOrganizationIds by lazy {
    sharedPreferencesHelper.read<List<String>>(ResourceType.Organization.name)
  }

  private val practitionerId: String? by lazy {
    sharedPreferencesHelper
      .read(SharedPreferenceKey.PRACTITIONER_ID.name, null)
      ?.extractLogicalIdUuid()
  }

  private val _questionnaireProgressStateLiveData = MutableLiveData<QuestionnaireProgressState?>()
  val questionnaireProgressStateLiveData: LiveData<QuestionnaireProgressState?>
    get() = _questionnaireProgressStateLiveData

  val applicationConfiguration: ApplicationConfiguration by lazy {
    configurationRegistry.retrieveConfiguration(ConfigType.Application)
  }

  /**
   * This function retrieves the [Questionnaire] as configured via the [QuestionnaireConfig]. The
   * retrieved [Questionnaire] can be pre-populated with computed values from the Rules engine.
   */
  suspend fun retrieveQuestionnaire(
    questionnaireConfig: QuestionnaireConfig,
    actionParameters: List<ActionParameter>?,
  ): Questionnaire? {
    if (questionnaireConfig.id.isEmpty() || questionnaireConfig.id.isBlank()) return null

    // Compute questionnaire config rules and add extra questionnaire params to action parameters
    val questionnaireComputedValues =
      questionnaireConfig.configRules?.let {
        resourceDataRulesExecutor.computeResourceDataRules(it, null, emptyMap())
      } ?: emptyMap()

    val allActionParameters =
      actionParameters?.plus(
        questionnaireConfig.extraParams?.map { it.interpolate(questionnaireComputedValues) }
          ?: emptyList(),
      )

    val questionnaire =
      defaultRepository.loadResource<Questionnaire>(questionnaireConfig.id)?.apply {
        if (questionnaireConfig.isReadOnly() || questionnaireConfig.isEditable()) {
          item.prepareQuestionsForReadingOrEditing(
            readOnly = questionnaireConfig.isReadOnly(),
            readOnlyLinkIds =
              questionnaireConfig.readOnlyLinkIds
                ?: questionnaireConfig.linkIds
                  ?.filter { it.type == LinkIdType.READ_ONLY }
                  ?.map { it.linkId },
          )
        }

        // Pre-populate questionnaire items with configured values
        allActionParameters
          ?.filter { (it.paramType == ActionParameterType.PREPOPULATE && it.value.isNotEmpty()) }
          ?.let { actionParam ->
            item.prePopulateInitialValues(DEFAULT_PLACEHOLDER_PREFIX, actionParam)
          }

        // Set barcode to the configured linkId default: "patient-barcode"
        if (!questionnaireConfig.resourceIdentifier.isNullOrEmpty()) {
          (questionnaireConfig.barcodeLinkId
              ?: questionnaireConfig.linkIds?.firstOrNull { it.type == LinkIdType.BARCODE }?.linkId)
            ?.let { barcodeLinkId ->
              find(barcodeLinkId)?.apply {
                initial =
                  mutableListOf(
                    Questionnaire.QuestionnaireItemInitialComponent()
                      .setValue(StringType(questionnaireConfig.resourceIdentifier)),
                  ) // TODO should this be resource identifier or OpenSrp unique ID?
                readOnly = true
              }
            }
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
    onSuccessfulSubmission: (List<IdType>, QuestionnaireResponse) -> Unit,
  ) {
    viewModelScope.launch(SupervisorJob()) {
      val questionnaireResponseValid =
        validateQuestionnaireResponse(
          questionnaire = questionnaire,
          questionnaireResponse = currentQuestionnaireResponse,
          context = context,
        )

      if (questionnaireConfig.saveQuestionnaireResponse && !questionnaireResponseValid) {
        Timber.e("Invalid questionnaire response")
        context.showToast(context.getString(R.string.questionnaire_response_invalid))
        setProgressState(QuestionnaireProgressState.ExtractionInProgress(false))
        return@launch
      }

      currentQuestionnaireResponse.processMetadata(questionnaire, questionnaireConfig, context)

      val bundle =
        performExtraction(
          extractByStructureMap = questionnaire.extractByStructureMap(),
          questionnaire = questionnaire,
          questionnaireResponse = currentQuestionnaireResponse,
          context = context,
        )

      saveExtractedResources(
        bundle = bundle,
        questionnaire = questionnaire,
        questionnaireConfig = questionnaireConfig,
        questionnaireResponse = currentQuestionnaireResponse,
        context = context,
      )

      updateResourcesLastUpdatedProperty(actionParameters)

      // Important to load subject resource to retrieve ID (as reference) correctly
      val subjectIdType: IdType? =
        if (currentQuestionnaireResponse.subject.reference.isNullOrEmpty()) {
          null
        } else {
          IdType(currentQuestionnaireResponse.subject.reference)
        }

      if (subjectIdType != null) {
        val subject =
          loadResource(ResourceType.valueOf(subjectIdType.resourceType), subjectIdType.idPart)

        if (subject != null && !questionnaireConfig.isReadOnly()) {
          val newBundle = bundle.copyBundle(currentQuestionnaireResponse)

          generateCarePlan(
            subject = subject,
            bundle = newBundle,
            questionnaireConfig = questionnaireConfig,
          )

          withContext(dispatcherProvider.io()) {
            executeCql(
              subject = subject,
              bundle = newBundle,
              questionnaire = questionnaire,
              questionnaireConfig = questionnaireConfig,
            )
          }

          fhirCarePlanGenerator.conditionallyUpdateResourceStatus(
            questionnaireConfig = questionnaireConfig,
            subject = subject,
            bundle = newBundle,
          )
        }
      }

      softDeleteResources(questionnaireConfig)

      val idTypes =
        bundle.entry?.map { IdType(it.resource.resourceType.name, it.resource.logicalId) }
          ?: emptyList()
      onSuccessfulSubmission(idTypes, currentQuestionnaireResponse)
    }
  }

  suspend fun saveExtractedResources(
    bundle: Bundle,
    questionnaire: Questionnaire,
    questionnaireConfig: QuestionnaireConfig,
    questionnaireResponse: QuestionnaireResponse,
    context: Context,
  ) {
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

    val subjectType = questionnaireSubjectType(questionnaire, questionnaireConfig)

    val previouslyExtractedResources =
      retrievePreviouslyExtractedResources(
        questionnaireConfig = questionnaireConfig,
        subjectType = subjectType,
        questionnaire = questionnaire,
      )

    val extractedResourceUniquePropertyExpressionsMap =
      questionnaireConfig.extractedResourceUniquePropertyExpressions?.associateBy {
        it.resourceType
      } ?: emptyMap()

    bundle.entry?.forEach { bundleEntryComponent ->
      bundleEntryComponent.resource?.run {
        applyResourceMetadata(questionnaireConfig, questionnaireResponse, context)
        if (
          questionnaireResponse.subject.reference.isNullOrEmpty() &&
            subjectType != null &&
            resourceType == subjectType &&
            logicalId.isNotEmpty()
        ) {
          questionnaireResponse.subject = this.logicalId.asReference(subjectType)
        }
        if (questionnaireConfig.isEditable()) {
          if (resourceType == subjectType) {
            this.id = questionnaireResponse.subject.extractId()
          } else if (
            extractedResourceUniquePropertyExpressionsMap.containsKey(resourceType) &&
              previouslyExtractedResources.containsKey(
                resourceType,
              )
          ) {
            val fhirPathExpression =
              extractedResourceUniquePropertyExpressionsMap
                .getValue(resourceType)
                .fhirPathExpression

            val currentResourceIdentifier =
              fhirPathDataExtractor.extractValue(
                base = this,
                expression = fhirPathExpression,
              )

            // Search for resource with property value matching extracted value
            val resource =
              previouslyExtractedResources.getValue(resourceType).find {
                val extractedValue =
                  fhirPathDataExtractor.extractValue(
                    base = it,
                    expression = fhirPathExpression,
                  )
                extractedValue.isNotEmpty() &&
                  extractedValue.equals(currentResourceIdentifier, true)
              }

            // Found match use the id on current resource; override identifiers for RelatedPerson
            if (resource != null) {
              this.id = resource.logicalId
              if (this is RelatedPerson && resource is RelatedPerson) {
                this.identifier = resource.identifier
              }
            }
          }
        }

        // Set the Group's Related Entity Location metadata tag on Resource before saving.
        this.applyRelatedEntityLocationMetaTag(questionnaireConfig, context, subjectType)

        defaultRepository.addOrUpdate(true, resource = this)

        updateGroupManagingEntity(
          resource = this,
          groupIdentifier = questionnaireConfig.groupResource?.groupIdentifier,
          managingEntityRelationshipCode = questionnaireConfig.managingEntityRelationshipCode,
        )
        addMemberToGroup(
          resource = this,
          memberResourceType = questionnaireConfig.groupResource?.memberResourceType,
          groupIdentifier = questionnaireConfig.groupResource?.groupIdentifier,
        )

        // Track ids for resources in ListResource added to the QuestionnaireResponse.contained
        val listEntryComponent =
          ListEntryComponent().apply {
            deleted = false
            date = extractionDate
            item = asReference()
          }
        listResource.addEntry(listEntryComponent)
      }
    }

    // Reference extracted resources in QR then save it if subject exists
    questionnaireResponse.apply { addContained(listResource) }

    if (
      !questionnaireResponse.subject.reference.isNullOrEmpty() &&
        questionnaireConfig.saveQuestionnaireResponse
    ) {
      // Set the Group's Related Entity Location meta tag on QuestionnaireResponse then save.
      questionnaireResponse.applyRelatedEntityLocationMetaTag(
        questionnaireConfig,
        context,
        subjectType,
      )
      defaultRepository.addOrUpdate(resource = questionnaireResponse)
    }
  }

  private suspend fun Resource.applyRelatedEntityLocationMetaTag(
    questionnaireConfig: QuestionnaireConfig,
    context: Context,
    subjectType: ResourceType?,
  ) {
    val resourceIdPair =
      when {
        !questionnaireConfig.resourceIdentifier.isNullOrEmpty() && subjectType != null -> {
          Pair(subjectType, questionnaireConfig.resourceIdentifier!!)
        }
        !questionnaireConfig.groupResource?.groupIdentifier.isNullOrEmpty() &&
          questionnaireConfig.groupResource?.removeGroup != true &&
          questionnaireConfig.groupResource?.removeMember != true -> {
          Pair(ResourceType.Group, questionnaireConfig.groupResource!!.groupIdentifier)
        }
        else -> null
      }
    if (resourceIdPair != null) {
      val (resourceType, resourceId) = resourceIdPair
      val resource = loadResource(resourceType = resourceType, resourceIdentifier = resourceId)
      if (resource != null) {
        val system =
          context.getString(
            org.smartregister.fhircore.engine.R.string.sync_strategy_related_entity_location_system,
          )
        resource.meta.tag.filter { coding -> coding.system == system }.forEach(this.meta::addTag)
      }
    }
  }

  private suspend fun retrievePreviouslyExtractedResources(
    questionnaireConfig: QuestionnaireConfig,
    subjectType: ResourceType?,
    questionnaire: Questionnaire,
  ): MutableMap<ResourceType, MutableList<Resource>> {
    val referencedResources = mutableMapOf<ResourceType, MutableList<Resource>>()
    if (
      questionnaireConfig.isEditable() &&
        !questionnaireConfig.resourceIdentifier.isNullOrEmpty() &&
        subjectType != null
    ) {
      searchLatestQuestionnaireResponse(
          resourceId = questionnaireConfig.resourceIdentifier!!,
          resourceType = questionnaireConfig.resourceType ?: subjectType,
          questionnaireId = questionnaire.logicalId,
        )
        ?.contained
        ?.asSequence()
        ?.filterIsInstance<ListResource>()
        ?.filter { it.title.equals(CONTAINED_LIST_TITLE, true) }
        ?.flatMap { it.entry }
        ?.forEach {
          val idType = IdType(it.item.reference)
          val resource = loadResource(ResourceType.fromCode(idType.resourceType), idType.idPart)
          if (resource != null) {
            referencedResources.getOrPut(resource.resourceType) { mutableListOf() }.add(resource)
          }
        }
    }
    return referencedResources
  }

  private fun Bundle.copyBundle(currentQuestionnaireResponse: QuestionnaireResponse): Bundle =
    this.copy().apply {
      addEntry(Bundle.BundleEntryComponent().apply { resource = currentQuestionnaireResponse })
    }

  private fun QuestionnaireResponse.processMetadata(
    questionnaire: Questionnaire,
    questionnaireConfig: QuestionnaireConfig,
    context: Context,
  ) {
    status = QuestionnaireResponse.QuestionnaireResponseStatus.COMPLETED
    authored = Date()

    questionnaire.useContext
      .filter { it.hasValueCodeableConcept() }
      .forEach { it.valueCodeableConcept.coding.forEach { coding -> this.meta.addTag(coding) } }
    applyResourceMetadata(questionnaireConfig, this, context)
    setQuestionnaire("${questionnaire.resourceType}/${questionnaire.logicalId}")

    // Set subject if exists
    val resourceType = questionnaireSubjectType(questionnaire, questionnaireConfig)
    val resourceIdentifier = questionnaireConfig.resourceIdentifier
    if (resourceType != null && !resourceIdentifier.isNullOrEmpty()) {
      subject = resourceIdentifier.asReference(resourceType)
    }
  }

  private fun questionnaireSubjectType(
    questionnaire: Questionnaire,
    questionnaireConfig: QuestionnaireConfig,
  ): ResourceType? {
    val questionnaireSubjectType = questionnaire.subjectType.firstOrNull()?.code
    return questionnaireConfig.resourceType
      ?: questionnaireSubjectType?.let {
        ResourceType.valueOf(
          it,
        )
      }
  }

  private fun Resource?.applyResourceMetadata(
    questionnaireConfig: QuestionnaireConfig,
    questionnaireResponse: QuestionnaireResponse,
    context: Context,
  ) =
    this?.apply {
      appendOrganizationInfo(authenticatedOrganizationIds)
      appendPractitionerInfo(practitionerId)
      appendRelatedEntityLocation(questionnaireResponse, questionnaireConfig, context)
      updateLastUpdated()
      generateMissingId()
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
  ): Bundle =
    kotlin
      .runCatching {
        if (extractByStructureMap) {
          ResourceMapper.extract(
            questionnaire = questionnaire,
            questionnaireResponse = questionnaireResponse,
            structureMapExtractionContext =
              StructureMapExtractionContext(
                context = context,
                transformSupportServices = transformSupportServices,
                structureMapProvider = { structureMapUrl: String?, _: IWorkerContext ->
                  structureMapUrl?.substringAfterLast("/")?.let {
                    defaultRepository.loadResource(it)
                  }
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
        viewModelScope.launch(dispatcherProvider.main()) {
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
      .getOrDefault(Bundle())

  /**
   * This function saves [QuestionnaireResponse] as draft if any of the [QuestionnaireResponse.item]
   * has an answer.
   */
  fun saveDraftQuestionnaire(questionnaireResponse: QuestionnaireResponse) {
    viewModelScope.launch {
      val questionnaireHasAnswer =
        questionnaireResponse.item.any {
          it.answer.any { answerComponent -> answerComponent.hasValue() }
        }
      if (questionnaireHasAnswer) {
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
        defaultRepository.run {
          val resourceType = param.resourceType
          if (resourceType != null) {
            loadResource(resourceType, param.value)?.let { addOrUpdate(resource = it) }
          } else {
            val valueResourceType = param.value.substringBefore("/")
            val valueResourceId = param.value.substringAfter("/")
            addOrUpdate(
              resource = loadResource(valueResourceId, ResourceType.valueOf(valueResourceType)),
            )
          }
        }
      } catch (resourceNotFoundException: ResourceNotFoundException) {
        Timber.e("Unable to update resource's _lastUpdated", resourceNotFoundException)
      } catch (illegalArgumentException: IllegalArgumentException) {
        Timber.e(
          "No enum constant org.hl7.fhir.r4.model.ResourceType.${param.value.substringBefore("/")}",
        )
      }
    }
  }

  /**
   * This function validates all [QuestionnaireResponse] and returns true if all the validation
   * result of [QuestionnaireResponseValidator] are [Valid] or [NotValidated] (validation is
   * optional on [Questionnaire] fields)
   */
  suspend fun validateQuestionnaireResponse(
    questionnaire: Questionnaire,
    questionnaireResponse: QuestionnaireResponse,
    context: Context,
  ): Boolean {
    val validQuestionnaireResponseItems =
      ArrayList<QuestionnaireResponse.QuestionnaireResponseItemComponent>()
    val validQuestionnaireItems = ArrayList<Questionnaire.QuestionnaireItemComponent>()
    val questionnaireItemsMap = questionnaire.item.groupBy { it.linkId }

    // Only validate items that are present on both Questionnaire and the QuestionnaireResponse
    questionnaireResponse.item.forEach {
      if (questionnaireItemsMap.containsKey(it.linkId)) {
        val questionnaireItem = questionnaireItemsMap.getValue(it.linkId).first()
        validQuestionnaireResponseItems.add(it)
        validQuestionnaireItems.add(questionnaireItem)
      }
    }

    return QuestionnaireResponseValidator.validateQuestionnaireResponse(
        questionnaire = Questionnaire().apply { item = validQuestionnaireItems },
        questionnaireResponse =
          QuestionnaireResponse().apply { item = validQuestionnaireResponseItems },
        context = context,
      )
      .values
      .flatten()
      .all { it is Valid || it is NotValidated }
  }

  suspend fun executeCql(
    subject: Resource,
    bundle: Bundle,
    questionnaire: Questionnaire,
    questionnaireConfig: QuestionnaireConfig? = null,
  ) {
    questionnaireConfig?.cqlInputResources?.forEach { resourceId ->
      val basicResource = defaultRepository.loadResource(resourceId) as Basic?
      bundle.addEntry(Bundle.BundleEntryComponent().setResource(basicResource))
    }

    val libraryFilters =
      questionnaire.cqfLibraryUrls().map {
        val apply: TokenParamFilterCriterion.() -> Unit = { value = of(it.extractLogicalIdUuid()) }
        apply
      }

    if (libraryFilters.isNotEmpty()) {
      defaultRepository.fhirEngine
        .search<Library> {
          filter(
            Resource.RES_ID,
            *libraryFilters.toTypedArray(),
          )
        }
        .forEach { librarySearchResult ->
          val result: Parameters =
            fhirOperator.evaluateLibrary(
              librarySearchResult.resource.url,
              subject.asReference().reference,
              null,
              bundle,
              null,
            ) as Parameters

          result.parameter.mapNotNull { cqlResultParameterComponent ->
            (cqlResultParameterComponent.value ?: cqlResultParameterComponent.resource)?.let {
              resultParameterResource ->
              if (
                cqlResultParameterComponent.name.equals(OUTPUT_PARAMETER_KEY) &&
                  resultParameterResource.isResource
              ) {
                defaultRepository.create(true, resultParameterResource as Resource)
              }

              if (BuildConfig.DEBUG) {
                Timber.d(
                  "CQL :: Param found: ${cqlResultParameterComponent.name} with value: ${
                                        getStringRepresentation(
                                            resultParameterResource,
                                        )
                                    }",
                )
              }
            }
          }
        }
    }
  }

  private fun getStringRepresentation(base: Base): String =
    if (base.isResource) parser.encodeResourceToString(base as Resource) else base.toString()

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
            generateCarePlanWithWorkflowApi = questionnaireConfig.generateCarePlanWithWorkflowApi,
          )
        }
        .onFailure { Timber.e(it) }
    }
  }

  /** Update the [Group.managingEntity] */
  private suspend fun updateGroupManagingEntity(
    resource: Resource,
    groupIdentifier: String?,
    managingEntityRelationshipCode: String?,
  ) {
    // Load the group from the database to get the updated Resource always.
    val group =
      groupIdentifier?.extractLogicalIdUuid()?.let { loadResource(ResourceType.Group, it) }
        as Group?

    if (
      group != null &&
        resource is RelatedPerson &&
        !resource.relationshipFirstRep.codingFirstRep.code.isNullOrEmpty() &&
        resource.relationshipFirstRep.codingFirstRep.code == managingEntityRelationshipCode
    ) {
      defaultRepository.addOrUpdate(
        resource = group.apply { managingEntity = resource.asReference() },
      )
    }
  }

  /**
   * Adds [Resource] to [Group.member] if the member does not exist and if [Resource.logicalId] is
   * NOT the same as the retrieved [GroupResourceConfig.groupIdentifier] (Cannot add a [Group] as
   * member of itself.
   */
  suspend fun addMemberToGroup(
    resource: Resource,
    memberResourceType: ResourceType?,
    groupIdentifier: String?,
  ) {
    // Load the Group resource from the database to get the updated one
    val group =
      groupIdentifier?.extractLogicalIdUuid()?.let { loadResource(ResourceType.Group, it) }
        as Group? ?: return

    val reference = resource.asReference()
    val member = group.member.find { it.entity.reference.equals(reference.reference, true) }

    // Cannot add Group as member of itself; Cannot not duplicate existing members
    if (resource.logicalId == group.logicalId || member != null) return

    if (
      resource.resourceType.isIn(
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
      ) && resource.resourceType == memberResourceType
    ) {
      group.addMember(Group.GroupMemberComponent().apply { entity = reference })
      defaultRepository.addOrUpdate(resource = group)
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

  /** Update the current progress state of the questionnaire. */
  fun setProgressState(questionnaireState: QuestionnaireProgressState) {
    _questionnaireProgressStateLiveData.postValue(questionnaireState)
  }

  companion object {
    const val CONTAINED_LIST_TITLE = "GeneratedResourcesList"
    const val OUTPUT_PARAMETER_KEY = "OUTPUT"
  }
}
