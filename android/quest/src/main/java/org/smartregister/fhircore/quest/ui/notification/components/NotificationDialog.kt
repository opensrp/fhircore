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

package org.smartregister.fhircore.quest.ui.notification.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.smartregister.fhircore.quest.R

@Composable
fun NotificationDialog(
  title: String,
  description: String,
  onDismissDialog: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Dialog(
    onDismissRequest = onDismissDialog,
    content = {
      Column(Modifier.background(Color.White)) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          modifier = Modifier.fillMaxWidth().align(Alignment.CenterHorizontally).padding(20.dp),
        )
        Column(
          modifier =
            Modifier.verticalScroll(rememberScrollState())
              .weight(1f, false)
              .fillMaxWidth()
              .padding(horizontal = 20.dp, vertical = 10.dp),
        ) {
          Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
          )
        }
        Row(
          modifier = modifier.fillMaxWidth().padding(20.dp),
          horizontalArrangement = Arrangement.End,
        ) {
          Text(
            text = stringResource(org.smartregister.fhircore.engine.R.string.ok),
            modifier = modifier.padding(horizontal = 10.dp).clickable { onDismissDialog() },
          )
        }
      }
    },
  )
}

@Preview
@Composable
fun PreviewNotificationDialog() {
  NotificationDialog(
    title = "Notification Title",
    description = "Notification Description",
    onDismissDialog = { /*TODO*/},
  )
}
