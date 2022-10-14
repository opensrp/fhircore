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
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import ca.uhn.fhir.parser.IParser
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.*
import org.smartregister.fhircore.eir.R
import org.smartregister.fhircore.eir.data.model.PatientVaccineSummary
import org.smartregister.fhircore.eir.ui.patient.details.nextDueDateFmt
import org.smartregister.fhircore.eir.ui.patient.details.ordinalOf
import org.smartregister.fhircore.engine.ui.base.AlertDialogue
import org.smartregister.fhircore.engine.ui.questionnaire.ExtractionProgress
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import timber.log.Timber

class RecordVaccineActivity : QuestionnaireActivity() {

  private lateinit var savedImmunization: Immunization

  @Inject lateinit var fhirParser: IParser

  override val questionnaireViewModel: RecordVaccineViewModel by viewModels()

  override fun handleQuestionnaireResponse(questionnaireResponse: QuestionnaireResponse) {
    lifecycleScope.launch {
      val bundle =
        questionnaireViewModel.performExtraction(
          this@RecordVaccineActivity,
          questionnaire,
          questionnaireResponse
        )

      if (bundle.entryFirstRep.resource is Immunization) {
        savedImmunization =
          bundle.entry.first { it.resource is Immunization }.resource as Immunization

        val lastVaccine = questionnaireViewModel.loadLatestVaccine(clientIdentifier!!)

        if (handleValidation(lastVaccine, savedImmunization)) {
          sanitizeExtractedData(savedImmunization, lastVaccine)

          // method below triggers save success automatically
          questionnaireViewModel.saveBundleResources(bundle)
          questionnaireViewModel.extractionProgress.postValue(ExtractionProgress.Success())
        } else dismissSaveProcessing()
      } else handleExtractionError(questionnaireResponse)
    }
  }

  fun sanitizeExtractedData(immunization: Immunization, lastVaccine: PatientVaccineSummary?) {
    // get from previous vaccine plus one OR as first dose
    val currentDose = lastVaccine?.doseNumber?.plus(1) ?: 1
    immunization.apply {
      protocolAppliedFirstRep.doseNumber = PositiveIntType(currentDose)
      occurrence = DateTimeType.now()
      patient = Reference().apply { reference = "Patient/$clientIdentifier" }
      status = Immunization.ImmunizationStatus.COMPLETED
    }
  }

  override fun postSaveSuccessful(questionnaireResponse: QuestionnaireResponse, extras: List<Resource>?) {
    val nextVaccineDate = savedImmunization.nextDueDateFmt()
    val currentDose = savedImmunization.protocolAppliedFirstRep.doseNumberPositiveIntType.value

    val title =
      getString(
        R.string.ordinal_vaccine_dose_recorded,
        savedImmunization.vaccineCode.codingFirstRep.code,
        currentDose.ordinalOf()
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
