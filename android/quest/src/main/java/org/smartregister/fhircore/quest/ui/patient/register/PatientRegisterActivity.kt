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
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.view.RegisterViewConfiguration
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.ui.register.BaseRegisterActivity
import org.smartregister.fhircore.engine.ui.register.model.NavigationMenuOption
import org.smartregister.fhircore.engine.ui.register.model.RegisterItem
import org.smartregister.fhircore.engine.ui.userprofile.UserProfileFragment
import org.smartregister.fhircore.engine.util.extension.getDrawable
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.configuration.view.NavigationConfiguration
import org.smartregister.fhircore.quest.configuration.view.NavigationOption
import org.smartregister.fhircore.quest.configuration.view.QuestionnaireNavigationAction
import org.smartregister.fhircore.quest.util.QuestConfigClassification

@AndroidEntryPoint
class PatientRegisterActivity : BaseRegisterActivity() {

  @Inject lateinit var configurationRegistry: ConfigurationRegistry

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val registerViewConfiguration =
      configurationRegistry.retrieveConfiguration<RegisterViewConfiguration>(
        configClassification = QuestConfigClassification.PATIENT_REGISTER
      )
    configureViews(registerViewConfiguration)
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
      .plus(
        getCustomNavigationOptions().navigationOptions.map {
          NavigationMenuOption(
            id = it.id.hashCode(),
            title = it.title,
            iconResource = this.getDrawable(it.icon)
          )
        }
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
      else ->
        getCustomNavigationOptions().navigationOptions.forEach {
          if (item.itemId == it.id.hashCode()) {
            handleCustomNavigation(it)
          }
        }
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

  fun getCustomNavigationOptions() =
    configurationRegistry.retrieveConfiguration<NavigationConfiguration>(
      QuestConfigClassification.REGISTER_NAVIGATION
    )

  fun handleCustomNavigation(navigationOption: NavigationOption) {
    when (navigationOption.action) {
      is QuestionnaireNavigationAction ->
        startActivity(
          Intent(this, QuestionnaireActivity::class.java)
            .putExtras(
              QuestionnaireActivity.intentArgs(
                formName = navigationOption.action.form,
              )
            )
        )
    }
  }
}
