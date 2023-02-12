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

package org.smartregister.fhircore.engine.ui.bottomsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.smartregister.fhircore.engine.configuration.navigation.NavigationMenuConfig
import org.smartregister.fhircore.engine.ui.theme.AppTheme

class RegisterBottomSheetFragment(
  val navigationMenuConfigs: List<NavigationMenuConfig>? = emptyList(),
  val registerCountMap: Map<String, Long> = emptyMap(),
  val menuClickListener: (NavigationMenuConfig) -> Unit
) : BottomSheetDialogFragment() {

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    return ComposeView(requireContext()).apply {
      setContent {
        AppTheme {
          RegisterBottomSheetView(
            navigationMenuConfigs = navigationMenuConfigs,
            registerCountMap = registerCountMap,
            menuClickListener = menuClickListener,
            onDismiss = { dismiss() }
          )
        }
      }
    }
  }

  companion object {
    const val TAG = "NavigationBottomSheetTag"
  }
}
