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
import org.smartregister.fhircore.mwcore.R
import org.smartregister.fhircore.mwcore.ui.fragments.AppointmentsFragment
import org.smartregister.fhircore.mwcore.ui.fragments.TracingFragment
import org.smartregister.fhircore.mwcore.util.MwCoreConfigClassification

@AndroidEntryPoint
class PatientRegisterActivity : BaseRegisterActivity() {

  @Inject lateinit var configurationRegistry: ConfigurationRegistry

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val registerViewConfiguration =
      configurationRegistry.retrieveConfiguration<RegisterViewConfiguration>(
        configClassification = MwCoreConfigClassification.PATIENT_REGISTER
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
      R.id.menu_item_register -> switchFragment(mainFragmentTag())
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

  override fun mainFragmentTag() = PatientRegisterFragment.TAG

  override fun supportedFragments(): Map<String, Fragment> =
    mapOf(
      Pair(PatientRegisterFragment.TAG, PatientRegisterFragment()),
      Pair(AppointmentsFragment.TAG, AppointmentsFragment()),
      Pair(TracingFragment.TAG, TracingFragment()),
      Pair(UserProfileFragment.TAG, UserProfileFragment())
    )

  override fun registersList(): List<RegisterItem> =
    listOf(
      RegisterItem(
        uniqueTag = PatientRegisterFragment.TAG,
        title = getString(R.string.clients),
        isSelected = true
      )
    )
}
