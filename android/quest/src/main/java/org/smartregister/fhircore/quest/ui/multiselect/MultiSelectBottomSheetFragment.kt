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

package org.smartregister.fhircore.quest.ui.multiselect

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.navArgs
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import org.smartregister.fhircore.engine.util.extension.isDeviceOnline
import org.smartregister.fhircore.engine.util.extension.showToast
import org.smartregister.fhircore.quest.event.AppEvent
import org.smartregister.fhircore.quest.event.EventBus
import org.smartregister.fhircore.quest.ui.main.AppMainViewModel

@AndroidEntryPoint
class MultiSelectBottomSheetFragment() : BottomSheetDialogFragment() {

  @Inject lateinit var eventBus: EventBus
  val bottomSheetArgs by navArgs<MultiSelectBottomSheetFragmentArgs>()
  val multiSelectViewModel by viewModels<MultiSelectViewModel>()
  private val appMainViewModel by activityViewModels<AppMainViewModel>()

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    isCancelable = false
    val multiSelectViewConfig = bottomSheetArgs?.multiSelectViewConfig
    if (multiSelectViewConfig != null) {
      multiSelectViewModel.populateLookupMap(requireContext(), multiSelectViewConfig)
    }
  }

  private fun onSelectionDone() {
    lifecycleScope.launch {
      multiSelectViewModel.saveSelectedLocations(requireContext())
      appMainViewModel.run {
        if (requireContext().isDeviceOnline()) {
          viewModelScope.launch { syncBroadcaster.runOneTimeSync() }
          schedulePeriodicSync()
        } else {
          requireContext()
            .showToast(
              getString(org.smartregister.fhircore.engine.R.string.sync_failed),
              Toast.LENGTH_LONG,
            )
          eventBus.triggerEvent(AppEvent.RefreshRegisterData)
        }
      }
      dismiss()
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    return ComposeView(requireContext()).apply {
      setContent {
        AppTheme {
          MultiSelectBottomSheetView(
            rootTreeNodes = multiSelectViewModel.rootTreeNodes,
            syncLocationStateMap = multiSelectViewModel.selectedNodes,
            title = bottomSheetArgs.screenTitle,
            onDismiss = { dismiss() },
            searchTextState = multiSelectViewModel.searchTextState,
            onSearchTextChanged = multiSelectViewModel::onTextChanged,
            onSelectionDone = ::onSelectionDone,
            search = multiSelectViewModel::search,
            isLoading = multiSelectViewModel.isLoading.observeAsState(),
          )
        }
      }
    }
  }
}
