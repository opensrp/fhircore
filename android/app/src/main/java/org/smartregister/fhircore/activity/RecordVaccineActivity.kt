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

package org.smartregister.fhircore.activity

import android.app.AlertDialog
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.fhir.datacapture.mapping.ResourceMapper
import java.util.Date
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.apache.commons.lang3.StringUtils
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.PositiveIntType
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Reference
import org.smartregister.fhircore.FhirApplication
import org.smartregister.fhircore.R
import org.smartregister.fhircore.activity.core.BaseActivity
import org.smartregister.fhircore.model.CovaxDetailView
import org.smartregister.fhircore.model.PatientItem
import org.smartregister.fhircore.model.RecordVaccineResult
import org.smartregister.fhircore.util.Utils
import org.smartregister.fhircore.viewmodel.CovaxListViewModel
import org.smartregister.fhircore.viewmodel.PatientListViewModelFactory
import org.smartregister.fhircore.viewmodel.QuestionnaireViewModel

class RecordVaccineActivity : BaseActivity() {

  private val questionnaireViewModel: QuestionnaireViewModel by viewModels()
  private lateinit var covaxListViewModel: CovaxListViewModel
  private lateinit var clientIdentifier: String
  private lateinit var detailView: CovaxDetailView
  private lateinit var recordVaccine: ActivityResultLauncher<CovaxDetailView>

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    clientIdentifier = intent.getStringExtra(CovaxDetailView.COVAX_ARG_ITEM_ID)!!
    detailView =
      Utils.loadConfig(
        CovaxDetailView.COVAX_DETAIL_VIEW_CONFIG_ID,
        CovaxDetailView::class.java,
        this
      )

    recordVaccine =
      registerForActivityResult(RecordVaccineResult(clientIdentifier)) {
        it?.run { handleImmunizationResult(it) } // todo handle questionnaire failures
      }

    supportActionBar!!.apply {
      title = detailView.vaccineQuestionnaireTitle
      setDisplayHomeAsUpEnabled(true)
    }

    covaxListViewModel =
      ViewModelProvider(
          this,
          PatientListViewModelFactory(application, FhirApplication.fhirEngine(this))
        )
        .get(CovaxListViewModel::class.java)

    recordVaccine()
  }

  override fun getContentLayout(): Int {
    return R.layout.activity_record_vaccine
  }

  private fun recordVaccine() {
    covaxListViewModel
      .getPatientItem(clientIdentifier)
      .observe(this, { recordVaccine.launch(detailView) })
  }

  private fun handleImmunizationResult(response: QuestionnaireResponse) {
    // TODO Replace manual mapping with resource mapper and simplify

    val questionnaire =
      questionnaireViewModel.loadQuestionnaire(detailView.vaccineQuestionnaireIdentifier)

    covaxListViewModel.getPatientItem(clientIdentifier).observe(this) {
      covaxListViewModel.viewModelScope.launch {
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
              val currentDoseNumber = it.vaccineSummary?.doseNumber ?: 0
              this.doseNumber = PositiveIntType(currentDoseNumber + 1)
            }
          )

        GlobalScope.launch(Dispatchers.Main) { showVaccineRecordDialog(immunization, it) }
      }
    }
  }

  // todo optimize
  private fun showVaccineRecordDialog(immunization: Immunization, patientItem: PatientItem) {
    val builder = AlertDialog.Builder(this)
    val doseNumber = immunization.protocolApplied.first().doseNumberPositiveIntType.value
    var msgText = ""
    var titleText = ""
    val vaccineDate = immunization.occurrenceDateTimeType.toHumanDisplay()
    val nextVaccineDate = Utils.addDays(vaccineDate, 28)
    val currentDose = immunization.vaccineCode.coding.first().code
    val initialDose = patientItem.vaccineSummary?.initialDose
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
      return bundleOf(Pair(CovaxDetailView.COVAX_ARG_ITEM_ID, patientId))
    }
  }
}
