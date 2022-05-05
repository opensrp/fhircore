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

package org.smartregister.fhircore.engine.ui.questionnaire

import android.content.Context
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.uhn.fhir.context.FhirContext
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.datacapture.mapping.ResourceMapper
import com.google.android.fhir.logicalId
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.context.IWorkerContext
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Group
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.RelatedPerson
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.StructureMap
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.AppConfigClassification
import org.smartregister.fhircore.engine.configuration.view.FormConfiguration
import org.smartregister.fhircore.engine.cql.LibraryEvaluator
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.data.remote.model.response.UserInfo
import org.smartregister.fhircore.engine.task.FhirTaskGenerator
import org.smartregister.fhircore.engine.util.AssetUtil
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.USER_INFO_SHARED_PREFERENCE_KEY
import org.smartregister.fhircore.engine.util.extension.asReference
import org.smartregister.fhircore.engine.util.extension.assertSubject
import org.smartregister.fhircore.engine.util.extension.cqfLibraryIds
import org.smartregister.fhircore.engine.util.extension.decodeJson
import org.smartregister.fhircore.engine.util.extension.deleteRelatedResources
import org.smartregister.fhircore.engine.util.extension.extractId
import org.smartregister.fhircore.engine.util.extension.find
import org.smartregister.fhircore.engine.util.extension.isExtractionCandidate
import org.smartregister.fhircore.engine.util.extension.isIn
import org.smartregister.fhircore.engine.util.extension.prepareQuestionsForReadingOrEditing
import org.smartregister.fhircore.engine.util.extension.referenceValue
import org.smartregister.fhircore.engine.util.extension.retainMetadata
import org.smartregister.fhircore.engine.util.extension.setPropertySafely
import org.smartregister.fhircore.engine.util.extension.yearsPassed
import org.smartregister.fhircore.engine.util.helper.TransformSupportServices
import timber.log.Timber

@HiltViewModel
open class QuestionnaireViewModel
@Inject
constructor(
  val fhirEngine: FhirEngine,
  val defaultRepository: DefaultRepository,
  val configurationRegistry: ConfigurationRegistry,
  val transformSupportServices: TransformSupportServices,
  val dispatcherProvider: DispatcherProvider,
  val sharedPreferencesHelper: SharedPreferencesHelper,
  val libraryEvaluator: LibraryEvaluator
) : ViewModel() {
  @Inject lateinit var fhirTaskGenerator: FhirTaskGenerator

  val extractionProgress = MutableLiveData<Boolean>()

  val extractionProgressMessage = MutableLiveData<String>()

  var editQuestionnaireResponse: QuestionnaireResponse? = null

  var structureMapProvider: (suspend (String, IWorkerContext) -> StructureMap?)? = null

  private lateinit var questionnaireConfig: QuestionnaireConfig

  private val authenticatedUserInfo by lazy {
    sharedPreferencesHelper.read(USER_INFO_SHARED_PREFERENCE_KEY, null)?.decodeJson<UserInfo>()
  }

  suspend fun loadQuestionnaire(id: String, type: QuestionnaireType): Questionnaire? =
    defaultRepository.loadResource<Questionnaire>(id)?.apply {
      if (type.isReadOnly() || type.isEditMode()) {
        item.prepareQuestionsForReadingOrEditing(QUESTIONNAIRE_RESPONSE_ITEM, type.isReadOnly())
      }

      // TODO https://github.com/opensrp/fhircore/issues/991#issuecomment-1027872061
      this.url = this.url ?: this.referenceValue()
    }

  suspend fun getQuestionnaireConfig(form: String, context: Context): QuestionnaireConfig {
    val loadConfig =
      loadQuestionnaireConfigFromRegistry() ?: loadQuestionnaireConfigFromAssets(context)
    questionnaireConfig = loadConfig!!.first { it.form == form }
    return questionnaireConfig
  }

  private fun loadQuestionnaireConfigFromRegistry(): List<QuestionnaireConfig>? {
    return kotlin
      .runCatching {
        configurationRegistry.retrieveConfiguration<FormConfiguration>(
          AppConfigClassification.FORMS
        )
      }
      .getOrNull()
      ?.forms
  }

  private suspend fun loadQuestionnaireConfigFromAssets(
    context: Context
  ): List<QuestionnaireConfig>? =
    kotlin
      .runCatching {
        withContext(dispatcherProvider.io()) {
          AssetUtil.decodeAsset<List<QuestionnaireConfig>>(
            fileName = QuestionnaireActivity.FORM_CONFIGURATIONS,
            context = context
          )
        }
      }
      .getOrNull()

  suspend fun fetchStructureMap(structureMapUrl: String?): StructureMap? {
    var structureMap: StructureMap? = null
    structureMapUrl?.substringAfterLast("/")?.run {
      structureMap = defaultRepository.loadResource(this)
    }
    return structureMap
  }

  fun appendOrganizationInfo(resource: Resource) {
    authenticatedUserInfo?.organization?.let { org ->
      val organizationRef = Reference().apply { reference = "Organization/$org" }

      if (resource is Patient) resource.managingOrganization = organizationRef
      else if (resource is Group) resource.managingEntity = organizationRef
    }
  }

  fun appendPractitionerInfo(resource: Resource) {
    authenticatedUserInfo?.keyclockuuid?.let { uuid ->
      val practitionerRef = Reference().apply { reference = "Practitioner/$uuid" }

      if (resource is Patient) resource.generalPractitioner = arrayListOf(practitionerRef)
      else if (resource is Encounter)
        resource.participant =
          arrayListOf(
            Encounter.EncounterParticipantComponent().apply { individual = practitionerRef }
          )
    }
  }

  suspend fun appendPatientsAndRelatedPersonsToGroups(resource: Resource, resourceId: String) {
    val family = defaultRepository.loadResource<Group>(resourceId)!!
    if (resource.resourceType == ResourceType.Patient) {
      family.member.add(
        Group.GroupMemberComponent().apply {
          entity = Reference().apply { reference = "Patient/${resource.logicalId}" }
        }
      )
    } else {
      family.managingEntity =
        Reference().apply { reference = "RelatedPerson/${resource.logicalId}" }
    }

    defaultRepository.addOrUpdate(family)
  }

  fun extractAndSaveResources(
    context: Context,
    resourceId: String?,
    questionnaire: Questionnaire,
    questionnaireResponse: QuestionnaireResponse,
    questionnaireType: QuestionnaireType = QuestionnaireType.DEFAULT
  ) {
    viewModelScope.launch(dispatcherProvider.io()) {
      // important to set response subject so that structure map can handle subject for all entities
      handleQuestionnaireResponseSubject(resourceId, questionnaire, questionnaireResponse)

      if (questionnaire.isExtractionCandidate()) {
        val bundle = performExtraction(context, questionnaire, questionnaireResponse)

        bundle.entry.forEach { bun ->
          // add organization to entities representing individuals in registration questionnaire
          if (bun.resource.resourceType.isIn(ResourceType.Patient, ResourceType.Group)) {
            if (questionnaireConfig.setOrganizationDetails) {
              appendOrganizationInfo(bun.resource)
            }
            // if it is new registration set response subject
            if (resourceId == null) questionnaireResponse.subject = bun.resource.asReference()
          }
          if (questionnaireConfig.setPractitionerDetails) {
            appendPractitionerInfo(bun.resource)
          }

          if (bun.resource.resourceType.isIn(ResourceType.Patient, ResourceType.RelatedPerson)) {
            resourceId?.let {
              appendPatientsAndRelatedPersonsToGroups(resource = bun.resource, resourceId = it)
            }
          }

          if (bun.resource.resourceType.isIn(ResourceType.Patient)) {
            generateCarePlanForUnder5(bun.resource as Patient)
          }

          // response MUST have subject by far otherwise flow has issues
          if (!questionnaire.experimental) questionnaireResponse.assertSubject()

          // TODO https://github.com/opensrp/fhircore/issues/900
          // for edit mode replace client and resource subject ids.
          // Ideally ResourceMapper should allow this internally via structure-map
          if (questionnaireType.isEditMode()) {
            if (bun.resource.resourceType.isIn(ResourceType.Patient, ResourceType.Group))
              bun.resource.id = questionnaireResponse.subject.extractId()
            else {
              bun.resource.setPropertySafely("subject", questionnaireResponse.subject)
              bun.resource.setPropertySafely("patient", questionnaireResponse.subject)
            }
          }
          questionnaireResponse.contained.add(bun.resource)
        }

        if (questionnaire.experimental) {
          Timber.w(
            "${questionnaire.name}(${questionnaire.logicalId}) is experimental and not save any data"
          )
        } else saveBundleResources(bundle)

        if (questionnaireType.isEditMode() && editQuestionnaireResponse != null) {
          questionnaireResponse.retainMetadata(editQuestionnaireResponse!!)
        }

        saveQuestionnaireResponse(questionnaire, questionnaireResponse)

        // TODO https://github.com/opensrp/fhircore/issues/900
        // reassess following i.e. deleting/updating older resources because one resource
        // might have generated other flow in subsequent followups
        if (questionnaireType.isEditMode() && editQuestionnaireResponse != null) {
          editQuestionnaireResponse!!.deleteRelatedResources(defaultRepository)
        }

        extractCqlOutput(questionnaire, questionnaireResponse, bundle)
      } else {
        saveQuestionnaireResponse(questionnaire, questionnaireResponse)
        extractCqlOutput(questionnaire, questionnaireResponse, null)
      }

      viewModelScope.launch(Dispatchers.Main) { extractionProgress.postValue(true) }
    }
  }

  // TODO Update the structure map id to be dynamic
  suspend fun generateCarePlanForUnder5(patient: Patient) {
    val age = patient.birthDate!!.yearsPassed()
    if (age < 5) {
      fhirTaskGenerator.generateCarePlan("105121", patient)
    }
  }

  suspend fun extractCqlOutput(
    questionnaire: Questionnaire,
    questionnaireResponse: QuestionnaireResponse,
    bundle: Bundle?
  ) {
    withContext(Dispatchers.Default) {
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
          authenticatedUserInfo!!.organization!!.asReference(ResourceType.Organization)
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

    questionnaireResponse.questionnaire = "${questionnaire.resourceType}/${questionnaire.logicalId}"

    if (questionnaireResponse.logicalId.isEmpty()) {
      questionnaireResponse.id = UUID.randomUUID().toString()
      questionnaireResponse.authored = Date()
    }

    questionnaire.useContext.filter { it.hasValueCodeableConcept() }.forEach {
      it.valueCodeableConcept.coding.forEach { questionnaireResponse.meta.addTag(it) }
    }

    defaultRepository.addOrUpdate(questionnaireResponse)
  }

  suspend fun performExtraction(
    context: Context,
    questionnaire: Questionnaire,
    questionnaireResponse: QuestionnaireResponse
  ): Bundle {

    return ResourceMapper.extract(
      context = context,
      questionnaire = questionnaire,
      questionnaireResponse = questionnaireResponse,
      structureMapProvider = retrieveStructureMapProvider(),
      transformSupportServices = transformSupportServices
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

  suspend fun loadRelatedPerson(patientId: String): List<RelatedPerson>? {
    return defaultRepository.loadRelatedPersons(patientId)
  }

  fun saveResource(resource: Resource) {
    viewModelScope.launch { defaultRepository.save(resource = resource) }
  }

  open suspend fun getPopulationResources(intent: Intent): Array<Resource> {
    val resourcesList = mutableListOf<Resource>()

    intent.getStringArrayListExtra(QuestionnaireActivity.QUESTIONNAIRE_POPULATION_RESOURCES)?.run {
      val jsonParser = FhirContext.forR4Cached().newJsonParser()
      forEach { resourcesList.add(jsonParser.parseResource(it) as Resource) }
    }

    intent.getStringExtra(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY)?.let { patientId ->
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
          Timber.e(FhirContext.forR4Cached().newJsonParser().encodeResourceToString(this))
        }

        resourcesList.add(this)
      }
      loadRelatedPerson(patientId)?.forEach { resourcesList.add(it) }
    }

    return resourcesList.toTypedArray()
  }

  suspend fun generateQuestionnaireResponse(
    questionnaire: Questionnaire,
    intent: Intent
  ): QuestionnaireResponse {
    return ResourceMapper.populate(questionnaire, *getPopulationResources(intent))
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

  companion object {
    private const val QUESTIONNAIRE_RESPONSE_ITEM = "QuestionnaireResponse.item"
  }
}
