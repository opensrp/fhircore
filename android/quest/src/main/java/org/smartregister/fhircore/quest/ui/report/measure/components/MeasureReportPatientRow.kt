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
import org.smartregister.fhircore.engine.ui.theme.SubtitleTextColor
import org.smartregister.fhircore.engine.util.annotation.PreviewWithBackgroundExcludeGenerated
import org.smartregister.fhircore.quest.ui.shared.models.MeasureReportPatientViewData

const val PATIENT_DETAILS_TEST_TAG = "patientDetailsTestTag"
const val FAMILY_NAME_TEST_TAG = "familyNameTestTag"
const val PATIENT_ROW_TEST_TAG = "patientRowTestTag"

@Composable
fun MeasureReportPatientRow(
  measureReportPatientViewData: MeasureReportPatientViewData,
  onRowClick: (MeasureReportPatientViewData) -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
    modifier =
      modifier
        .fillMaxWidth()
        .height(IntrinsicSize.Min)
        .clickable { onRowClick(measureReportPatientViewData) }
        .testTag(PATIENT_ROW_TEST_TAG)
  ) {
    Column(
      modifier =
        modifier.wrapContentWidth(Alignment.Start).padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
      Text(
        text =
          listOf(
              measureReportPatientViewData.name,
              measureReportPatientViewData.gender,
              measureReportPatientViewData.age
            )
            .joinToString(", "),
        fontSize = 18.sp,
        modifier = modifier.wrapContentWidth().testTag(PATIENT_DETAILS_TEST_TAG),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Spacer(modifier = modifier.height(8.dp))
      if (measureReportPatientViewData.family != null) {
        Text(
          color = SubtitleTextColor,
          text = measureReportPatientViewData.family,
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
private fun MeasureReportPatientRowPreview() {
  MeasureReportPatientRow(
    measureReportPatientViewData =
      MeasureReportPatientViewData(
        logicalId = "1291029",
        name = "John Jared",
        gender = "M",
        age = "56",
        family = "Oduor"
      ),
    onRowClick = {}
  )
}
