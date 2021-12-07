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

package org.smartregister.fhircore.anc.ui.details

import android.app.AlertDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.data.model.PatientDetailItem
import org.smartregister.fhircore.anc.data.patient.DeletionReason
import org.smartregister.fhircore.anc.databinding.ActivityNonAncDetailsBinding
import org.smartregister.fhircore.anc.ui.anccare.details.AncDetailsViewModel
import org.smartregister.fhircore.anc.ui.anccare.encounters.EncounterListActivity
import org.smartregister.fhircore.anc.ui.details.adapter.ViewPagerAdapter
import org.smartregister.fhircore.anc.ui.details.bmicompute.BmiQuestionnaireActivity
import org.smartregister.fhircore.anc.ui.details.form.FormConfig
import org.smartregister.fhircore.anc.util.startAncEnrollment
import org.smartregister.fhircore.engine.ui.base.AlertDialogListItem
import org.smartregister.fhircore.engine.ui.base.AlertDialogue
import org.smartregister.fhircore.engine.ui.base.AlertDialogue.getSingleChoiceSelectedValue
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import java.util.Date

@AndroidEntryPoint
class PatientDetailsActivity : BaseMultiLanguageActivity() {

  private lateinit var adapter: ViewPagerAdapter
  private lateinit var patientId: String
  private var isPregnant: Boolean = false
  private var isMale: Boolean = false
  private var isFamilyHead: Boolean = false

  val ancDetailsViewModel by viewModels<AncDetailsViewModel>()
  private lateinit var activityAncDetailsBinding: ActivityNonAncDetailsBinding

  val details = arrayOf("CARE PLAN", "DETAILS")

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // TODO add progress dialog when loading content
    activityAncDetailsBinding =
      DataBindingUtil.setContentView(this, R.layout.activity_non_anc_details)
    setSupportActionBar(activityAncDetailsBinding.patientDetailsToolbar)

    patientId = intent.extras?.getString(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY) ?: ""

    activityAncDetailsBinding.patientDetailsToolbar.setNavigationOnClickListener { onBackPressed() }
  }

  override fun onResume() {
    super.onResume()
    activityAncDetailsBinding.txtViewPatientId.text = patientId
    ancDetailsViewModel
      .fetchDemographics(patientId)
      .observe(this@PatientDetailsActivity, this::handlePatientDemographics)
  }

  override fun onCreateOptionsMenu(menu: Menu?): Boolean {
    menuInflater.inflate(R.menu.profile_menu, menu)
    return true
  }

  override fun onPrepareOptionsMenu(menu: Menu): Boolean {
    menu.findItem(R.id.anc_enrollment).run { this.isVisible = !isMale && !isPregnant }
    menu.findItem(R.id.pregnancy_outcome).run { this.isVisible = !isMale }

    menu.findItem(R.id.remove_this_person).run {
      highlightItem(this)
      this.isVisible = !isFamilyHead
    }
    menu.findItem(R.id.log_death).run {
      highlightItem(this)
    }
    return super.onPrepareOptionsMenu(menu)
  }

  private fun highlightItem(menuItem: MenuItem){
    val span =
      SpannableString(menuItem.title).apply {
        setSpan(
          ForegroundColorSpan(android.graphics.Color.parseColor("#DD0000")),
          0,
          length,
          android.text.Spannable.SPAN_INCLUSIVE_INCLUSIVE
        )
      }
    menuItem.title = span
  }

  @RequiresApi(Build.VERSION_CODES.N)
  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      R.id.add_vitals -> {
        showAlert()
        true
      }
      R.id.view_past_encounters -> {
        startActivity(
          Intent(this, EncounterListActivity::class.java).apply {
            putExtra(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY, patientId)
          }
        )
        true
      }
      R.id.anc_enrollment -> {
        this.startAncEnrollment(patientId)
        true
      }
      R.id.bmi_widget -> {
        startActivity(
          Intent(this, BmiQuestionnaireActivity::class.java)
            .putExtras(
              QuestionnaireActivity.intentArgs(
                clientIdentifier = patientId,
                formName = FormConfig.FAMILY_PATIENT_BMI_FORM
              )
            )
        )
        true
      }
      R.id.log_death -> {
        AlertDialogue.showDatePrompt(
          context = this,
          max = Date(),
          title = getString(R.string.log_death_confirm_message),
          dangerActionColor = true,
          confirmButtonText = getString(R.string.log_death_button_title),
          confirmButtonListener = {
            lifecycleScope.launch {
              ancDetailsViewModel.markDeceased(patientId, it).observe(
                this@PatientDetailsActivity,
                {
                  if (it) {
                    finish()
                  }
                }
              )
            }
          }
        )
        true
      }
      R.id.remove_this_person -> {
        AlertDialogue.showConfirmAlert(
          this,
          R.string.remove_this_person_confirm_message,
          R.string.remove_this_person_confirm_title,
          {
            val selection = (it as AlertDialog).getSingleChoiceSelectedValue()
            if (selection?.isNotBlank() == true)
              lifecycleScope.launch {
                ancDetailsViewModel.deletePatient(
                  patientId,
                  DeletionReason.values().single { getString(it.label) == selection }
                )
                it.dismiss()
                finish()
              }
          },
          R.string.remove_this_person_button_title,
          DeletionReason.values().map { AlertDialogListItem(it.name, it.name) }.toTypedArray()
        )
        return true
      }
      else -> return super.onOptionsItemSelected(item)
    }
  }

  private fun handlePatientDemographics(patient: PatientDetailItem) {
    with(patient) {
      val patientDetails =
        listOf(this.patientDetails.name, this.patientDetails.gender, this.patientDetails.age)
          .joinToString(separator = ", ")
      val patientAddress =
        if (this.patientDetailsHead.demographics.isNotBlank()) this.patientDetailsHead.demographics
        else this.patientDetails.demographics
      val houseHoldHeadText =
        if (this.patientDetails.isHouseHoldHead) getString(R.string.head_of_household) else ""
      val patientIdText =
        listOf(patientAddress, "ID: ${this.patientDetails.patientIdentifier}", houseHoldHeadText)
          .joinToString(separator = " " + getString(R.string.bullet_character) + " ")
      activityAncDetailsBinding.txtViewPatientDetails.text = patientDetails
      activityAncDetailsBinding.txtViewPatientId.text = patientIdText
      isMale = this.patientDetails.gender == getString(R.string.male)
      isPregnant = this.patientDetails.isPregnant
      isFamilyHead = this.patientDetails.isHouseHoldHead

      if (isMale) {
        adapter =
          ViewPagerAdapter(
            fragmentManager = supportFragmentManager,
            lifecycle = lifecycle,
            isPregnant = false,
            bundleOf =
              bundleOf(Pair(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY, patientId))
          )
        activityAncDetailsBinding.pager.adapter = adapter
        TabLayoutMediator(activityAncDetailsBinding.tablayout, activityAncDetailsBinding.pager) {
            tab,
            position ->
            tab.text = details[position]
          }
          .attach()
      } else {
        adapter =
          ViewPagerAdapter(
            fragmentManager = supportFragmentManager,
            lifecycle = lifecycle,
            isPregnant = isPregnant,
            bundleOf =
              bundleOf(Pair(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY, patientId))
          )
        activityAncDetailsBinding.pager.adapter = adapter
        TabLayoutMediator(activityAncDetailsBinding.tablayout, activityAncDetailsBinding.pager) {
            tab,
            position ->
            tab.text = details[position]
          }
          .attach()
      }
    }
  }

  private fun showAlert() {
    AlertDialog.Builder(this)
      .setMessage(getString(R.string.select_unit))
      .setCancelable(false)
      .setNegativeButton("Metric unit") { dialogInterface, _ ->
        dialogInterface.dismiss()
        openVitalSignsMetric(patientId)
      }
      .setPositiveButton("Standard unit") { dialogInterface, _ ->
        dialogInterface.dismiss()
        openVitalSignsStandard(patientId)
      }
      .show()
  }

  private fun openVitalSignsMetric(patientId: String) {
    startActivity(
      Intent(this, QuestionnaireActivity::class.java)
        .putExtras(
          QuestionnaireActivity.intentArgs(
            clientIdentifier = patientId,
            formName = FormConfig.ANC_VITAL_SIGNS_METRIC
          )
        )
    )
  }

  private fun openVitalSignsStandard(patientId: String) {
    startActivity(
      Intent(this, QuestionnaireActivity::class.java)
        .putExtras(
          QuestionnaireActivity.intentArgs(
            clientIdentifier = patientId,
            formName = FormConfig.ANC_VITAL_SIGNS_STANDARD
          )
        )
    )
  }
}
