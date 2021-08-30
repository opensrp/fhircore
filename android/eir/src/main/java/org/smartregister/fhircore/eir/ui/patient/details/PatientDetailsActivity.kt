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
import kotlinx.android.synthetic.main.activity_patient_details.patientDetailsToolbar
import org.smartregister.fhircore.eir.R
import org.smartregister.fhircore.eir.form.config.QuestionnaireFormConfig
import org.smartregister.fhircore.eir.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.util.FormConfigUtil

class PatientDetailsActivity : BaseMultiLanguageActivity() {

  private lateinit var patientId: String

  private lateinit var questionnaireFormConfig: QuestionnaireFormConfig

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_patient_details)
    setSupportActionBar(patientDetailsToolbar)

    if (savedInstanceState == null) {
      questionnaireFormConfig =
        FormConfigUtil.loadConfig(QuestionnaireFormConfig.COVAX_DETAIL_VIEW_CONFIG_ID, this)

      patientId = intent.extras?.getString(QuestionnaireFormConfig.COVAX_ARG_ITEM_ID) ?: ""
      supportFragmentManager
        .beginTransaction()
        .replace(
          R.id.container,
          PatientDetailsFragment.newInstance(
            bundleOf(Pair(QuestionnaireFormConfig.COVAX_ARG_ITEM_ID, patientId))
          )
        )
        .commitNow()
    }
  }

  override fun onCreateOptionsMenu(menu: Menu?): Boolean {
    menuInflater.inflate(R.menu.profile_menu, menu)
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == R.id.patient_profile_edit) {
      startActivity(
        Intent(this, QuestionnaireActivity::class.java)
          .putExtras(QuestionnaireActivity.getExtrasBundle(patientId, questionnaireFormConfig))
      )
      return true
    }
    return super.onOptionsItemSelected(item)
  }
}
