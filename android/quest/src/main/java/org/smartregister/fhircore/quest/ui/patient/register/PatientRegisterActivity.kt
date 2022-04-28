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
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.view.RegisterViewConfiguration
import org.smartregister.fhircore.engine.task.FhirTaskGenerator
import org.smartregister.fhircore.engine.ui.register.BaseRegisterActivity
import org.smartregister.fhircore.engine.ui.register.model.RegisterItem
import org.smartregister.fhircore.engine.ui.userprofile.UserProfileFragment
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.configuration.view.ActionSwitchFragment
import org.smartregister.fhircore.quest.configuration.view.QuestionnaireDataDetailsNavigationAction
import org.smartregister.fhircore.quest.ui.patient.details.QuestionnaireDataDetailActivity
import org.smartregister.fhircore.quest.ui.patient.details.QuestionnaireDataDetailActivity.Companion.CLASSIFICATION_ARG
import org.smartregister.fhircore.quest.ui.task.PatientTaskFragment
import org.smartregister.fhircore.quest.util.QuestConfigClassification
import org.smartregister.fhircore.quest.util.QuestJsonSpecificationProvider
import timber.log.Timber

@AndroidEntryPoint
class PatientRegisterActivity : BaseRegisterActivity() {

  @Inject lateinit var configurationRegistry: ConfigurationRegistry
  @Inject lateinit var questJsonSpecificationProvider: QuestJsonSpecificationProvider
  @Inject lateinit var fhirTaskGenerator: FhirTaskGenerator

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val registerViewConfiguration =
      configurationRegistry.retrieveConfiguration<RegisterViewConfiguration>(
        configClassification = QuestConfigClassification.PATIENT_REGISTER,
        questJsonSpecificationProvider.getJson()
      )
    configureViews(registerViewConfiguration)

    with(registerViewModel.configService) {
      if (true /*registerViewModel.applicationConfiguration.scheduleDefaultPlanWorker*/)
        this.schedulePlan(this@PatientRegisterActivity)
      else this.unschedulePlan(this@PatientRegisterActivity)
    }

    // TODO move to where required.. Elly
    runBlocking {
      fhirTaskGenerator.generateCarePlan(
        "105121",
        Patient().apply {
          birthDate = DateTimeType.now().value
          id = "327378278"
        }
      )
    }
  }

  override fun onBottomNavigationOptionItemSelected(
    item: MenuItem,
    viewConfiguration: RegisterViewConfiguration
  ): Boolean {
    viewConfiguration.bottomNavigationOptions?.forEach { navigationOption ->
      if (item.itemId == navigationOption.id.hashCode()) {
        when (val action = navigationOption.action) {
          is ActionSwitchFragment -> {
            switchFragment(
              action.tag,
              action.isRegisterFragment,
              action.isFilterVisible,
              action.toolbarTitle
            )
          }
          is QuestionnaireDataDetailsNavigationAction -> {
            startActivity(
              Intent(this, QuestionnaireDataDetailActivity::class.java).apply {
                putExtra(CLASSIFICATION_ARG, action.classification)
              }
            )
          }
        }
      }
    }
    return super.onBottomNavigationOptionItemSelected(item, viewConfiguration)
  }

  override fun mainFragmentTag() = PatientRegisterFragment.TAG

  override fun supportedFragments(): Map<String, Fragment> =
    mapOf(
      Pair(PatientRegisterFragment.TAG, PatientRegisterFragment()),
      Pair(PatientTaskFragment.TAG, PatientTaskFragment()),
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
