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

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import androidx.activity.viewModels
import androidx.lifecycle.ViewModelProvider
import org.smartregister.fhircore.FhirApplication
import org.smartregister.fhircore.R
import org.smartregister.fhircore.activity.core.BaseActivity
import org.smartregister.fhircore.activity.core.QuestionnaireActivity
import org.smartregister.fhircore.activity.core.QuestionnaireActivity.Companion.QUESTIONNAIRE_ARG_PATIENT_KEY
import org.smartregister.fhircore.activity.core.QuestionnaireActivity.Companion.QUESTIONNAIRE_PATH_KEY
import org.smartregister.fhircore.activity.core.QuestionnaireActivity.Companion.QUESTIONNAIRE_TITLE_KEY
import org.smartregister.fhircore.fragment.CovaxDetailFragment
import org.smartregister.fhircore.model.CovaxDetailView
import org.smartregister.fhircore.util.Utils.loadConfig
import org.smartregister.fhircore.viewmodel.CovaxListViewModel
import org.smartregister.fhircore.viewmodel.PatientListViewModelFactory
import org.smartregister.fhircore.viewmodel.QuestionnaireViewModel

/** An activity representing a single Patient detail screen. */
class CovaxDetailActivity : BaseActivity() {
  lateinit var viewModel: CovaxListViewModel
  private val questionnaireViewModel: QuestionnaireViewModel by viewModels()

  private lateinit var clientIdentifier: String
  private lateinit var detailView: CovaxDetailView

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Show the Up button in the action bar.
    supportActionBar?.setDisplayHomeAsUpEnabled(true)

    val fhirEngine = FhirApplication.fhirEngine(this)

    viewModel =
      ViewModelProvider(this, PatientListViewModelFactory(application, fhirEngine))
        .get(CovaxListViewModel::class.java)

    clientIdentifier = intent.getStringExtra(CovaxDetailView.COVAX_ARG_ITEM_ID)

    detailView =
      loadConfig(CovaxDetailView.COVAX_DETAIL_VIEW_CONFIG_ID, CovaxDetailView::class.java, this)

    if (savedInstanceState == null) {
      startPatientDetailFragment()
    }

    findViewById<Button>(R.id.btn_record_vaccine).setOnClickListener {
      launchRecordVaccineActivity()
    }
  }

  override fun getContentLayout(): Int {
    return R.layout.activity_covax_client_detail
  }

  override fun toolBarId(): Int? {
    return R.id.detail_toolbar
  }

  private fun launchRecordVaccineActivity() {
    startActivity(
      Intent(this, RecordVaccineActivity::class.java).apply {
        putExtra(CovaxDetailView.COVAX_ARG_ITEM_ID, clientIdentifier)
      }
    )
  }

  override fun onCreateOptionsMenu(menu: Menu?): Boolean {
    menuInflater.inflate(R.menu.profile_menu, menu)
    return true
  }
  override fun onOptionsItemSelected(item: MenuItem) =
    when (item.itemId) {
      R.id.patient_profile_edit -> {
        editRegistration()

        true
      }
      else -> super.onOptionsItemSelected(item)
    }

  private fun startPatientDetailFragment() {
    // Create the detail fragment and add it to the activity
    // using a fragment transaction.
    val fragment =
      CovaxDetailFragment().apply {
        arguments =
          Bundle().apply { putString(CovaxDetailView.COVAX_ARG_ITEM_ID, clientIdentifier) }
      }

    supportFragmentManager.beginTransaction().add(R.id.patient_detail_container, fragment).commit()
  }

  private fun editRegistration() {
    startActivity(
      Intent(this, QuestionnaireActivity::class.java).apply {
        putExtra(QUESTIONNAIRE_TITLE_KEY, detailView.registrationQuestionnaireTitle)
        putExtra(QUESTIONNAIRE_PATH_KEY, detailView.registrationQuestionnaireIdentifier)
        putExtra(QUESTIONNAIRE_ARG_PATIENT_KEY, clientIdentifier)
      }
    )
  }
}
