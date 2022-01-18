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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.google.android.material.datepicker.MaterialDatePicker
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import org.smartregister.fhircore.anc.data.model.PatientItem
import org.smartregister.fhircore.anc.data.patient.PatientRepository
import org.smartregister.fhircore.anc.data.report.ReportRepository
import org.smartregister.fhircore.anc.data.report.model.ReportItem
import org.smartregister.fhircore.anc.data.report.model.ResultItem
import org.smartregister.fhircore.anc.data.report.model.ResultItemPopulation
import org.smartregister.fhircore.anc.ui.anccare.register.AncRowClickListenerIntent
import org.smartregister.fhircore.anc.ui.anccare.register.OpenPatientProfile
import org.smartregister.fhircore.engine.data.domain.util.PaginationUtil
import org.smartregister.fhircore.engine.ui.register.model.RegisterFilterType
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.ListenerIntent
import timber.log.Timber

@HiltViewModel
class ReportViewModel
@Inject
constructor(
  val repository: ReportRepository,
  val dispatcher: DispatcherProvider,
  val patientRepository: PatientRepository,
) : ViewModel() {

  val backPress: MutableLiveData<Boolean> = MutableLiveData(false)

  val showDatePicker: MutableLiveData<Boolean> = MutableLiveData(false)

  val alertSelectPatient: MutableLiveData<Boolean> = MutableLiveData(false)

  val isChangingStartDate: MutableLiveData<Boolean> = MutableLiveData(false)

  val selectedMeasureReportItem: MutableLiveData<ReportItem> = MutableLiveData(null)

  val selectedPatientItem: MutableLiveData<PatientItem?> = MutableLiveData(null)

  val currentReportType: MutableLiveData<String> = MutableLiveData("")

  val generateReport = MutableLiveData(false)

  val resultForPopulation: MutableLiveData<List<ResultItemPopulation>> =
    MutableLiveData(loadDummyResultForPopulation())

  val resultForIndividual: MutableLiveData<ResultItem> =
    MutableLiveData(ResultItem(status = "True", isMatchedIndicator = true))

  val dateRange: LiveData<androidx.core.util.Pair<Long, Long>>
    get() = _dateRange

  var currentScreen by mutableStateOf(ReportScreen.HOME)

  var searchTextState = mutableStateOf(TextFieldValue(""))

  private val regex = Regex(".*\\d.*")

  private val _dateRange =
    MutableLiveData(
      androidx.core.util.Pair(
        MaterialDatePicker.thisMonthInUtcMilliseconds(),
        MaterialDatePicker.todayInUtcMilliseconds()
      )
    )

  private val _startDate = MutableLiveData("")
  val startDate: LiveData<String>
    get() = _startDate

  private val _endDate = MutableLiveData("")
  val endDate: LiveData<String>
    get() = _endDate

  private val _filterValue = MutableLiveData<Pair<RegisterFilterType, Any?>>()
  val filterValue
    get() = _filterValue

  private val _isReadyToGenerateReport = MutableLiveData(false)
  val isReadyToGenerateReport: LiveData<Boolean>
    get() = _isReadyToGenerateReport

  fun onDateRangeClick() {
    showDatePicker.value = true
  }

  fun getSelectedPatient(): MutableLiveData<PatientItem?> {
    return if (selectedPatientItem.value != null) selectedPatientItem else MutableLiveData(null)
  }

  fun loadDummyResultForPopulation(): List<ResultItemPopulation>? {
    val testResultItem1 = ResultItem(title = "10 - 15 years", percentage = "10%", count = "1/10")
    val testResultItem2 = ResultItem(title = "16 - 20 years", percentage = "50%", count = "30/60")
    return listOf(
      ResultItemPopulation(title = "Age Range", listOf(testResultItem1, testResultItem2)),
      ResultItemPopulation(title = "Education Level", listOf(testResultItem1, testResultItem2))
    )
  }

  fun getReportsTypeList(): Flow<PagingData<ReportItem>> {
    return Pager(PagingConfig(pageSize = PaginationUtil.DEFAULT_PAGE_SIZE)) { repository }.flow
  }

  fun onPatientItemClicked(listenerIntent: ListenerIntent, data: PatientItem) {
    if (listenerIntent is AncRowClickListenerIntent) {
      when (listenerIntent) {
        OpenPatientProfile -> updateSelectedPatient(data)
      }
    }
  }

  fun updateSelectedPatient(patient: PatientItem) {
    selectedPatientItem.value = patient
    currentScreen = ReportScreen.FILTER
  }

  fun onReportMeasureItemClicked(item: ReportItem) {
    selectedMeasureReportItem.value = item
    currentScreen = ReportScreen.FILTER
  }

  fun onBackPress() {
    backPress.value = true
  }

  fun onBackPressFromFilter() {
    currentScreen = ReportScreen.HOME
    currentReportType.value = ""
  }

  fun onBackPressFromPatientSearch() {
    currentScreen = ReportScreen.FILTER
    currentReportType.value = ""
  }

  fun onBackPressFromResult() {
    currentScreen = ReportScreen.FILTER
    currentReportType.value = ""
  }

  // TODO Run measure evaluate and open result screen depending on user selection
  fun onGenerateReportClicked() {
    currentScreen = ReportScreen.RESULT
  }

  fun onReportTypeSelected(reportType: String, launchPatientList: Boolean = false) {
    this.currentReportType.value = reportType
    if (launchPatientList) {
      filterValue.postValue(Pair(RegisterFilterType.SEARCH_FILTER, ""))
      currentScreen = ReportScreen.PICK_PATIENT
    }
    // Update generate report value
    updateGenerateReport()
  }

  fun setDateRange(dateRange: androidx.core.util.Pair<Long, Long>) {
    this._dateRange.value = dateRange
    // Format displayed date e.g 16 Nov, 2020 - 29 Oct, 2021
    val simpleDateFormat = SimpleDateFormat(DATE_RANGE_DATE_FORMAT, Locale.getDefault())
    try {
      setStartEndDate(
        startDate = simpleDateFormat.format(Date(dateRange.first)),
        endDate = simpleDateFormat.format(Date(dateRange.second))
      )
    } catch (illegalArgumentException: IllegalArgumentException) {
      Timber.e(illegalArgumentException)
    }
  }

  fun setStartEndDate(startDate: String, endDate: String) {
    this._startDate.value = startDate
    this._endDate.value = endDate
    updateGenerateReport()
  }

  private fun updateGenerateReport() {
    generateReport.value =
      _startDate.value!!.matches(regex) &&
        _endDate.value!!.matches(regex) &&
        currentReportType.value!!.isNotEmpty()
  }

  enum class ReportScreen {
    HOME,
    FILTER,
    PICK_PATIENT,
    RESULT,
  }

  companion object {
    const val DATE_RANGE_DATE_FORMAT = "d MMM, yyyy"
  }
}
