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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.smartregister.fhircore.engine.ui.components.CircularPercentageIndicator
import org.smartregister.fhircore.engine.ui.theme.DividerColor
import org.smartregister.fhircore.engine.util.annotation.ExcludeFromJacocoGeneratedReport
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.ui.report.measure.models.MeasureReportIndividualResult
import org.smartregister.fhircore.quest.ui.report.measure.models.MeasureReportPopulationResult

const val POPULATION_TITLE_TEST_TAG = "populationTitleTestTag"
const val POPULATION_COUNT_TEST_TAG = "populationCountTestTag"
const val POPULATION_INDICATOR_TITLE = "populationIndicatorTitle"
const val POPULATION_RESULT_CARD_DIVIDER_TEST_TAG = "populationResultCardDividerTestTag"
const val POPULATION_RESULT_ITEM_PROGRESS_BAR_TEST_TAG = "populationResultItemProgressBarTestTag"
const val POPULATION_REPORT_INDIVIDUAL_RESULT_TITLE_TEST_TAG =
  "populationReportIndividualResultTitleTestTag"
const val POPULATION_REPORT_INDIVIDUAL_RESULT_PERCENTAGE_TEST_TAG =
  "populationReportIndividualResultPercentageTestTag"
const val POPULATION_REPORT_INDIVIDUAL_RESULT_COUNT_TEST_TAG =
  "populationReportIndividualResultCountTestTag"

@Composable
fun MeasureReportPopulationResultView(dataList: List<MeasureReportPopulationResult>) {
  LazyColumn { itemsIndexed(dataList) { _, item -> PopulationResultCard(item) } }
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
            text = resultItem.indicatorTitle.uppercase(),
            color = colorResource(id = R.color.black),
            fontSize = 16.sp,
            modifier = modifier.weight(1.0f).testTag(POPULATION_INDICATOR_TITLE),
            textAlign = TextAlign.Start
          )
          Text(
            text = resultItem.title.uppercase(),
            color = colorResource(id = R.color.black),
            fontSize = 16.sp,
            modifier = modifier.weight(1.0f).testTag(POPULATION_TITLE_TEST_TAG),
            textAlign = TextAlign.Start
          )
          Text(
            text = resultItem.count.toString().uppercase(),
            color = colorResource(id = R.color.black),
            fontSize = 16.sp,
            modifier = modifier.weight(1.0f).testTag(POPULATION_COUNT_TEST_TAG),
            textAlign = TextAlign.End
          )
        }
        if (resultItem.dataList.isNotEmpty()) {
          Divider(
            color = DividerColor,
            modifier =
              modifier.padding(vertical = 20.dp).testTag(POPULATION_RESULT_CARD_DIVIDER_TEST_TAG)
          )
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
      CircularPercentageIndicator(
        percentage = measureReportIndividualResult.percentage,
        modifier = modifier.testTag(POPULATION_RESULT_ITEM_PROGRESS_BAR_TEST_TAG)
      )

      Text(
        text = measureReportIndividualResult.title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        modifier =
          modifier
            .wrapContentWidth()
            .padding(horizontal = 20.dp)
            .testTag(POPULATION_REPORT_INDIVIDUAL_RESULT_TITLE_TEST_TAG),
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
        modifier =
          modifier
            .wrapContentWidth()
            .testTag(POPULATION_REPORT_INDIVIDUAL_RESULT_PERCENTAGE_TEST_TAG),
      )
      Text(
        text = measureReportIndividualResult.count,
        fontSize = 16.sp,
        color = colorResource(id = R.color.darkGrayText),
        modifier =
          modifier.wrapContentWidth().testTag(POPULATION_REPORT_INDIVIDUAL_RESULT_COUNT_TEST_TAG)
      )
    }
  }
}

@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
fun MeasureReportPopulationResultPreview() {
  val dataList =
    listOf(
      MeasureReportPopulationResult(
        title = "Population Title",
        count = "2",
        indicatorTitle = "Still birth",
        dataList =
          listOf(
            MeasureReportIndividualResult(
              status = "Test Status",
              isMatchedIndicator = false,
              description = "This is sample description",
              title = "Title Individual Result",
              percentage = "50.0",
              count = "1",
            )
          )
      )
    )
  MeasureReportPopulationResultView(dataList = dataList)
}
