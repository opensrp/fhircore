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
import com.google.android.fhir.datacapture.targetStructureMap
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
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.data.remote.model.response.UserInfo
import org.smartregister.fhircore.engine.util.AssetUtil
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.USER_INFO_SHARED_PREFERENCE_KEY
import org.smartregister.fhircore.engine.util.extension.decodeJson
import org.smartregister.fhircore.engine.util.extension.deleteRelatedResources
import org.smartregister.fhircore.engine.util.extension.find
import org.smartregister.fhircore.engine.util.extension.isIn
import org.smartregister.fhircore.engine.util.extension.prepareQuestionsForReadingOrEditing
import org.smartregister.fhircore.engine.util.extension.retainMetadata
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
  val sharedPreferencesHelper: SharedPreferencesHelper
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

  fun extractAndSaveResources(
    resourceId: String?,
    questionnaire: Questionnaire,
    questionnaireResponse: QuestionnaireResponse,
    editMode: Boolean = false
  ) {
    viewModelScope.launch {

      // if no structure-map and no extraction is configured return without further processing
      if (questionnaire.targetStructureMap != null ||
          questionnaire.extension.any { it.url.contains("sdc-questionnaire-itemExtractionContext") }
      ) {
        val bundle = performExtraction(questionnaire, questionnaireResponse)

        bundle.entry.forEach { bun ->
          // if it is a registration questionnaire add tags to entities representing individuals
          if (resourceId == null &&
              bun.resource.resourceType.isIn(ResourceType.Patient, ResourceType.Group)
          ) {
            questionnaire.useContext.filter { it.hasValueCodeableConcept() }.forEach {
              it.valueCodeableConcept.coding.forEach { bun.resource.meta.addTag(it) }
            }

            // add managing organization of logged in user to record
            authenticatedUserInfo?.organization?.let { org ->
              val organizationRef = Reference().apply { reference = "Organization/$org" }
              val resource = bun.resource

              if (resource is Patient) resource.managingOrganization = organizationRef
              else if (resource is Group) resource.managingEntity = organizationRef
              // TODO: calculate birthDate from AgeInput as calculation isn't supported by sdk
              // https://github.com/google/android-fhir/issues/803
              if (resource is Patient) {
                getAgeInput(questionnaireResponse)?.let {
                  resource.birthDate = calculateDobFromAge(it)
                }
              }
            }
          }

          if (bun.resource != null) {
            questionnaireResponse.contained.add(bun.resource)
          }
        }

        saveBundleResources(bundle)

        if (editMode && editQuestionnaireResponse != null) {
          questionnaireResponse.retainMetadata(editQuestionnaireResponse!!)
        }
        saveQuestionnaireResponse(resourceId, questionnaire, questionnaireResponse)

        // Delete the previous resources
        if (editMode && editQuestionnaireResponse != null) {
          editQuestionnaireResponse!!.deleteRelatedResources(defaultRepository)
        }
      } else {
        saveQuestionnaireResponse(resourceId, questionnaire, questionnaireResponse)
        viewModelScope.launch(Dispatchers.Main) { extractionProgress.postValue(true) }
      }
    }
  }

  suspend fun saveQuestionnaireResponse(
    resourceId: String?,
    questionnaire: Questionnaire,
    questionnaireResponse: QuestionnaireResponse
  ) {
    val subjectType = questionnaire.subjectType.firstOrNull()?.code ?: ResourceType.Patient.name

    if (resourceId?.isNotBlank() == true) {
      // TODO revise this logic when syncing strategy has final decision
      // https://github.com/opensrp/fhircore/issues/726
      loadPatient(resourceId)?.meta?.tag?.forEach { questionnaireResponse.meta.addTag(it) }
      questionnaireResponse.subject = Reference().apply { reference = "$subjectType/$resourceId" }
      questionnaireResponse.questionnaire =
        "${questionnaire.resourceType}/${questionnaire.logicalId}"

      if (questionnaireResponse.logicalId.isEmpty()) {
        questionnaireResponse.id = UUID.randomUUID().toString()
        questionnaireResponse.authored = Date()
      }

      questionnaire.useContext.filter { it.hasValueCodeableConcept() }.forEach {
        it.valueCodeableConcept.coding.forEach { questionnaireResponse.meta.addTag(it) }
      }

      defaultRepository.addOrUpdate(questionnaireResponse)
    }
  }

  suspend fun performExtraction(
    questionnaire: Questionnaire,
    questionnaireResponse: QuestionnaireResponse
  ): Bundle {

    return ResourceMapper.extract(
      questionnaire = questionnaire,
      questionnaireResponse = questionnaireResponse,
      structureMapProvider = retrieveStructureMapProvider(),
      transformSupportServices = transformSupportServices
    )
  }

  fun saveBundleResources(bundle: Bundle) {
    viewModelScope.launch {
      if (!bundle.isEmpty) {
        bundle.entry.forEach { bundleEntry -> defaultRepository.addOrUpdate(bundleEntry.resource) }
      }

      viewModelScope.launch(Dispatchers.Main) { extractionProgress.postValue(true) }
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
      val jsonParser = FhirContext.forR4().newJsonParser()
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
          Timber.e(FhirContext.forR4().newJsonParser().encodeResourceToString(this))
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
