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

package org.smartregister.fhircore.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.os.bundleOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import com.google.android.fhir.datacapture.mapping.ResourceMapper
import java.util.UUID
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.StructureMap
import org.smartregister.fhircore.FhirApplication
import org.smartregister.fhircore.activity.core.QuestionnaireActivity.Companion.QUESTIONNAIRE_ARG_BARCODE_KEY
import org.smartregister.fhircore.activity.core.QuestionnaireActivity.Companion.QUESTIONNAIRE_ARG_PATIENT_KEY
import org.smartregister.fhircore.activity.core.QuestionnaireActivity.Companion.QUESTIONNAIRE_ARG_RELATED_PATIENT_KEY
import org.smartregister.fhircore.activity.core.QuestionnaireActivity.Companion.QUESTIONNAIRE_BYPASS_SDK_EXTRACTOR
import org.smartregister.fhircore.activity.core.QuestionnaireActivity.Companion.QUESTIONNAIRE_PATH_KEY
import org.smartregister.fhircore.util.QuestionnaireUtils
import org.smartregister.fhircore.util.QuestionnaireUtils.asPatientReference
import org.smartregister.fhircore.util.Utils
import timber.log.Timber

class QuestionnaireViewModel(application: Application, private val state: SavedStateHandle) :
  AndroidViewModel(application) {

  var structureMapProvider: (suspend (String) -> StructureMap?)? = null
  var fhirEngine = FhirApplication.fhirEngine(application)

  val questionnaire: Questionnaire
    get() {
      val id: String = state[QUESTIONNAIRE_PATH_KEY]!!
      return loadQuestionnaire(id)
    }

  fun loadQuestionnaire(id: String): Questionnaire {
    return runBlocking { fhirEngine.load(Questionnaire::class.java, id) }
  }

  fun saveResource(resource: Resource) {
    GlobalScope.launch { fhirEngine.save(resource) }
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

  fun fetchStructureMap(context: Context, structureMapUrl: String?): StructureMap? {
    var structureMap: StructureMap? = null
    runBlocking {
      launch {
        val structureMapId = structureMapUrl?.substringAfterLast("/")
        if (structureMapId != null) {
          structureMap =
            FhirApplication.fhirEngine(context).load(StructureMap::class.java, structureMapId)
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
  ): android.os.Bundle {
    // todo remove if condition below when structure-map has obs and flag and risk-assessment
    if (intent.hasExtra(QUESTIONNAIRE_BYPASS_SDK_EXTRACTOR)) {
      return saveParsedResource(questionnaireResponse, questionnaire, intent)
    }

    var bundle: Bundle = runBlocking {
      ResourceMapper.extract(
        questionnaire,
        questionnaireResponse,
        getStructureMapProvider(context),
        context
      )
    }

    // todo Assign Encounter , Observation etc their separate Ids with reference to Patient Id
    val resourceId = intent.getStringExtra(QUESTIONNAIRE_ARG_PATIENT_KEY)

    saveBundleResources(bundle, resourceId)

    return bundleOf()
  }

  fun getStructureMapProvider(context: Context): (suspend (String) -> StructureMap?) {
    if (structureMapProvider == null) {
      structureMapProvider =
        { structureMapUrl: String ->
          fetchStructureMap(context, structureMapUrl)
        }
    }

    return structureMapProvider!!
  }

  fun saveParsedResource(
    questionnaireResponse: QuestionnaireResponse,
    questionnaire: Questionnaire,
    intent: Intent
  ): android.os.Bundle {
    Timber.i(Utils.parser.encodeResourceToString(questionnaireResponse)) // todo

    if (!questionnaire.hasSubjectType("Patient")) return bundleOf()

    val patient = runBlocking {
      ResourceMapper.extract(questionnaire, questionnaireResponse).entry[0].resource as Patient
    }
    val barcode =
      QuestionnaireUtils.valueStringWithLinkId(questionnaireResponse, QUESTIONNAIRE_ARG_BARCODE_KEY)

    patient.id = barcode ?: UUID.randomUUID().toString().toLowerCase()

    // only one level of nesting per obs group is supported by fhircore for now
    val observations =
      QuestionnaireUtils.extractObservations(questionnaireResponse, questionnaire, patient)

    observations.forEach { saveResource(it) }

    // only one risk assessment per questionnaire is supported by fhircore for now
    val riskAssessment =
      QuestionnaireUtils.extractRiskAssessment(observations, questionnaireResponse, questionnaire)

    riskAssessment?.let { saveResource(it) }

    val flags = QuestionnaireUtils.extractFlags(questionnaireResponse, questionnaire, patient)
    flags.forEach {
      val flag = it.first
      saveResource(flag)

      patient.addExtension(it.second) // do this directly from ResourceMapper
    }

    // do this directly from ResourceMapper when structure map is integrated
    QuestionnaireUtils.extractTags(questionnaireResponse, questionnaire).forEach {
      patient.meta.addTag(it)
    }

    var groupId = intent.getStringExtra(QUESTIONNAIRE_ARG_RELATED_PATIENT_KEY)

    groupId?.let { applyRelationship(patient, groupId) }

    // replace updated patient with extended
    val extendedPatient = Utils.convertToExtendedPatientResource(patient)

    saveResource(extendedPatient)

    Timber.e("PatIENT: ${Utils.parser.encodeResourceToString(extendedPatient)}")

    return buildResponseBundle(patient, groupId)
  }

  fun applyRelationship(patient: Patient, groupId: String) {
    val link = Patient.PatientLinkComponent()
    link.other = asPatientReference(groupId)
    link.type = Patient.LinkType.REFER

    patient.addLink(link)
  }

  fun buildResponseBundle(patient: Patient?, groupId: String?): android.os.Bundle {
    val result = bundleOf()
    result.putString(QUESTIONNAIRE_ARG_PATIENT_KEY, patient?.id)
    result.putString(QUESTIONNAIRE_ARG_RELATED_PATIENT_KEY, groupId)
    return result
  }
}
