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

package org.smartregister.fhircore.quest.ui.report.measure.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.util.annotation.PreviewWithBackgroundExcludeGenerated

const val MEASURE_ROW_TITLE_TEST_TAG = "measureRowTitleTestTag"
const val MEASURE_ROW_FORWARD_ARROW_TEST_TAG = "measureRowForwardArrowTestTag"
const val MEASURE_ROW_TEST_TAG = "measureRowTestTag"

@Composable
fun MeasureReportRow(title: String, onRowClick: () -> Unit, modifier: Modifier = Modifier) {
  Row(
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
    modifier =
      modifier
        .clickable { onRowClick() }
        .fillMaxWidth()
        .height(IntrinsicSize.Min)
        .testTag(MEASURE_ROW_TEST_TAG),
  ) {
    Column(modifier = modifier.padding(16.dp).weight(0.70f)) {
      Text(text = title, modifier.wrapContentWidth().testTag(MEASURE_ROW_TITLE_TEST_TAG))
      Spacer(modifier = modifier.height(8.dp))
    }
    Image(
      painter = painterResource(id = org.smartregister.fhircore.quest.R.drawable.ic_forward_arrow),
      contentDescription = "",
      colorFilter = ColorFilter.tint(colorResource(id = R.color.status_gray)),
      modifier = Modifier.padding(end = 12.dp).testTag(MEASURE_ROW_FORWARD_ARROW_TEST_TAG),
    )
  }
}

@PreviewWithBackgroundExcludeGenerated
@Composable
fun MeasureReportRowPreview() {
  MeasureReportRow(title = "Module 1- ANC Contacts ", onRowClick = {})
}
