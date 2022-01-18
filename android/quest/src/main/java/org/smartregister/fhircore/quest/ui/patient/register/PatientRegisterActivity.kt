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

import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.famoco.desfireservicelib.DESFireServiceAccess
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.view.RegisterViewConfiguration
import org.smartregister.fhircore.engine.nfc.MainViewModel
import org.smartregister.fhircore.engine.ui.register.BaseRegisterActivity
import org.smartregister.fhircore.engine.ui.register.model.NavigationMenuOption
import org.smartregister.fhircore.engine.ui.register.model.RegisterItem
import org.smartregister.fhircore.engine.ui.userprofile.UserProfileFragment
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.util.QuestConfigClassification

@AndroidEntryPoint
class PatientRegisterActivity : BaseRegisterActivity() {

  @Inject lateinit var configurationRegistry: ConfigurationRegistry

  private val mainViewModel: MainViewModel by viewModels()

  private lateinit var eventJob: Job

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val registerViewConfiguration =
      configurationRegistry.retrieveConfiguration<RegisterViewConfiguration>(
        configClassification = QuestConfigClassification.PATIENT_REGISTER
      )
    configureViews(registerViewConfiguration)
    // Connect to DES service
    mainViewModel.connectService(this)

    // Add DES service listeners
    addDesServiceListeners()
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
        /*        switchFragment(
          tag = UserProfileFragment.TAG,
          isRegisterFragment = false,
          toolbarTitle = getString(R.string.settings)
        )*/
        initializeNfc()
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

  override fun onResume() {
    super.onResume()
    // Event that prompt only once to be able to know what has just happen with the card reader
    // This consumption will be used if the end-user want to use the Read/Write Activities
    // from the DESFire Service, so that the event can be consume inside the end-user app.
    eventJob =
      lifecycleScope.launchWhenStarted {
        DESFireServiceAccess.eventFlow.collect { event ->
          Toast.makeText(baseContext, event.name, Toast.LENGTH_SHORT).show()
        }
      }
    eventJob.start()
  }

  override fun onPause() {
    super.onPause()
    // In order to consume the event in ReadNfcActivity or WriteNfcActivity,
    // we need to cancel this eventJob before leaving the MainActivity,
    // because the event will be consumed only once
    eventJob.cancel()
  }

  private fun initializeNfc() {

    mainViewModel.generateProtoFile()
    // InitializeSAM
    mainViewModel.initSAM()
    // Perform Read action with the UI given by the Service
    mainViewModel.readSerialized()
  }

  private fun addDesServiceListeners() {
    // Check state connection with the service
    DESFireServiceAccess.DESFireServiceConnectionState.observe(this) {
      val state = it.name
    }

    // Check if card interaction status
    DESFireServiceAccess.cardReaderState.observe(this) {
      val cardReaderState = it.name
    }

    // After reading the card, the end-user will need the content that has been read on the card
    DESFireServiceAccess.readResult.observe(this) { result ->
      if (!result.isNullOrEmpty()) {
        val stringBuilder = StringBuilder().append("Here is the result after reading the card :\n")
        result.forEach { stringBuilder.append(it).append("\n") }
        val readResult = stringBuilder.toString()
      }
    }
  }
}
