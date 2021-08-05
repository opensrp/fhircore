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
import kotlinx.android.synthetic.main.activity_register_list.base_register_toolbar
import kotlinx.android.synthetic.main.activity_register_list.btn_register_new_client
import kotlinx.android.synthetic.main.activity_register_list.list_pager
import kotlinx.android.synthetic.main.toolbar_base_register.edit_text_search
import kotlinx.android.synthetic.main.toolbar_base_register.tv_clients_list_title
import org.smartregister.fhircore.R
import org.smartregister.fhircore.activity.core.BaseRegisterActivity
import org.smartregister.fhircore.fragment.AncListFragment
import org.smartregister.fhircore.fragment.PatientListFragment
import org.smartregister.fhircore.model.BaseRegister

class AncListActivity : BaseRegisterActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setToolbarItemText(R.id.tv_clients_list_title, base_register_toolbar, R.string.client_list_title_anc)
  }

  override fun register(): BaseRegister {
    return BaseRegister(
      context = this,
      contentLayoutId = R.layout.activity_register_list,
      listFragment = AncListFragment(),
      viewPagerId = R.id.list_pager,
      newRegistrationViewId = R.id.btn_register_new_client,
      newRegistrationQuestionnaireIdentifier = "207",
      newRegistrationQuestionnaireTitle = getString(R.string.add_client),
      searchBoxId = R.id.edit_text_search
    )
  }
}
