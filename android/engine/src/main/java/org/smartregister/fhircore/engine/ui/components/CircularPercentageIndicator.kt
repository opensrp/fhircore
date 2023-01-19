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

package org.smartregister.fhircore.engine.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.ui.theme.ProgressBarBlueColor
import org.smartregister.fhircore.engine.util.annotation.ExcludeFromJacocoGeneratedReport
import org.smartregister.fhircore.engine.util.annotation.PreviewWithBackgroundExcludeGenerated

const val CIRCULAR_PERCENTAGE_INDICATOR = "CIRCULAR_PERCENTAGE_INDICATOR"
const val CIRCULAR_CANVAS_CIRCLE_TAG = "CIRCULAR_CANVAS_CIRCLE_TAG"
const val CIRCULAR_PERCENTAGE_TEXT_TAG = "CIRCULAR_PERCENTAGE_TEXT_TAG"

@Composable
fun CircularPercentageIndicator(
  modifier: Modifier = Modifier,
  percentage: String,
) {
  Box(
    modifier =
      Modifier.size(60.dp)
        .padding(2.dp)
        .clip(CircleShape)
        .background(Color.Transparent)
        .testTag(CIRCULAR_PERCENTAGE_INDICATOR),
    contentAlignment = Alignment.Center
  ) {
    Canvas(
      onDraw = {
        drawCircle(
          Brush.linearGradient(colors = listOf(Color.LightGray, Color.LightGray)),
          radius = size.width / 2,
          center = center,
          style = Stroke(width = size.width * 0.075f)
        )
      },
      modifier = modifier.fillMaxSize().testTag(CIRCULAR_CANVAS_CIRCLE_TAG),
    )
    CircularProgressIndicator(
      progress = percentage.toFloat() / 100,
      modifier = modifier.fillMaxSize(),
      strokeWidth = 2.4.dp,
      color = ProgressBarBlueColor
    )
    Text(
      text =
        buildAnnotatedString {
          pushStyle(SpanStyle(fontSize = 20.sp))
          append(percentage)
          pushStyle(SpanStyle(fontSize = 14.sp))
          append(stringResource(R.string.percentage))
        },
      textAlign = TextAlign.Center,
      modifier = Modifier.testTag(CIRCULAR_PERCENTAGE_TEXT_TAG)
    )
  }
}

@PreviewWithBackgroundExcludeGenerated
@Composable
fun CircularPercentageIndicatorPreview() {
  CircularPercentageIndicator(percentage = "10")
}
