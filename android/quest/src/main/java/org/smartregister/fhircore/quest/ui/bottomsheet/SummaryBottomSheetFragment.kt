/*
 * Copyright 2021-2024 Ona Systems, Inc
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

package org.smartregister.fhircore.quest.ui.bottomsheet

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.smartregister.fhircore.engine.configuration.geowidget.SummaryBottomSheetConfig
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import org.smartregister.fhircore.quest.R

class SummaryBottomSheetFragment(
  private val summaryBottomSheetConfig: SummaryBottomSheetConfig,
  private val resourceData: ResourceData,
) : BottomSheetDialogFragment() {

  override fun getTheme(): Int = R.style.BottomSheetDialogTheme

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
    BottomSheetDialog(requireContext(), theme)

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    return ComposeView(requireContext()).apply {
      setContent {
        AppTheme {
          SummaryBottomSheetView(
            properties = summaryBottomSheetConfig.views ?: emptyList(),
            resourceData = resourceData,
            navController = findNavController(),
          )
        }
      }
    }
  }

  companion object {
    const val TAG = "SummaryBottomSheetTag"
  }
}
