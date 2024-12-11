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

package org.smartregister.fhircore.engine.ui.components.register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.ui.components.LineSpinFadeLoaderProgressIndicator
import org.smartregister.fhircore.engine.util.annotation.PreviewWithBackgroundExcludeGenerated

const val LOADER_DIALOG_PROGRESS_BAR_TAG = "loaderDialogProgressBarTag"
const val LOADER_DIALOG_PROGRESS_MSG_TAG = "loaderDialogProgressMsgTag"

@Composable
fun LoaderDialog(
  modifier: Modifier = Modifier,
  dialogMessage: String? = null,
  percentageProgressFlow: Flow<Int> = flowOf(0),
  showPercentageProgress: Boolean = false,
  boxWidth: Dp = 240.dp,
  boxHeight: Dp = 180.dp,
  progressBarSize: Dp = 40.dp,
  showBackground: Boolean = true,
  showLineSpinIndicator: Boolean = false,
  showOverlay: Boolean = true,
  alignment: Alignment = Alignment.Center,
) {
  val currentPercentage = percentageProgressFlow.collectAsState(0).value

  if (showOverlay) {
    Dialog(onDismissRequest = {}, properties = DialogProperties(dismissOnBackPress = false)) {
      LoaderContent(
        modifier = modifier,
        dialogMessage = dialogMessage,
        currentPercentage = currentPercentage,
        showPercentageProgress = showPercentageProgress,
        boxWidth = boxWidth,
        boxHeight = boxHeight,
        progressBarSize = progressBarSize,
        showBackground = showBackground,
        showLineSpinIndicator = showLineSpinIndicator,
      )
    }
  } else {
    Box(
      modifier = modifier.wrapContentSize(),
      contentAlignment = alignment,
    ) {
      LoaderContent(
        modifier = modifier,
        dialogMessage = dialogMessage,
        currentPercentage = currentPercentage,
        showPercentageProgress = showPercentageProgress,
        boxWidth = boxWidth,
        boxHeight = boxHeight,
        progressBarSize = progressBarSize,
        showBackground = showBackground,
        showLineSpinIndicator = showLineSpinIndicator,
      )
    }
  }
}

@Composable
private fun LoaderContent(
  modifier: Modifier,
  dialogMessage: String?,
  currentPercentage: Int,
  showPercentageProgress: Boolean,
  boxWidth: Dp,
  boxHeight: Dp,
  progressBarSize: Dp,
  showBackground: Boolean,
  showLineSpinIndicator: Boolean,
) {
  val openDialog = remember { mutableStateOf(true) }
  if (openDialog.value) {
    Box(modifier.size(boxWidth, boxHeight)) {
      Column(
        modifier = modifier.padding(8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Surface(
          modifier = modifier.size(boxWidth, boxHeight),
          shape = RoundedCornerShape(8.dp),
          color = if (showBackground) Color.Black.copy(alpha = 0.56f) else Color.Transparent,
        ) {
          Column(
            modifier = modifier.padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            if (showLineSpinIndicator) {
              LineSpinFadeLoaderProgressIndicator(
                color = Color.White,
                lineLength = 12f,
                innerRadius = 16f,
              )
            } else {
              CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 3.dp,
                modifier = modifier.testTag(LOADER_DIALOG_PROGRESS_BAR_TAG).size(progressBarSize),
              )
            }

            dialogMessage?.let {
              Text(
                text = it,
                color = Color.White,
                fontSize = 14.sp,
                modifier = modifier.testTag(LOADER_DIALOG_PROGRESS_MSG_TAG).padding(top = 8.dp),
              )
            }

            if (showPercentageProgress) {
              Text(
                fontSize = 15.sp,
                color = Color.White,
                text = "$currentPercentage%",
                modifier = modifier.padding(top = 4.dp),
              )
            }
          }
        }
      }
    }
  }
  SideEffect {
    if (currentPercentage >= 100) {
      openDialog.value = false
    }
  }
}

@PreviewWithBackgroundExcludeGenerated
@Composable
fun LoaderDialogPreview() {
  LoaderDialog(dialogMessage = stringResource(id = R.string.syncing))
}

@PreviewWithBackgroundExcludeGenerated
@Composable
fun LoaderDialogPreviewTest() {
  LoaderDialog(
    boxWidth = 50.dp,
    boxHeight = 50.dp,
    progressBarSize = 25.dp,
    showBackground = false,
    showLineSpinIndicator = true,
    showOverlay = false,
  )
}
