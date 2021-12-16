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
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.data.patient.PatientRepository
import org.smartregister.fhircore.anc.ui.anccare.register.AncRegisterFragment
import org.smartregister.fhircore.anc.ui.report.ReportHomeActivity
import org.smartregister.fhircore.anc.util.AncConfigClassification
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.view.RegisterViewConfiguration
import org.smartregister.fhircore.engine.ui.register.BaseRegisterActivity
import org.smartregister.fhircore.engine.ui.register.model.NavigationMenuOption
import org.smartregister.fhircore.engine.ui.register.model.RegisterItem
import org.smartregister.fhircore.engine.ui.register.model.SideMenuOption
import org.smartregister.fhircore.engine.ui.userprofile.UserProfileFragment

@AndroidEntryPoint
class FamilyRegisterActivity : BaseRegisterActivity() {

  @Inject lateinit var patientRepository: PatientRepository

  @Inject lateinit var configurationRegistry: ConfigurationRegistry

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val registerViewConfiguration =
      configurationRegistry.retrieveConfiguration<RegisterViewConfiguration>(
        configClassification = AncConfigClassification.PATIENT_REGISTER,
      )
    configureViews(registerViewConfiguration)
  }

  override fun supportedFragments(): Map<String, Fragment> =
    mapOf(
      Pair(FamilyRegisterFragment.TAG, FamilyRegisterFragment()),
      Pair(AncRegisterFragment.TAG, AncRegisterFragment()),
      Pair(UserProfileFragment.TAG, UserProfileFragment())
    )

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

  override fun onNavigationOptionItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.menu_item_profile ->
        switchFragment(
          tag = UserProfileFragment.TAG,
          isRegisterFragment = false,
          toolbarTitle = getString(R.string.profile)
        )
      R.id.menu_item_register, R.id.menu_item_families, R.id.menu_item_family_planning_clients ->
        switchFragment(mainFragmentTag())
      R.id.menu_item_reports -> navigateToReports()
      R.id.menu_item_anc_clients -> switchFragment(tag = AncRegisterFragment.TAG)
    }
    return true
  }

  override fun sideMenuOptions(): List<SideMenuOption> =
    listOf(
      SideMenuOption(
        itemId = R.id.menu_item_families,
        titleResource = R.string.households,
        iconResource = ContextCompat.getDrawable(this, R.drawable.ic_households)!!,
      ),
      SideMenuOption(
        itemId = R.id.menu_item_anc_clients,
        titleResource = R.string.pregnant_clients,
        iconResource = ContextCompat.getDrawable(this, R.drawable.ic_pregnant_clients)!!,
      ),
      SideMenuOption(
        itemId = R.id.menu_item_post_natal_clients,
        titleResource = R.string.post_natal_clients,
        iconResource = ContextCompat.getDrawable(this, R.drawable.ic_post_natal_client)!!,
      ),
      SideMenuOption(
        itemId = R.id.menu_item_child_clients,
        titleResource = R.string.child_clients,
        iconResource = ContextCompat.getDrawable(this, R.drawable.ic_family_clients)!!,
      ),
      SideMenuOption(
        itemId = R.id.menu_item_family_planning_clients,
        titleResource = R.string.family_planning_clients,
        iconResource = ContextCompat.getDrawable(this, R.drawable.ic_family_clients)!!,
      ),
      SideMenuOption(
        itemId = R.id.menu_item_profile,
        titleResource = R.string.profile,
        iconResource = ContextCompat.getDrawable(this, R.drawable.ic_user)!!
      )
    )

  fun navigateToReports() {
    val intent = Intent(this, ReportHomeActivity::class.java)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
  }

  override fun registersList() =
    listOf(
      RegisterItem(
        uniqueTag = FamilyRegisterFragment.TAG,
        title = getString(R.string.families),
        isSelected = true
      ),
      RegisterItem(
        uniqueTag = AncRegisterFragment.TAG,
        title = getString(R.string.anc_clients),
        isSelected = false
      )
    )

  override fun mainFragmentTag() = FamilyRegisterFragment.TAG
}
