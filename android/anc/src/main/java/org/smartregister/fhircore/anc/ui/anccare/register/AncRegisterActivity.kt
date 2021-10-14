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

package org.smartregister.fhircore.anc.ui.anccare.register

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import kotlinx.coroutines.runBlocking
import org.smartregister.fhircore.anc.AncApplication
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.data.anc.AncPatientRepository
import org.smartregister.fhircore.anc.data.family.FamilyRepository
import org.smartregister.fhircore.anc.ui.anccare.details.CarePlanItemMapper
import org.smartregister.fhircore.anc.ui.anccare.details.LastSceneItemMapper
import org.smartregister.fhircore.anc.ui.anccare.details.UpcomingServiceItemMapper
import org.smartregister.fhircore.anc.ui.family.register.FamilyItemMapper
import org.smartregister.fhircore.anc.ui.family.register.FamilyRegisterActivity
import org.smartregister.fhircore.engine.configuration.view.registerViewConfigurationOf
import org.smartregister.fhircore.engine.ui.register.BaseRegisterActivity
import org.smartregister.fhircore.engine.ui.register.model.SideMenuOption
import org.smartregister.fhircore.engine.util.extension.hide

class AncRegisterActivity : BaseRegisterActivity() {
  private lateinit var familyRepository: FamilyRepository
  private lateinit var ancPatientRepository: AncPatientRepository

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    configureViews(
      registerViewConfigurationOf().apply {
        showScanQRCode = false
        showNewClientButton = false
        appTitle = getString(R.string.app_name)
        registrationForm = "anc-patient-registration"
      }
    )

    familyRepository =
      FamilyRepository((application as AncApplication).fhirEngine, FamilyItemMapper)

    ancPatientRepository =
      AncPatientRepository(
        (application as AncApplication).fhirEngine,
        AncItemMapper,
        CarePlanItemMapper,
        UpcomingServiceItemMapper,
        LastSceneItemMapper
      )

    findViewById<Button>(R.id.btn_register_new_client).hide()
  }

  override fun sideMenuOptions(): List<SideMenuOption> =
    listOf(
      SideMenuOption(
        itemId = R.id.menu_item_anc,
        titleResource = R.string.anc_register_title,
        iconResource = ContextCompat.getDrawable(this, R.drawable.ic_baby_mother)!!,
        opensMainRegister = true,
        countMethod = { runBlocking { ancPatientRepository.countAll() } }
      ),
      SideMenuOption(
        itemId = R.id.menu_item_family,
        titleResource = R.string.family_register_title,
        iconResource = ContextCompat.getDrawable(this, R.drawable.ic_calender)!!,
        opensMainRegister = false,
        countMethod = { runBlocking { familyRepository.countAll() } }
      )
    )

  override fun onMenuOptionSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.menu_item_family -> startActivity(Intent(this, FamilyRegisterActivity::class.java))
      R.id.menu_item_anc -> startActivity(Intent(this, AncRegisterActivity::class.java))
    }
    return true
  }

  override fun supportedFragments(): List<Fragment> = listOf(AncRegisterFragment())
}
