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
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.fhir.datacapture.mapping.ResourceMapper
import java.util.Date
import java.util.UUID
import kotlinx.coroutines.launch
import org.apache.commons.lang3.StringUtils
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.PositiveIntType
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Reference
import org.smartregister.fhircore.eir.R
import org.smartregister.fhircore.eir.form.config.QuestionnaireFormConfig
import org.smartregister.fhircore.eir.ui.patient.register.PatientItemMapper
import org.smartregister.fhircore.eir.ui.questionnaire.QuestionnaireViewModel
import org.smartregister.fhircore.engine.configuration.app.ConfigurableApplication
import org.smartregister.fhircore.engine.data.local.repository.model.PatientVaccineSummary
import org.smartregister.fhircore.engine.data.local.repository.patient.PatientRepository
import org.smartregister.fhircore.engine.util.DateUtils
import org.smartregister.fhircore.engine.util.FormConfigUtil
import org.smartregister.fhircore.engine.util.extension.createFactory

class RecordVaccineActivity : AppCompatActivity() {

  private val questionnaireViewModel: QuestionnaireViewModel by viewModels()
  private lateinit var recordVaccineViewModel: RecordVaccineViewModel
  private lateinit var clientIdentifier: String
  private lateinit var detailView: QuestionnaireFormConfig
  private lateinit var recordVaccine: ActivityResultLauncher<QuestionnaireFormConfig>

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    clientIdentifier = intent.getStringExtra(QuestionnaireFormConfig.COVAX_ARG_ITEM_ID)!!
    detailView =
      FormConfigUtil.loadConfig(QuestionnaireFormConfig.COVAX_DETAIL_VIEW_CONFIG_ID, this)

    recordVaccine =
      registerForActivityResult(RecordVaccineResult(clientIdentifier)) {
        it?.run { handleImmunizationResult(it) } // todo handle questionnaire failures
      }

    supportActionBar!!.apply {
      title = detailView.vaccineQuestionnaireTitle
      setDisplayHomeAsUpEnabled(true)
    }

    recordVaccineViewModel =
      ViewModelProvider(
          this,
          RecordVaccineViewModel(
              application,
              PatientRepository.getInstance(
                (application as ConfigurableApplication).fhirEngine,
                PatientItemMapper
              )
            )
            .createFactory()
        )
        .get(RecordVaccineViewModel::class.java)

    recordVaccine()
  }

  private fun recordVaccine() {
    lifecycleScope.launch {
      recordVaccineViewModel
        .getPatientItem(clientIdentifier)
        .observe(this@RecordVaccineActivity, { recordVaccine.launch(detailView) })
    }
  }

  private fun handleImmunizationResult(response: QuestionnaireResponse) {
    val questionnaire =
      questionnaireViewModel.loadQuestionnaire(detailView.vaccineQuestionnaireIdentifier)
    lifecycleScope.launch {
      recordVaccineViewModel.getPatientItem(clientIdentifier).observe(this@RecordVaccineActivity) {
        val immunization =
          ResourceMapper.extract(questionnaire, response).entry[0].resource as Immunization
        immunization.id = UUID.randomUUID().toString()
        immunization.recorded = Date()
        immunization.status = Immunization.ImmunizationStatus.COMPLETED
        immunization.vaccineCode =
          CodeableConcept().apply {
            this.text = response.item[0].answer[0].valueCoding.code
            this.coding = listOf(response.item[0].answer[0].valueCoding)
          }
        immunization.occurrence = DateTimeType.today()
        immunization.patient = Reference().apply { this.reference = "Patient/$clientIdentifier" }

        immunization.protocolApplied =
          listOf(
            Immunization.ImmunizationProtocolAppliedComponent().apply {
              val currentDoseNumber = it.doseNumber
              this.doseNumber = PositiveIntType(currentDoseNumber + 1)
            }
          )
        showVaccineRecordDialog(immunization, it)
      }
    }
  }

  // todo optimize
  private fun showVaccineRecordDialog(
    immunization: Immunization,
    vaccineSummary: PatientVaccineSummary
  ) {
    val builder = AlertDialog.Builder(this)
    val doseNumber = immunization.protocolApplied.first().doseNumberPositiveIntType.value
    var msgText = ""
    var titleText = ""
    val vaccineDate = immunization.occurrenceDateTimeType.toHumanDisplay()
    val nextVaccineDate = DateUtils.addDays(vaccineDate, 28)
    val currentDose = immunization.vaccineCode.coding.first().code
    val initialDose = vaccineSummary.initialDose
    val isSameAsFirstDose = StringUtils.isEmpty(initialDose) || currentDose.equals(initialDose)
    if (isSameAsFirstDose) {
      msgText =
        if (doseNumber == 2) {
          resources.getString(R.string.fully_vaccinated)
        } else {
          resources.getString(R.string.immunization_next_dose_text, doseNumber + 1, nextVaccineDate)
        }
      titleText =
        this.getString(R.string.ordinal_vaccine_dose_recorded, immunization.vaccineCode.text)
    } else {
      msgText = "Second vaccine dose should be same as first"
      titleText = "Initially received $initialDose"
    }

    // set title for alert dialog
    builder.setTitle(titleText)

    // set message for alert dialog
    builder.setMessage(msgText)

    // performing negative action
    builder.setNegativeButton(R.string.done) { dialogInterface, _ ->
      dialogInterface.dismiss()
      if (isSameAsFirstDose) {
        questionnaireViewModel.saveResource(immunization)
        finish()
      } else recordVaccine() // todo optimize flow... questionnaire should validate in itself
    }
    // Create the AlertDialog
    val alertDialog: AlertDialog = builder.create()
    // Set other dialog properties
    alertDialog.setCancelable(false)
    alertDialog.show()
  }

  companion object {
    fun getExtraBundles(patientId: String): Bundle {
      return bundleOf(Pair(QuestionnaireFormConfig.COVAX_ARG_ITEM_ID, patientId))
    }
  }
}
