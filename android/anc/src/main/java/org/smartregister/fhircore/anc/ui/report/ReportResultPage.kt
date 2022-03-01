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

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Date
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.data.model.PatientItem
import org.smartregister.fhircore.anc.data.model.demographics
import org.smartregister.fhircore.anc.data.report.model.ReportItem
import org.smartregister.fhircore.anc.data.report.model.ResultItem
import org.smartregister.fhircore.anc.data.report.model.ResultItemPopulation
import org.smartregister.fhircore.engine.ui.components.CircularPercentageIndicator
import org.smartregister.fhircore.engine.ui.theme.DividerColor
import org.smartregister.fhircore.engine.ui.theme.SubtitleTextColor
import org.smartregister.fhircore.engine.util.annotation.ExcludeFromJacocoGeneratedReport
import org.smartregister.fhircore.engine.util.extension.plusYears

@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
fun PreviewResultItemIndividual() {
  ResultItemIndividual(
    selectedPatient =
      PatientItem(name = "Jacky Coughlin", gender = "F", birthDate = Date().plusYears(27)),
    isMatchedIndicator = true,
    indicatorStatus = "True",
    indicatorDescription = ""
  )
}

@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
fun PreviewIndividualReportResult() {
  ReportResultPage(
    topBarTitle = "First ANC",
    onBackPress = {},
    reportMeasureItem =
      ReportItem(
        description = "Description For Preview, i.e 4+ Anc women etc, 2 lines text in preview"
      ),
    startDate = "25 Nov, 2021",
    endDate = "29 Nov, 2021",
    selectedPatient =
      PatientItem(name = "Jacky Coughlin", gender = "F", birthDate = Date().plusYears(28)),
    ResultItem(status = "True", isMatchedIndicator = true, description = ""),
    null
  )
}

@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
fun PreviewAllPatientReportResult() {
  val testResultItem1 = ResultItem(title = "10 - 15 years", percentage = "10", count = "1/10")
  val testResultItem2 = ResultItem(title = "16 - 20 years", percentage = "50", count = "30/60")
  ReportResultPage(
    topBarTitle = "First ANC",
    onBackPress = {},
    reportMeasureItem =
      ReportItem(
        description = "Description For Preview, i.e 4+ Anc women etc, 2 lines text in preview"
      ),
    startDate = "25 Nov, 2021",
    endDate = "29 Nov, 2021",
    selectedPatient =
      PatientItem(name = "Jacky Coughlin", gender = "F", birthDate = Date().plusYears(27)),
    resultForIndividual = null,
    resultItemPopulation =
      listOf(
        ResultItemPopulation(title = "Age Range", listOf(testResultItem1, testResultItem2)),
        ResultItemPopulation(title = "Education Level", listOf(testResultItem1, testResultItem2))
      )
  )
}

@Composable
@ExcludeFromJacocoGeneratedReport
fun ReportResultScreen(viewModel: ReportViewModel) {

  val reportMeasureItem by remember { mutableStateOf(viewModel.selectedMeasureReportItem.value) }
  val selectedPatient by remember { mutableStateOf(viewModel.getSelectedPatient().value) }
  val startDate by viewModel.startDate.observeAsState("")
  val endDate by viewModel.endDate.observeAsState("")
  val resultForIndividual by viewModel.resultForIndividual.observeAsState(ResultItem())
  val resultForPopulation by viewModel.resultForPopulation.observeAsState(emptyList())

  ReportResultPage(
    topBarTitle = reportMeasureItem?.title ?: "",
    onBackPress = viewModel::onBackPress,
    reportMeasureItem = reportMeasureItem
        ?: ReportItem(title = stringResource(R.string.missing_measure_report)),
    startDate = startDate,
    endDate = endDate,
    selectedPatient = selectedPatient,
    resultForIndividual = resultForIndividual,
    resultItemPopulation = resultForPopulation
  )
}

@Composable
fun ReportResultPage(
  topBarTitle: String,
  onBackPress: (ReportViewModel.ReportScreen) -> Unit,
  reportMeasureItem: ReportItem,
  startDate: String,
  endDate: String,
  selectedPatient: PatientItem?,
  resultForIndividual: ResultItem?,
  resultItemPopulation: List<ResultItemPopulation>?
) {
  Surface(color = colorResource(id = R.color.white)) {
    Column(
      modifier =
        Modifier.background(color = colorResource(id = R.color.backgroundGray))
          .fillMaxSize()
          .testTag(REPORT_RESULT_PAGE)
    ) {
      TopBarBox(
        topBarTitle = topBarTitle,
        onBackPress = { onBackPress(ReportViewModel.ReportScreen.FILTER) }
      )
      Column(modifier = Modifier.padding(16.dp)) {
        Box(
          modifier =
            Modifier.clip(RoundedCornerShape(8.dp))
              .background(color = colorResource(id = R.color.light_gray_background))
              .padding(12.dp)
              .wrapContentWidth()
              .testTag(REPORT_RESULT_MEASURE_DESCRIPTION),
          contentAlignment = Alignment.Center
        ) {
          Text(text = reportMeasureItem.description, textAlign = TextAlign.Start, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
        DateSelectionBox(
          startDate = startDate,
          endDate = endDate,
          canChange = false,
          showDateRangePicker = false
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (resultForIndividual != null && selectedPatient != null) {
          ResultItemIndividual(
            selectedPatient = selectedPatient,
            isMatchedIndicator = resultForIndividual.isMatchedIndicator,
            indicatorStatus = resultForIndividual.status,
            indicatorDescription = resultForIndividual.description
          )
        }
        if (resultItemPopulation != null) {
          ResultForPopulation(resultItemPopulation)
        }
      }
    }
  }
}

@Composable
fun ResultItemIndividual(
  modifier: Modifier = Modifier,
  selectedPatient: PatientItem,
  isMatchedIndicator: Boolean = true,
  indicatorStatus: String = "",
  indicatorDescription: String = ""
) {
  Box(
    modifier =
      modifier
        .clip(RoundedCornerShape(15.dp))
        .background(color = colorResource(id = R.color.white))
        .wrapContentWidth()
        .testTag(REPORT_RESULT_ITEM_INDIVIDUAL),
    contentAlignment = Alignment.Center
  ) {
    Column(
      modifier = Modifier.wrapContentWidth().padding(16.dp),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.Start
    ) {
      Text(
        color = SubtitleTextColor,
        text = selectedPatient.demographics(),
        fontSize = 16.sp,
        modifier = Modifier.wrapContentWidth().testTag(REPORT_RESULT_PATIENT_DATA)
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
            contentDescription = INDICATOR_STATUS,
            modifier = modifier.wrapContentWidth().requiredHeight(40.dp)
          )
        } else {
          Image(
            painter = painterResource(id = R.drawable.ic_stalled),
            contentDescription = INDICATOR_STATUS,
            modifier = modifier.wrapContentWidth().requiredHeight(40.dp)
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
            modifier = Modifier.wrapContentWidth()
          )
          Spacer(modifier = Modifier.height(4.dp))
          if (indicatorDescription.isNotEmpty()) {
            Text(
              color = SubtitleTextColor,
              text = indicatorDescription,
              fontSize = 14.sp,
              modifier = modifier.wrapContentWidth().testTag(INDICATOR_TEXT)
            )
          }
        }
      }
    }
  }
}

@Composable
fun ResultPopulationBox(
  resultItem: ResultItemPopulation,
  modifier: Modifier = Modifier,
) {
  Column(modifier = Modifier.padding(top = 12.dp).testTag(REPORT_RESULT_POPULATION_BOX)) {
    Box(
      modifier =
        Modifier.clip(RoundedCornerShape(8.dp))
          .background(color = colorResource(id = R.color.white))
          .padding(16.dp)
          .fillMaxWidth()
    ) {
      Column {
        Text(
          text = resultItem.title.toUpperCase(Locale.current),
          color = colorResource(id = R.color.darkGrayText),
          fontSize = 16.sp,
          modifier = modifier.wrapContentWidth()
        )
        Divider(color = DividerColor, modifier = modifier.padding(vertical = 20.dp))
        resultItem.dataList.forEach { item -> ResultPopulationItem(item) }
      }
    }
  }
}

@Composable
fun ResultPopulationItem(
  resultItem: ResultItem,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier =
      Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag(REPORT_RESULT_POPULATION_ITEM),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Row(
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      CircularPercentageIndicator(percentage = resultItem.percentage)

      Text(
        text = resultItem.title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier.wrapContentWidth().padding(horizontal = 20.dp),
      )
    }

    Column(
      modifier = Modifier.wrapContentWidth(),
      verticalArrangement = Arrangement.SpaceBetween,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = "${resultItem.percentage}%",
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        modifier = modifier.wrapContentWidth()
      )
      Text(
        text = resultItem.count,
        fontSize = 16.sp,
        color = colorResource(id = R.color.darkGrayText),
        modifier = modifier.wrapContentWidth()
      )
    }
  }
}

@Composable
fun ResultForPopulation(dataList: List<ResultItemPopulation>) {
  LazyColumn(modifier = Modifier.testTag(REPORT_RESULT_POPULATION_DATA)) {
    items(dataList, key = { it.title }) { item -> ResultPopulationBox(item) }
  }
}
