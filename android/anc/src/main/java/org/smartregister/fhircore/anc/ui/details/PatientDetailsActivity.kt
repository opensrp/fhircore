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

import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.google.android.fhir.FhirEngine
import com.google.android.material.tabs.TabLayoutMediator
import org.smartregister.fhircore.anc.AncApplication
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.data.model.AncPatientDetailItem
import org.smartregister.fhircore.anc.data.patient.PatientRepository
import org.smartregister.fhircore.anc.databinding.ActivityNonAncDetailsBinding
import org.smartregister.fhircore.anc.ui.anccare.details.AncDetailsViewModel
import org.smartregister.fhircore.anc.ui.anccare.register.AncItemMapper
import org.smartregister.fhircore.anc.ui.details.adapter.ViewPagerAdapter
import org.smartregister.fhircore.anc.ui.details.form.FormConfig
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.util.extension.createFactory

class PatientDetailsActivity : BaseMultiLanguageActivity() {

  private lateinit var adapter: ViewPagerAdapter
  private lateinit var patientId: String

  private lateinit var fhirEngine: FhirEngine

  lateinit var ancDetailsViewModel: AncDetailsViewModel

  private lateinit var patientRepository: PatientRepository

  private lateinit var activityAncDetailsBinding: ActivityNonAncDetailsBinding

  val details = arrayOf("CARE PLAN", "DETAILS")

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    activityAncDetailsBinding =
      DataBindingUtil.setContentView(this, R.layout.activity_non_anc_details)
    setSupportActionBar(activityAncDetailsBinding.patientDetailsToolbar)

    fhirEngine = AncApplication.getContext().fhirEngine

    patientId = intent.extras?.getString(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY) ?: ""

    patientRepository = PatientRepository((application as AncApplication).fhirEngine, AncItemMapper)

    ancDetailsViewModel =
      ViewModelProvider(
        this,
        AncDetailsViewModel(patientRepository, patientId = patientId).createFactory()
      )[AncDetailsViewModel::class.java]

    activityAncDetailsBinding.txtViewPatientId.text = patientId

    ancDetailsViewModel
      .fetchDemographics()
      .observe(this@PatientDetailsActivity, this::handlePatientDemographics)

    adapter =
      ViewPagerAdapter(
        supportFragmentManager,
        lifecycle,
        bundleOf(Pair(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY, patientId))
      )
    activityAncDetailsBinding.pager.adapter = adapter

    TabLayoutMediator(activityAncDetailsBinding.tablayout, activityAncDetailsBinding.pager) {
        tab,
        position ->
        tab.text = details[position]
      }
      .attach()

    activityAncDetailsBinding.patientDetailsToolbar.setNavigationOnClickListener { onBackPressed() }
  }

  override fun onCreateOptionsMenu(menu: Menu?): Boolean {
    menuInflater.inflate(R.menu.profile_menu_madx, menu)
    return true
  }

  override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
    val removeThisPerson = menu!!.findItem(R.id.remove_this_person)

    val title = removeThisPerson.title.toString()
    val s = SpannableString(title)
    with(s) {
      setSpan(
        ForegroundColorSpan(android.graphics.Color.parseColor("#DD0000")),
        0,
        length,
        android.text.Spannable.SPAN_INCLUSIVE_INCLUSIVE
      )
    } // provide whatever color you want here.
    removeThisPerson.title = s
    return super.onPrepareOptionsMenu(menu)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == R.id.add_vitals) {
      showAlert()
      return true
    }
    return super.onOptionsItemSelected(item)
  }

  private fun handlePatientDemographics(patient: AncPatientDetailItem) {
    with(patient) {
      val patientDetails =
        listOf(this.patientDetails.name, this.patientDetails.gender, this.patientDetails.age)
          .joinToString(separator = ", ")
      val patientId =
        listOf(this.patientDetailsHead.demographics, this.patientDetails.patientIdentifier)
          .joinToString(separator = " ID: ")
      activityAncDetailsBinding.txtViewPatientDetails.text = patientDetails
      activityAncDetailsBinding.txtViewPatientId.text = patientId
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
            form = FormConfig.ANC_VITAL_SIGNS_METRIC
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
            form = FormConfig.ANC_VITAL_SIGNS_STANDARD
          )
        )
    )
  }
}
