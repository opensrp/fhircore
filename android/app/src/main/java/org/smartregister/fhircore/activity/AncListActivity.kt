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
import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModelProvider
import kotlinx.android.synthetic.main.activity_register_list.base_register_toolbar
import kotlinx.android.synthetic.main.activity_register_list.btn_register_new_client
import kotlinx.android.synthetic.main.toolbar_base_register.layout_scan_barcode
import org.smartregister.fhircore.FhirApplication
import org.smartregister.fhircore.R
import org.smartregister.fhircore.activity.core.BaseRegisterActivity
import org.smartregister.fhircore.fragment.AncListFragment
import org.smartregister.fhircore.model.AncDetailView
import org.smartregister.fhircore.model.BaseRegister
import org.smartregister.fhircore.util.Utils
import org.smartregister.fhircore.viewmodel.AncListViewModel
import org.smartregister.fhircore.viewmodel.FhirListViewModelFactory

class AncListActivity : BaseRegisterActivity() {
  lateinit var listViewModel: AncListViewModel
  @VisibleForTesting private lateinit var detailView: AncDetailView

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setToolbarItemText(
      R.id.tv_clients_list_title,
      base_register_toolbar,
      R.string.client_list_title_anc
    )

    setNavigationHeaderTitle(detailView.registerTitle, R.id.tv_nav_header)

    btn_register_new_client.visibility = View.INVISIBLE // dont set GONE otherwise VPager not loaded
    layout_scan_barcode.visibility = View.GONE

    listViewModel =
      ViewModelProvider(
          this,
          FhirListViewModelFactory(application, FhirApplication.fhirEngine(baseContext))
        )
        .get(AncListViewModel::class.java)
  }

  override fun buildRegister(): BaseRegister {
    detailView =
      Utils.loadConfig(AncDetailView.ANC_DETAIL_VIEW_CONFIG_ID, AncDetailView::class.java, this)

    return BaseRegister(
      context = this,
      contentLayoutId = R.layout.activity_register_list,
      listFragment = AncListFragment(),
      viewPagerId = R.id.list_pager,
      newRegistrationQuestionnaireIdentifier = detailView.registrationQuestionnaireIdentifier,
      newRegistrationQuestionnaireTitle = detailView.registrationQuestionnaireTitle,
      searchBoxId = R.id.edit_text_search
    )
  }
}
