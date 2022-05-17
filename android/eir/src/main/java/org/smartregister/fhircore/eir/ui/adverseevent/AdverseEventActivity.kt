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

package org.smartregister.fhircore.eir.ui.adverseevent

import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import org.hl7.fhir.r4.model.Immunization
import org.smartregister.fhircore.eir.R
import org.smartregister.fhircore.eir.databinding.ActivityAdverseEventBinding
import org.smartregister.fhircore.eir.ui.patient.details.AdverseEventItem
import org.smartregister.fhircore.eir.ui.patient.details.toImmunizationAdverseEventItem
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.util.extension.hide
import org.smartregister.fhircore.engine.util.extension.show

@AndroidEntryPoint
class AdverseEventActivity : BaseMultiLanguageActivity() {

  private lateinit var patientId: String
  private val adverseEventViewModel: AdverseEventViewModel by viewModels()
  private val adverseEventAdapter = MainAdverseEventAdapter()
  private lateinit var activityAdverseEventBinding: ActivityAdverseEventBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    activityAdverseEventBinding =
      DataBindingUtil.setContentView(this, R.layout.activity_adverse_event)
    setSupportActionBar(activityAdverseEventBinding.adverseEventsToolbar)

    patientId = intent.extras?.getString(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY) ?: ""

    setupViews()

    adverseEventViewModel
      .getPatientImmunizations(patientId = patientId)
      .observe(this@AdverseEventActivity, this::handleImmunizations)

    activityAdverseEventBinding.adverseEventsToolbar.setNavigationOnClickListener {
      onBackPressed()
    }
  }

  private fun setupViews() {
    activityAdverseEventBinding.adverseEventListView.apply {
      adapter = adverseEventAdapter
      layoutManager = LinearLayoutManager(this@AdverseEventActivity)
    }
  }

  private fun handleImmunizations(immunizations: List<Immunization>) {
    when {
      immunizations.isEmpty() -> {
        activityAdverseEventBinding.adverseEventListView.hide()
        activityAdverseEventBinding.noAdverseEventTextView.show()
      }
      else -> {
        adverseEventViewModel.getAdverseEvents(immunizations).observe(this@AdverseEventActivity) {
          immunizationAdverseEvents ->
          immunizationAdverseEvents.filter { it.second.isNotEmpty() }.run {
            if (this.isEmpty()) {
              activityAdverseEventBinding.adverseEventListView.hide()
              activityAdverseEventBinding.noAdverseEventTextView.show()
            } else {
              activityAdverseEventBinding.noAdverseEventTextView.hide()
              activityAdverseEventBinding.adverseEventListView.show()
              populateAdverseEventsList(immunizations, immunizationAdverseEvents)
            }
          }
        }
      }
    }
  }

  private fun populateAdverseEventsList(
    immunizations: List<Immunization>,
    immunizationAdverseEvents: List<Pair<String, List<AdverseEventItem>>>
  ) {
    adverseEventAdapter.submitList(
      immunizations.toImmunizationAdverseEventItem(
        this@AdverseEventActivity,
        immunizationAdverseEvents
      )
    )
  }

  companion object {
    fun requiredIntentArgs(clientIdentifier: String) =
      bundleOf(Pair(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY, clientIdentifier))
  }
}
