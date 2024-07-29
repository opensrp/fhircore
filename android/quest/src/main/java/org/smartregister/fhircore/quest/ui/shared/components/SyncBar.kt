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

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.ui.theme.SubtitleTextColor

const val MAX_PROGRESS = 100

@Composable
fun SubsequentSyncDetailsBar(
  modifier: Modifier = Modifier,
  percentageProgressFlow: Flow<Int> = flowOf(0),
  hideExtraInformation: Boolean = true,
  onCancelButtonClick: () -> Unit,
) {
  val context = LocalContext.current
  var currentPercentage by remember { mutableIntStateOf(0) }

  LaunchedEffect(percentageProgressFlow) {
    percentageProgressFlow.collect { progress -> currentPercentage = progress }
  }

  val progress = currentPercentage.toFloat() / MAX_PROGRESS
  val backgroundColor = Color(0xFF002B4A)
  val syncBarHeight = if (hideExtraInformation) 66.dp else 24.dp

  Box(
    modifier =
      modifier
        .fillMaxWidth()
        .height(syncBarHeight)
        .animateContentSize()
        .background(backgroundColor)
        .padding(horizontal = 16.dp),
    contentAlignment = Alignment.Center,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.weight(1f),
      ) {
        if (hideExtraInformation) {
          Text(
            text = "${(progress * 100).toInt()}% ${context.getString(R.string.sync_inprogress)}",
            color = Color.White,
            fontSize = 15.sp,
            modifier = Modifier.align(Alignment.Start), // Adjust padding
          )
        }
        LinearProgressIndicator(
          progress = progress,
          color = Color(0xFF28B8F9),
          backgroundColor = Color.White,
          modifier =
            Modifier.fillMaxWidth().padding(vertical = 4.dp), // Adjust the padding as needed
        )
        if (hideExtraInformation) {
          Text(
            text = context.getString(R.string.minutes_remaining),
            color = SubtitleTextColor,
            fontSize = 13.sp,
            textAlign = TextAlign.Start,
            modifier = Modifier.align(Alignment.Start), // Adjust padding
          )
        }
      }
      if (hideExtraInformation) {
        Spacer(modifier = Modifier.width(16.dp))
        TextButton(
          onClick = onCancelButtonClick,
          colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF28B8F9)),
        ) {
          Text(text = context.getString(R.string.cancel_sync))
        }
      }
    }
  }
}
