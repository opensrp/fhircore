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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import kotlinx.android.synthetic.main.activity_patient_details.*
import org.smartregister.fhircore.R
import org.smartregister.fhircore.activity.core.QuestionnaireActivity
import org.smartregister.fhircore.fragment.PatientDetailsFragment
import org.smartregister.fhircore.model.CovaxDetailView
import org.smartregister.fhircore.util.Utils

class PatientDetailsActivity : AppCompatActivity() {

  private lateinit var patientId: String

  private lateinit var detailView: CovaxDetailView

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_patient_details)
    setSupportActionBar(patientDetailsToolbar)

    if (savedInstanceState == null) {
      detailView =
        Utils.loadConfig(
          CovaxDetailView.COVAX_DETAIL_VIEW_CONFIG_ID,
          CovaxDetailView::class.java,
          this
        )

      patientId = intent.extras?.getString(CovaxDetailView.COVAX_ARG_ITEM_ID) ?: ""
      supportFragmentManager
        .beginTransaction()
        .replace(
          R.id.container,
          PatientDetailsFragment.newInstance(
            bundleOf(Pair(CovaxDetailView.COVAX_ARG_ITEM_ID, patientId))
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
          .putExtras(QuestionnaireActivity.getExtrasBundle(patientId, detailView))
      )
      return true
    }
    return super.onOptionsItemSelected(item)
  }
}
