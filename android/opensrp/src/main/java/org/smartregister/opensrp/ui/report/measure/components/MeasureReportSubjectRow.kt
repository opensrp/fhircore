/*
 * Copyright 2021-2023 Ona Systems, Inc
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

package org.smartregister.opensrp.ui.report.measure.components

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.ui.theme.SubtitleTextColor
import org.smartregister.fhircore.engine.util.annotation.PreviewWithBackgroundExcludeGenerated
import org.smartregister.opensrp.ui.shared.models.MeasureReportSubjectViewData

const val SUBJECT_DETAILS_TEST_TAG = "subjectDetailsTestTag"
const val FAMILY_NAME_TEST_TAG = "familyNameTestTag"
const val SUBJECT_ROW_TEST_TAG = "subjectRowTestTag"

@Composable
fun MeasureReportSubjectRow(
  measureReportSubjectViewData: MeasureReportSubjectViewData,
  onRowClick: (MeasureReportSubjectViewData) -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
    modifier =
      modifier
        .fillMaxWidth()
        .height(IntrinsicSize.Min)
        .clickable { onRowClick(measureReportSubjectViewData) }
        .testTag(SUBJECT_ROW_TEST_TAG)
  ) {
    Column(
      modifier =
        modifier.wrapContentWidth(Alignment.Start).padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
      Text(
        text = measureReportSubjectViewData.display,
        fontSize = 18.sp,
        modifier = modifier.wrapContentWidth().testTag(SUBJECT_DETAILS_TEST_TAG),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Spacer(modifier = modifier.height(8.dp))
      if (measureReportSubjectViewData.family != null) {
        Text(
          color = SubtitleTextColor,
          text = measureReportSubjectViewData.family,
          fontSize = 14.sp,
          modifier = modifier.wrapContentWidth().testTag(FAMILY_NAME_TEST_TAG),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
  }
}

@PreviewWithBackgroundExcludeGenerated
@Composable
private fun MeasureReportSubjectRowPreview() {
  MeasureReportSubjectRow(
    measureReportSubjectViewData =
      MeasureReportSubjectViewData(
        type = ResourceType.Patient,
        logicalId = "1291029",
        display = "John Jared, M, 56",
        family = "Oduor"
      ),
    onRowClick = {}
  )
}
