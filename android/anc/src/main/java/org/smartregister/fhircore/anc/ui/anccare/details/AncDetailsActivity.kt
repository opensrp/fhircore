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
import android.view.View
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.databinding.ActivityAncDetailsBinding
import org.smartregister.fhircore.anc.form.config.AncFormConfig
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.util.FormConfigUtil

class AncDetailsActivity : BaseMultiLanguageActivity() {

    private lateinit var patientId: String

    private lateinit var activityAncDetailsBinding: ActivityAncDetailsBinding

    private lateinit var ancFormConfig: AncFormConfig

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityAncDetailsBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_anc_details)
        setSupportActionBar(activityAncDetailsBinding.patientDetailsToolbar)

        if (savedInstanceState == null) {
            ancFormConfig = FormConfigUtil.loadConfig(AncFormConfig.ANC_DETAIL_VIEW_CONFIG_ID, this)

            patientId = intent.extras?.getString(AncFormConfig.ANC_ARG_ITEM_ID) ?: ""

            supportFragmentManager
                .beginTransaction()
                .replace(
                    R.id.container,
                    AncDetailsFragment.newInstance(
                        bundleOf(Pair(AncFormConfig.ANC_ARG_ITEM_ID, patientId))
                    )
                )
                .commitNow()
        }

        activityAncDetailsBinding.patientDetailsToolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.profile_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return super.onOptionsItemSelected(item)
    }
}
