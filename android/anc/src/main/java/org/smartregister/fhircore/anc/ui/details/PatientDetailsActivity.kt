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
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.data.model.PatientDetailItem
import org.smartregister.fhircore.anc.databinding.ActivityNonAncDetailsBinding
import org.smartregister.fhircore.anc.ui.anccare.details.AncDetailsViewModel
import org.smartregister.fhircore.anc.ui.anccare.encounters.EncounterListActivity
import org.smartregister.fhircore.anc.ui.details.adapter.ViewPagerAdapter
import org.smartregister.fhircore.anc.ui.details.bmicompute.BmiQuestionnaireActivity
import org.smartregister.fhircore.anc.ui.details.form.FormConfig
import org.smartregister.fhircore.anc.util.startAncEnrollment
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity

@AndroidEntryPoint
class PatientDetailsActivity : BaseMultiLanguageActivity() {

  private lateinit var adapter: ViewPagerAdapter
  private lateinit var patientId: String
  private var isPregnant: Boolean = false
  private var isMale: Boolean = false

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

    ancDetailsViewModel.patientId = patientId
    activityAncDetailsBinding.patientDetailsToolbar.setNavigationOnClickListener { onBackPressed() }
  }

  override fun onResume() {
    super.onResume()
    activityAncDetailsBinding.txtViewPatientId.text = patientId
    ancDetailsViewModel
      .fetchDemographics()
      .observe(this@PatientDetailsActivity, this::handlePatientDemographics)
  }

  override fun onCreateOptionsMenu(menu: Menu?): Boolean {
    menuInflater.inflate(R.menu.profile_menu, menu)
    return true
  }

  override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
    val removeThisPerson = menu!!.findItem(R.id.remove_this_person)
    val ancEnrollment = menu!!.findItem(R.id.anc_enrollment)
    val pregnancyOutcome = menu!!.findItem(R.id.pregnancy_outcome)
    if (isMale) pregnancyOutcome.isVisible = false
    ancEnrollment.isVisible = if (isMale) false else !isPregnant
    val title = removeThisPerson.title.toString()
    val spannableString = SpannableString(title)
    with(spannableString) {
      setSpan(
        ForegroundColorSpan(android.graphics.Color.parseColor("#DD0000")),
        0,
        length,
        android.text.Spannable.SPAN_INCLUSIVE_INCLUSIVE
      )
    } // provide whatever color you want here.
    removeThisPerson.title = spannableString
    return super.onPrepareOptionsMenu(menu)
  }

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

      if (isMale) {
        adapter =
          ViewPagerAdapter(
            supportFragmentManager,
            lifecycle,
            false,
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
            supportFragmentManager,
            lifecycle,
            isPregnant = isPregnant,
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
