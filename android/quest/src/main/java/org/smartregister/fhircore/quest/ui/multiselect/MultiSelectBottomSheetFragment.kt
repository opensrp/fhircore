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
import androidx.compose.foundation.layout.Box
import androidx.compose.material.Text
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.smartregister.fhircore.engine.domain.model.MultiSelectViewAction
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.event.AppEvent
import org.smartregister.fhircore.quest.event.EventBus
import org.smartregister.fhircore.quest.ui.main.AppMainEvent
import org.smartregister.fhircore.quest.ui.main.AppMainViewModel

@AndroidEntryPoint
class MultiSelectBottomSheetFragment : BottomSheetDialogFragment() {

  @Inject lateinit var dispatcherProvider: DefaultDispatcherProvider

  @Inject lateinit var eventBus: EventBus
  private val bottomSheetArgs by navArgs<MultiSelectBottomSheetFragmentArgs>()
  private val multiSelectViewModel by viewModels<MultiSelectViewModel>()
  private val appMainViewModel by activityViewModels<AppMainViewModel>()

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    isCancelable = false
    val multiSelectViewConfig = bottomSheetArgs.multiSelectViewConfig
    if (multiSelectViewConfig != null) {
      multiSelectViewModel.populateLookupMap(requireContext(), multiSelectViewConfig)
    }
  }

  private fun onSelectionDone(viewActions: List<MultiSelectViewAction>) {
    val context = requireContext()
    lifecycleScope.launch {
      multiSelectViewModel.saveSelectedLocations(context, viewActions) {
        viewActions.distinct().forEach { viewAction ->
          when (viewAction) {
            MultiSelectViewAction.SYNC_DATA ->
              appMainViewModel.onEvent(AppMainEvent.SyncData(context))
            MultiSelectViewAction.FILTER_DATA ->
              lifecycleScope.launch {
                eventBus.triggerEvent(
                  AppEvent.RefreshData,
                )
              }
          }
        }
        dismiss()
      }
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
          val multiSelectViewConfig = bottomSheetArgs.multiSelectViewConfig
          if (multiSelectViewConfig != null) {
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
              multiSelectViewAction = multiSelectViewConfig.viewActions,
              mutuallyExclusive = multiSelectViewConfig.mutuallyExclusive,
            )
          } else {
            Box(contentAlignment = Alignment.Center) {
              Text(text = stringResource(R.string.missing_multi_select_view_configs))
            }
          }
        }
      }
    }
  }
}
