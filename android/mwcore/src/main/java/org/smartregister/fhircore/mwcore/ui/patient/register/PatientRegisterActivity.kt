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

package org.smartregister.fhircore.mwcore.ui.patient.register

import android.os.Bundle
import android.view.MenuItem
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.view.RegisterViewConfiguration
import org.smartregister.fhircore.engine.ui.register.BaseRegisterActivity
import org.smartregister.fhircore.engine.ui.register.model.NavigationMenuOption
import org.smartregister.fhircore.engine.ui.register.model.RegisterItem
import org.smartregister.fhircore.engine.ui.userprofile.UserProfileFragment
import org.smartregister.fhircore.engine.util.extension.toggleVisibility
import org.smartregister.fhircore.mwcore.R
import org.smartregister.fhircore.mwcore.ui.fragments.AppointmentsFragment
import org.smartregister.fhircore.mwcore.ui.patient.register.fragments.ExposedInfantsRegisterFragment
import org.smartregister.fhircore.mwcore.ui.fragments.TracingFragment
import org.smartregister.fhircore.mwcore.ui.patient.register.fragments.ClientsRegisterFragment
import org.smartregister.fhircore.mwcore.util.MwCoreConfigClassification
import org.smartregister.fhircore.mwcore.util.SharedPrefsKeys.REGISTER_CONFIG

@AndroidEntryPoint
class PatientRegisterActivity : BaseRegisterActivity() {

  @Inject lateinit var configurationRegistry: ConfigurationRegistry

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val registerViewConfiguration =
      configurationRegistry.retrieveConfiguration<RegisterViewConfiguration>(
        configClassification = MwCoreConfigClassification.PATIENT_REGISTER_CLIENT
      )
    configureViews(registerViewConfiguration)
  }

  override fun bottomNavigationMenuOptions(): List<NavigationMenuOption> {
    return listOf(
      NavigationMenuOption(
        id = R.id.menu_item_register,
        title = getString(R.string.menu_register),
        iconResource = ContextCompat.getDrawable(this, R.drawable.ic_users)!!
      ),
      NavigationMenuOption(
        id = R.id.menu_item_appointments,
        title = getString(R.string.menu_appointments),
        iconResource = ContextCompat.getDrawable(this, R.drawable.ic_appoinments)!!
      ),
      NavigationMenuOption(
        id = R.id.menu_item_tracing,
        title = getString(R.string.menu_tracing),
        iconResource = ContextCompat.getDrawable(this, R.drawable.ic_tracing)!!
      ),
      NavigationMenuOption(
        id = R.id.menu_item_settings,
        title = getString(R.string.menu_settings),
        iconResource = ContextCompat.getDrawable(this, R.drawable.ic_settings)!!
      )
    )
  }

  override fun onNavigationOptionItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.menu_item_register -> onRegisterTabSelected()
      R.id.menu_item_appointments ->
        switchFragment(
          tag = AppointmentsFragment.TAG,
          isRegisterFragment = false,
          toolbarTitle = getString(R.string.menu_appointments)
        )
      R.id.menu_item_tracing ->
        switchFragment(
          tag = TracingFragment.TAG,
          isRegisterFragment = false,
          toolbarTitle = getString(R.string.menu_tracing)
        )
      R.id.menu_item_settings ->
        switchFragment(
          tag = UserProfileFragment.TAG,
          isRegisterFragment = false,
          toolbarTitle = getString(R.string.settings)
        )
    }
    return true
  }

  private fun onRegisterTabSelected() {
    loadClientsUI()
    switchFragment(mainFragmentTag())
  }

  override fun mainFragmentTag() = ClientsRegisterFragment.TAG

  override fun supportedFragments(): Map<String, Fragment> =
    mapOf(
      Pair(ClientsRegisterFragment.TAG, ClientsRegisterFragment()),
      Pair(ExposedInfantsRegisterFragment.TAG, ExposedInfantsRegisterFragment()),
      Pair(AppointmentsFragment.TAG, AppointmentsFragment()),
      Pair(TracingFragment.TAG, TracingFragment()),
      Pair(UserProfileFragment.TAG, UserProfileFragment())
    )

  override fun registersList(): List<RegisterItem> =
    listOf(
      RegisterItem(
        uniqueTag = ClientsRegisterFragment.TAG,
        title = getString(R.string.register_clients),
        isSelected = true
      ),
      RegisterItem(
        uniqueTag = ExposedInfantsRegisterFragment.TAG,
        title = getString(R.string.register_exposed_infants),
        isSelected = false
      )
    )

  override fun setupBottomNavigationMenu(viewConfiguration: RegisterViewConfiguration) {
    val bottomMenu = registerActivityBinding.bottomNavView.menu
    registerActivityBinding.bottomNavView.apply {
      toggleVisibility(viewConfiguration.showBottomMenu)
      setOnItemSelectedListener(this@PatientRegisterActivity)
    }

    // don't add any new items if they already exist
    if (bottomMenu.hasVisibleItems()) {
      return
    }

    for ((index, it) in bottomNavigationMenuOptions().withIndex()) {
      bottomMenu.add(org.smartregister.fhircore.engine.R.id.menu_group_default_item_id, it.id, index, it.title).apply {
        it.iconResource.let { icon -> this.icon = icon }
      }
    }
  }

  override fun onSelectRegister(fragmentTag: String) {
    updateUI(fragmentTag)
    super.onSelectRegister(fragmentTag)
  }

  private fun updateUI(fragmentTag: String) {
    if (fragmentTag == ClientsRegisterFragment.TAG) {
      loadClientsUI()
    } else {
      loadExposedInfantsUI()
    }
  }

  private fun loadClientsUI() {
    val registerViewConfiguration =
      configurationRegistry.retrieveConfiguration<RegisterViewConfiguration>(
        configClassification = MwCoreConfigClassification.PATIENT_REGISTER_CLIENT
      )
    configureViews(registerViewConfiguration)
    configurationRegistry.sharedPreferencesHelper.write(REGISTER_CONFIG, ClientsRegisterFragment.TAG)
  }

  private fun loadExposedInfantsUI() {
    val registerViewConfiguration =
      configurationRegistry.retrieveConfiguration<RegisterViewConfiguration>(
        configClassification = MwCoreConfigClassification.PATIENT_REGISTER_EXPOSED_INFANT
      )
    configureViews(registerViewConfiguration)
    configurationRegistry.sharedPreferencesHelper.write(REGISTER_CONFIG, ExposedInfantsRegisterFragment.TAG)
  }

  override fun switchFragment(
    tag: String,
    isRegisterFragment: Boolean,
    toolbarTitle: String?
  ) {
    super.switchFragment(tag, isRegisterFragment, toolbarTitle)
    registerActivityBinding.btnRegisterNewClient.toggleVisibility(isRegisterFragment)
  }
}
