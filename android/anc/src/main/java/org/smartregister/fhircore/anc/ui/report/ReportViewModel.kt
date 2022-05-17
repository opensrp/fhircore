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

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.workflow.FhirOperator
import com.google.android.material.datepicker.MaterialDatePicker
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.math.ceil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.MeasureReport
import org.smartregister.fhircore.anc.data.model.PatientItem
import org.smartregister.fhircore.anc.data.patient.PatientRepository
import org.smartregister.fhircore.anc.data.report.ReportRepository
import org.smartregister.fhircore.anc.data.report.model.ReportItem
import org.smartregister.fhircore.anc.data.report.model.ResultItem
import org.smartregister.fhircore.anc.data.report.model.ResultItemPopulation
import org.smartregister.fhircore.anc.ui.anccare.register.AncRowClickListenerIntent
import org.smartregister.fhircore.anc.ui.anccare.register.OpenPatientProfile
import org.smartregister.fhircore.engine.data.remote.model.response.UserInfo
import org.smartregister.fhircore.engine.domain.util.PaginationConstant
import org.smartregister.fhircore.engine.ui.register.model.RegisterFilterType
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.ListenerIntent
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.USER_INFO_SHARED_PREFERENCE_KEY
import org.smartregister.fhircore.engine.util.extension.decodeJson
import org.smartregister.fhircore.engine.util.extension.loadCqlLibraryBundle

@HiltViewModel
class ReportViewModel
@Inject
constructor(
  val repository: ReportRepository,
  val dispatcher: DispatcherProvider,
  val patientRepository: PatientRepository,
  val fhirEngine: FhirEngine,
  val fhirOperator: FhirOperator,
  val sharedPreferencesHelper: SharedPreferencesHelper
) : ViewModel() {

  val backPress: MutableLiveData<Boolean> = MutableLiveData(false)

  val onGenerateReportClicked: MutableLiveData<Boolean> = MutableLiveData(false)

  val showDatePicker: MutableLiveData<Boolean> = MutableLiveData(false)

  val showProgressIndicator: MutableLiveData<Boolean> = MutableLiveData(false)

  val selectedMeasureReportItem: MutableLiveData<ReportItem> = MutableLiveData(null)

  val selectedPatientItem: MutableLiveData<PatientItem?> = MutableLiveData(null)

  val currentReportType: MutableLiveData<String> = MutableLiveData("")

  val generateReport = MutableLiveData(false)

  val resultForPopulation: MutableLiveData<List<ResultItemPopulation>?> = MutableLiveData(null)

  val resultForIndividual: MutableLiveData<ResultItem?> = MutableLiveData(null)

  var currentScreen by mutableStateOf(ReportScreen.HOME)

  var searchTextState = mutableStateOf(TextFieldValue(""))

  private val regex = Regex(".*\\d.*")

  private val dateRangeDateFormatter = SimpleDateFormat(DATE_RANGE_DATE_FORMAT, Locale.getDefault())

  private val measureReportDateFormatter =
    SimpleDateFormat(MEASURE_REPORT_DATE_FORMAT, Locale.getDefault())

  private val authenticatedUserInfo by lazy {
    sharedPreferencesHelper.read(USER_INFO_SHARED_PREFERENCE_KEY, null)?.decodeJson<UserInfo>()
  }

  private val _dateRange =
    MutableLiveData(
      androidx.core.util.Pair(
        MaterialDatePicker.thisMonthInUtcMilliseconds(),
        MaterialDatePicker.todayInUtcMilliseconds()
      )
    )
  val dateRange: LiveData<androidx.core.util.Pair<Long, Long>>
    get() = _dateRange

  private val _startDate = MutableLiveData("")
  val startDate: LiveData<String>
    get() = _startDate

  private val _endDate = MutableLiveData("")
  val endDate: LiveData<String>
    get() = _endDate

  private val _filterValue = MutableLiveData<Pair<RegisterFilterType, Any?>>()
  val filterValue
    get() = _filterValue

  fun onDateRangeClick() {
    showDatePicker.value = true
  }

  fun getSelectedPatient(): MutableLiveData<PatientItem?> =
    if (selectedPatientItem.value != null) selectedPatientItem else MutableLiveData(null)

  fun getReportsTypeList(): Flow<PagingData<ReportItem>> =
    Pager(PagingConfig(pageSize = PaginationConstant.DEFAULT_PAGE_SIZE)) { repository }.flow

  fun onPatientItemClicked(listenerIntent: ListenerIntent, data: PatientItem) {
    if (listenerIntent is AncRowClickListenerIntent) {
      if (listenerIntent == OpenPatientProfile) updateSelectedPatient(data)
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
    resetValues()
  }

  fun onBackPress(reportScreen: ReportScreen) {
    currentScreen = reportScreen
    resetValues()
    updateGenerateReport()
  }

  fun resetValues() {
    currentReportType.value = ""
    selectedPatientItem.value = null
    resultForIndividual.value = null
    resultForPopulation.value = null
  }

  fun onGenerateReportClicked() {
    onGenerateReportClicked.value = true
  }

  fun evaluateMeasure(
    context: Context,
    measureUrl: String,
    individualEvaluation: Boolean,
    measureResourceBundleUrl: String
  ) {
    viewModelScope.launch {
      val startDateFormatted =
        measureReportDateFormatter.format(dateRangeDateFormatter.parse(startDate.value!!)!!)
      val endDateFormatted =
        measureReportDateFormatter.format(dateRangeDateFormatter.parse(endDate.value!!)!!)
      showProgressIndicator.value = true

      withContext(dispatcher.io()) {
        fhirEngine.loadCqlLibraryBundle(
          context = context,
          fhirOperator = fhirOperator,
          sharedPreferencesHelper = sharedPreferencesHelper,
          resourcesBundlePath = measureResourceBundleUrl
        )
      }

      if (selectedPatientItem.value != null && individualEvaluation) {
        val measureReport =
          withContext(dispatcher.io()) {
            fhirOperator.evaluateMeasure(
              measureUrl = measureUrl,
              start = startDateFormatted,
              end = endDateFormatted,
              reportType = SUBJECT,
              subject = selectedPatientItem.value!!.patientIdentifier,
              practitioner = authenticatedUserInfo?.keyclockuuid!!,
              lastReceivedOn = null // Non-null value not supported yet
            )
          }

        if (measureReport.type == MeasureReport.MeasureReportType.INDIVIDUAL) {
          val population: MeasureReport.MeasureReportGroupPopulationComponent? =
            measureReport.group.first().population.find { it.id == NUMERATOR }
          resultForIndividual.postValue(
            ResultItem(status = if (population != null && population.count > 0) "True" else "False")
          )
          showProgressIndicator.postValue(false)
          currentScreen = ReportScreen.RESULT
        }
      } else if (selectedPatientItem.value == null && !individualEvaluation) {
        val measureReport =
          withContext(dispatcher.io()) {
            fhirOperator.evaluateMeasure(
              measureUrl = measureUrl,
              start = startDateFormatted,
              end = endDateFormatted,
              reportType = POPULATION,
              subject = null,
              practitioner = authenticatedUserInfo?.keyclockuuid!!,
              lastReceivedOn = null // Non-null value not supported yet
            )
          }

        resultForPopulation.postValue(formatPopulationMeasureReport(measureReport))
        showProgressIndicator.postValue(false)
        currentScreen = ReportScreen.RESULT
      }
    }
  }

  fun formatPopulationMeasureReport(measureReport: MeasureReport): List<ResultItemPopulation> {
    return measureReport.group.flatMap { reportGroup: MeasureReport.MeasureReportGroupComponent ->
      reportGroup.stratifier.map { stratifier ->
        val resultItems: List<ResultItem> =
          stratifier.stratum.filter { it.hasValue() }.map { stratifierComponent ->
            val text =
              when {
                stratifierComponent.value.hasText() -> stratifierComponent.value.text
                stratifierComponent.value.hasCoding() ->
                  stratifierComponent.value.coding.last().display
                else -> ""
              }

            val numerator = stratifierComponent.findPopulation(NUMERATOR)?.count ?: 0
            val denominator = stratifierComponent.findPopulation(DENOMINATOR)?.count ?: 0

            val percentage =
              ceil((numerator / if (denominator == 0) 1 else denominator) * 100.0).toInt()
            val count = "$numerator/$denominator"
            ResultItem(title = text, percentage = percentage.toString(), count = count)
          }
        ResultItemPopulation(title = stratifier.id.replaceDashes(), dataList = resultItems)
      }
    }
  }

  private fun MeasureReport.StratifierGroupComponent.findPopulation(
    id: String
  ): MeasureReport.StratifierGroupPopulationComponent? {
    return this.population.find { it.hasId() && it.id.equals(id, ignoreCase = true) }
  }

  private fun String.replaceDashes() = this.replace("-", " ").uppercase(Locale.getDefault())

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
    setStartEndDate(
      startDate = dateRangeDateFormatter.format(Date(dateRange.first)),
      endDate = dateRangeDateFormatter.format(Date(dateRange.second))
    )
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

  override fun onCleared() {
    resetValues()
  }

  enum class ReportScreen {
    HOME,
    FILTER,
    PICK_PATIENT,
    RESULT,
  }

  companion object {
    const val DATE_RANGE_DATE_FORMAT = "d MMM, yyyy"
    const val MEASURE_REPORT_DATE_FORMAT = "yyyy-MM-dd"
    const val NUMERATOR = "numerator"
    const val DENOMINATOR = "denominator"
    const val INITIAL_POPULATION = "initial-population"
    const val SUBJECT = "subject"
    const val POPULATION = "population"
  }
}
