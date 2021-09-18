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
import org.hl7.fhir.r4.model.Encounter
import org.smartregister.fhircore.anc.AncApplication
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.data.family.FamilyDetailRepository
import org.smartregister.fhircore.anc.data.family.model.FamilyMemberItem
import org.smartregister.fhircore.anc.ui.family.form.FamilyFormConstants
import org.smartregister.fhircore.anc.ui.family.form.FamilyQuestionnaireActivity
import org.smartregister.fhircore.engine.configuration.app.ConfigurableApplication
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.ui.theme.AppTheme


class FamilyDetailsActivity : BaseMultiLanguageActivity() {

  private lateinit var familyId: String

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    familyId = intent.extras?.getString(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY) ?: ""
    val fhirEngine = (AncApplication.getContext() as ConfigurableApplication).fhirEngine
    val familyDetailRepository = FamilyDetailRepository(familyId, fhirEngine)
    val viewModel = FamilyDetailViewModel.get(this, application as AncApplication, familyDetailRepository)

    viewModel.setAppBackClickListener(this::onBackIconClicked)
    viewModel.setMemberItemClickListener(this::onFamilyMemberItemClicked)
    viewModel.setAddMemberItemClickListener(this::onAddNewMemberButtonClicked)
    viewModel.setSeeAllEncounterClickListener(this::onSeeAllEncounterClicked)
    viewModel.setEncounterItemClickListener(this::onFamilyEncounterItemClicked)

    setContent {
      AppTheme {
        FamilyDetailScreen(viewModel)
      }
    }
  }

  private fun onBackIconClicked() {
    finish()
  }

  private fun onFamilyMemberItemClicked(item: FamilyMemberItem) {}

  private fun onAddNewMemberButtonClicked() {
    val bundle =
      QuestionnaireActivity.requiredIntentArgs(
        clientIdentifier = null,
        form = FamilyFormConstants.FAMILY_MEMBER_REGISTER_FORM
      )
    bundle.putString(FamilyQuestionnaireActivity.QUESTIONNAIRE_RELATED_TO_KEY, familyId)
    startActivity(Intent(this, FamilyQuestionnaireActivity::class.java).putExtras(bundle))
  }

  private fun onSeeAllEncounterClicked() {}

  private fun onFamilyEncounterItemClicked(item: Encounter) {}
}
