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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.smartregister.fhircore.engine.ui.components.CircularPercentageIndicator
import org.smartregister.fhircore.engine.ui.theme.DividerColor
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.ui.report.measure.models.MeasureReportIndividualResult
import org.smartregister.fhircore.quest.ui.report.measure.models.MeasureReportPopulationResult

@Composable
fun MeasureReportPopulationResultView(dataList: List<MeasureReportPopulationResult>) {
  LazyColumn { items(dataList, key = { it.title }) { item -> PopulationResultCard(item) } }
}

@Composable
private fun PopulationResultCard(
  resultItem: MeasureReportPopulationResult,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.padding(top = 12.dp)) {
    Box(
      modifier =
        modifier
          .clip(RoundedCornerShape(8.dp))
          .background(color = colorResource(id = R.color.white))
          .padding(16.dp)
          .fillMaxWidth()
    ) {
      Column {
        Row(modifier = modifier.fillMaxWidth()) {
          Text(
            text = resultItem.title.uppercase(),
            color = colorResource(id = R.color.black),
            fontSize = 16.sp,
            modifier = modifier.weight(1.0f),
            textAlign = TextAlign.Start
          )
          Text(
            text = resultItem.count.toString().uppercase(),
            color = colorResource(id = R.color.black),
            fontSize = 16.sp,
            modifier = modifier.weight(1.0f),
            textAlign = TextAlign.End
          )
        }
        if (resultItem.dataList.isNotEmpty()) {
          Divider(color = DividerColor, modifier = modifier.padding(vertical = 20.dp))
          resultItem.dataList.forEach { item -> PopulationResultItem(item) }
        }
      }
    }
  }
}

@Composable
private fun PopulationResultItem(
  measureReportIndividualResult: MeasureReportIndividualResult,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Row(
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      CircularPercentageIndicator(percentage = measureReportIndividualResult.percentage)

      Text(
        text = measureReportIndividualResult.title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier.wrapContentWidth().padding(horizontal = 20.dp),
      )
    }

    Column(
      modifier = modifier.wrapContentWidth(),
      verticalArrangement = Arrangement.SpaceBetween,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = "${measureReportIndividualResult.percentage}%",
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        modifier = modifier.wrapContentWidth()
      )
      Text(
        text = measureReportIndividualResult.count,
        fontSize = 16.sp,
        color = colorResource(id = R.color.darkGrayText),
        modifier = modifier.wrapContentWidth()
      )
    }
  }
}
