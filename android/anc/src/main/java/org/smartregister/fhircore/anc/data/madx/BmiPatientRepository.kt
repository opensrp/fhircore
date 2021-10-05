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

package org.smartregister.fhircore.anc.data.madx

import com.google.android.fhir.FhirEngine
import com.google.android.fhir.search.count
import java.util.Date
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.DateType
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Resource
import org.smartregister.fhircore.anc.AncApplication
import org.smartregister.fhircore.anc.data.madx.model.PatientBMIItem
import org.smartregister.fhircore.anc.sdk.QuestionnaireUtils
import org.smartregister.fhircore.anc.sdk.ResourceMapperExtended
import org.smartregister.fhircore.anc.util.RegisterType
import org.smartregister.fhircore.anc.util.filterBy
import org.smartregister.fhircore.anc.util.loadRegisterConfig
import org.smartregister.fhircore.engine.data.domain.util.DomainMapper
import org.smartregister.fhircore.engine.data.domain.util.RegisterRepository
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.extension.format
import org.smartregister.fhircore.engine.util.extension.loadResourceTemplate
import org.smartregister.fhircore.engine.util.extension.searchActivePatients

class BmiPatientRepository(
  override val fhirEngine: FhirEngine,
  override val domainMapper: DomainMapper<Patient, PatientBMIItem>,
  private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider
) : RegisterRepository<Patient, PatientBMIItem> {

  private val registerConfig =
    AncApplication.getContext().loadRegisterConfig(RegisterType.ANC_REGISTER_ID)

  private val resourceMapperExtended = ResourceMapperExtended(fhirEngine)

  override suspend fun loadData(
    query: String,
    pageNumber: Int,
    loadAll: Boolean
  ): List<PatientBMIItem> {
    return withContext(dispatcherProvider.io()) {
      val patients =
        fhirEngine.searchActivePatients(query = query, pageNumber = pageNumber, loadAll = loadAll)
      patients.map { domainMapper.mapToDomainModel(it) }
    }
  }

  override suspend fun countAll(): Long {
    return fhirEngine.count<Patient> { filterBy(registerConfig.primaryFilter!!) }
  }

  suspend fun recordComputedBMI(
    questionnaire: Questionnaire,
    questionnaireResponse: QuestionnaireResponse,
    patientId: String,
    encounterID: String,
    height: Double,
    weight: Double,
    computedBMI: Double
  ): Boolean {
    resourceMapperExtended.saveParsedResource(questionnaireResponse, questionnaire, patientId, null)
    return recordBmi(patientId, encounterID, height, weight, computedBMI)
  }

  private suspend fun recordBmi(
    patientId: String,
    formEncounterId: String,
    height: Double? = null,
    weight: Double? = null,
    computedBMI: Double? = null
  ): Boolean {
    try {
      val bmiEncounterData =
        buildBMIConfigData(
          patientId = patientId,
          recordId = formEncounterId,
          height = height,
          weight = weight,
          computedBMI = computedBMI
        )
      val bmiEncounter = loadConfig(Template.BMI_ENCOUNTER, Encounter::class.java, bmiEncounterData)
      fhirEngine.save(bmiEncounter)

      val bmiWeightObservationRecordId = QuestionnaireUtils.getUniqueId()
      val bmiWeightObservationData =
        buildBMIConfigData(
          patientId = patientId,
          recordId = bmiWeightObservationRecordId,
          bmiEncounter = bmiEncounter,
          height = height,
          weight = weight,
          computedBMI = computedBMI
        )
      val bmiWeightObservation =
        loadConfig(Template.BMI_PATIENT_WEIGHT, Observation::class.java, bmiWeightObservationData)
      fhirEngine.save(bmiWeightObservation)

      val bmiHeightObservationRecordId = QuestionnaireUtils.getUniqueId()
      val bmiHeightObservationData =
        buildBMIConfigData(
          patientId = patientId,
          recordId = bmiHeightObservationRecordId,
          bmiEncounter = bmiEncounter,
          height = height,
          weight = weight,
          computedBMI = computedBMI,
          refObsWeightFormId = bmiWeightObservationRecordId
        )
      val bmiHeightObservation =
        loadConfig(Template.BMI_PATIENT_HEIGHT, Observation::class.java, bmiHeightObservationData)
      fhirEngine.save(bmiHeightObservation)

      val bmiObservationRecordId = QuestionnaireUtils.getUniqueId()
      val bmiObservationData =
        buildBMIConfigData(
          patientId = patientId,
          recordId = bmiObservationRecordId,
          bmiEncounter = bmiEncounter,
          height = height,
          weight = weight,
          computedBMI = computedBMI,
          refObsWeightFormId = bmiWeightObservationRecordId,
          refObsHeightFormId = bmiHeightObservationRecordId
        )
      val bmiObservation =
        loadConfig(Template.BMI_PATIENT_BMI, Observation::class.java, bmiObservationData)
      fhirEngine.save(bmiObservation)
      return true
    } catch (e: Exception) {
      e.printStackTrace()
      return false
    }
  }

  private fun <T : Resource> loadConfig(
    id: String,
    clazz: Class<T>,
    data: Map<String, String?> = emptyMap()
  ): T {
    return AncApplication.getContext().loadResourceTemplate(id, clazz, data)
  }

  private fun buildBMIConfigData(
    recordId: String,
    patientId: String,
    bmiEncounter: Encounter? = null,
    height: Double? = null,
    weight: Double? = null,
    computedBMI: Double? = null, // refObsEncounterId: String? = null,
    refObsWeightFormId: String? = null,
    refObsHeightFormId: String? = null
  ): Map<String, String?> {
    return mapOf(
      "#Id" to recordId, // QuestionnaireUtils.getUniqueId(),
      "#RefPatient" to QuestionnaireUtils.asPatientReference(patientId).reference,
      "#RefEncounter" to bmiEncounter?.id,
      "#RefPractitioner" to "Practitioner/399",
      "#EffectiveDate" to DateType(Date()).format(),
      "#ValueWeight" to weight?.toString(),
      "#ValueHeight" to height?.toString(),
      "#ValueBMI" to computedBMI.toString(),
      "#RefIdObservationBodyHeight" to refObsHeightFormId,
      "#RefIdObservationBodyWeight" to refObsWeightFormId,
    )
  }

  companion object {
    object Template {
      const val BMI_ENCOUNTER = "bmi_patient_encounter_template.json"
      const val BMI_PATIENT_WEIGHT = "bmi_patient_weight_observation_template.json"
      const val BMI_PATIENT_HEIGHT = "bmi_patient_height_observation_template.json"
      const val BMI_PATIENT_BMI = "bmi_patient_computed_bmi_observation_template.json"
    }
  }
}
