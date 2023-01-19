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

package org.smartregister.fhircore.quest.ui.report.measure.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.smartregister.fhircore.engine.ui.theme.SubtitleTextColor
import org.smartregister.fhircore.engine.util.annotation.ExcludeFromJacocoGeneratedReport
import org.smartregister.fhircore.engine.util.annotation.PreviewWithBackgroundExcludeGenerated

const val DATE_RANGE_TEXT_TEST_TAG = "dateRangeTextTestTag"

@Composable
fun DateRangeItem(text: String, showBackground: Boolean = true, modifier: Modifier = Modifier) {
  Row(modifier = modifier.wrapContentWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
    Box(
      modifier =
        modifier
          .clip(RoundedCornerShape(if (showBackground) 12.dp else 0.dp))
          .background(
            color = if (showBackground) Color.LightGray.copy(alpha = 0.4f) else Color.Transparent
          )
          .wrapContentWidth()
          .padding(horizontal = 16.dp, vertical = 4.dp),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = text,
        textAlign = TextAlign.Start,
        fontSize = 16.sp,
        color = SubtitleTextColor,
        modifier = modifier.testTag(DATE_RANGE_TEXT_TEST_TAG)
      )
    }
  }
}

@PreviewWithBackgroundExcludeGenerated
@Composable
private fun DateRangeItemPreview() {
  DateRangeItem(text = "Date Range", showBackground = true)
}
