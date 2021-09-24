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

package org.smartregister.fhircore.eir.ui.vaccine

import android.app.AlertDialog
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.fhir.datacapture.mapping.ResourceMapper
import java.util.Date
import java.util.UUID
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.PositiveIntType
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Reference
import org.smartregister.fhircore.eir.R
import org.smartregister.fhircore.eir.data.PatientRepository
import org.smartregister.fhircore.eir.data.model.PatientVaccineSummary
import org.smartregister.fhircore.eir.ui.patient.register.PatientItemMapper
import org.smartregister.fhircore.engine.configuration.app.ConfigurableApplication
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.util.DateUtils
import org.smartregister.fhircore.engine.util.extension.createFactory

class RecordVaccineActivity : QuestionnaireActivity() {

  lateinit var recordVaccineViewModel: RecordVaccineViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    recordVaccineViewModel =
      ViewModelProvider(
          this@RecordVaccineActivity,
          RecordVaccineViewModel(
              application,
              PatientRepository(
                (application as ConfigurableApplication).fhirEngine,
                PatientItemMapper
              )
            )
            .createFactory()
        )
        .get(RecordVaccineViewModel::class.java)
  }

  override fun handleQuestionnaireResponse(questionnaireResponse: QuestionnaireResponse) {
    lifecycleScope.launch {
      clientIdentifier?.let { identifier: String ->
        recordVaccineViewModel.getVaccineSummary(identifier).observe(this@RecordVaccineActivity) {
          vaccineSummary: PatientVaccineSummary? ->
          if (vaccineSummary != null) {
            lifecycleScope.launch {
              questionnaire?.let { questionnaire ->
                getImmunization(questionnaire, questionnaireResponse, vaccineSummary).run {
                  showVaccineRecordDialog(this, vaccineSummary)
                }
              }
            }
          }
        }
      }
    }
  }

  private suspend fun getImmunization(
    questionnaire: Questionnaire,
    questionnaireResponse: QuestionnaireResponse,
    vaccineSummary: PatientVaccineSummary
  ): Immunization {
    val immunization =
      ResourceMapper.extract(questionnaire, questionnaireResponse).entry!![0].resource as
        Immunization
    immunization.apply {
      id = UUID.randomUUID().toString()
      recorded = Date()
      status = Immunization.ImmunizationStatus.COMPLETED
      vaccineCode =
        CodeableConcept().apply {
          this.text = questionnaireResponse.item[0].answer[0].valueCoding.code
          this.coding = listOf(questionnaireResponse.item[0].answer[0].valueCoding)
        }
      occurrence = DateTimeType.today()
      patient = Reference().apply { this.reference = "Patient/$clientIdentifier" }
      protocolApplied =
        listOf(
          Immunization.ImmunizationProtocolAppliedComponent().apply {
            val currentDoseNumber = vaccineSummary.doseNumber
            this.doseNumber = PositiveIntType(currentDoseNumber + 1)
          }
        )
    }
    return immunization
  }

  private fun showVaccineRecordDialog(
    immunization: Immunization,
    vaccineSummary: PatientVaccineSummary
  ) {
    val doseNumber = immunization.protocolApplied.first().doseNumberPositiveIntType.value
    val message: String
    val titleText: String
    val vaccineDate = immunization.occurrenceDateTimeType.toHumanDisplay()
    val nextVaccineDate = DateUtils.addDays(vaccineDate, 28)
    val currentDose = immunization.vaccineCode.coding.first().code
    val initialDose = vaccineSummary.initialDose
    val isSameAsFirstDose =
      initialDose.isEmpty() || currentDose.equals(initialDose, ignoreCase = true)
    if (isSameAsFirstDose) {
      message =
        when (doseNumber) {
          2 -> resources.getString(R.string.fully_vaccinated)
          else ->
            resources.getString(
              R.string.immunization_next_dose_text,
              doseNumber + 1,
              nextVaccineDate
            )
        }
      titleText =
        this.getString(R.string.ordinal_vaccine_dose_recorded, immunization.vaccineCode.text)
    } else {
      message = "Second vaccine dose should be same as first"
      titleText = "Initially received $initialDose"
    }

    val dialogBuilder =
      AlertDialog.Builder(this).apply {
        setTitle(titleText)
        setMessage(message)
        setNegativeButton(R.string.done) { dialogInterface, _ ->
          dialogInterface.dismiss()
          if (isSameAsFirstDose) {
            questionnaireViewModel.saveResource(immunization)
            finish()
          }
        }
      }

    dialogBuilder.create().run {
      setCancelable(false)
      show()
    }
  }
}
