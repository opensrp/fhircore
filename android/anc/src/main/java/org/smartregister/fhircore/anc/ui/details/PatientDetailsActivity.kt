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
import android.app.DatePickerDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.os.bundleOf
import androidx.core.view.get
import androidx.core.view.setPadding
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.fhir.FhirEngine
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.item_services.view.imageView_calender
import kotlinx.coroutines.launch
import org.smartregister.fhircore.anc.AncApplication
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.data.model.PatientDetailItem
import org.smartregister.fhircore.anc.data.patient.DeletionReason
import org.smartregister.fhircore.anc.data.patient.PatientRepository
import org.smartregister.fhircore.anc.databinding.ActivityNonAncDetailsBinding
import org.smartregister.fhircore.anc.ui.anccare.details.AncDetailsViewModel
import org.smartregister.fhircore.anc.ui.anccare.encounters.EncounterListActivity
import org.smartregister.fhircore.anc.ui.anccare.register.AncItemMapper
import org.smartregister.fhircore.anc.ui.details.adapter.ViewPagerAdapter
import org.smartregister.fhircore.anc.ui.details.bmicompute.BmiQuestionnaireActivity
import org.smartregister.fhircore.anc.ui.details.form.FormConfig
import org.smartregister.fhircore.anc.util.startAncEnrollment
import org.smartregister.fhircore.engine.ui.base.AlertDialogue
import org.smartregister.fhircore.engine.ui.base.AlertDialogue.getSingleChoiceSelectedText
import org.smartregister.fhircore.engine.ui.base.AlertDialogue.showProgressAlert
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.extension.createFactory
import org.smartregister.fhircore.engine.util.extension.hide
import timber.log.Timber
import java.util.Date

class PatientDetailsActivity : BaseMultiLanguageActivity() {

  private lateinit var adapter: ViewPagerAdapter
  private lateinit var patientId: String
  private var isPregnant: Boolean = false
  private var isMale: Boolean = false
  private lateinit var loadProgress: AlertDialog
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
    loadProgress = showProgressAlert(this@PatientDetailsActivity, R.string.loading)

    fhirEngine = AncApplication.getContext().fhirEngine

    patientId = intent.extras?.getString(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY) ?: ""

    patientRepository = PatientRepository((application as AncApplication).fhirEngine, AncItemMapper)

    ancDetailsViewModel =
      ViewModelProvider(
        this,
        AncDetailsViewModel(patientRepository, patientId = patientId).createFactory()
      )[AncDetailsViewModel::class.java]

    activityAncDetailsBinding.patientDetailsToolbar.setNavigationOnClickListener { onBackPressed() }
  }

  override fun onResume() {
    super.onResume()
    activityAncDetailsBinding.txtViewPatientId.text = patientId

    loadProgress.show()

    ancDetailsViewModel
      .fetchDemographics()
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
              patientRepository.markDeceased(patientId, it)
              finish()
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
            val selection = (it as AlertDialog).getSingleChoiceSelectedText()
            if (selection?.isNotBlank() == true)
              lifecycleScope.launch {
                patientRepository.deletePatient(
                  patientId,
                  DeletionReason.values().single { getString(it.label) == selection }
                )
                it.dismiss()
                finish()
              }
          },
          R.string.remove_this_person_button_title,
          DeletionReason.values().map { getString(it.label) }
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
      val patientId =
        listOf(this.patientDetailsHead.demographics, this.patientDetails.patientIdentifier)
          .joinToString(separator = " ID: ")
      activityAncDetailsBinding.txtViewPatientDetails.text = patientDetails
      activityAncDetailsBinding.txtViewPatientId.text = patientId
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
        loadProgress.hide()
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
        loadProgress.hide()
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
