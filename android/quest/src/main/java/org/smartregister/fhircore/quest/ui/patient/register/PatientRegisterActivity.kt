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

package org.smartregister.fhircore.quest.ui.patient.register

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.fhir.sync.State
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.smartregister.fhircore.engine.configuration.view.RegisterViewConfiguration
import org.smartregister.fhircore.engine.configuration.view.registerViewConfigurationOf
import org.smartregister.fhircore.engine.ui.register.BaseRegisterActivity
import org.smartregister.fhircore.engine.ui.register.model.NavigationMenuOption
import org.smartregister.fhircore.engine.ui.register.model.RegisterItem
import org.smartregister.fhircore.engine.ui.userprofile.UserProfileFragment
import org.smartregister.fhircore.engine.util.extension.runPeriodicSync
import org.smartregister.fhircore.engine.util.extension.showToast
import org.smartregister.fhircore.quest.QuestApplication.Companion.getPatientRegisterConfig
import org.smartregister.fhircore.quest.QuestFhirSyncWorker
import org.smartregister.fhircore.quest.R
import timber.log.Timber

class PatientRegisterActivity : BaseRegisterActivity() {
  private lateinit var registerViewConfiguration: RegisterViewConfiguration

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    registerViewConfiguration =
      getPatientRegisterConfig()
        ?: registerViewConfigurationOf(
          showScanQRCode = false,
          appTitle = getString(R.string.clients)
        )
    configureViews(registerViewConfiguration)

    handleFirstSync()
  }

  override fun bottomNavigationMenuOptions(): List<NavigationMenuOption> {
    return listOf(
      NavigationMenuOption(
        id = R.id.menu_item_clients,
        title = getString(R.string.menu_clients),
        iconResource = ContextCompat.getDrawable(this, R.drawable.ic_users)!!
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
      R.id.menu_item_clients -> switchFragment(mainFragmentTag())
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

  // TODO remove it once metadata is loaded before opening register
  // https://github.com/opensrp/fhircore/issues/728
  private fun handleFirstSync() {
    if (registerViewModel.lastSyncTimestamp.value?.isBlank() == true)
      lifecycleScope.launch {
        registerViewModel.sharedSyncStatus.collect {
          if (it is State.Finished || it is State.Failed) {
            Timber.i("Running sync again for clinical data")

            this@PatientRegisterActivity.showToast(getString(R.string.syncing_in_progress))
            application.runPeriodicSync<QuestFhirSyncWorker>()

            Timber.i("Restarting activity to reload config")
            startActivity(Intent(this@PatientRegisterActivity, PatientRegisterActivity::class.java))
            finish()
          }
        }
      }
  }
}
