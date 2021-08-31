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

package org.smartregister.fhircore.anc.ui.anccare.details

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import org.smartregister.fhircore.anc.form.config.AncFormConfig
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.util.FormConfigUtil

class AncDetailsActivity : BaseMultiLanguageActivity() {

  private lateinit var patientId: String

  private lateinit var ancFormConfig: AncFormConfig

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // setContentView(R.layout.activity_patient_details) // todo use details activity for anc
    // setSupportActionBar(patientDetailsToolbar) // todo add anc details activity toolbar

    if (savedInstanceState == null) {
      ancFormConfig = FormConfigUtil.loadConfig(AncFormConfig.ANC_DETAIL_VIEW_CONFIG_ID, this)

      patientId = intent.extras?.getString(AncFormConfig.ANC_ARG_ITEM_ID) ?: ""
    }
  }

  override fun onCreateOptionsMenu(menu: Menu?): Boolean {
    // menuInflater.inflate(R.menu.profile_menu, menu) // todo use anc menu
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return super.onOptionsItemSelected(item)
  }
}
