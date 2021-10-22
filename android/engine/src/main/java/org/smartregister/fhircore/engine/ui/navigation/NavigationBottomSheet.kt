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

package org.smartregister.fhircore.engine.ui.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.smartregister.fhircore.engine.ui.register.model.RegisterItem
import org.smartregister.fhircore.engine.ui.theme.AppTheme

class NavigationBottomSheet private constructor(private val itemListener: (String) -> Unit) :
  BottomSheetDialogFragment() {

  var registersList: List<RegisterItem> = emptyList()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    return ComposeView(requireContext()).apply {
      setContent {
        AppTheme { RegisterBottomSheet(registers = registersList, itemListener = itemListener) }
      }
    }
  }

  companion object {

    const val TAG = "NavigationBottomSheetTag"

    @Volatile private var instance: NavigationBottomSheet? = null

    fun getInstance(itemListener: (String) -> Unit): NavigationBottomSheet =
      instance ?: synchronized(this) { NavigationBottomSheet(itemListener).also { instance = it } }
  }
}
