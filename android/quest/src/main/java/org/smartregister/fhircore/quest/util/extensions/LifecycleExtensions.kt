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

package org.smartregister.fhircore.quest.util.extensions

import androidx.compose.material.ScaffoldState
import androidx.compose.material.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.domain.model.SnackBarMessageConfig

@Composable
fun rememberLifecycleEvent(
  lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
): Lifecycle.Event {
  var state by remember { mutableStateOf(Lifecycle.Event.ON_ANY) }
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event -> state = event }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }
  return state
}

suspend fun SharedFlow<SnackBarMessageConfig>.hookSnackBar(
  scaffoldState: ScaffoldState,
  resourceData: ResourceData?,
  navController: NavController,
  action: () -> Unit = {}
) {
  this.collectLatest { snackBarState ->
    if (snackBarState.message.isNotEmpty()) {
      val snackBarResult =
        scaffoldState.snackbarHostState.showSnackbar(
          message = snackBarState.message,
          actionLabel = snackBarState.actionLabel,
          duration = snackBarState.duration
        )
      when (snackBarResult) {
        SnackbarResult.ActionPerformed -> {
          snackBarState.snackBarActions.handleClickEvent(navController, resourceData)
          action()
        }
        SnackbarResult.Dismissed -> {
          /* Do nothing (for now) when snackBar is dismissed */
        }
      }
    }
  }
}
