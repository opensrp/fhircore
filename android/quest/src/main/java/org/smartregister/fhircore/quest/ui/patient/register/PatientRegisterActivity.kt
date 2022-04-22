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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.view.RegisterViewConfiguration
import org.smartregister.fhircore.engine.p2p.dao.P2PSenderTransferDao
import org.smartregister.fhircore.engine.p2p.dao.util.P2PConstants
import org.smartregister.fhircore.engine.ui.register.BaseRegisterActivity
import org.smartregister.fhircore.engine.ui.register.model.RegisterItem
import org.smartregister.fhircore.engine.ui.userprofile.UserProfileFragment
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.configuration.view.ActionSwitchFragment
import org.smartregister.fhircore.quest.configuration.view.P2PSyncNavigationAction
import org.smartregister.fhircore.quest.configuration.view.QuestionnaireDataDetailsNavigationAction
import org.smartregister.fhircore.quest.ui.patient.details.QuestionnaireDataDetailActivity
import org.smartregister.fhircore.quest.ui.patient.details.QuestionnaireDataDetailActivity.Companion.CLASSIFICATION_ARG
import org.smartregister.fhircore.quest.ui.task.PatientTaskFragment
import org.smartregister.fhircore.quest.util.QuestConfigClassification
import org.smartregister.fhircore.quest.util.QuestJsonSpecificationProvider
import org.smartregister.p2p.sync.DataType
import org.smartregister.p2p.utils.startP2PScreen
import javax.inject.Inject


@AndroidEntryPoint
class PatientRegisterActivity : BaseRegisterActivity() {

  @Inject lateinit var configurationRegistry: ConfigurationRegistry
  @Inject lateinit var questJsonSpecificationProvider: QuestJsonSpecificationProvider

  @Inject lateinit var p2PSenderTransferDao: P2PSenderTransferDao

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val registerViewConfiguration =
      configurationRegistry.retrieveConfiguration<RegisterViewConfiguration>(
        configClassification = QuestConfigClassification.PATIENT_REGISTER,
        questJsonSpecificationProvider.getJson()
      )
    configureViews(registerViewConfiguration)

    // TODO: REmove this test code
/*    GlobalScope.launch(Dispatchers.IO) {
      p2PSenderTransferDao.getJsonData(DataType(P2PConstants.P2PDataTypes.QUESTIONNAIRE, DataType.Filetype.JSON, 1), 0, 20)
    }*/
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
          is P2PSyncNavigationAction -> {
            startP2PScreen(this
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

 /* override fun registerClient(clientIdentifier: String?) {
    startP2PScreen(this
    )
  }*/
}
