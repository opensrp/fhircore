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

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.smartregister.fhircore.engine.ui.theme.DividerColor
import org.smartregister.fhircore.engine.ui.theme.SubtitleTextColor
import org.smartregister.fhircore.engine.util.annotation.ExcludeFromJacocoGeneratedReport
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.ui.shared.models.MeasureReportPatientViewData

const val PERSONAL_DETAILS_TEST_TAG = "personalDetailsTestTag"
const val RESULT_VIEW_CHECK_ICON = "resultViewCheckIcon"
const val RESULT_VIEW_STALLED_ICON = "resultViewStalledIcon"
const val RESULT_VIEW_INDICATOR_STATUS = "resultViewIndicatorStatus"
const val RESULT_VIEW_INDICATOR_DESCRIPTION = "resultViewIndicatorDescription"

@Composable
fun MeasureReportIndividualResultView(
  modifier: Modifier = Modifier,
  patientViewData: MeasureReportPatientViewData,
  isMatchedIndicator: Boolean = true,
  indicatorStatus: String = "",
  indicatorDescription: String = ""
) {
  Box(
    modifier =
      modifier
        .clip(RoundedCornerShape(15.dp))
        .background(color = colorResource(id = R.color.white))
        .wrapContentWidth(),
    contentAlignment = Alignment.Center
  ) {
    Column(
      modifier = Modifier.wrapContentWidth().padding(16.dp),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.Start
    ) {
      Text(
        color = SubtitleTextColor,
        text = patientViewData.personalDetails(),
        fontSize = 16.sp,
        modifier = Modifier.wrapContentWidth().testTag(PERSONAL_DETAILS_TEST_TAG)
      )
      Spacer(modifier = Modifier.height(12.dp))
      Divider(color = DividerColor)
      Spacer(modifier = Modifier.height(12.dp))
      Row(
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (isMatchedIndicator) {
          Image(
            painter = painterResource(id = R.drawable.ic_check),
            contentDescription = null,
            modifier =
              modifier.wrapContentWidth().requiredHeight(40.dp).testTag(RESULT_VIEW_CHECK_ICON)
          )
        } else {
          Image(
            painter = painterResource(id = R.drawable.ic_stalled),
            contentDescription = null,
            modifier =
              modifier.wrapContentWidth().requiredHeight(40.dp).testTag(RESULT_VIEW_STALLED_ICON)
          )
        }
        Column(
          modifier = Modifier.wrapContentWidth().padding(horizontal = 16.dp, vertical = 4.dp),
          verticalArrangement = Arrangement.Center,
          horizontalAlignment = Alignment.Start
        ) {
          Text(
            text = indicatorStatus,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.wrapContentWidth().testTag(RESULT_VIEW_INDICATOR_STATUS)
          )
          Spacer(modifier = Modifier.height(4.dp))
          if (indicatorDescription.isNotEmpty()) {
            Text(
              color = SubtitleTextColor,
              text = indicatorDescription,
              fontSize = 14.sp,
              modifier = modifier.wrapContentWidth().testTag(RESULT_VIEW_INDICATOR_DESCRIPTION)
            )
          }
        }
      }
    }
  }
}

@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
private fun MeasureReportIndividualResultViewPreview() {
  MeasureReportIndividualResultView(
    patientViewData =
      MeasureReportPatientViewData(
        name = "Jacky Coughlin",
        gender = "F",
        age = "27",
        logicalId = "12444"
      ),
    isMatchedIndicator = true,
    indicatorStatus = "True",
    indicatorDescription = ""
  )
}
