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

package org.smartregister.fhircore.engine.ui.components.register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.util.annotation.ExcludeFromJacocoGeneratedReport

const val LOADER_DIALOG_PROGRESS_BAR_TAG = "loaderDialogProgressBarTag"
const val LOADER_DIALOG_PROGRESS_MSG_TAG = "loaderDialogProgressMsgTag"

@Composable
fun LoaderDialog(
  modifier: Modifier = Modifier,
  dialogMessage: String = stringResource(id = R.string.syncing),
  syncProgressStateFlow: StateFlow<String> = MutableStateFlow("")
) {
  val openDialog = remember { mutableStateOf(true) }
  val syncProgressState = syncProgressStateFlow.collectAsState()
  val syncProgressStateText by remember { syncProgressState }

  if (openDialog.value) {
    Dialog(
      onDismissRequest = { openDialog.value = true },
      properties = DialogProperties(dismissOnBackPress = true)
    ) {
      Box(Modifier.size(240.dp, 180.dp)) {
        Column(
          modifier = modifier.padding(8.dp),
          verticalArrangement = Arrangement.Center,
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Surface(
            color = Color.Black.copy(alpha = 0.56f),
            modifier = modifier.fillMaxSize(),
            shape = RoundedCornerShape(8)
          ) {
            Column(
              verticalArrangement = Arrangement.Center,
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 3.dp,
                modifier = modifier.testTag(LOADER_DIALOG_PROGRESS_BAR_TAG).size(40.dp),
              )

              Text(
                text = dialogMessage,
                style = MaterialTheme.typography.h5,
                color = Color.White,
                modifier = modifier.testTag(LOADER_DIALOG_PROGRESS_MSG_TAG).padding(8.dp)
              )

              if (syncProgressStateText.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  color = Color.White,
                  style = MaterialTheme.typography.subtitle1,
                  text = syncProgressStateText,
                  modifier = modifier.testTag(LOADER_DIALOG_PROGRESS_MSG_TAG),
                )
              }
            }
          }
        }
      }
    }
  }
}

@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
@Composable
fun LoaderPreviewDownload() {
  LoaderDialog(syncProgressStateFlow = MutableStateFlow("75% downloaded"))
}

@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
@Composable
fun LoaderPreviewUpload() {
  LoaderDialog(syncProgressStateFlow = MutableStateFlow("25% uploaded"))
}

@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
@Composable
fun LoaderPreview() {
  LoaderDialog()
}
