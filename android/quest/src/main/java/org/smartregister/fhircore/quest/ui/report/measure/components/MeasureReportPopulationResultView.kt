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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.smartregister.fhircore.engine.util.annotation.PreviewWithBackgroundExcludeGenerated
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.ui.report.measure.models.MeasureReportIndividualResult
import org.smartregister.fhircore.quest.ui.report.measure.models.MeasureReportPopulationResult

const val POPULATION_RESULT_VIEW_CONTAINER_TEST_TAG = "populationResultViewContainer"

const val POPULATION_COUNT_TEST_TAG = "populationCountTestTag"
const val POPULATION_INDICATOR_TITLE = "populationIndicatorTitle"

const val DETAILS_COUNT_TEST_TAG = "detailsCountTestTag"
const val DETAILS_INDICATOR_TITLE = "detailsIndicatorTitle"

@Composable
fun MeasureReportPopulationResultView(dataList: List<MeasureReportPopulationResult>) {
  LazyColumn(modifier = Modifier.testTag(POPULATION_RESULT_VIEW_CONTAINER_TEST_TAG)) {
    itemsIndexed(dataList) { _, item -> PopulationResultCard(item) }
  }
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
          .background(color = colorResource(id = org.smartregister.fhircore.engine.R.color.white))
          .padding(16.dp)
          .fillMaxWidth(),
    ) {
      Column {
        Row(modifier = modifier.fillMaxWidth()) {
          Text(
            text = resultItem.title.ifEmpty { resultItem.indicatorTitle }.uppercase(),
            color = colorResource(id = org.smartregister.fhircore.engine.R.color.black),
            fontSize = 16.sp,
            modifier = modifier.weight(1.0f).testTag(POPULATION_INDICATOR_TITLE),
            textAlign = TextAlign.Start,
          )
          Text(
            text = resultItem.measureReportDenominator,
            color = colorResource(id = org.smartregister.fhircore.engine.R.color.black),
            fontSize = 16.sp,
            modifier = modifier.weight(1.0f).testTag(POPULATION_COUNT_TEST_TAG),
            textAlign = TextAlign.End,
          )
        }
        resultItem.dataList.forEach {
          Row(modifier = modifier.fillMaxWidth()) {
            Text(
              text = it.title,
              color = colorResource(id = org.smartregister.fhircore.engine.R.color.black),
              fontSize = 15.sp,
              modifier = modifier.weight(1.0f).testTag(DETAILS_INDICATOR_TITLE),
              textAlign = TextAlign.Start,
            )
            Text(
              text = it.count,
              color = colorResource(id = org.smartregister.fhircore.engine.R.color.black),
              fontSize = 15.sp,
              modifier = modifier.weight(1.0f).testTag(DETAILS_COUNT_TEST_TAG),
              textAlign = TextAlign.End,
            )
          }
        }
      }
    }
  }
}

@PreviewWithBackgroundExcludeGenerated
@Composable
fun MeasureReportPopulationResultPreview() {
  val dataList =
    listOf(
      MeasureReportPopulationResult(
        title = "Population Title",
        count = "2",
        measureReportDenominator = "4",
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
            ),
          ),
      ),
    )
  MeasureReportPopulationResultView(dataList = dataList)
}
