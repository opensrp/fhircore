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

package org.smartregister.fhircore.quest.ui.family.remove.member

import androidx.activity.viewModels
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.engine.util.extension.extractName
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.ui.family.remove.BaseRemoveFamilyEntityQuestionnaireActivity

class RemoveFamilyMemberQuestionnaireActivity :
  BaseRemoveFamilyEntityQuestionnaireActivity<Patient>() {

  override val viewModel by viewModels<RemoveFamilyMemberViewModel>()

  override fun onReceive(profile: Patient) {
    profileName = profile.extractName()
  }

  override fun setRemoveButtonText(): String {
    return getString(R.string.remove_member)
  }

  override fun setRemoveDialogTitle(): String {
    return getString(R.string.confirm_remove_family_member_title)
  }

  override fun setRemoveDialogMessage(profileName: String): String {
    return getString(R.string.remove_family_member_warning, profileName)
  }
}
