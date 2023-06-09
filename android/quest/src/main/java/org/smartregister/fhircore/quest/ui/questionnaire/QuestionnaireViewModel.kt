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
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.fhir.datacapture.mapping.ResourceMapper
import com.google.android.fhir.datacapture.mapping.StructureMapExtractionContext
import com.google.android.fhir.datacapture.validation.QuestionnaireResponseValidator
import com.google.android.fhir.db.ResourceNotFoundException
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.search
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.context.IWorkerContext
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Group
import org.hl7.fhir.r4.model.Location
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.RelatedPerson
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.StructureMap
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.cql.LibraryEvaluator
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.domain.model.ActionParameter
import org.smartregister.fhircore.engine.domain.model.ActionParameterType
import org.smartregister.fhircore.engine.domain.model.QuestionnaireType
import org.smartregister.fhircore.engine.task.FhirCarePlanGenerator
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.asReference
import org.smartregister.fhircore.engine.util.extension.cqfLibraryIds
import org.smartregister.fhircore.engine.util.extension.deleteRelatedResources
import org.smartregister.fhircore.engine.util.extension.extractId
import org.smartregister.fhircore.engine.util.extension.extractLogicalIdUuid
import org.smartregister.fhircore.engine.util.extension.extractType
import org.smartregister.fhircore.engine.util.extension.find
import org.smartregister.fhircore.engine.util.extension.findSubject
import org.smartregister.fhircore.engine.util.extension.isExtractionCandidate
import org.smartregister.fhircore.engine.util.extension.isIn
import org.smartregister.fhircore.engine.util.extension.prePopulateInitialValues
import org.smartregister.fhircore.engine.util.extension.prepareQuestionsForReadingOrEditing
import org.smartregister.fhircore.engine.util.extension.referenceValue
import org.smartregister.fhircore.engine.util.extension.resourceClassType
import org.smartregister.fhircore.engine.util.extension.retainMetadata
import org.smartregister.fhircore.engine.util.extension.setPropertySafely
import org.smartregister.fhircore.engine.util.extension.showToast
import org.smartregister.fhircore.engine.util.extension.updateLastUpdated
import org.smartregister.fhircore.engine.util.helper.TransformSupportServices
import org.smartregister.fhircore.quest.BuildConfig
import org.smartregister.fhircore.quest.R
import timber.log.Timber

@HiltViewModel
open class QuestionnaireViewModel
@Inject
constructor(
  val defaultRepository: DefaultRepository,
  val configurationRegistry: ConfigurationRegistry,
  val transformSupportServices: TransformSupportServices,
  val dispatcherProvider: DispatcherProvider,
  val sharedPreferencesHelper: SharedPreferencesHelper,
  val libraryEvaluator: LibraryEvaluator,
  val fhirCarePlanGenerator: FhirCarePlanGenerator,
) : ViewModel() {

  val extractionProgress = MutableLiveData<Boolean>()
  val extractionProgressMessage = MutableLiveData<String>()
  val removeOperation = MutableLiveData(false)
  var editQuestionnaireResponse: QuestionnaireResponse? = null
  var structureMapProvider: (suspend (String, IWorkerContext) -> StructureMap?)? = null

  private val authenticatedOrganizationIds by lazy {
    sharedPreferencesHelper.read<List<String>>(ResourceType.Organization.name)
  }

  private val practitionerId: String? by lazy {
    sharedPreferencesHelper
      .read(SharedPreferenceKey.PRACTITIONER_ID.name, null)
      ?.extractLogicalIdUuid()
  }

  private var editQuestionnaireResourceParams: List<ActionParameter>? = emptyList()

  suspend fun loadQuestionnaire(
    id: String,
    type: QuestionnaireType,
    prePopulationParams: List<ActionParameter>? = emptyList(),
    readOnlyLinkIds: List<String>? = emptyList()
  ): Questionnaire? =
    defaultRepository.loadResource<Questionnaire>(id)?.apply {
      if (type.isReadOnly() || type.isEditMode()) {
        item.prepareQuestionsForReadingOrEditing(
          QUESTIONNAIRE_RESPONSE_ITEM,
          type.isReadOnly(),
          readOnlyLinkIds
        )
      }
      // prepopulate questionnaireItems with initial values
      prePopulationParams?.takeIf { it.isNotEmpty() }?.let { nonEmptyParams ->
        editQuestionnaireResourceParams =
          nonEmptyParams.filter { it.paramType == ActionParameterType.UPDATE_DATE_ON_EDIT }
        item.prePopulateInitialValues(STRING_INTERPOLATION_PREFIX, nonEmptyParams)
      }

      // TODO https://github.com/opensrp/fhircore/issues/991#issuecomment-1027872061
      this.url = this.url ?: this.referenceValue()
    }

  suspend fun fetchStructureMap(structureMapUrl: String?): StructureMap? {
    var structureMap: StructureMap? = null
    structureMapUrl?.substringAfterLast("/")?.run {
      structureMap = defaultRepository.loadResource(this)
    }
    return structureMap
  }

  suspend fun addGroupMember(resource: Resource, groupResourceId: String) {
    defaultRepository.loadResource<Group>(groupResourceId)?.run {
      // Support all the valid group member references as per the FHIR specs
      if (resource.resourceType.isIn(
          ResourceType.CareTeam,
          ResourceType.Device,
          ResourceType.Group,
          ResourceType.HealthcareService,
          ResourceType.Location,
          ResourceType.Organization,
          ResourceType.Patient,
          ResourceType.Practitioner,
          ResourceType.PractitionerRole,
          ResourceType.Specimen
        )
      ) {
        this.member?.add(Group.GroupMemberComponent().apply { entity = resource.asReference() })
      }

      // set managing entity for extracted related resource
      if (resource.resourceType == ResourceType.RelatedPerson) {
        this.managingEntity = resource.logicalId.asReference(ResourceType.RelatedPerson)
      }
      defaultRepository.addOrUpdate(resource = this)
    }
  }

  fun extractAndSaveResources(
    context: Context,
    questionnaireResponse: QuestionnaireResponse,
    questionnaire: Questionnaire,
    questionnaireConfig: QuestionnaireConfig
  ) {
    questionnaireResponse.questionnaire = "${questionnaire.resourceType}/${questionnaire.logicalId}"

    if (questionnaireResponse.logicalId.isEmpty()) {
      questionnaireResponse.id = UUID.randomUUID().toString()
      questionnaireResponse.authored = Date()
    }

    viewModelScope.launch(dispatcherProvider.io()) {
      questionnaire.useContext.filter { it.hasValueCodeableConcept() }.forEach {
        it.valueCodeableConcept.coding.forEach { coding ->
          questionnaireResponse.meta.addTag(coding)
        }
      }

      // important to set response subject so that structure map can handle subject for all entities
      handleQuestionnaireResponseSubject(
        questionnaireConfig.resourceIdentifier,
        questionnaire,
        questionnaireResponse
      )
      if (questionnaire.isExtractionCandidate()) {
        val bundle = performExtraction(context, questionnaire, questionnaireResponse)
        bundle.entry.forEach { bundleEntry ->
          // add organization to entities representing individuals in registration questionnaire
          // if (bundleEntry.resource.resourceType.isIn(ResourceType.Patient, ResourceType.Group,
          // ResourceType.Encounter)) {
          // if it is new registration set response subject
          if (questionnaireConfig.resourceIdentifier == null)
            questionnaireResponse.subject = bundleEntry.resource.asReference()
          // }
          if (questionnaireConfig.setPractitionerDetails) {
            appendPractitionerInfo(bundleEntry.resource)
          }
          if (questionnaireConfig.setOrganizationDetails) {
            appendOrganizationInfo(bundleEntry.resource)
          }

          if (questionnaireConfig.setAppVersion) {
            appendAppVersion(context, bundleEntry.resource)
          }
          if (bundleEntry.hasResource()) bundleEntry.resource.updateLastUpdated()
          if (questionnaireConfig.type != QuestionnaireType.EDIT &&
              bundleEntry.resource.resourceType.isIn(
                ResourceType.Patient,
                ResourceType.RelatedPerson
              )
          ) {
            questionnaireConfig.groupResource?.groupIdentifier?.let {
              addGroupMember(resource = bundleEntry.resource, groupResourceId = it)
            }
          }
          updateResourceLastUpdatedLinkedAsSubject(questionnaireResponse)

          // TODO https://github.com/opensrp/fhircore/issues/900
          // for edit mode replace client and resource subject ids.
          // Ideally ResourceMapper should allow this internally via structure-map
          if (questionnaireConfig.type.isEditMode()) {
            if (bundleEntry.resource.resourceType.isIn(ResourceType.Patient, ResourceType.Group))
              bundleEntry.resource.id = questionnaireResponse.subject.extractId()
            else {
              bundleEntry.resource.setPropertySafely("subject", questionnaireResponse.subject)
              bundleEntry.resource.setPropertySafely("patient", questionnaireResponse.subject)
            }
          }
          questionnaireResponse.contained.add(bundleEntry.resource)
        }
        updateResourceLastUpdatedLinkedAsSubject(questionnaireResponse)

        if (questionnaire.experimental) {
          Timber.w(
            "${questionnaire.name}(${questionnaire.logicalId}) is experimental and not save any data"
          )
        } else saveBundleResources(bundle)

        if (questionnaireConfig.type.isEditMode() && editQuestionnaireResponse != null) {
          questionnaireResponse.retainMetadata(editQuestionnaireResponse!!)
        }

        saveQuestionnaireResponse(questionnaire, questionnaireResponse)

        // TODO https://github.com/opensrp/fhircore/issues/900
        // reassess following i.e. deleting/updating older resources because one resource
        // might have generated other flow in subsequent followups
        if (questionnaireConfig.type.isEditMode() && editQuestionnaireResponse != null) {
          editQuestionnaireResponse!!.deleteRelatedResources(defaultRepository)
        }
        performExtraction(questionnaireResponse, questionnaireConfig, questionnaire, bundle)
      } else {
        saveQuestionnaireResponse(questionnaire, questionnaireResponse)
        performExtraction(questionnaireResponse, questionnaireConfig, questionnaire, bundle = null)
      }
      viewModelScope.launch(dispatcherProvider.main()) { extractionProgress.postValue(true) }
    }
  }

  /* We can remove this after we review why a subject is needed for every questionnaire response in fhir core.
  The subject is not required in the questionnaire response
   https://www.hl7.org/fhir/questionnaireresponse-definitions.html#QuestionnaireResponse.subject */
  suspend fun performExtraction(
    questionnaireResponse: QuestionnaireResponse,
    questionnaireConfig: QuestionnaireConfig,
    questionnaire: Questionnaire,
    bundle: Bundle?
  ) {
    if (bundle?.entry?.isNotEmpty() == true) {
      extractCqlOutput(questionnaire, questionnaireResponse, bundle)
      extractCarePlan(questionnaireResponse, bundle, questionnaireConfig)
    }
  }

  fun savePartialQuestionnaireResponse(
    questionnaire: Questionnaire,
    questionnaireResponse: QuestionnaireResponse
  ) {
    viewModelScope.launch(dispatcherProvider.io()) {
      questionnaireResponse.status = QuestionnaireResponse.QuestionnaireResponseStatus.INPROGRESS
      saveQuestionnaireResponse(questionnaire, questionnaireResponse)
    }
  }

  fun appendOrganizationInfo(resource: Resource) {
    // Organization reference in shared pref as "Organization/some-gibberish-uuid"
    authenticatedOrganizationIds.let { ids ->
      val organizationRef =
        ids?.first()?.extractLogicalIdUuid()?.asReference(ResourceType.Organization)

      when (resource) {
        is Patient -> resource.managingOrganization = organizationRef
        is Group -> resource.managingEntity = organizationRef
        is Encounter -> resource.serviceProvider = organizationRef
        is Location -> resource.managingOrganization = organizationRef
      }
    }
  }

  fun appendPractitionerInfo(resource: Resource) {
    practitionerId?.let {
      // Convert practitioner uuid to reference e.g. "Practitioner/some-gibberish-uuid"
      val practitionerRef = it.asReference(ResourceType.Practitioner)

      if (resource is Patient) resource.generalPractitioner = arrayListOf(practitionerRef)
      else if (resource is Encounter)
        resource.participant =
          arrayListOf(
            Encounter.EncounterParticipantComponent().apply { individual = practitionerRef }
          )
    }
  }

  /**
   * This creates a meta tag that records the App Version as defined in the build.gradle and updates
   * all resources created by the App with the relevant app version name. The tag defines three
   * strings: 'setSystem - The code system' , 'setCode - The code would be the App Version defined
   * on the build.gradle.', and 'setDisplay - The display name'. All resources created by the App
   * will have a tag of the App Version on the meta.tag.
   *
   * @property resource The resource to add the meta tag to.
   */
  fun appendAppVersion(context: Context, resource: Resource) {
    // Create a tag with the app version
    val metaTag = resource.meta.addTag()
    metaTag
      .setSystem(context.getString(R.string.app_version_tag_url))
      .setCode(BuildConfig.VERSION_NAME)
      .display = context.getString(R.string.application_version)
  }

  suspend fun extractCarePlan(
    questionnaireResponse: QuestionnaireResponse,
    bundle: Bundle?,
    questionnaireConfig: QuestionnaireConfig
  ) {
    val subject =
      questionnaireResponse.findSubject(bundle)
        ?: defaultRepository.loadResource(questionnaireResponse.subject)

    questionnaireConfig.planDefinitions?.forEach { planId ->
      val data =
        Bundle().apply {
          bundle?.entry?.map { this.addEntry(it) }
          addEntry().resource = questionnaireResponse
        }

      kotlin
        .runCatching { fhirCarePlanGenerator.generateOrUpdateCarePlan(planId, subject, data) }
        .onFailure {
          Timber.e(it)
          extractionProgressMessage.postValue("Error extracting care plan. ${it.message}")
        }
      fhirCarePlanGenerator.conditionallyUpdateCarePlanStatus(questionnaireConfig, subject, data)
    }
  }

  suspend fun extractCqlOutput(
    questionnaire: Questionnaire,
    questionnaireResponse: QuestionnaireResponse,
    bundle: Bundle?
  ) {
    withContext(dispatcherProvider.default()) {
      val data = bundle ?: Bundle().apply { addEntry().apply { resource = questionnaireResponse } }
      questionnaire
        .cqfLibraryIds()
        .map {
          val patient =
            if (questionnaireResponse.hasSubject())
              loadPatient(questionnaireResponse.subject.extractId())
            else null
          libraryEvaluator.runCqlLibrary(it, patient, data, defaultRepository)
        }
        .forEach { output ->
          if (output.isNotEmpty()) extractionProgressMessage.postValue(output.joinToString("\n"))
        }
    }
  }

  /**
   * Sets questionnaireResponse subject with proper subject-type defined in questionnaire with an
   * existing resourceId or organization or null
   */
  fun handleQuestionnaireResponseSubject(
    resourceId: String?,
    questionnaire: Questionnaire,
    questionnaireResponse: QuestionnaireResponse
  ) {
    val subjectType = questionnaire.subjectType.firstOrNull()?.code ?: ResourceType.Patient.name
    questionnaireResponse.subject =
      when (subjectType) {
        ResourceType.Organization.name ->
          authenticatedOrganizationIds?.first()?.asReference(ResourceType.Organization)
        else -> resourceId?.asReference(ResourceType.valueOf(subjectType))
      }
  }

  /**
   * Add or update the [questionnaireResponse] resource with the passed content, and if an
   * [editQuestionnaireResourceParams] represents the IDs of the resources to be updated on
   * Questionnaire edit.
   *
   * @param questionnaire the [Questionnaire] this response is related to
   * @param questionnaireResponse the questionnaireResponse resource to save
   */
  suspend fun saveQuestionnaireResponse(
    questionnaire: Questionnaire,
    questionnaireResponse: QuestionnaireResponse
  ) {
    if (questionnaire.experimental) {
      Timber.w(
        "${questionnaire.name}(${questionnaire.logicalId}) is experimental and not save any data"
      )
      return
    }
    defaultRepository.addOrUpdate(resource = questionnaireResponse)
    editQuestionnaireResourceParams?.forEach { param ->
      try {
        val resource =
          param.value.let {
            val resourceType =
              it.substringBefore("/").resourceClassType().newInstance().resourceType
            defaultRepository.loadResource(it.extractLogicalIdUuid(), resourceType)
          }
        resource.let { defaultRepository.addOrUpdate(resource = it) }
      } catch (e: ResourceNotFoundException) {
        Timber.e(e)
      }
    }
  }

  suspend fun performExtraction(
    context: Context,
    questionnaire: Questionnaire,
    questionnaireResponse: QuestionnaireResponse
  ): Bundle {
    return kotlin
      .runCatching {
        ResourceMapper.extract(
          questionnaire = questionnaire,
          questionnaireResponse = questionnaireResponse,
          StructureMapExtractionContext(
            context = context,
            transformSupportServices = transformSupportServices,
            structureMapProvider = retrieveStructureMapProvider()
          )
        )
      }
      .onSuccess {
        Timber.d(
          "Questionnaire (${questionnaire.name}) with ${questionnaire.id} extracted successfully"
        )
      }
      .onFailure { exception ->
        Timber.e(exception)
        viewModelScope.launch {
          if (exception is NullPointerException && exception.message!!.contains("StructureMap")) {
            context.showToast(
              context.getString(R.string.structure_map_missing_message),
              Toast.LENGTH_LONG
            )
          } else {
            context.showToast(
              context.getString(R.string.structuremap_failed, questionnaire.name),
              Toast.LENGTH_LONG
            )
          }
        }
      }
      .getOrDefault(Bundle())
  }

  suspend fun saveBundleResources(bundle: Bundle) {
    if (!bundle.isEmpty) {
      bundle.entry.forEach { bundleEntry ->
        defaultRepository.addOrUpdate(resource = bundleEntry.resource)
      }
    }
  }

  fun retrieveStructureMapProvider(): (suspend (String, IWorkerContext) -> StructureMap?) {
    if (structureMapProvider == null) {
      structureMapProvider =
        { structureMapUrl: String, _: IWorkerContext ->
          fetchStructureMap(structureMapUrl)
        }
    }

    return structureMapProvider!!
  }

  fun saveResource(resource: Resource) {
    viewModelScope.launch { defaultRepository.create(true, resource) }
  }

  fun partialQuestionnaireResponseHasValues(questionnaireResponse: QuestionnaireResponse): Boolean {
    val questionnaireResponseItemListIterator = questionnaireResponse.item.iterator()
    while (questionnaireResponseItemListIterator.hasNext()) {
      val questionnaireResponseItem = questionnaireResponseItemListIterator.next()
      questionnaireResponseItem.answer?.forEach { if (it.hasValue()) return true }
    }
    return false
  }

  fun getAgeInput(questionnaireResponse: QuestionnaireResponse): Int? {
    return questionnaireResponse
      .find(QuestionnaireActivity.QUESTIONNAIRE_AGE)
      ?.answer
      ?.firstOrNull()
      ?.valueDecimalType
      ?.value
      ?.toInt()
  }

  /** Subtract [age] from today's date */
  fun calculateDobFromAge(age: Int): Date =
    Calendar.getInstance()
      .apply {
        add(Calendar.YEAR, -age)
        set(Calendar.DAY_OF_YEAR, 1)
        set(Calendar.MONTH, 1)
      }
      .time

  fun removeGroup(groupId: String, removeGroup: Boolean, deactivateMembers: Boolean) {
    if (removeGroup) {
      viewModelScope.launch(dispatcherProvider.io()) {
        try {
          defaultRepository.removeGroup(
            groupId,
            deactivateMembers,
            configComputedRuleValues = emptyMap()
          )
        } catch (exception: Exception) {
          Timber.e(exception)
        } finally {
          removeOperation.postValue(true)
        }
      }
    }
  }

  fun removeGroupMember(
    memberId: String?,
    groupIdentifier: String?,
    memberResourceType: String?,
    removeMember: Boolean
  ) {
    if (removeMember && !memberId.isNullOrEmpty()) {
      viewModelScope.launch(dispatcherProvider.io()) {
        try {
          defaultRepository.removeGroupMember(
            memberId = memberId,
            groupId = groupIdentifier,
            groupMemberResourceType = memberResourceType,
            emptyMap()
          )
        } catch (exception: Exception) {
          Timber.e(exception)
        } finally {
          removeOperation.postValue(true)
        }
      }
    }
  }

  fun deleteResource(resourceType: ResourceType, resourceIdentifier: String) {
    viewModelScope.launch {
      defaultRepository.delete(resourceType = resourceType, resourceId = resourceIdentifier)
    }
  }

  suspend fun updateResourceLastUpdatedLinkedAsSubject(
    questionnaireResponse: QuestionnaireResponse
  ) {
    if (questionnaireResponse.hasSubject() && questionnaireResponse.subject.hasReference()) {
      val resourceId = questionnaireResponse.subject.reference.extractLogicalIdUuid()
      val resourceType =
        questionnaireResponse.subject.extractType()!!
          .name
          .resourceClassType()
          .newInstance()
          .resourceType
      try {
        if (resourceType.isIn(ResourceType.Patient, ResourceType.Group)) {
          defaultRepository.loadResource(resourceId, resourceType).let { resource ->
            resource.updateLastUpdated()
            defaultRepository.addOrUpdate(true, resource)
          }
        }
      } catch (exception: ResourceNotFoundException) {
        Timber.e(exception)
      }
    }
  }

  /**
   * Validates the given Questionnaire Response using the SDK [QuestionnaireResponseValidator].
   *
   * @param questionnaire Questionnaire to use in validation
   * @param questionnaireResponse QuestionnaireResponse to validate
   * @param context Context to use in validation
   */
  fun isQuestionnaireResponseValid(
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
      Timber.tag("QuestionnaireViewModel.isQuestionnaireResponseValid").d(e)
      false
    }
  }

  /**
   * Gets a Questionnaire Response from the database if it exists. Generates Questionnaire Response
   * from population, otherwise.
   *
   * @param questionnaire Questionnaire as the basis for how the resources are to be populated
   * @param subjectId ID of the resource that submitted the Questionnaire Response, and related with
   * the population resources
   * @param subjectType resource type of the resource that submitted the Questionnaire Response
   */
  suspend fun getQuestionnaireResponseFromDbOrPopulation(
    questionnaire: Questionnaire,
    subjectId: String?,
    subjectType: ResourceType?,
    questionnaireConfig: QuestionnaireConfig,
    resourceMap: Map<ResourceType?, String>,
  ): QuestionnaireResponse {
    var questionnaireResponse = QuestionnaireResponse()

    if (!subjectId.isNullOrEmpty() && subjectType != null) {
      // Load questionnaire response from DB for Questionnaires opened in EDIT/READONLY mode
      if (!questionnaireConfig.type.isDefault()) {
        questionnaireResponse =
          searchQuestionnaireResponses(
            subjectId = subjectId,
            subjectType = subjectType,
            questionnaireId = questionnaire.logicalId
          )
            .maxByOrNull { it.meta.lastUpdated }
            ?: QuestionnaireResponse()
      }

      /**
       * This will catch an exception and return QR from DB when population resource is empty,
       * ResourceMapper.selectPopulateContext() will return null, then that null will get evaluated
       * and gives an exception as a result.
       */
      questionnaireResponse =
        runCatching {
            // load required resources sent through Param for questionnaire Response expressions
            val populationResources = arrayListOf<Resource>()
            if (resourceMap.isEmpty()) {
              populationResources.addAll(loadPopulationResources(subjectId, subjectType))
            } else {
              resourceMap.forEach {
                populationResources.addAll(
                  loadPopulationResources(it.value.extractLogicalIdUuid(), it.key!!)
                )
              }
            }

            populateQuestionnaireResponse(
              questionnaire = questionnaire,
              populationResources = populationResources
            )
          }
          .onFailure { Timber.e(it, "Error encountered while populating QuestionnaireResponse") }
          .getOrDefault(questionnaireResponse)
    }

    return questionnaireResponse
  }

  /**
   * Generates a Questionnaire Response by populating the given resources.
   *
   * @param questionnaire Questionnaire as the basis for how the resources are to be populated
   * @param populationResources resources to be populated
   */
  @VisibleForTesting
  suspend fun populateQuestionnaireResponse(
    questionnaire: Questionnaire,
    populationResources: List<Resource>
  ): QuestionnaireResponse {
    return ResourceMapper.populate(questionnaire, *populationResources.toTypedArray()).also {
      questionnaireResponse ->
      if (!questionnaireResponse.hasItem()) {
        Timber.tag("QuestionnaireViewModel.populateQuestionnaireResponse")
          .d("Questionnaire response has no populated answers")
      }
    }
  }

  /**
   * Search Questionnaire Response resources that are associated with the given subject ID and
   * Questionnaire ID.
   *
   * @param subjectId ID of the resource that submitted the Questionnaire Response
   * @param subjectType resource type of the resource that submitted the Questionnaire Response
   * @param questionnaireId ID of the Questionnaire that owns the Questionnaire Response
   */
  private suspend fun searchQuestionnaireResponses(
    subjectId: String,
    subjectType: ResourceType,
    questionnaireId: String
  ): List<QuestionnaireResponse> =
    withContext(dispatcherProvider.io()) {
      defaultRepository.fhirEngine.search {
        filter(QuestionnaireResponse.SUBJECT, { value = "${subjectType.name}/$subjectId" })
        filter(
          QuestionnaireResponse.QUESTIONNAIRE,
          { value = "${ResourceType.Questionnaire.name}/$questionnaireId" }
        )
      }
    }

  /**
   * Loads resources to be populated into a Questionnaire Response.
   *
   * @param subjectId can be Patient ID or Group ID
   * @param subjectType resource type of the ID
   */
  private suspend fun loadPopulationResources(
    subjectId: String,
    subjectType: ResourceType
  ): List<Resource> {
    val populationResources = arrayListOf<Resource>()
    try {
      populationResources.add(defaultRepository.loadResource(subjectId, subjectType))
    } catch (exception: ResourceNotFoundException) {
      Timber.e(exception)
    }
    if (subjectType == ResourceType.Patient) {
      loadRelatedPerson(subjectId)?.run { populationResources.add(this) }
    }
    return populationResources
  }

  /** Loads a Patient resource with the given ID. */
  private suspend fun loadPatient(patientId: String): Patient? {
    return defaultRepository.loadResource(patientId)
  }

  /** Loads a RelatedPerson resource that belongs to the given Patient ID. */
  private suspend fun loadRelatedPerson(patientId: String): RelatedPerson? {
    return defaultRepository
      .searchResourceFor<RelatedPerson>(
        subjectType = ResourceType.Patient,
        subjectId = patientId,
        subjectParam = RelatedPerson.PATIENT,
        configComputedRuleValues = emptyMap()
      )
      .singleOrNull()
  }

  companion object {
    private const val QUESTIONNAIRE_RESPONSE_ITEM = "QuestionnaireResponse.item"
    private const val STRING_INTERPOLATION_PREFIX = "@{"
  }
}
