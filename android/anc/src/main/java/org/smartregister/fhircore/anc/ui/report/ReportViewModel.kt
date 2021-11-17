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
import androidx.core.util.Pair
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import ca.uhn.fhir.parser.IParser
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.smartregister.fhircore.anc.data.report.ReportRepository
import org.smartregister.fhircore.anc.data.report.model.ReportItem
import org.smartregister.fhircore.engine.data.domain.util.PaginationUtil
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.util.DispatcherProvider

class ReportViewModel(
  private val repository: ReportRepository,
  var dispatcher: DispatcherProvider,
) : ViewModel() {

  val backPress: MutableLiveData<Boolean> = MutableLiveData(false)
  val showDatePicker: MutableLiveData<Boolean> = MutableLiveData(false)
  val selectedMeasureReportItem: MutableLiveData<ReportItem> = MutableLiveData(null)
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

  fun onReportMeasureItemClicked(item: ReportItem) {
    selectedMeasureReportItem.value = item
    reportState.currentScreen = ReportScreen.FILTER
  }

  fun getSelectedReport(): ReportItem? {
    return selectedMeasureReportItem.value
  }

  fun getPatientSelectionType(): String? {
    return patientSelectionType.value
  }

  fun onBackPress() {
    backPress.value = true
  }

  fun fetchCQLLibraryData(
    parser: IParser,
    fhirResourceDataSource: FhirResourceDataSource,
    libraryURL: String
  ): LiveData<String> {
    val libraryData = MutableLiveData<String>()
    viewModelScope.launch(dispatcher.io()) {
      val auxCQLLibraryData =
        parser.encodeResourceToString(fhirResourceDataSource.loadData(libraryURL).entry[0].resource)
      libraryData.postValue(auxCQLLibraryData)
    }
    return libraryData
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

  fun fetchCQLFhirHelperData(
    parser: IParser,
    fhirResourceDataSource: FhirResourceDataSource,
    helperURL: String
  ): LiveData<String> {
    val helperData = MutableLiveData<String>()
    viewModelScope.launch(dispatcher.io()) {
      val auxCQLHelperData =
        parser.encodeResourceToString(fhirResourceDataSource.loadData(helperURL).entry[0].resource)
      helperData.postValue(auxCQLHelperData)
    }
    return helperData
  }

  fun fetchCQLValueSetData(
    parser: IParser,
    fhirResourceDataSource: FhirResourceDataSource,
    valueSetURL: String
  ): LiveData<String> {
    val valueSetData = MutableLiveData<String>()
    viewModelScope.launch(dispatcher.io()) {
      val auxCQLValueSetData =
        parser.encodeResourceToString(fhirResourceDataSource.loadData(valueSetURL))
      valueSetData.postValue(auxCQLValueSetData)
    }
    return valueSetData
  }

  fun fetchCQLPatientData(
    parser: IParser,
    fhirResourceDataSource: FhirResourceDataSource,
    patientURL: String
  ): LiveData<String> {
    val patientData = MutableLiveData<String>()
    viewModelScope.launch(dispatcher.io()) {
      val auxCQLPatientData =
        parser.encodeResourceToString(fhirResourceDataSource.loadData(patientURL))
      patientData.postValue(auxCQLPatientData)
    }
    return patientData
  }

  fun fetchCQLMeasureEvaluateLibraryAndValueSets(
    parser: IParser,
    fhirResourceDataSource: FhirResourceDataSource,
    libAndValueSetURL: String,
    measureURL: String,
    cqlMeasureReportLibInitialString: String
  ): LiveData<String> {
    val valueSetData = MutableLiveData<String>()
    val equalsIndexUrl: Int = libAndValueSetURL.indexOf("=")

    val libStrAfterEquals = libAndValueSetURL.substring(libAndValueSetURL.lastIndexOf("=") + 1)
    val libList = libStrAfterEquals.split(",").map { it.trim() }

    val libURLStrBeforeEquals = libAndValueSetURL.substring(0, equalsIndexUrl) + "="
    val fullResourceString = StringBuilder(cqlMeasureReportLibInitialString)

    viewModelScope.launch(dispatcher.io()) {
      val measureObject =
        parser.encodeResourceToString(fhirResourceDataSource.loadData(measureURL).entry[0].resource)

      fullResourceString.append("{\"resource\":")
      fullResourceString.append(measureObject)
      fullResourceString.append("}")

      var auxCQLValueSetData: String
      for (lib in libList) {
        auxCQLValueSetData =
          parser.encodeResourceToString(
            fhirResourceDataSource.loadData(libURLStrBeforeEquals + lib).entry[0].resource
          )

        fullResourceString.append(",")
        fullResourceString.append("{\"resource\":")
        fullResourceString.append(auxCQLValueSetData)
        fullResourceString.append("}")
      }
      fullResourceString.deleteCharAt(fullResourceString.length - 1)
      fullResourceString.append("}]}")
      valueSetData.postValue(fullResourceString.toString())
    }
    return valueSetData
  }

  class ReportState {
    var currentScreen by mutableStateOf(ReportScreen.HOME)
  }

  enum class ReportScreen {
    HOME,
    FILTER,
    PICK_PATIENT,
    RESULT,
    PREHOMElOADING
  }

  object PatientSelectionType {
    const val ALL = "All"
    const val INDIVIDUAL = "Individual"
  }
}
