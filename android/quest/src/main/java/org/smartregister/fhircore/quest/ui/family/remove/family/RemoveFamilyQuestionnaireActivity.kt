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

package org.smartregister.fhircore.quest.ui.family.remove.family

import androidx.activity.viewModels
import org.hl7.fhir.r4.model.Group
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.ui.family.remove.BaseRemoveFamilyEntityQuestionnaireActivity

class RemoveFamilyQuestionnaireActivity : BaseRemoveFamilyEntityQuestionnaireActivity<Group>() {

  override val viewModel by viewModels<RemoveFamilyViewModel>()

  override fun onReceive(profile: Group) {
    profileName = profile.name
  }

  override fun setRemoveButtonText(): String {
    return getString(R.string.remove_family)
  }

  override fun setRemoveDialogTitle(): String {
    return getString(R.string.remove_family)
  }

  override fun setRemoveDialogMessage(profileName: String): String {
    return getString(R.string.remove_family_warning, profileName)
  }
}
