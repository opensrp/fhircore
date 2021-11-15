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

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import ca.uhn.fhir.context.FhirContext
import com.google.android.fhir.datacapture.mapping.ResourceMapper
import com.google.android.fhir.datacapture.targetStructureMap
import com.google.android.fhir.logicalId
import java.util.Date
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.context.IWorkerContext
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Expression
import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.RelatedPerson
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.StructureMap
import org.smartregister.fhircore.engine.configuration.app.ConfigurableApplication
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.FormConfigUtil
import org.smartregister.fhircore.engine.util.extension.isIn
import org.smartregister.fhircore.engine.util.helper.TransformSupportServices

open class QuestionnaireViewModel(
  application: Application,
  private val readOnly: Boolean = false,
) : AndroidViewModel(application) {

  val extractionProgress = MutableLiveData<Boolean>()

  val defaultRepository: DefaultRepository =
    DefaultRepository(
      (application as ConfigurableApplication).fhirEngine,
    )

  var structureMapProvider: (suspend (String, IWorkerContext) -> StructureMap?)? = null

  suspend fun loadQuestionnaire(id: String): Questionnaire? =
    defaultRepository.loadResource<Questionnaire>(id)?.apply {
      if (readOnly) {
        changeQuestionsToReadOnly(this.item, "QuestionnaireResponse.item")
      }
    }

  suspend fun getQuestionnaireConfig(form: String): QuestionnaireConfig {
    val loadConfig =
      withContext(DefaultDispatcherProvider.io()) {
        FormConfigUtil.loadConfig(QuestionnaireActivity.FORM_CONFIGURATIONS, getApplication())
      }

    val appId = (getApplication() as ConfigurableApplication).configurationRegistry.appId
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
    context: Context,
    questionnaire: Questionnaire,
    questionnaireResponse: QuestionnaireResponse
  ) {
    viewModelScope.launch {
      saveQuestionnaireResponse(resourceId, questionnaire, questionnaireResponse)

      // if no structure-map and no extraction is configured return without further processing
      if (questionnaire.targetStructureMap != null ||
          questionnaire.extension.any { it.url.contains("sdc-questionnaire-itemExtractionContext") }
      ) {
        val bundle = performExtraction(questionnaire, questionnaireResponse, context)

        bundle.entry.forEach { bun ->
          // if it is a registration questionnaire add tags to entities representing individuals
          if (resourceId == null &&
              bun.resource.resourceType.isIn(ResourceType.Patient, ResourceType.Group)
          ) {
            questionnaire.useContext.filter { it.hasValueCodeableConcept() }.forEach {
              it.valueCodeableConcept.coding.forEach { bun.resource.meta.addTag(it) }
            }
          }
        }

        saveBundleResources(bundle)
      } else viewModelScope.launch(Dispatchers.Main) { extractionProgress.postValue(true) }
    }
  }

  suspend fun saveQuestionnaireResponse(
    resourceId: String?,
    questionnaire: Questionnaire,
    questionnaireResponse: QuestionnaireResponse
  ) {
    val subjectType = questionnaire.subjectType.firstOrNull()?.code ?: ResourceType.Patient.name
    if (resourceId?.isNotBlank() == true) {
      questionnaireResponse.id = UUID.randomUUID().toString()
      questionnaireResponse.authored = Date()
      questionnaire.useContext.filter { it.hasValueCodeableConcept() }.forEach {
        it.valueCodeableConcept.coding.forEach { questionnaireResponse.meta.addTag(it) }
      }
      // TODO revise this logic when syncing strategy has final decision
      // https://github.com/opensrp/fhircore/issues/726
      loadPatient(resourceId)?.meta?.tag?.forEach { questionnaireResponse.meta.addTag(it) }
      questionnaireResponse.subject = Reference().apply { reference = "$subjectType/$resourceId" }
      questionnaireResponse.questionnaire =
        "${questionnaire.resourceType}/${questionnaire.logicalId}"

      defaultRepository.save(questionnaireResponse)
    }
  }

  suspend fun performExtraction(
    questionnaire: Questionnaire,
    questionnaireResponse: QuestionnaireResponse,
    context: Context
  ): Bundle {
    val transformSupportServices =
      TransformSupportServices(
        mutableListOf(),
        getApplication<Application>() as ConfigurableApplication
      )

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
        { structureMapUrl: String, workerContext: IWorkerContext ->
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

  private fun changeQuestionsToReadOnly(
    items: List<Questionnaire.QuestionnaireItemComponent>,
    path: String
  ) {
    items.forEach { item ->
      if (item.type != Questionnaire.QuestionnaireItemType.GROUP) {
        item.readOnly = true
        item.extension =
          listOf(
            Extension().apply {
              url =
                "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-initialExpression"
              setValue(
                Expression().apply {
                  language = "text/fhirpath"
                  expression = "$path.where(linkId = '${item.linkId}').answer.value"
                }
              )
            }
          )
        changeQuestionsToReadOnly(item.item, "$path.where(linkId = '${item.linkId}').answer.item")
      } else {
        changeQuestionsToReadOnly(item.item, "$path.where(linkId = '${item.linkId}').item")
      }
    }
  }
}
