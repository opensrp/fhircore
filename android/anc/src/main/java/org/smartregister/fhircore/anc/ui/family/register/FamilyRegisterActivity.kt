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
import org.smartregister.fhircore.anc.AncApplication
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.data.family.FamilyRepository
import org.smartregister.fhircore.anc.data.patient.PatientRepository
import org.smartregister.fhircore.anc.ui.anccare.register.AncItemMapper
import org.smartregister.fhircore.anc.ui.anccare.register.AncRegisterActivity
import org.smartregister.fhircore.anc.ui.family.form.FamilyFormConstants
import org.smartregister.fhircore.anc.util.getFamilyQuestionnaireIntent
import org.smartregister.fhircore.engine.configuration.view.registerViewConfigurationOf
import org.smartregister.fhircore.engine.ui.register.BaseRegisterActivity
import org.smartregister.fhircore.engine.ui.register.model.NavigationMenuOption

class FamilyRegisterActivity : BaseRegisterActivity() {
  private lateinit var familyRepository: FamilyRepository
  private lateinit var patientRepository: PatientRepository

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    configureViews(
      registerViewConfigurationOf(
        showScanQRCode = false,
        appTitle = getString(R.string.family_register_title),
        newClientButtonText = getString(R.string.add_family),
        showSideMenu = false,
        showBottomMenu = true
      )
    )

    familyRepository =
      FamilyRepository((application as AncApplication).fhirEngine, FamilyItemMapper)

    patientRepository = PatientRepository((application as AncApplication).fhirEngine, AncItemMapper)
  }

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

  override fun bottomNavigationMenuOptions(): List<NavigationMenuOption> =
    listOf(
      NavigationMenuOption(
        id = R.id.menu_item_register,
        title = getString(R.string.register),
        iconResource = ContextCompat.getDrawable(this, R.drawable.ic_home)!!
      ),
      NavigationMenuOption(
        id = R.id.menu_item_tasks,
        title = getString(R.string.tasks),
        iconResource = ContextCompat.getDrawable(this, R.drawable.ic_tasks)!!
      ),
      NavigationMenuOption(
        id = R.id.menu_item_reports,
        title = getString(R.string.reports),
        iconResource = ContextCompat.getDrawable(this, R.drawable.ic_reports)!!
      ),
      NavigationMenuOption(
        id = R.id.menu_item_profile,
        title = getString(R.string.profile),
        iconResource = ContextCompat.getDrawable(this, R.drawable.ic_user)!!
      )
    )
}
