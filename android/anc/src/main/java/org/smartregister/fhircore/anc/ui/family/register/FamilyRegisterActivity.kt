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

package org.smartregister.fhircore.anc.ui.family.register

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import kotlinx.coroutines.runBlocking
import org.smartregister.fhircore.anc.AncApplication
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.data.anc.AncPatientRepository
import org.smartregister.fhircore.anc.data.family.FamilyRepository
import org.smartregister.fhircore.anc.ui.anccare.register.AncItemMapper
import org.smartregister.fhircore.anc.ui.anccare.register.AncRegisterActivity
import org.smartregister.fhircore.anc.ui.family.form.FamilyFormConstants
import org.smartregister.fhircore.anc.util.getFamilyQuestionnaireIntent
import org.smartregister.fhircore.engine.configuration.view.registerViewConfigurationOf
import org.smartregister.fhircore.engine.ui.register.BaseRegisterActivity
import org.smartregister.fhircore.engine.ui.register.model.SideMenuOption

class FamilyRegisterActivity : BaseRegisterActivity() {
  private lateinit var familyRepository: FamilyRepository
  private lateinit var ancPatientRepository: AncPatientRepository

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    configureViews(
      registerViewConfigurationOf(
        showScanQRCode = false,
        appTitle = getString(R.string.family_register_title),
        newClientButtonText = getString(R.string.add_family),
        showSideMenu = false
      )
    )

    familyRepository =
      FamilyRepository((application as AncApplication).fhirEngine, FamilyItemMapper)

    ancPatientRepository =
      AncPatientRepository((application as AncApplication).fhirEngine, AncItemMapper)
  }

  override fun sideMenuOptions(): List<SideMenuOption> =
    listOf(
      SideMenuOption(
        itemId = R.id.menu_item_family,
        titleResource = R.string.family_register_title,
        iconResource = ContextCompat.getDrawable(this, R.drawable.ic_calender)!!,
        opensMainRegister = true,
        countMethod = { runBlocking { familyRepository.countAll() } }
      ),
      SideMenuOption(
        itemId = R.id.menu_item_anc,
        titleResource = R.string.anc_register_title,
        iconResource = ContextCompat.getDrawable(this, R.drawable.ic_baby_mother)!!,
        opensMainRegister = false,
        countMethod = { runBlocking { ancPatientRepository.countAll() } }
      )
    )

  override fun onMenuOptionSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.menu_item_family -> startActivity(Intent(this, FamilyRegisterActivity::class.java))
      R.id.menu_item_anc -> startActivity(Intent(this, AncRegisterActivity::class.java))
    }
    return true
  }

  override fun registerClient() {
    startActivity(getFamilyQuestionnaireIntent(form = FamilyFormConstants.FAMILY_REGISTER_FORM))
  }

  override fun supportedFragments(): List<Fragment> = listOf(FamilyRegisterFragment())
}
