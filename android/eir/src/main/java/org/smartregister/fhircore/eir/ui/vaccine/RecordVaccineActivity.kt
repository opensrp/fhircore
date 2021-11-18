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
import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import ca.uhn.fhir.context.FhirContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.eir.R
import org.smartregister.fhircore.eir.data.PatientRepository
import org.smartregister.fhircore.eir.data.model.PatientVaccineSummary
import org.smartregister.fhircore.eir.ui.patient.register.PatientItemMapper
import org.smartregister.fhircore.engine.configuration.app.ConfigurableApplication
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireViewModel
import org.smartregister.fhircore.engine.util.DateUtils
import org.smartregister.fhircore.engine.util.extension.createFactory
import timber.log.Timber

class RecordVaccineActivity : QuestionnaireActivity() {

  override fun createViewModel(application: Application): QuestionnaireViewModel {
    return ViewModelProvider(
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
        (questionnaireViewModel as RecordVaccineViewModel).getVaccineSummary(identifier).observe(
            this@RecordVaccineActivity
          ) { vaccineSummary: PatientVaccineSummary? ->
          if (vaccineSummary != null) {
            lifecycleScope.launch {
              questionnaire.let { questionnaire ->
                questionnaireViewModel.performExtraction(
                    questionnaire,
                    questionnaireResponse,
                    this@RecordVaccineActivity
                  )
                  .run {
                    val immunizationEntry = entry.firstOrNull { it.resource is Immunization }

                    if (immunizationEntry == null) {
                      val fhirJsonParser = FhirContext.forR4().newJsonParser()
                      Timber.e(
                        "Immunization extraction failed for ${
                        fhirJsonParser.encodeResourceToString(
                          questionnaireResponse
                        )
                        } producing ${
                        fhirJsonParser.encodeResourceToString(
                          this
                        )
                        }"
                      )
                      lifecycleScope.launch(Dispatchers.Main) { handleExtractionError() }
                    } else {
                      showVaccineRecordDialog(this, vaccineSummary)
                    }
                  }
              }
            }
          }
        }
      }
    }
  }

  private fun showVaccineRecordDialog(
    bundle: org.hl7.fhir.r4.model.Bundle,
    vaccineSummary: PatientVaccineSummary
  ) {
    val immunization = bundle.entry.first { it.resource is Immunization }.resource as Immunization

    val doseNumber = immunization.protocolApplied.first().doseNumberPositiveIntType.value
    val message: String
    val titleText: String
    val vaccineDate = immunization.occurrenceDateTimeType.toHumanDisplay()
    val nextVaccineDate =
      DateUtils.addDays(vaccineDate, 28, dateTimeFormat = "MMM d, yyyy h:mm:ss a")
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
      message = getString(R.string.second_vaccine_dose_should_be_same_first)
      titleText = getString(R.string.initially_received_x).format(initialDose)
    }

    val dialogBuilder =
      AlertDialog.Builder(this).apply {
        setTitle(titleText)
        setMessage(message)
        setNegativeButton(R.string.done) { dialogInterface, _ ->
          dialogInterface.dismiss()
          if (isSameAsFirstDose) {
            questionnaireViewModel.saveBundleResources(bundle)
            finish()
          }
        }
      }

    dialogBuilder.create().run {
      setCancelable(false)
      show()
    }
  }

  private fun handleExtractionError() {
    AlertDialog.Builder(this)
      .setTitle(getString(R.string.error_reading_immunization_details))
      .setMessage(getString(R.string.kindly_retry_contact_devs_problem_persists))
      .setPositiveButton(android.R.string.ok) { dialogInterface, _ -> dialogInterface.dismiss() }
      .setCancelable(true)
      .show()
  }
}
