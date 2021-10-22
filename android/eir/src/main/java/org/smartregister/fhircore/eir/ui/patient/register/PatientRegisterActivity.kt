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

import android.os.Bundle
import android.view.MenuItem
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import org.smartregister.fhircore.eir.R
import org.smartregister.fhircore.engine.configuration.view.registerViewConfigurationOf
import org.smartregister.fhircore.engine.ui.register.BaseRegisterActivity
import org.smartregister.fhircore.engine.ui.register.model.SideMenuOption

class PatientRegisterActivity : BaseRegisterActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    configureViews(
      registerViewConfigurationOf(
        appTitle = getString(R.string.covax_app),
        registrationForm = "patient-registration",
        showSideMenu = true
      )
    )
  }

  override fun sideMenuOptions(): List<SideMenuOption> =
    listOf(
      SideMenuOption(
        itemId = R.id.menu_item_covax,
        titleResource = R.string.client_list_title_covax,
        iconResource = ContextCompat.getDrawable(this, R.drawable.ic_baby_mother)!!,
      )
    )

  override fun mainFragmentTag() = PatientRegisterFragment.TAG

  override fun onMenuOptionSelected(item: MenuItem): Boolean {
    return true
  }

  override fun supportedFragments(): Map<String, Fragment> =
    mapOf(Pair(PatientRegisterFragment.TAG, PatientRegisterFragment()))
}
