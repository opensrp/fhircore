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

package org.smartregister.fhircore.eir.ui.patient.details

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import dagger.hilt.android.AndroidEntryPoint
import org.smartregister.fhircore.eir.R
import org.smartregister.fhircore.eir.databinding.ActivityPatientDetailsBinding
import org.smartregister.fhircore.eir.ui.adverseevent.AdverseEventActivity
import org.smartregister.fhircore.eir.util.PATIENT_REGISTRATION
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity

@AndroidEntryPoint
class PatientDetailsActivity : BaseMultiLanguageActivity() {

  private lateinit var patientId: String

  private lateinit var patientDetailsActivityBinding: ActivityPatientDetailsBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    patientDetailsActivityBinding =
      DataBindingUtil.setContentView(this, R.layout.activity_patient_details)
    setSupportActionBar(patientDetailsActivityBinding.patientDetailsToolbar)

    patientId = intent.extras?.getString(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY) ?: ""
    supportFragmentManager
      .beginTransaction()
      .replace(
        R.id.container,
        PatientDetailsFragment.newInstance(
          bundleOf(Pair(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY, patientId))
        )
      )
      .commitNow()
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.profile_menu, menu)
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == R.id.patient_profile_edit) {
      startActivity(
        Intent(this, QuestionnaireActivity::class.java)
          .putExtras(
            QuestionnaireActivity.intentArgs(
              clientIdentifier = patientId,
              formName = PATIENT_REGISTRATION
            )
          )
      )
      return true
    } else if (item.itemId == R.id.vaccine_adverse_events) {
      startActivity(
        Intent(this, AdverseEventActivity::class.java)
          .putExtras(AdverseEventActivity.requiredIntentArgs(clientIdentifier = patientId))
      )
      return true
    }
    return super.onOptionsItemSelected(item)
  }

  companion object {
    fun requiredIntentArgs(clientIdentifier: String) =
      bundleOf(
        Pair(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY, clientIdentifier),
      )
  }
}
