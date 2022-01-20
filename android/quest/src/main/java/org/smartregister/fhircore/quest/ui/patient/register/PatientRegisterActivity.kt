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

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.InputType
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import ca.uhn.fhir.context.FhirContext
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Resource
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.view.RegisterViewConfiguration
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.ui.register.BaseRegisterActivity
import org.smartregister.fhircore.engine.ui.register.model.NavigationMenuOption
import org.smartregister.fhircore.engine.ui.register.model.RegisterItem
import org.smartregister.fhircore.engine.ui.userprofile.UserProfileFragment
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.util.QuestConfigClassification

@AndroidEntryPoint
class PatientRegisterActivity : BaseRegisterActivity() {

  @Inject lateinit var configurationRegistry: ConfigurationRegistry
  @Inject lateinit var defaultRepository: DefaultRepository

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val registerViewConfiguration =
      configurationRegistry.retrieveConfiguration<RegisterViewConfiguration>(
        configClassification = QuestConfigClassification.PATIENT_REGISTER
      )
    configureViews(registerViewConfiguration)

    loadLocalDevWfpCodaFiles()
  }

  fun loadLocalDevWfpCodaFiles() {
    val files =
      listOf(
        "fhir-questionnaires/CODA/anthro-following-visit.json",
        "fhir-questionnaires/CODA/assistance-visit.json",
        "fhir-questionnaires/CODA/coda-child-registration.json",
        "fhir-questionnaires/CODA/coda-child-structure-map.json",
      )

    files.forEach { fileName ->
      val jsonString = assets.open(fileName).bufferedReader().readText()
      val questionnaire = FhirContext.forR4().newJsonParser().parseResource(jsonString)

      GlobalScope.launch { defaultRepository.addOrUpdate(questionnaire as Resource) }
    }
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

  override fun registerClient(clientIdentifier: String?) {
    showAgeDialog({ super.registerClient(clientIdentifier) }, { dialog, which -> dialog.dismiss() })
  }

  fun showAgeDialog(
    okClickListener: () -> Unit,
    cancelClickListener: DialogInterface.OnClickListener
  ) {
    val input =
      EditText(this).apply {
        setHint(getString(R.string.enter_age_in_months))
        inputType = InputType.TYPE_CLASS_NUMBER
      }

    AlertDialog.Builder(this)
      .setTitle(getString(R.string.enter_beneficiary_age))
      .setView(input)
      .setPositiveButton(android.R.string.ok) { dialog, which ->
        val age = input.text.toString()

        if (age.isNotEmpty() && age.toInt() in (6..59)) {
          okClickListener()
        } else {
          Toast.makeText(
              this,
              getString(R.string.beneficiary_not_eligible_for_program),
              Toast.LENGTH_LONG
            )
            .show()
        }
        dialog.dismiss()
      }
      .setNegativeButton(R.string.cancel, cancelClickListener)
      .show()
  }
}
