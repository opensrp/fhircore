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
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.os.bundleOf
import androidx.fragment.app.commit
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.IParser
import com.google.android.fhir.datacapture.QuestionnaireFragment
import com.google.android.fhir.datacapture.mapping.ResourceMapper
import org.apache.commons.lang3.StringUtils
import org.hl7.fhir.r4.model.*
import org.smartregister.fhircore.R
import org.smartregister.fhircore.util.Utils
import org.smartregister.fhircore.viewmodel.QuestionnaireViewModel
import java.util.*

const val PATIENT_ID = "patient_id"
const val DOSE_NUMBER = "dose_number"
const val INITIAL_DOSE = "initial_dose"

class RecordVaccineActivity : MultiLanguageBaseActivity() {

  private val viewModel: QuestionnaireViewModel by viewModels()
  private lateinit var fragment: QuestionnaireFragment

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_record_vaccine)

    supportActionBar!!.apply {
      title = intent.getStringExtra(QuestionnaireActivity.QUESTIONNAIRE_TITLE_KEY)
      setDisplayHomeAsUpEnabled(true)
    }

    // Only add the fragment once, when the activity is first created.
    if (savedInstanceState == null) {
      fragment = QuestionnaireFragment()
      fragment.arguments =
        bundleOf(QuestionnaireFragment.BUNDLE_KEY_QUESTIONNAIRE to viewModel.questionnaire)

      supportFragmentManager.commit {
        add(R.id.container, fragment, QuestionnaireActivity.QUESTIONNAIRE_FRAGMENT_TAG)
      }
    }

    findViewById<Button>(R.id.btn_record_vaccine).setOnClickListener {
      val questionnaireResponse = fragment.getQuestionnaireResponse()

      val iParser: IParser = FhirContext.forR4().newJsonParser()
      val questionnaire =
        iParser.parseResource(
          org.hl7.fhir.r4.model.Questionnaire::class.java,
          viewModel.questionnaire
        ) as
          Questionnaire

      // TODO Replace manual mapping with resource mapper
      try {
        val immunization =
          ResourceMapper.extract(questionnaire, questionnaireResponse).entry[0].resource as
            Immunization
        immunization.id = UUID.randomUUID().toString().toLowerCase()
        immunization.recorded = Date()
        immunization.status = Immunization.ImmunizationStatus.COMPLETED
        immunization.vaccineCode =
          CodeableConcept().apply {
            this.text = questionnaireResponse.item[0].answer[0].valueCoding.code
            this.coding = listOf(questionnaireResponse.item[0].answer[0].valueCoding)
          }
        immunization.occurrence = DateTimeType.today()
        immunization.patient =
          Reference().apply { this.reference = "Patient/" + intent?.getStringExtra(PATIENT_ID) }

        immunization.protocolApplied =
          listOf(
            Immunization.ImmunizationProtocolAppliedComponent().apply {
              val currentDoseNumber = intent?.getIntExtra(DOSE_NUMBER, 0)
              if (currentDoseNumber != null) {
                this.doseNumber = PositiveIntType(currentDoseNumber + 1)
              }
            }
          )
        showVaccineRecordDialog(immunization)
      } catch (e: IndexOutOfBoundsException) {
        Toast.makeText(this, R.string.please_select_vaccine, Toast.LENGTH_SHORT).show()
      }
    }
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      android.R.id.home -> {
        finish()
        true
      } // do whatever
      else -> super.onOptionsItemSelected(item)
    }
  }

  private fun showVaccineRecordDialog(immunization: Immunization) {

    val builder = AlertDialog.Builder(this)
    val doseNumber = immunization.protocolApplied.first().doseNumberPositiveIntType.value
    var msgText = ""
    var titleText = ""
    val vaccineDate = immunization.occurrenceDateTimeType.toHumanDisplay()
    val nextVaccineDate = Utils.addDays(vaccineDate, 28)
    val currentDose = immunization.vaccineCode.coding.first().code
    val initialDose = intent?.getStringExtra(INITIAL_DOSE)
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
      titleText = "Initially  received $initialDose"
    }

    // set title for alert dialog
    builder.setTitle(titleText)

    // set message for alert dialog
    builder.setMessage(msgText)

    // performing negative action
    builder.setNegativeButton(R.string.done) { dialogInterface, _ ->
      dialogInterface.dismiss()
      if (isSameAsFirstDose) {
        viewModel.saveResource(immunization)
        finish()
      }
    }
    // Create the AlertDialog
    val alertDialog: AlertDialog = builder.create()
    // Set other dialog properties
    alertDialog.setCancelable(false)
    alertDialog.show()
  }

  companion object {
    fun getExtraBundles (title: String, patientId: String) : Bundle {
      return bundleOf (
        Pair(QuestionnaireActivity.QUESTIONNAIRE_TITLE_KEY, title),
        Pair(QuestionnaireActivity.QUESTIONNAIRE_FILE_PATH_KEY, "record-vaccine.json"),
        Pair(PATIENT_ID, patientId)
      )
    }
  }
}
