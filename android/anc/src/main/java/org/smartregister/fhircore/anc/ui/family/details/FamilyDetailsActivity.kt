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

package org.smartregister.fhircore.anc.ui.family.details

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.data.family.model.FamilyMemberItem
import org.smartregister.fhircore.anc.ui.details.PatientDetailsActivity
import org.smartregister.fhircore.anc.util.startFamilyMemberRegistration
import org.smartregister.fhircore.engine.ui.base.AlertDialogue
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_ARG_PATIENT_KEY
import org.smartregister.fhircore.engine.ui.theme.AppTheme

@AndroidEntryPoint
class FamilyDetailsActivity : BaseMultiLanguageActivity() {

  val familyDetailViewModel by viewModels<FamilyDetailViewModel>()

  private lateinit var familyId: String

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    familyId = intent.extras?.getString(QUESTIONNAIRE_ARG_PATIENT_KEY) ?: ""

    familyDetailViewModel.apply {
      val familyDetailsActivity = this@FamilyDetailsActivity
      backClicked.observe(familyDetailsActivity, { if (it) finish() })
      memberItemClicked.observe(
        familyDetailsActivity,
        familyDetailsActivity::onFamilyMemberItemClicked
      )
      addNewMember.observe(
        familyDetailsActivity,
        { addNewMember ->
          if (addNewMember) {
            familyDetailsActivity.startFamilyMemberRegistration(familyId)
            finish()
          }
        }
      )

      familyDetailViewModel.apply {
        isRemoveFamily.observe(familyDetailsActivity, { if (it) finish() })
      }

      familyDetailViewModel.apply {
        isRemoveFamilyMenuItemClicked.observe(
          familyDetailsActivity,
          {
            if (it) {
              removeFamilyMenuItemClicked(familyId = familyId)
            }
          }
        )
      }
    }

    familyDetailViewModel.run {
      fetchDemographics(familyId)
      fetchFamilyMembers(familyId)
      fetchCarePlans(familyId)
      fetchEncounters(familyId)
    }

    setContent { AppTheme { FamilyDetailScreen(familyDetailViewModel) } }
  }

  private fun onFamilyMemberItemClicked(familyMemberItem: FamilyMemberItem?) {
    if (familyMemberItem != null)
      startActivity(
        Intent(this, PatientDetailsActivity::class.java).apply {
          putExtra(QUESTIONNAIRE_ARG_PATIENT_KEY, familyMemberItem.id)
        }
      )
  }

  private fun removeFamilyMenuItemClicked(familyId: String) {
    AlertDialogue.showConfirmAlert(
      this,
      R.string.confirm_remove_family_message,
      R.string.confirm_remove_family_title,
      { familyDetailViewModel.removeFamily(familyId = familyId) },
      R.string.family_register_ok_title
    )
  }
}
