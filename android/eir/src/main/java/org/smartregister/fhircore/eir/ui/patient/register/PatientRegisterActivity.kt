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
import org.smartregister.fhircore.eir.R
import org.smartregister.fhircore.engine.configuration.view.registerViewConfigurationOf
import org.smartregister.fhircore.engine.ui.model.SideMenuOption
import org.smartregister.fhircore.engine.ui.register.BaseRegisterActivity
import org.smartregister.fhircore.engine.util.extension.showToast

class PatientRegisterActivity : BaseRegisterActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    configureViews(registerViewConfigurationOf().apply { appTitle = getString(R.string.covax_app) })
  }

  override fun sideMenuOptions(): List<SideMenuOption> =
    listOf(
      SideMenuOption(
        itemId = COVAX_MENU_OPTION,
        titleResource = R.string.client_list_title_covax,
        iconResource = ContextCompat.getDrawable(this, R.drawable.ic_baby_mother)!!,
        opensMainRegister = false
      )
    )

  override fun onSideMenuOptionSelected(item: MenuItem): Boolean {
    showToast("Clicked ${item.title}")
    return true
  }

  override fun registerClient() {}

  override fun customEntityCount(sideMenuOption: SideMenuOption): Long {
    return 0
  }

  companion object {
    const val COVAX_MENU_OPTION = 1000
  }
}
