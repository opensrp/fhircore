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

import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.commit
import com.google.android.fhir.datacapture.QuestionnaireFragment
import org.smartregister.fhircore.R
import org.smartregister.fhircore.util.SharedPreferencesHelper
import org.smartregister.fhircore.viewmodel.QuestionnaireViewModel

const val USER_ID = "user_id"

class RecordVaccineActivity : MultiLanguageBaseActivity() {

  private val viewModel: QuestionnaireViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_record_vaccine)

    supportActionBar!!.apply {
      title = intent.getStringExtra(QuestionnaireActivity.QUESTIONNAIRE_TITLE_KEY)
      setDisplayHomeAsUpEnabled(true)
    }

    // Only add the fragment once, when the activity is first created.
    if (savedInstanceState == null) {
      val fragment = QuestionnaireFragment()
      fragment.arguments =
        bundleOf(QuestionnaireFragment.BUNDLE_KEY_QUESTIONNAIRE to viewModel.questionnaire)

      supportFragmentManager.commit {
        add(R.id.container, fragment, QuestionnaireActivity.QUESTIONNAIRE_FRAGMENT_TAG)
      }
    }

    findViewById<Button>(R.id.btn_record_vaccine).setOnClickListener {
      val questionnaireFragment =
        supportFragmentManager.findFragmentByTag(
          QuestionnaireActivity.QUESTIONNAIRE_FRAGMENT_TAG
        ) as
          QuestionnaireFragment
      val vaccineSelected = questionnaireFragment.getQuestionnaireResponse()
      try {
        showVaccineRecordDialog(vaccineSelected.item[0].answer[0].valueCoding.code)
      } catch (e: IndexOutOfBoundsException) {
        Toast.makeText(this, "Please Select Vaccine", Toast.LENGTH_SHORT).show()
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

  private fun showVaccineRecordDialog(vaccineName: String) {
    val userId = intent?.getStringExtra(USER_ID)
    if (userId != null) {
      SharedPreferencesHelper.write(userId, vaccineName)
    }

    val builder = AlertDialog.Builder(this)
    // set title for alert dialog
    builder.setTitle("$vaccineName 1st dose recorded")
    // set message for alert dialog
    builder.setMessage("Second dose due at 27-04-2021")

    // performing negative action
    builder.setNegativeButton("Done") { dialogInterface, _ ->
      dialogInterface.dismiss()
      finish()
    }
    // Create the AlertDialog
    val alertDialog: AlertDialog = builder.create()
    // Set other dialog properties
    alertDialog.setCancelable(false)
    alertDialog.show()
  }
}
