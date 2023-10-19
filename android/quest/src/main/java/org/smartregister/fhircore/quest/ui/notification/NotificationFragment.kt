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

package org.smartregister.fhircore.quest.ui.notification

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import androidx.paging.compose.collectAsLazyPagingItems
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import org.smartregister.fhircore.quest.ui.notification.components.NotificationDialog

@ExperimentalMaterialApi
@AndroidEntryPoint
class NotificationFragment : Fragment() {

  private val registerFragmentArgs by navArgs<NotificationFragmentArgs>()
  private val registerViewModel by viewModels<NotificationViewModel>()

  @OptIn(ExperimentalPermissionsApi::class)
  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    with(registerFragmentArgs) {
      lifecycleScope.launch {
        registerViewModel.retrieveRegisterUiState(
          registerId = notificationId,
          params = params,
          clearCache = false,
        )
      }
    }
    return ComposeView(requireContext()).apply {
      setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
      setContent {
        val scaffoldState = rememberScaffoldState()

        AppTheme {
          val pagingItems =
            registerViewModel.paginatedRegisterData
              .collectAsState(emptyFlow())
              .value
              .collectAsLazyPagingItems()

          // Register screen provides access to the side navigation
          Scaffold(
            drawerGesturesEnabled = scaffoldState.drawerState.isOpen,
            scaffoldState = scaffoldState,
          ) { innerPadding ->
            Box(modifier = Modifier
              .padding(innerPadding)
              .testTag(NOTIFICATION_SCREEN_BOX_TAG)) {

              if(registerViewModel.notificationDialogData.observeAsState(mapOf()).value.isNotEmpty()) {
                val data = registerViewModel.notificationDialogData.value!!
                NotificationDialog(
                  title = data["notificationTitle"] as String,
                  description = data["notificationDescription"] as String,
                  onDismissDialog = {
                    registerViewModel.onEvent(NotificationEvent.ShowNotification())
                  }
                )
              }

              NotificationScreen(
                onEvent = registerViewModel::onEvent,
                registerUiState = registerViewModel.registerUiState.value,
                searchText = registerViewModel.searchText,
                currentPage = registerViewModel.currentPage,
                pagingItems = pagingItems,
                navController = findNavController(),
              )
            }
          }
        }
      }
    }
  }

  override fun onStop() {
    super.onStop()
    registerViewModel.searchText.value = "" // Clear the search term
  }

  companion object {
    const val NOTIFICATION_SCREEN_BOX_TAG = "fragmentNotificationScreenTestTag"
  }
}
