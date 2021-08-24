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

package org.smartregister.fhircore.eir.ui.questionnaire

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.google.android.fhir.datacapture.mapping.ResourceMapper
import java.util.UUID
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.StructureMap
import org.smartregister.fhircore.eir.EirApplication

class QuestionnaireViewModel(application: Application, private val state: SavedStateHandle) :
  AndroidViewModel(application) {

  var structureMapProvider: (suspend (String) -> StructureMap?)? = null
  var fhirEngine = EirApplication.getContext().fhirEngine

  val questionnaire: Questionnaire
    get() {
      val id: String = state[QuestionnaireActivity.QUESTIONNAIRE_PATH_KEY]!!
      return loadQuestionnaire(id)
    }

  fun loadQuestionnaire(id: String): Questionnaire {
    return runBlocking { fhirEngine.load(Questionnaire::class.java, id) }
  }

  fun saveResource(resource: Resource) {
    viewModelScope.launch { fhirEngine.save(resource) }
  }

  fun saveBundleResources(bundle: Bundle, resourceId: String? = null) {
    if (!bundle.isEmpty) {
      bundle.entry.forEach { bundleEntry ->
        if (resourceId != null && bundleEntry.hasResource()) {
          bundleEntry.resource.id = resourceId
        }

        saveResource(bundleEntry.resource)
      }
    }
  }

  fun fetchStructureMap(structureMapUrl: String?): StructureMap? {
    var structureMap: StructureMap? = null
    runBlocking {
      launch {
        val structureMapId = structureMapUrl?.substringAfterLast("/")
        if (structureMapId != null) {
          structureMap =
            EirApplication.getContext().fhirEngine.load(StructureMap::class.java, structureMapId)
        }
      }
    }

    return structureMap
  }

  fun saveExtractedResources(
    context: Context,
    intent: Intent,
    questionnaire: Questionnaire,
    questionnaireResponse: QuestionnaireResponse
  ) {

    // todo remove if below to turn below login on, when structure-map has obs and flag and
    // risk-assessment as well
    if (intent.hasExtra(QuestionnaireActivity.QUESTIONNAIRE_BYPASS_SDK_EXTRACTOR)) {
      saveParsedResource(questionnaireResponse, questionnaire)
      return
    }

    var bundle: Bundle

    viewModelScope.launch {
      bundle =
        ResourceMapper.extract(
          questionnaire,
          questionnaireResponse,
          getStructureMapProvider(context),
          context
        )

      // todo Assign Encounter , Observation etc their separate Ids with reference to Patient Id
      val resourceId = intent.getStringExtra(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY)

      saveBundleResources(bundle, resourceId)
    }
  }

  fun getStructureMapProvider(context: Context): (suspend (String) -> StructureMap?) {
    if (structureMapProvider == null) {
      structureMapProvider = { structureMapUrl: String -> fetchStructureMap(structureMapUrl) }
    }

    return structureMapProvider!!
  }

  fun saveParsedResource(
    questionnaireResponse: QuestionnaireResponse,
    questionnaire: Questionnaire
  ) {
    if (!questionnaire.hasSubjectType("Patient")) return

    viewModelScope.launch {
      val patient =
        ResourceMapper.extract(questionnaire, questionnaireResponse).entry[0].resource as Patient

      val barcode =
        QuestionnaireUtils.valueStringWithLinkId(
          questionnaireResponse,
          QuestionnaireActivity.QUESTIONNAIRE_ARG_BARCODE_KEY
        )

      patient.id = barcode ?: UUID.randomUUID().toString().lowercase()

      saveResource(patient)

      // only one level of nesting per obs group is supported by fhircore for now
      val observations =
        QuestionnaireUtils.extractObservations(questionnaireResponse, questionnaire, patient)

      observations.forEach { saveResource(it) }

      // only one risk assessment per questionnaire is supported by fhircore for now
      val riskAssessment =
        QuestionnaireUtils.extractRiskAssessment(observations, questionnaireResponse, questionnaire)

      if (riskAssessment != null) {
        saveResource(riskAssessment)

        val flag =
          QuestionnaireUtils.extractFlag(questionnaireResponse, questionnaire, riskAssessment)

        if (flag != null) {
          saveResource(flag)

          // todo remove this when sync is implemented
          val ext =
            QuestionnaireUtils.extractFlagExtension(flag, questionnaireResponse, questionnaire)
          if (ext != null) {
            patient.addExtension(ext)
          }

          saveResource(patient)
        }
      }
    }
  }
}
