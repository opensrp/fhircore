/*
 * Copyright 2021-2023 Ona Systems, Inc
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

package org.smartregister.fhircore.quest.ui.profile.bottomSheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.smartregister.fhircore.engine.configuration.profile.ManagingEntityConfig
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import org.smartregister.fhircore.quest.ui.profile.components.ChangeManagingEntityView
import org.smartregister.fhircore.quest.ui.profile.model.EligibleManagingEntity

class ProfileBottomSheetFragment
constructor(
  val eligibleManagingEntities: List<EligibleManagingEntity> = emptyList(),
  val onSaveClick: (EligibleManagingEntity) -> Unit,
  val managingEntity: ManagingEntityConfig? = null,
) : BottomSheetDialogFragment() {

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    return ComposeView(requireContext()).apply {
      setContent {
        AppTheme {
          ChangeManagingEntityView(
            onSaveClick = onSaveClick,
            eligibleManagingEntities = eligibleManagingEntities,
            onDismiss = { dismiss() },
            managingEntity = managingEntity
          )
        }
      }
    }
  }
  companion object {
    const val TAG = "ProfileBottomSheetTag"
  }
}
