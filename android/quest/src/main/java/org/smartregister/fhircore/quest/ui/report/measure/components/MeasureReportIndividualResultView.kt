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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.ui.theme.SubtitleTextColor
import org.smartregister.fhircore.engine.util.annotation.PreviewWithBackgroundExcludeGenerated
import org.smartregister.fhircore.quest.ui.shared.models.MeasureReportSubjectViewData

const val PERSONAL_DETAILS_TEST_TAG = "personalDetailsTestTag"
const val INDIVIDUAL_RESULT_VIEW_CONTAINER_TEST_TAG = "individualResultViewContainer"
const val RESULT_VIEW_CHECK_ICON = "resultViewCheckIcon"
const val RESULT_VIEW_STALLED_ICON = "resultViewStalledIcon"
const val RESULT_VIEW_INDICATOR_STATUS = "resultViewIndicatorStatus"
const val RESULT_VIEW_INDICATOR_DESCRIPTION = "resultViewIndicatorDescription"

@Composable
fun MeasureReportIndividualResultView(
  modifier: Modifier = Modifier,
  subjectViewData: MeasureReportSubjectViewData,
) {
  Box(
    modifier =
      modifier
        .clip(RoundedCornerShape(15.dp))
        .background(color = colorResource(id = org.smartregister.fhircore.engine.R.color.white))
        .wrapContentWidth()
        .testTag(INDIVIDUAL_RESULT_VIEW_CONTAINER_TEST_TAG),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier = Modifier.wrapContentWidth().padding(16.dp),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.Start,
    ) {
      Text(
        color = SubtitleTextColor,
        text = subjectViewData.display,
        fontSize = 16.sp,
        modifier = Modifier.wrapContentWidth().testTag(PERSONAL_DETAILS_TEST_TAG),
      )
    }
  }
}

@PreviewWithBackgroundExcludeGenerated
@Composable
private fun MeasureReportIndividualResultViewPreview() {
  MeasureReportIndividualResultView(
    subjectViewData =
      MeasureReportSubjectViewData(
        display = "Jacky Coughlin, F, 27",
        logicalId = "12444",
        type = ResourceType.Patient,
      ),
  )
}
