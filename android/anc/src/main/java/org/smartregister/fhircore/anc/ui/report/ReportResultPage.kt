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

package org.smartregister.fhircore.anc.ui.report

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.data.report.model.ReportItem
import org.smartregister.fhircore.engine.ui.theme.SubtitleTextColor
import org.smartregister.fhircore.engine.util.annotation.ExcludeFromJacocoGeneratedReport

@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
fun ReportResultPreview() {
  ReportResultPage(
    topBarTitle = "PageTitle",
    onBackPress = {},
    reportMeasureItem = ReportItem(description = "Test Description")
  )
}

@Composable
fun ReportResultScreen(viewModel: ReportViewModel) {
  ReportResultPage(
    topBarTitle = stringResource(id = R.string.reports),
    onBackPress = viewModel::onBackPress,
    reportMeasureItem = viewModel.getSelectedReport()!!
  )
}

@Composable
fun ReportResultPage(topBarTitle: String, onBackPress: () -> Unit, reportMeasureItem: ReportItem) {
  Surface(color = colorResource(id = R.color.white)) {
    Column {
      TopBarBox(topBarTitle = topBarTitle, onBackPress = onBackPress)

      Text(
        text = stringResource(id = R.string.date_range),
        fontSize = 18.sp,
        modifier = Modifier.wrapContentWidth()
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        color = SubtitleTextColor,
        text = reportMeasureItem.description,
        fontSize = 14.sp,
        modifier = Modifier.wrapContentWidth()
      )
      Spacer(modifier = Modifier.height(8.dp))
      Row(
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          color = SubtitleTextColor,
          text = stringResource(id = R.string.start_date),
          fontSize = 14.sp,
          modifier = Modifier.wrapContentWidth()
        )
        Text(
          color = SubtitleTextColor,
          text = stringResource(id = R.string.end_date),
          fontSize = 14.sp,
          modifier = Modifier.wrapContentWidth()
        )
      }
    }
  }
}
