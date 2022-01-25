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
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Group
import org.hl7.fhir.r4.model.HumanName
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
import org.smartregister.fhircore.engine.cql.LibraryEvaluator
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.data.remote.model.response.UserInfo
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity.Companion.questionnaireResponse
import org.smartregister.fhircore.engine.util.AssetUtil
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.USER_INFO_SHARED_PREFERENCE_KEY
import org.smartregister.fhircore.engine.util.extension.asReference
import org.smartregister.fhircore.engine.util.extension.assertSubject
import org.smartregister.fhircore.engine.util.extension.decodeJson
import org.smartregister.fhircore.engine.util.extension.deleteRelatedResources
import org.smartregister.fhircore.engine.util.extension.extractId
import org.smartregister.fhircore.engine.util.extension.find
import org.smartregister.fhircore.engine.util.extension.isExtractionCandidate
import org.smartregister.fhircore.engine.util.extension.isIn
import org.smartregister.fhircore.engine.util.extension.prepareQuestionsForReadingOrEditing
import org.smartregister.fhircore.engine.util.extension.retainMetadata
import org.smartregister.fhircore.engine.util.extension.setPropertySafely
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

  private val authenticatedUserInfo by lazy {
    sharedPreferencesHelper.read(USER_INFO_SHARED_PREFERENCE_KEY, null)?.decodeJson<UserInfo>()
  }

  val extractionProgress = MutableLiveData<Boolean>()

  var editQuestionnaireResponse: QuestionnaireResponse? = null

  var structureMapProvider: (suspend (String, IWorkerContext) -> StructureMap?)? = null

  suspend fun loadQuestionnaire(
    id: String,
    readOnly: Boolean = false,
    editMode: Boolean = false
  ): Questionnaire? =
    defaultRepository.loadResource<Questionnaire>(id)?.apply {
      if (readOnly || editMode) {
        item.prepareQuestionsForReadingOrEditing("QuestionnaireResponse.item", readOnly)
      }
    }

  suspend fun getQuestionnaireConfig(form: String, context: Context): QuestionnaireConfig {
    val loadConfig =
      withContext(dispatcherProvider.io()) {
        AssetUtil.decodeAsset<List<QuestionnaireConfig>>(
          fileName = QuestionnaireActivity.FORM_CONFIGURATIONS,
          context = context
        )
      }

    val appId = configurationRegistry.appId
    return loadConfig.associateBy { it.appId + it.form }.getValue(appId + form)
  }

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

  fun extractAndSaveResources(
    context: Context,
    resourceId: String?,
    questionnaire: Questionnaire,
    questionnaireResponse: QuestionnaireResponse,
    editMode: Boolean = false
  ) {
    viewModelScope.launch(dispatcherProvider.io()) {
      // important to set response subject so that structure map can handle subject for all entities
      handleQuestionnaireResponseSubject(resourceId, questionnaire, questionnaireResponse)

      if (questionnaire.isExtractionCandidate()) {
        // val bundle = performExtraction(context, questionnaire, questionnaireResponse)
        val patient =
          Patient().apply {
            name =
              listOf(
                HumanName().apply {
                  given.add(
                    questionnaireResponse
                      .find("f0361e64-db57-495a-8c82-fa6576e84f74")
                      ?.answer
                      ?.get(0)
                      ?.valueStringType
                  )
                  family =
                    questionnaireResponse
                      .find("007eadd1-3929-4786-803a-72df7a11735b")
                      ?.answer
                      ?.get(0)
                      ?.valueStringType
                      ?.toString()
                }
              )
            active = true
            gender =
              Enumerations.AdministrativeGender.fromCode(
                questionnaireResponse
                  .find("23e7f371-6996-417e-af8c-6df395ba04e1")
                  ?.answer
                  ?.get(0)
                  ?.valueCoding
                  ?.code
              )
            birthDate =
              Calendar.getInstance().run {
                add(
                  Calendar.MONTH,
                  -(questionnaireResponse
                      .find("38896946-7046-42f0-dabd-6ed855965a38")
                      ?.answer
                      ?.get(0)!!
                    .valueIntegerType
                    .value)
                )
                time
              }
          }
        val bundle =
          Bundle().apply { addEntry(Bundle.BundleEntryComponent().apply { resource = patient }) }

        bundle.entry.forEach { bun ->
          // add organization to entities representing individuals in registration questionnaire
          if (bun.resource.resourceType.isIn(ResourceType.Patient, ResourceType.Group)) {
            appendOrganizationInfo(bun.resource)

            // if it is new registration set response subject
            if (resourceId == null) questionnaireResponse.subject = bun.resource.asReference()
          }

          // response MUST have subject by far otherwise flow has issues
          questionnaireResponse.assertSubject()

          // TODO https://github.com/opensrp/fhircore/issues/900
          // for edit mode replace client and resource subject ids.
          // Ideally ResourceMapper should allow this internally via structure-map
          if (editMode) {
            if (bun.resource.resourceType.isIn(ResourceType.Patient, ResourceType.Group))
              bun.resource.id = questionnaireResponse.subject.extractId()
            else {
              bun.resource.setPropertySafely("subject", questionnaireResponse.subject)
              bun.resource.setPropertySafely("patient", questionnaireResponse.subject)
            }
          }

          questionnaireResponse.contained.add(bun.resource)
        }

        saveBundleResources(bundle)

        if (editMode && editQuestionnaireResponse != null) {
          questionnaireResponse.retainMetadata(editQuestionnaireResponse!!)
        }

        saveQuestionnaireResponse(questionnaire, questionnaireResponse)

        // TODO https://github.com/opensrp/fhircore/issues/900
        // reassess following i.e. deleting/updating older resources because one resource
        // might have generated other flow in subsequent followups
        if (editMode && editQuestionnaireResponse != null) {
          editQuestionnaireResponse!!.deleteRelatedResources(defaultRepository)
        }

        /*if (questionnaireResponse.subject.reference.startsWith("Patient/"))
        questionnaire.cqfLibraryId()?.run {
          libraryEvaluator.runCqlLibrary(
            this,
            loadPatient(questionnaireResponse.subject.extractId())!!,
            bundle.entry.map { it.resource },
            defaultRepository
          )
        }*/
      } else {
        saveQuestionnaireResponse(questionnaire, questionnaireResponse)
      }

      viewModelScope.launch(Dispatchers.Main) { extractionProgress.postValue(true) }
    }
  }

  /**
   * Sets questionnaireResponse subject with proper subject-type defined in questionnaire with an
   * existing resourceId or if null generate a new one
   */
  fun handleQuestionnaireResponseSubject(
    resourceId: String?,
    questionnaire: Questionnaire,
    questionnaireResponse: QuestionnaireResponse
  ) {
    if (resourceId?.isNotBlank() == true) {
      val subjectType = questionnaire.subjectType.firstOrNull()?.code ?: ResourceType.Patient.name
      questionnaireResponse.subject = Reference().apply { reference = "$subjectType/$resourceId" }
    }
  }

  suspend fun saveQuestionnaireResponse(
    questionnaire: Questionnaire,
    questionnaireResponse: QuestionnaireResponse
  ) {
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

  fun calculateDobFromAge(age: Int): Date {
    val cal: Calendar = Calendar.getInstance()
    // Subtract #age years from the calendar
    cal.add(Calendar.YEAR, -age)
    cal.set(Calendar.DAY_OF_YEAR, 1)
    cal.set(Calendar.MONTH, 1)
    return cal.time
  }
}
