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
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.google.android.fhir.FhirEngine
import kotlinx.android.synthetic.main.activity_register_list.base_register_toolbar
import kotlinx.android.synthetic.main.activity_register_list.btn_register_new_client
import kotlinx.android.synthetic.main.activity_register_list.list_pager
import kotlinx.android.synthetic.main.toolbar_base_register.edit_text_search
import org.smartregister.fhircore.FhirApplication
import org.smartregister.fhircore.R
import org.smartregister.fhircore.activity.core.BaseRegisterActivity
import org.smartregister.fhircore.fragment.PatientDetailFragment
import org.smartregister.fhircore.fragment.PatientListFragment
import org.smartregister.fhircore.model.BaseRegister
import org.smartregister.fhircore.viewmodel.PatientListViewModel
import org.smartregister.fhircore.viewmodel.PatientListViewModelFactory
import timber.log.Timber

class CovaxListActivity : BaseRegisterActivity() {
  lateinit var listViewModel: PatientListViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setToolbarItemText(R.id.tv_clients_list_title, base_register_toolbar, R.string.client_list_title_covax)

    listViewModel =
      ViewModelProvider(
        this,
        PatientListViewModelFactory(application, FhirApplication.fhirEngine(baseContext))
      )
        .get(PatientListViewModel::class.java)
  }

  override fun register(): BaseRegister {
    return BaseRegister(
      context = this,
      contentLayoutId = R.layout.activity_register_list,
      listFragment = PatientListFragment("800"),
      viewPagerId = R.id.list_pager,
      newRegistrationViewId = R.id.btn_register_new_client,
      newRegistrationQuestionnaireIdentifier = "754",
      newRegistrationQuestionnaireTitle = getString(R.string.add_client),
      searchBoxId = R.id.edit_text_search,
      barcodeScannerViewId = R.id.layout_scan_barcode
    )
  }

  override fun onBarcodeResult(barcode: String) {
    Timber.i("Read barcode $barcode")
    listViewModel
      .isPatientExists(barcode)
      .observe(
        this,
        {
          if (it.isSuccess) {
            launchPatientDetailActivity(barcode)
          } else {
            startRegistrationActivity(barcode)
          }
        }
      )
  }

  private fun launchPatientDetailActivity(patientLogicalId: String) {
    val intent =
      Intent(this, PatientDetailActivity::class.java).apply {
        putExtra(PatientDetailFragment.ARG_ITEM_ID, patientLogicalId)
      }
    this.startActivity(intent)
  }
}
