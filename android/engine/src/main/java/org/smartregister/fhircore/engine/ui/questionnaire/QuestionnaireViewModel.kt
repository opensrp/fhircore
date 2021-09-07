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
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.IParser
import com.google.android.fhir.datacapture.mapping.ResourceMapper
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.StructureMap
import org.smartregister.fhircore.engine.configuration.app.ConfigurableApplication
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_ARG_PATIENT_KEY
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_ARG_RELATED_PATIENT_KEY
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_BYPASS_SDK_EXTRACTOR
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireUtils.asPatientReference
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireUtils.extractFlags
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireUtils.extractTags
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireUtils.getUniqueId
import org.smartregister.fhircore.engine.util.extension.extractExtendedPatient

class QuestionnaireViewModel(application: Application, private val state: SavedStateHandle) :
  AndroidViewModel(application) {

  var structureMapProvider: (suspend (String) -> StructureMap?)? = null
  val fhirEngine = (application as ConfigurableApplication).fhirEngine

  val questionnaire: Questionnaire
    get() {
      val id: String = state[QuestionnaireActivity.QUESTIONNAIRE_PATH_KEY]!!
      return loadQuestionnaire(id)
    }

  val app = application
  fun loadQuestionnaire(id: String): Questionnaire {
    // todo DELETEEEEEEEEEEEEEEEEE IT
    val iParser: IParser = FhirContext.forR4().newJsonParser()
    val qJson = app.assets.open(id).bufferedReader().use { it.readText() }

    if (true) return iParser.parseResource(qJson) as Questionnaire

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
          structureMap = fhirEngine.load(StructureMap::class.java, structureMapId)
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
  ): String {
    val patientArg = intent.getStringExtra(QUESTIONNAIRE_ARG_PATIENT_KEY)

    // TODO https://github.com/opensrp/fhircore/issues/403
    // change when above supports this
    if (intent.hasExtra(QUESTIONNAIRE_BYPASS_SDK_EXTRACTOR)) {
      val relatedTo = intent.getStringExtra(QUESTIONNAIRE_ARG_RELATED_PATIENT_KEY)
      val patientId = patientArg ?: getUniqueId()

      saveParsedResource(questionnaireResponse, questionnaire, patientId, relatedTo)

      return patientId
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
      val resourceId = intent.getStringExtra(QUESTIONNAIRE_ARG_PATIENT_KEY)

      saveBundleResources(bundle, resourceId)
    }

    return patientArg!!
  }

  fun getStructureMapProvider(context: Context): (suspend (String) -> StructureMap?) {
    if (structureMapProvider == null) {
      structureMapProvider = { structureMapUrl: String -> fetchStructureMap(structureMapUrl) }
    }

    return structureMapProvider!!
  }

  fun saveParsedResource(
    questionnaireResponse: QuestionnaireResponse,
    questionnaire: Questionnaire,
    patientId: String,
    relatedTo: String?
  ) {
    if (!questionnaire.hasSubjectType("Patient")) return

    viewModelScope.launch {
      val patient =
        kotlin.runCatching { fhirEngine.load(Patient::class.java, patientId) }.getOrElse {
          ResourceMapper.extract(questionnaire, questionnaireResponse).entry[0].resource as Patient
        }

      patient.id = patientId

      val tags = extractTags(questionnaireResponse, questionnaire)
      tags.forEach { patient.meta.addTag(it) }

      val flags = extractFlags(questionnaireResponse, questionnaire, patient)
      flags.forEach {
        patient.addExtension(it.second)

        saveResource(it.first)
      }

      relatedTo?.let { patient.addLink(getLink(relatedTo)) }

      // TODO https://github.com/google/android-fhir/pull/488
      // replace with patient when above is merged and released
      val extendedPatient = patient.extractExtendedPatient()

      saveResource(extendedPatient)

      // only one level of nesting per obs group is supported by fhircore for now
      val observations =
        QuestionnaireUtils.extractObservations(questionnaireResponse, questionnaire, patient)

      observations.forEach { saveResource(it) }

      // only one risk assessment per questionnaire is supported by fhircore for now
      val riskAssessment =
        QuestionnaireUtils.extractRiskAssessment(observations, questionnaireResponse, questionnaire)

      riskAssessment?.let { saveResource(riskAssessment) }
    }
  }

  private fun getLink(relatedTo: String): Patient.PatientLinkComponent {
    val link = Patient.PatientLinkComponent()
    link.other = asPatientReference(relatedTo)
    link.type = Patient.LinkType.REFER
    return link
  }
}
