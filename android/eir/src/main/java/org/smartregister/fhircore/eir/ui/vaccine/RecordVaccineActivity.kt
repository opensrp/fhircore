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
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.PositiveIntType
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Reference
import org.smartregister.fhircore.eir.R
import org.smartregister.fhircore.eir.data.PatientRepository
import org.smartregister.fhircore.eir.data.model.PatientVaccineSummary
import org.smartregister.fhircore.eir.ui.patient.register.PatientItemMapper
import org.smartregister.fhircore.engine.configuration.app.ConfigurableApplication
import org.smartregister.fhircore.engine.ui.base.AlertDialogue
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireViewModel
import org.smartregister.fhircore.engine.util.extension.createFactory
import org.smartregister.fhircore.engine.util.extension.plusDaysAsString
import timber.log.Timber

class RecordVaccineActivity : QuestionnaireActivity() {

  private lateinit var immunization: Immunization
  private val fhirParser = FhirContext.forR4Cached().newJsonParser()

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

  private fun getViewModel(): RecordVaccineViewModel {
    return questionnaireViewModel as RecordVaccineViewModel
  }

  override fun handleQuestionnaireResponse(questionnaireResponse: QuestionnaireResponse) {
    lifecycleScope.launch {
      val bundle =
        questionnaireViewModel.performExtraction(
          questionnaire,
          questionnaireResponse,
          this@RecordVaccineActivity
        )

      if (bundle.entryFirstRep.resource is Immunization) {
        immunization = bundle.entry.first { it.resource is Immunization }.resource as Immunization

        val lastVaccine = getViewModel().loadLatestVaccine(clientIdentifier!!)

        if (handleValidation(lastVaccine, immunization)) {
          sanitizeExtractedData(immunization, lastVaccine)

          // method below triggers save success automatically
          questionnaireViewModel.saveBundleResources(bundle)
        } else dismissSaveProcessing()
      } else handleExtractionError(questionnaireResponse)
    }
  }

  private fun sanitizeExtractedData(
    immunization: Immunization,
    lastVaccine: PatientVaccineSummary?
  ) {
    // get from previous vaccine plus one OR as first dose
    var currentDose = lastVaccine?.doseNumber?.plus(1) ?: 1

    immunization.protocolAppliedFirstRep.doseNumber = PositiveIntType(currentDose)
    immunization.occurrence = DateTimeType.now()
    immunization.patient = Reference().apply { reference = "Patient/$clientIdentifier" }
    immunization.status = Immunization.ImmunizationStatus.COMPLETED
  }

  override fun postSaveSuccessful() {
    val nextVaccineDate = immunization.occurrenceDateTimeType.plusDaysAsString(28)
    val currentDose = immunization.protocolAppliedFirstRep.doseNumberPositiveIntType.value

    val title =
      getString(
        R.string.ordinal_vaccine_dose_recorded,
        immunization.vaccineCode.codingFirstRep.code
      )
    val message =
      when (currentDose) {
        2 -> resources.getString(R.string.fully_vaccinated)
        else ->
          resources.getString(
            R.string.immunization_next_dose_text,
            currentDose + 1,
            nextVaccineDate
          )
      }

    AlertDialogue.showInfoAlert(
      this,
      message,
      title,
      {
        it.dismiss()
        finish()
      },
      R.string.done
    )
  }

  private fun handleValidation(previous: PatientVaccineSummary?, current: Immunization): Boolean {
    if (previous?.doseNumber == 2) {
      AlertDialogue.showErrorAlert(this, R.string.already_fully_vaccinated)
      return false
    }

    if (previous != null &&
        !current.vaccineCode.codingFirstRep.code.contentEquals(previous.initialDose, true)
    ) {
      AlertDialogue.showErrorAlert(
        this,
        getString(R.string.second_vaccine_dose_should_be_same_first),
        getString(R.string.initially_received_x).format(previous.initialDose)
      )
      return false
    }

    return true
  }

  private fun handleExtractionError(questionnaireResponse: QuestionnaireResponse) {
    Timber.e(
      "Immunization extraction failed for ${
      fhirParser.encodeResourceToString(
        questionnaireResponse
      )
      }"
    )

    AlertDialog.Builder(this)
      .setTitle(getString(R.string.error_reading_immunization_details))
      .setMessage(getString(R.string.kindly_retry_contact_devs_problem_persists))
      .setPositiveButton(android.R.string.ok) { dialogInterface, _ -> dialogInterface.dismiss() }
      .setCancelable(true)
      .show()
  }
}
