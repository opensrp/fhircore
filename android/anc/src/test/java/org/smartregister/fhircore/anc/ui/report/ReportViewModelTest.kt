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

import androidx.test.core.app.ApplicationProvider
import io.mockk.mockk
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.robolectric.Robolectric
import org.smartregister.fhircore.anc.data.report.ReportRepository
import org.smartregister.fhircore.anc.data.report.model.ReportItem
import org.smartregister.fhircore.anc.robolectric.RobolectricTest

class ReportViewModelTest : RobolectricTest() {

  private lateinit var repository: ReportRepository
  private lateinit var viewModel: ReportViewModel
  private val testReportItem = ReportItem(title = "TestReportItem")

  @Before
  fun setUp() {
    repository = mockk()
    viewModel =
      ReportViewModel.get(
        Robolectric.buildActivity(ReportHomeActivity::class.java).get(),
        ApplicationProvider.getApplicationContext(),
        repository
      )
  }

  @Test
  fun testShouldVerifyBackClickListener() {
    viewModel.onBackPress()
    Assert.assertEquals(true, viewModel.backPress.value)
  }

  @Test
  fun testShouldVerifyDatePickerPressListener() {
    viewModel.onDateRangePress()
    Assert.assertEquals(true, viewModel.showDatePicker.value)
  }

  @Test
  fun testShouldVerifyBackFromFilterClickListener() {
    viewModel.onBackPressFromFilter()
    Assert.assertEquals(ReportViewModel.ReportScreen.HOME, viewModel.reportState.currentScreen)
  }

  @Test
  fun testShouldVerifyBackFromResultClickListener() {
    viewModel.onBackPressFromResult()
    Assert.assertEquals(ReportViewModel.ReportScreen.FILTER, viewModel.reportState.currentScreen)
  }

  @Test
  fun testShouldVerifyReportItemClickListener() {
    val expectedReportItem = testReportItem
    viewModel.onReportMeasureItemClicked(testReportItem)
    Assert.assertEquals(expectedReportItem, viewModel.getSelectedReport())
    Assert.assertEquals(ReportViewModel.ReportScreen.FILTER, viewModel.reportState.currentScreen)
  }

  @Test
  fun testShouldVerifyPatientSelectionChanged() {
    val expectedSelection = ReportViewModel.PatientSelectionType.ALL
    viewModel.onPatientSelectionTypeChanged("All")
    Assert.assertEquals(expectedSelection, viewModel.patientSelectionType.value)
  }

  @Test
  fun testShouldVerifyGenerateReportClickListener() {
    viewModel.onGenerateReportPress()
    Assert.assertEquals(ReportViewModel.ReportScreen.RESULT, viewModel.reportState.currentScreen)
  }

  @Test
  fun testShouldVerifyDateRangeSelected() {
    //  2021-11-11 16:04:43.212 E/aw: onDatePicked-> start=1637798400000 end=1639094400000
    //  25 Nov, 2021  -  10 Dec, 2021
    val expectedStartDate = "25 Nov, 2021"
    val expectedEndDate = "10 Dec, 2021"
    val dateSelection = androidx.core.util.Pair(1637798400000, 1639094400000)
    viewModel.onDateSelected(dateSelection)
    Assert.assertEquals(expectedStartDate, viewModel.startDate.value)
    Assert.assertEquals(expectedEndDate, viewModel.endDate.value)
    Assert.assertEquals(true, viewModel.isReadyToGenerateReport.value)
    Assert.assertEquals(ReportViewModel.ReportScreen.FILTER, viewModel.reportState.currentScreen)
  }
}
