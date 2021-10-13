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
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.RelatedPerson
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.StructureMap
import org.smartregister.fhircore.engine.configuration.app.ConfigurableApplication
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.FormConfigUtil

open class QuestionnaireViewModel(
  application: Application,
) : AndroidViewModel(application) {

  val extractionProgress = MutableLiveData<Boolean>()

  val defaultRepository: DefaultRepository =
    DefaultRepository(
      (application as ConfigurableApplication).fhirEngine,
    )

  var structureMapProvider: (suspend (String) -> StructureMap?)? = null

  suspend fun loadQuestionnaire(id: String): Questionnaire? = defaultRepository.loadResource(id)

  suspend fun getQuestionnaireConfig(form: String): QuestionnaireConfig {
    val loadConfig =
      withContext(DefaultDispatcherProvider.io()) {
        FormConfigUtil.loadConfig(QuestionnaireActivity.FORM_CONFIGURATIONS, getApplication())
      }

    return loadConfig.associateBy { it.form }.getValue(form)
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
      resourceId?.let {
        questionnaireResponse.id = UUID.randomUUID().toString()
        questionnaire.useContext.firstOrNull()?.let {
          if (it.hasValue() && it.hasValueCodeableConcept())
            questionnaireResponse.meta.tag = it.valueCodeableConcept.coding
        }
        questionnaireResponse.subject = Reference().apply { reference = "Patient/$it" }
        defaultRepository.save(questionnaireResponse)
      }

      val bundle = performExtraction(questionnaire, questionnaireResponse, context)

      saveBundleResources(bundle)
    }
  }

  suspend fun performExtraction(
    questionnaire: Questionnaire,
    questionnaireResponse: QuestionnaireResponse,
    context: Context
  ): Bundle {
    return ResourceMapper.extract(
      questionnaire = questionnaire,
      questionnaireResponse = questionnaireResponse,
      structureMapProvider = retrieveStructureMapProvider(),
      context = context
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

  fun retrieveStructureMapProvider(): (suspend (String) -> StructureMap?) {
    if (structureMapProvider == null) {
      structureMapProvider = { structureMapUrl: String -> fetchStructureMap(structureMapUrl) }
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
      loadPatient(patientId)?.apply { resourcesList.add(this) }
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
}
