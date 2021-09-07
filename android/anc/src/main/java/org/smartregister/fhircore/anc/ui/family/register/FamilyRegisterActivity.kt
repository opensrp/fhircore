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
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.fhir.search.Search
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.ui.anccare.register.AncRegisterActivity
import org.smartregister.fhircore.anc.ui.family.FamilyFormConfig
import org.smartregister.fhircore.anc.ui.family.register.form.RegisterFamilyMemberData
import org.smartregister.fhircore.anc.ui.family.register.form.RegisterFamilyMemberResult
import org.smartregister.fhircore.anc.ui.family.register.form.RegisterFamilyResult
import org.smartregister.fhircore.engine.configuration.view.registerViewConfigurationOf
import org.smartregister.fhircore.engine.sdk.PatientExtended
import org.smartregister.fhircore.engine.ui.register.BaseRegisterActivity
import org.smartregister.fhircore.engine.ui.register.model.SideMenuOption
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.FormConfigUtil

class FamilyRegisterActivity : BaseRegisterActivity() {

  val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider

  private lateinit var familyFormConfig: FamilyFormConfig
  private lateinit var registerFragment: FamilyRegisterFragment
  private lateinit var familyMemberRegistration: ActivityResultLauncher<RegisterFamilyMemberData>
  private lateinit var familyRegistration: ActivityResultLauncher<FamilyFormConfig>

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    configureViews(
      registerViewConfigurationOf().apply {
        appTitle = getString(R.string.menu_family)
        newClientButtonText = getString(R.string.family_register_button_title)
        showScanQRCode = false
      }
    )

    familyRegistration =
      registerForActivityResult(RegisterFamilyResult()) {
        it?.run { handleRegisterFamilyResult(it) }
      }

    familyMemberRegistration =
      registerForActivityResult(RegisterFamilyMemberResult()) {
        it?.run { handleRegisterFamilyResult(it) }
      }
  }

  override fun sideMenuOptions(): List<SideMenuOption> = listOf(
    SideMenuOption(
      itemId = R.id.menu_item_family,
      titleResource = R.string.menu_family,
      iconResource = ContextCompat.getDrawable(this, R.drawable.ic_hamburger)!!,
      opensMainRegister = true,
      searchFilterLambda = { search -> search.filter(PatientExtended.TAG){value = "Family"} }
  ),
    SideMenuOption(
      itemId = R.id.menu_item_anc,
      titleResource = R.string.menu_anc,
      iconResource = ContextCompat.getDrawable(this, R.drawable.ic_baby_mother)!!,
      opensMainRegister = false
    )
  )

  override fun onSideMenuOptionSelected(item: MenuItem): Boolean {
    when(item.itemId){
      R.id.menu_item_family -> startActivity(Intent(this, FamilyRegisterActivity::class.java))
      R.id.menu_item_anc -> startActivity(Intent(this, AncRegisterActivity::class.java))
    }
    return true
  }

  override fun registerClient() {
    lifecycleScope.launch {
      familyFormConfig =
        withContext(dispatcherProvider.io()) {
          FormConfigUtil.loadConfig(
            FamilyFormConfig.FAMILY_DETAIL_VIEW_CONFIG_ID,
            this@FamilyRegisterActivity
          )
        }

      familyRegistration.launch(familyFormConfig)
    }
  }

  override fun supportedFragments(): List<Fragment> {
    registerFragment = FamilyRegisterFragment()
    return listOf(registerFragment)
  }

  private fun handleRegisterFamilyResult(headId: String) {
    lifecycleScope.launch { registerFragment.familyRepository.enrollIntoAnc(headId) }

    AlertDialog.Builder(this)
      .setMessage(R.string.family_register_message_alert)
      .setCancelable(false)
      .setNegativeButton(R.string.family_register_cancel_title) { dialogInterface, _ ->
        dialogInterface.dismiss()
        reloadList()
      }
      .setPositiveButton(R.string.family_register_ok_title) { dialogInterface, _ ->
        dialogInterface.dismiss()
        registerMember(headId)
      }
      .show()
  }

  fun reloadList() {
    startActivity(Intent(this, FamilyRegisterActivity::class.java))
  }

  fun registerMember(headId: String) {
    familyMemberRegistration.launch(RegisterFamilyMemberData(headId, familyFormConfig))
  }
}
