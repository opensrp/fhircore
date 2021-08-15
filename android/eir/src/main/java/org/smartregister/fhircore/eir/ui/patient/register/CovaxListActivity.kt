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

package org.smartregister.fhircore.eir.ui.patient.register

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import kotlinx.android.synthetic.main.activity_register_list.base_register_toolbar
import org.smartregister.fhircore.eir.EirApplication
import org.smartregister.fhircore.eir.R
import org.smartregister.fhircore.eir.ui.base.BaseRegisterActivity
import org.smartregister.fhircore.eir.ui.base.model.BaseRegister
import org.smartregister.fhircore.eir.ui.patient.details.PatientDetailsActivity
import org.smartregister.fhircore.eir.ui.patient.details.PatientDetailsFormConfig
import org.smartregister.fhircore.eir.util.Utils.loadConfig

class CovaxListActivity : BaseRegisterActivity() {
  lateinit var listViewModel: CovaxListViewModel

  private lateinit var detailView: PatientDetailsFormConfig

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setToolbarItemText(
      R.id.tv_clients_list_title,
      base_register_toolbar,
      R.string.client_list_title_covax
    )

    setNavigationHeaderTitle(detailView.registerTitle, R.id.tv_nav_header)

    listViewModel =
      ViewModelProvider(
          this,
          PatientListViewModelFactory(application, EirApplication.fhirEngine(baseContext))
        )
        .get(CovaxListViewModel::class.java)

    setupBarcodeScanner()
  }

  override fun buildRegister(): BaseRegister {
    detailView =
      loadConfig(
        PatientDetailsFormConfig.COVAX_DETAIL_VIEW_CONFIG_ID,
        PatientDetailsFormConfig::class.java,
        this
      )

    return BaseRegister(
      context = this,
      contentLayoutId = R.layout.activity_register_list,
      listFragment = CovaxListFragment(),
      viewPagerId = R.id.list_pager,
      newRegistrationViewId = R.id.btn_register_new_client,
      newRegistrationQuestionnaireIdentifier = detailView.registrationQuestionnaireIdentifier,
      newRegistrationQuestionnaireTitle = detailView.registrationQuestionnaireTitle,
      searchBoxId = R.id.edit_text_search
    )
  }

  private fun setupBarcodeScanner() {
    initBarcodeScanner(R.id.layout_scan_barcode) { barcode, _ ->
      listViewModel
        .isPatientExists(barcode)
        .observe(
          this,
          {
            if (it.isSuccess) {
              launchDetailActivity(barcode)
            } else {
              startRegistrationActivity(barcode)
            }
          }
        )
    }
  }

  fun launchDetailActivity(patientLogicalId: String) {
    val intent =
      Intent(this, PatientDetailsActivity::class.java).apply {
        putExtra(PatientDetailsFormConfig.COVAX_ARG_ITEM_ID, patientLogicalId)
      }
    this.startActivity(intent)
  }
}
