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

package org.smartregister.fhircore.quest.ui.cleardata

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.smartregister.fhircore.engine.R as EngineR
import org.smartregister.fhircore.engine.util.annotation.PreviewWithBackgroundExcludeGenerated
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.ui.login.APP_LOGO_TAG

@Composable
fun ClearDataScreen(viewModel: ClearDataViewModel) {
  val unsyncedResourceCount by viewModel.unsyncedResourceCount.collectAsState()
  val appName by viewModel.appName.collectAsState()
  val isClearing by viewModel.isClearing.collectAsState()
  val isSyncing by viewModel.isSyncing.collectAsState()
  val navigateBack by viewModel.navigateBack.collectAsState()
  val context = LocalContext.current

  LaunchedEffect(navigateBack) { if (navigateBack) (context as? Activity)?.finish() }

  ClearData(
    unsyncedResourceCount = unsyncedResourceCount,
    appName = appName,
    isDebug = viewModel.isDebug,
    onSyncData = { viewModel.onEvent(ClearDataEvent.SyncData(context)) },
    onDeleteData = { viewModel.onEvent(ClearDataEvent.ClearAppData(context)) },
    onAbort = { viewModel.onEvent(ClearDataEvent.AbortClearData) },
    isClearing = isClearing,
    isSyncing = isSyncing,
  )
}

@Composable
fun ClearData(
  unsyncedResourceCount: Int,
  appName: String,
  isDebug: Boolean,
  onSyncData: () -> Unit,
  onDeleteData: () -> Unit,
  onAbort: () -> Unit = {},
  isClearing: Boolean = false,
  isSyncing: Boolean = false,
) {
  val hasUnsyncedData = unsyncedResourceCount >= 1
  var showConfirmDialog by remember { mutableStateOf(false) }

  if (showConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showConfirmDialog = false },
      title = {
        Text(
          text = stringResource(R.string.clear_data_confirm_title, appName),
          fontWeight = FontWeight.Bold,
          fontSize = 20.sp,
        )
      },
      text = {
        Column {
          Text(
            text = stringResource(R.string.clear_data_confirm_message),
            fontSize = 16.sp,
          )
          Spacer(modifier = Modifier.height(16.dp))
          val deletionItems =
            listOf(
              stringResource(R.string.clear_data_item_cache),
              stringResource(R.string.clear_data_item_databases),
              stringResource(R.string.clear_data_item_files),
              stringResource(R.string.clear_data_item_preferences),
              stringResource(R.string.clear_data_item_logs),
            )
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            deletionItems.forEach { item ->
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Filled.Check,
                  contentDescription = null,
                  tint = MaterialTheme.colors.primary,
                  modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = item, fontSize = 16.sp)
              }
            }
          }
        }
      },
      buttons = {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
          horizontalArrangement = Arrangement.End,
        ) {
          Text(
            text = stringResource(EngineR.string.cancel),
            modifier = Modifier.padding(horizontal = 10.dp).clickable { showConfirmDialog = false },
          )
          Text(
            text = stringResource(R.string.clear_data_delete_button_text, appName),
            color = MaterialTheme.colors.error,
            modifier =
              Modifier.padding(horizontal = 10.dp).clickable {
                showConfirmDialog = false
                onDeleteData()
              },
          )
        }
      },
    )
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(text = stringResource(R.string.clear_data_title, appName)) },
      )
    },
  ) { paddingValues ->
    Column(
      modifier =
        Modifier.fillMaxSize().padding(paddingValues).padding(vertical = 32.dp, horizontal = 16.dp),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.Center,
      ) {
        Image(
          painter = painterResource(R.drawable.ic_app_logo),
          contentDescription = stringResource(id = EngineR.string.app_logo),
          modifier = Modifier.requiredHeight(120.dp).requiredWidth(140.dp).testTag(APP_LOGO_TAG),
        )
      }

      Text(
        text =
          if (!hasUnsyncedData) {
            stringResource(R.string.clear_data_all_data_synced)
          } else {
            stringResource(R.string.clear_data_unsynced_resource_count, unsyncedResourceCount)
          },
        style =
          MaterialTheme.typography.h6.copy(
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colors.onSurface,
          ),
      )

      if (hasUnsyncedData) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
          text = stringResource(R.string.clear_data_data_loss_warning, appName, appName),
          style =
            MaterialTheme.typography.h6.copy(
              fontSize = 18.sp,
              color = MaterialTheme.colors.onSurface,
            ),
        )
      }

      Spacer(modifier = Modifier.weight(1f))

      Button(
        onClick = onSyncData,
        enabled = hasUnsyncedData && !isSyncing,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().height(50.dp).padding(horizontal = 16.dp),
      ) {
        if (isSyncing) {
          CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colors.onPrimary,
          )
        } else {
          Text(text = stringResource(R.string.clear_data_sync, appName))
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      Button(
        onClick = { showConfirmDialog = true },
        enabled = (!hasUnsyncedData || isDebug) && !isClearing && !isSyncing,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().height(50.dp).padding(horizontal = 16.dp),
      ) {
        if (isClearing) {
          CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colors.onPrimary,
          )
        } else {
          Text(text = stringResource(R.string.clear_data_delete_button_text, appName))
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      Button(
        onClick = onAbort,
        enabled = !isSyncing,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().height(50.dp).padding(horizontal = 16.dp),
      ) {
        Text(text = stringResource(EngineR.string.cancel))
      }
    }
  }
}

@Composable
@PreviewWithBackgroundExcludeGenerated
private fun ClearDataScreenSyncedPreview() {
  ClearData(
    unsyncedResourceCount = 0,
    appName = "OpenSRP 2",
    isDebug = false,
    onSyncData = {},
    onDeleteData = {},
  )
}

@Composable
@PreviewWithBackgroundExcludeGenerated
private fun ClearDataScreenUnsyncedPreview() {
  ClearData(
    unsyncedResourceCount = 77,
    appName = "OpenSRP 2",
    isDebug = true,
    onSyncData = {},
    onDeleteData = {},
    isSyncing = false,
  )
}

@Composable
@PreviewWithBackgroundExcludeGenerated
private fun ClearDataScreenUnsyncedNonDebugModePreview() {
  ClearData(
    unsyncedResourceCount = 500,
    appName = "OpenSRP 2",
    isDebug = false,
    onSyncData = {},
    onDeleteData = {},
    isSyncing = false,
  )
}

@Composable
@PreviewWithBackgroundExcludeGenerated
private fun ClearDataScreenSyncingPreview() {
  ClearData(
    unsyncedResourceCount = 345,
    appName = "OpenSRP 2",
    isDebug = false,
    onSyncData = {},
    onDeleteData = {},
    isSyncing = true,
  )
}
