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

import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.smartregister.fhircore.engine.util.annotation.PreviewWithBackgroundExcludeGenerated
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.ui.login.APP_LOGO_TAG

@Composable
fun ClearDataScreen(
  viewModel: ClearDataViewModel,
  unsyncedResourceCount: Int = 0,
  appName: String,
  onDeleteData: () -> Unit = {},
) {
  val dataCleared = viewModel.dataCleared.collectAsState()
  val isClearing by remember { mutableStateOf(false) }
  val context = LocalContext.current

  LaunchedEffect(dataCleared.value) {
    if (dataCleared.value) {
      Toast.makeText(context, "$appName's data has been cleared.", Toast.LENGTH_SHORT).show()
    }
  }

  ClearData(
    unsyncedResourceCount,
    appName,
    onDeleteData,
    isClearing,
  )
}

@Composable
fun ClearData(
  unsyncedResourceCount: Int,
  appName: String,
  onDeleteData: () -> Unit,
  isClearing: Boolean = false,
) {
  val isDeleteEnabled = unsyncedResourceCount == 0

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(text = stringResource(R.string.clear_data_title, appName)) },
      )
    },
  ) { paddingValues ->
    Column(
      modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Top,
      ) {
        Image(
          painter = painterResource(R.drawable.ic_app_logo),
          contentDescription =
            stringResource(id = org.smartregister.fhircore.engine.R.string.app_logo),
          modifier = Modifier.requiredHeight(120.dp).requiredWidth(140.dp).testTag(APP_LOGO_TAG),
        )
      }
      Spacer(modifier = Modifier.height(20.dp))
      Text(
        text =
          if (isDeleteEnabled) {
            stringResource(R.string.clear_data_all_data_synced)
          } else {
            stringResource(
              R.string.clear_data_unsynced_resource_count,
              unsyncedResourceCount,
            )
          },
        style =
          MaterialTheme.typography.h6.copy(
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colors.onSurface,
          ),
      )
      Spacer(modifier = Modifier.height(24.dp))
      Text(
        text =
          stringResource(
            R.string.clear_data_data_loss_warning,
            appName,
            appName,
          ),
        fontSize = 16.sp,
        style =
          MaterialTheme.typography.h6.copy(
            fontSize = 18.sp,
            color = MaterialTheme.colors.onSurface,
          ),
      )
      Spacer(modifier = Modifier.weight(1f))
      Button(
        onClick = onDeleteData,
        enabled = isDeleteEnabled,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().height(50.dp).padding(horizontal = 16.dp),
      ) {
        Text(
          text =
            stringResource(
              R.string.clear_data_delete_button_text,
              appName,
            ),
        )
      }

      if (isClearing) {
        Spacer(modifier = Modifier.height(24.dp))
        CircularProgressIndicator(
          modifier = Modifier.padding(top = 16.dp).size(48.dp),
        )
      }
    }
  }
}

@Composable
@PreviewWithBackgroundExcludeGenerated
private fun ClearDataScreenPreview() {
  ClearData(
    unsyncedResourceCount = 0,
    appName = "OpenSRP 2",
    onDeleteData = {},
  )
}
