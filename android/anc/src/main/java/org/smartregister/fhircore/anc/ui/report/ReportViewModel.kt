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

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.util.Pair
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.smartregister.fhircore.anc.AncApplication
import org.smartregister.fhircore.anc.data.model.PatientItem
import org.smartregister.fhircore.anc.data.patient.PatientRepository
import org.smartregister.fhircore.anc.data.report.ReportRepository
import org.smartregister.fhircore.anc.data.report.model.ReportItem
import org.smartregister.fhircore.anc.ui.anccare.register.AncRowClickListenerIntent
import org.smartregister.fhircore.anc.ui.anccare.register.OpenPatientProfile
import org.smartregister.fhircore.engine.data.domain.util.PaginatedDataSource
import org.smartregister.fhircore.engine.data.domain.util.PaginationUtil
import org.smartregister.fhircore.engine.util.ListenerIntent
import org.smartregister.fhircore.engine.util.extension.createFactory

class ReportViewModel(
  application: AncApplication,
  private val repository: ReportRepository,
  val patientRepository: PatientRepository
) : AndroidViewModel(application) {

  val backPress: MutableLiveData<Boolean> = MutableLiveData(false)
  val showDatePicker: MutableLiveData<Boolean> = MutableLiveData(false)
  val selectedMeasureReportItem: MutableLiveData<ReportItem> = MutableLiveData(null)
  val selectedPatientItem: MutableLiveData<PatientItem> = MutableLiveData(null)
  val simpleDateFormatPattern = "d MMM, yyyy"

  private val _startDate = MutableLiveData("start date")
  val startDate: LiveData<String>
    get() = _startDate

  private val _endDate = MutableLiveData("end date")
  val endDate: LiveData<String>
    get() = _endDate

  private val _patientSelectionType = MutableLiveData("All")
  val patientSelectionType: LiveData<String>
    get() = _patientSelectionType

  private val _isReadyToGenerateReport = MutableLiveData(true)
  val isReadyToGenerateReport: LiveData<Boolean>
    get() = _isReadyToGenerateReport

  var reportState: ReportState = ReportState()

  fun getReportsTypeList(): Flow<PagingData<ReportItem>> {
    return Pager(PagingConfig(pageSize = PaginationUtil.DEFAULT_PAGE_SIZE)) { repository }.flow
  }

  fun getPatientList(): Flow<PagingData<PatientItem>> {
    //    return Pager(PagingConfig(pageSize = PaginationUtil.DEFAULT_PAGE_SIZE)) { repository
    // }.flow
    return getPagingData(0, true)
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
    reportState.currentScreen = ReportScreen.RESULT
  }

  fun onReportMeasureItemClicked(item: ReportItem) {
    selectedMeasureReportItem.value = item
    reportState.currentScreen = ReportScreen.FILTER
  }

  fun getSelectedReport(): ReportItem? {
    return selectedMeasureReportItem.value
  }

  fun getSelectedPatient(): PatientItem {
    return selectedPatientItem.value ?: PatientItem()
  }

  fun getPatientSelectionType(): String? {
    return patientSelectionType.value
  }

  fun onBackPress() {
    backPress.value = true
  }

  fun onBackPressFromFilter() {
    reportState.currentScreen = ReportScreen.HOME
  }

  fun onBackPressFromResult() {
    reportState.currentScreen = ReportScreen.FILTER
  }

  fun onDateRangePress() {
    showDatePicker.value = true
  }

  fun onPatientSelectionTypeChanged(newType: String) {
    _patientSelectionType.value = newType
  }

  fun onGenerateReportPress() {
    reportState.currentScreen = ReportScreen.RESULT
  }

  fun onDateSelected(selection: Pair<Long, Long>?) {
    showDatePicker.value = false
    if (selection == null) {
      _isReadyToGenerateReport.value = false
      return
    }
    val startDate = Date().apply { time = selection.first }
    val endDate = Date().apply { time = selection.second }
    val formattedStartDate =
      SimpleDateFormat(simpleDateFormatPattern, Locale.getDefault()).format(startDate)
    val formattedEndDate =
      SimpleDateFormat(simpleDateFormatPattern, Locale.getDefault()).format(endDate)

    _startDate.value = formattedStartDate
    _endDate.value = formattedEndDate
    _isReadyToGenerateReport.value = true
    reportState.currentScreen = ReportScreen.FILTER
  }

  @Stable
  val allRegisterData: MutableStateFlow<Flow<PagingData<PatientItem>>> =
    MutableStateFlow(getPagingData(currentPage = 0, loadAll = true))

  private fun getPagingData(currentPage: Int, loadAll: Boolean) =
    Pager(
        config =
          PagingConfig(
            pageSize = PaginationUtil.DEFAULT_PAGE_SIZE,
            initialLoadSize = PaginationUtil.DEFAULT_INITIAL_LOAD_SIZE,
          ),
        pagingSourceFactory = {
          PaginatedDataSource(patientRepository).apply {
            this.loadAll = loadAll
            this.currentPage = currentPage
          }
        }
      )
      .flow

  companion object {
    fun get(
      owner: ViewModelStoreOwner,
      application: AncApplication,
      repository: ReportRepository,
      ancPatientRepository: PatientRepository
    ): ReportViewModel {
      return ViewModelProvider(
        owner,
        ReportViewModel(application, repository, ancPatientRepository).createFactory()
      )[ReportViewModel::class.java]
    }
  }

  class ReportState {
    var currentScreen by mutableStateOf(ReportScreen.HOME)
  }

  enum class ReportScreen {
    HOME,
    FILTER,
    PICK_PATIENT,
    RESULT
  }

  object PatientSelectionType {
    const val ALL = "All"
    const val INDIVIDUAL = "Individual"
  }
}
