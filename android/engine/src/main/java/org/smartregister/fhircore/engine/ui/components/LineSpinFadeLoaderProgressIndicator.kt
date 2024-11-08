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

package org.smartregister.fhircore.engine.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import org.smartregister.fhircore.engine.util.annotation.PreviewWithBackgroundExcludeGenerated

const val ANIMATION_LABEL = "LineSpinFadeLoaderProgressIndicator"

/**
 * A custom progress indicator that displays rotating lines in a circular pattern. Each line fades
 * in and out as it rotates, creating a smooth loading animation effect.
 *
 * @param modifier Modifier to be applied to the Canvas composable
 * @param color The color of the lines in the loading indicator
 * @param lineCount The number of lines to be displayed in the circular pattern (default is 8)
 * @param lineWidth The width/thickness of each line (default is 3f)
 * @param lineLength The length of each line (default is 8f)
 * @param innerRadius The radius of the circle on which the lines are positioned (default is 10f)
 *
 * Example usage:
 * ```
 * LineSpinFadeLoaderProgressIndicator(
 *     modifier = Modifier.size(80.dp),
 *     color = Color.Blue,
 *     lineCount = 8,
 *     lineWidth = 3f,
 *     lineLength = 8f,
 *     innerRadius = 10f
 * )
 * ```
 *
 * The animation creates a rotating effect where:
 * - All lines are visible simultaneously
 * - Each line's opacity changes based on its current position in the rotation
 * - Lines maintain fixed positions but fade in/out to create a rotation illusion
 * - The animation continuously loops with a smooth transition
 *
 * @see Canvas
 * @see rememberInfiniteTransition
 */
@Composable
fun LineSpinFadeLoaderProgressIndicator(
  modifier: Modifier = Modifier,
  color: Color = Color.Blue,
  lineCount: Int = 12,
  lineWidth: Float = 4f,
  lineLength: Float = 20f,
  innerRadius: Float = 20f,
) {
  val infiniteTransition = rememberInfiniteTransition(ANIMATION_LABEL)

  val rotationAnimation by
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = lineCount.toFloat(),
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 1000, easing = LinearEasing),
          repeatMode = RepeatMode.Restart,
        ),
      label = ANIMATION_LABEL,
    )

  Canvas(modifier = modifier.wrapContentSize()) {
    val canvasWidth = size.width
    val canvasHeight = size.height
    val centerX = canvasWidth / 2
    val centerY = canvasHeight / 2

    for (i in 0 until lineCount) {
      val angle = 2 * PI * i / lineCount
      val startX = centerX + cos(angle).toFloat() * innerRadius
      val startY = centerY + sin(angle).toFloat() * innerRadius
      val endX = centerX + cos(angle).toFloat() * (innerRadius + lineLength)
      val endY = centerY + sin(angle).toFloat() * (innerRadius + lineLength)

      // Calculate alpha based on the current rotation
      val distance = (i - rotationAnimation + lineCount) % lineCount
      val alpha =
        when {
          distance < lineCount / 2f -> 1f - (distance / (lineCount / 2f))
          else -> (distance - (lineCount / 2f)) / (lineCount / 2f)
        }

      drawLine(
        color = color.copy(alpha = alpha),
        start = androidx.compose.ui.geometry.Offset(startX, startY),
        end = androidx.compose.ui.geometry.Offset(endX, endY),
        strokeWidth = lineWidth,
        cap = StrokeCap.Round,
      )
    }
  }
}

@PreviewWithBackgroundExcludeGenerated
@Composable
fun LoadingScreen() {
  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    LineSpinFadeLoaderProgressIndicator(
      modifier = Modifier.padding(8.dp),
      color = Color.Blue,
    )

    Spacer(modifier = Modifier.height(32.dp))
  }
}
