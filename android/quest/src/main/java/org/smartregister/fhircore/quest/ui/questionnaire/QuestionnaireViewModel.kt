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

package org.smartregister.fhircore.quest.ui.questionnaire

import android.content.Context
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.uhn.fhir.parser.IParser
import com.google.android.fhir.datacapture.mapping.ResourceMapper
import com.google.android.fhir.datacapture.mapping.StructureMapExtractionContext
import com.google.android.fhir.logicalId
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
import org.hl7.fhir.r4.model.Identifier
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
import org.smartregister.fhircore.engine.domain.model.QuestionnaireType
import org.smartregister.fhircore.engine.task.FhirCarePlanGenerator
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.asReference
import org.smartregister.fhircore.engine.util.extension.assertSubject
import org.smartregister.fhircore.engine.util.extension.cqfLibraryIds
import org.smartregister.fhircore.engine.util.extension.deleteRelatedResources
import org.smartregister.fhircore.engine.util.extension.extractId
import org.smartregister.fhircore.engine.util.extension.extractLogicalIdUuid
import org.smartregister.fhircore.engine.util.extension.find
import org.smartregister.fhircore.engine.util.extension.findSubject
import org.smartregister.fhircore.engine.util.extension.isExtractionCandidate
import org.smartregister.fhircore.engine.util.extension.isIn
import org.smartregister.fhircore.engine.util.extension.prepareQuestionsForReadingOrEditing
import org.smartregister.fhircore.engine.util.extension.referenceValue
import org.smartregister.fhircore.engine.util.extension.retainMetadata
import org.smartregister.fhircore.engine.util.extension.setPropertySafely
import org.smartregister.fhircore.engine.util.helper.TransformSupportServices
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
  val jsonParser: IParser
) : ViewModel() {

  val extractionProgress = MutableLiveData<Boolean>()

  val extractionProgressMessage = MutableLiveData<String>()

  val removeOperation = MutableLiveData(false)

  var editQuestionnaireResponse: QuestionnaireResponse? = null

  var structureMapProvider: (suspend (String, IWorkerContext) -> StructureMap?)? = null

  private val authenticatedOrganizationIds by lazy {
    sharedPreferencesHelper.read<List<String>>(
      SharedPreferenceKey.PRACTITIONER_DETAILS_ORGANIZATION_IDS.name
    )
  }

  private val practitionerId: String? by lazy {
    sharedPreferencesHelper
      .read(SharedPreferenceKey.PRACTITIONER_ID.name, null)
      ?.extractLogicalIdUuid()
  }

  suspend fun loadQuestionnaire(id: String, type: QuestionnaireType): Questionnaire? =
    defaultRepository.loadResource<Questionnaire>(id)?.apply {
      if (type.isReadOnly() || type.isEditMode()) {
        item.prepareQuestionsForReadingOrEditing(QUESTIONNAIRE_RESPONSE_ITEM, type.isReadOnly())
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
      defaultRepository.addOrUpdate(this)
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
          if (bundleEntry.resource.resourceType.isIn(ResourceType.Patient, ResourceType.Group)) {
            // if it is new registration set response subject
            if (questionnaireConfig.resourceIdentifier == null)
              questionnaireResponse.subject = bundleEntry.resource.asReference()
          }
          if (questionnaireConfig.setPractitionerDetails) {
            appendPractitionerInfo(bundleEntry.resource)
          }
          if (questionnaireConfig.setOrganizationDetails) {
            appendOrganizationInfo(bundleEntry.resource)
          }

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

          // response MUST have subject by far otherwise flow has issues
          if (!questionnaire.experimental) questionnaireResponse.assertSubject()

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

        extractCqlOutput(questionnaire, questionnaireResponse, bundle)
        extractCarePlan(questionnaireResponse, bundle, questionnaireConfig)
      } else {
        saveQuestionnaireResponse(questionnaire, questionnaireResponse)
        extractCqlOutput(questionnaire, questionnaireResponse, null)
        extractCarePlan(questionnaireResponse, null, questionnaireConfig)
      }

      viewModelScope.launch(dispatcherProvider.main()) { extractionProgress.postValue(true) }
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

    questionnaireResponse.assertSubject() // should not allow further flow without subject

    defaultRepository.addOrUpdate(questionnaireResponse)
  }

  suspend fun performExtraction(
    context: Context,
    questionnaire: Questionnaire,
    questionnaireResponse: QuestionnaireResponse
  ): Bundle {

    return ResourceMapper.extract(
      questionnaire = questionnaire,
      questionnaireResponse = questionnaireResponse,
      StructureMapExtractionContext(
        context = context,
        transformSupportServices = transformSupportServices,
        structureMapProvider = retrieveStructureMapProvider()
      )
    )
  }

  suspend fun saveBundleResources(bundle: Bundle) {
    if (!bundle.isEmpty) {
      bundle.entry.forEach { bundleEntry -> defaultRepository.addOrUpdate(bundleEntry.resource) }
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

  suspend fun loadPatient(patientId: String): Patient? {
    return defaultRepository.loadResource(patientId)
  }

  suspend fun loadRelatedPerson(patientId: String): List<RelatedPerson> {
    return defaultRepository.searchResourceFor(
      token = RelatedPerson.RES_ID,
      subjectType = ResourceType.RelatedPerson,
      subjectId = patientId
    )
  }

  fun saveResource(resource: Resource) {
    viewModelScope.launch { defaultRepository.save(resource = resource) }
  }

  open suspend fun getPopulationResources(
    intent: Intent,
    questionnaireConfig: QuestionnaireConfig
  ): Array<Resource> {
    val resourcesList = mutableListOf<Resource>()

    intent.getStringArrayListExtra(QuestionnaireActivity.QUESTIONNAIRE_POPULATION_RESOURCES)?.run {
      forEach { resourcesList.add(jsonParser.parseResource(it) as Resource) }
    }

    questionnaireConfig.resourceIdentifier?.let { patientId ->
      loadPatient(patientId)?.apply {
        if (identifier.isEmpty()) {
          identifier =
            mutableListOf(
              Identifier().apply {
                value = logicalId
                use = Identifier.IdentifierUse.OFFICIAL
                system = QuestionnaireActivity.WHO_IDENTIFIER_SYSTEM
              }
            )
          Timber.e(jsonParser.encodeResourceToString(this))
        }

        resourcesList.add(this)
      }
        ?: defaultRepository.loadResource<Group>(patientId)?.apply { resourcesList.add(this) }
      loadRelatedPerson(patientId).forEach { resourcesList.add(it) }
    }

    return resourcesList.toTypedArray()
  }

  suspend fun generateQuestionnaireResponse(
    questionnaire: Questionnaire,
    intent: Intent,
    questionnaireConfig: QuestionnaireConfig
  ): QuestionnaireResponse {
    return ResourceMapper.populate(
      questionnaire,
      *getPopulationResources(intent, questionnaireConfig = questionnaireConfig)
    )
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
          defaultRepository.removeGroup(groupId, deactivateMembers)
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
            groupMemberResourceType = memberResourceType
          )
        } catch (exception: Exception) {
          Timber.e(exception)
        } finally {
          removeOperation.postValue(true)
        }
      }
    }
  }

  fun deleteResource(resourceIdentifier: String) {
    viewModelScope.launch { defaultRepository.delete(resourceIdentifier) }
  }

  companion object {
    private const val QUESTIONNAIRE_RESPONSE_ITEM = "QuestionnaireResponse.item"
  }
}
