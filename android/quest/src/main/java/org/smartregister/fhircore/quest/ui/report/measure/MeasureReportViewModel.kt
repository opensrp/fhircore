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

package org.smartregister.fhircore.quest.ui.report.measure

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.filter
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.MeasureReport
import org.smartregister.fhircore.engine.data.remote.model.response.UserInfo
import org.smartregister.fhircore.engine.domain.util.PaginationConstant
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.USER_INFO_SHARED_PREFERENCE_KEY
import org.smartregister.fhircore.engine.util.extension.decodeJson
import org.smartregister.fhircore.engine.util.extension.loadCqlLibraryBundle
import org.smartregister.fhircore.quest.data.report.measure.MeasureReportPatientsPagingSource
import org.smartregister.fhircore.quest.data.report.measure.MeasureReportRepository
import org.smartregister.fhircore.quest.data.report.measure.models.MeasureReportRowData
import org.smartregister.fhircore.quest.navigation.MeasureReportNavigationScreen
import org.smartregister.fhircore.quest.navigation.NavigationArg
import org.smartregister.fhircore.quest.ui.report.measure.models.MeasureReportIndividualResult
import org.smartregister.fhircore.quest.ui.report.measure.models.MeasureReportPopulationResult
import org.smartregister.fhircore.quest.ui.shared.models.MeasureReportPatientViewData
import org.smartregister.fhircore.quest.util.mappers.MeasureReportPatientViewDataMapper

@HiltViewModel
class MeasureReportViewModel
@Inject
constructor(
  val fhirEngine: FhirEngine,
  val fhirOperator: FhirOperator,
  val sharedPreferencesHelper: SharedPreferencesHelper,
  val dispatcherProvider: DefaultDispatcherProvider,
  val measureReportRepository: MeasureReportRepository,
  val measureReportPatientViewDataMapper: MeasureReportPatientViewDataMapper
) : ViewModel() {

  private val dateRangeDateFormatter = SimpleDateFormat(DATE_RANGE_DATE_FORMAT, Locale.getDefault())

  private val measureReportDateFormatter =
    SimpleDateFormat(MEASURE_REPORT_DATE_FORMAT, Locale.getDefault())

  private val authenticatedUserInfo by lazy {
    sharedPreferencesHelper.read(USER_INFO_SHARED_PREFERENCE_KEY, null)?.decodeJson<UserInfo>()
  }

  val measureReportRowData: MutableState<MeasureReportRowData?> = mutableStateOf(null)

  val measureReportIndividualResult: MutableState<MeasureReportIndividualResult?> =
    mutableStateOf(null)

  val measureReportPopulationResults: MutableState<List<MeasureReportPopulationResult>?> =
    mutableStateOf(null)

  val reportTypeState: MutableState<MeasureReport.MeasureReportType> =
    mutableStateOf(MeasureReport.MeasureReportType.SUMMARY)

  val dateRange: MutableState<androidx.core.util.Pair<Long, Long>> =
    mutableStateOf(defaultDateRangeState())

  val reportTypeSelectorUiState: MutableState<ReportTypeSelectorUiState> =
    mutableStateOf(ReportTypeSelectorUiState())

  val searchTextState: MutableState<TextFieldValue> = mutableStateOf(TextFieldValue())

  val patientsData: MutableStateFlow<Flow<PagingData<MeasureReportPatientViewData>>> =
    MutableStateFlow(emptyFlow())

  init {
    patientsData.value = retrieveAncPatients()
  }

  private fun defaultDateRangeState() =
    androidx.core.util.Pair(
      MaterialDatePicker.thisMonthInUtcMilliseconds(),
      MaterialDatePicker.todayInUtcMilliseconds()
    )

  fun reportMeasuresList(): Flow<PagingData<MeasureReportRowData>> =
    Pager(PagingConfig(pageSize = PaginationConstant.DEFAULT_PAGE_SIZE)) { measureReportRepository }
      .flow

  fun onEvent(event: MeasureReportEvent) {
    when (event) {
      is MeasureReportEvent.OnSelectMeasure -> {
        measureReportRowData.value = event.measureReportRowData
        event.navController.navigate(
          MeasureReportNavigationScreen.ReportTypeSelector.route +
            NavigationArg.bindArgumentsOf(
              Pair(NavigationArg.SCREEN_TITLE, event.measureReportRowData.title)
            )
        ) { launchSingleTop = true }
      }
      is MeasureReportEvent.GenerateReport -> evaluateMeasure(event.context, event.navController)
      is MeasureReportEvent.OnDateRangeSelected -> {
        //  Update dateRange and format start/end dates e.g 16 Nov, 2020 - 29 Oct, 2021
        dateRange.value = event.newDateRange
        reportTypeSelectorUiState.value =
          reportTypeSelectorUiState.value.copy(
            startDate = dateRangeDateFormatter.format(Date(dateRange.value.first)),
            endDate = dateRangeDateFormatter.format(Date(dateRange.value.second))
          )
      }
      is MeasureReportEvent.OnReportTypeChanged -> {
        with(event.measureReportType) {
          reportTypeState.value = this
          if (this == MeasureReport.MeasureReportType.INDIVIDUAL) {
            event.navController.navigate(MeasureReportNavigationScreen.PatientsList.route)
          } else {
            // Reset previously selected patient
            reportTypeSelectorUiState.value =
              reportTypeSelectorUiState.value.copy(patientViewData = null)
          }
        }
      }
      is MeasureReportEvent.OnPatientSelected -> {
        reportTypeSelectorUiState.value =
          reportTypeSelectorUiState.value.copy(patientViewData = event.patientViewData)
      }
      is MeasureReportEvent.OnSearchTextChanged ->
        patientsData.value =
          retrieveAncPatients().map { pagingData: PagingData<MeasureReportPatientViewData> ->
            pagingData.filter {
              it.name.contains(event.searchText, ignoreCase = true) ||
                it.logicalId.contentEquals(event.searchText, ignoreCase = true)
            }
          }
    }
  }

  private fun retrieveAncPatients(): Flow<PagingData<MeasureReportPatientViewData>> =
    Pager(
        config =
          PagingConfig(
            pageSize = PaginationConstant.DEFAULT_PAGE_SIZE,
            initialLoadSize = PaginationConstant.DEFAULT_INITIAL_LOAD_SIZE
          ),
        pagingSourceFactory = {
          MeasureReportPatientsPagingSource(
            measureReportRepository,
            measureReportPatientViewDataMapper
          )
        }
      )
      .flow

  // TODO: Enhancement - use FhirPathEngine evaluator for data extraction
  fun evaluateMeasure(context: Context, navController: NavController) {
    // Run evaluate measure only for existing report
    if (measureReportRowData.value != null) {
      val reportName = measureReportRowData.value?.name
      val measureUrl = "http://fhir.org/guides/who/anc-cds/Measure/$reportName"
      val measureResourceBundleUrl = "measure/$reportName-bundle.json"
      val individualEvaluation = reportTypeState.value == MeasureReport.MeasureReportType.INDIVIDUAL
      // Retrieve and parse dates from this format (16 Nov, 2020) to this (2020-11-16)
      val startDateFormatted =
        measureReportDateFormatter.format(
          dateRangeDateFormatter.parse(reportTypeSelectorUiState.value.startDate)!!
        )
      val endDateFormatted =
        measureReportDateFormatter.format(
          dateRangeDateFormatter.parse(reportTypeSelectorUiState.value.endDate)!!
        )

      viewModelScope.launch {
        kotlin
          .runCatching {
            // Show Progress indicator while evaluating measure
            toggleProgressIndicatorVisibility(true)

            withContext(dispatcherProvider.io()) {
              fhirEngine.loadCqlLibraryBundle(
                context = context,
                fhirOperator = fhirOperator,
                sharedPreferencesHelper = sharedPreferencesHelper,
                resourcesBundlePath = measureResourceBundleUrl
              )
            }
            if (reportTypeSelectorUiState.value.patientViewData != null && individualEvaluation) {
              val measureReport =
                withContext(dispatcherProvider.io()) {
                  fhirOperator.evaluateMeasure(
                    url = measureUrl,
                    start = startDateFormatted,
                    end = endDateFormatted,
                    reportType = SUBJECT,
                    subject = reportTypeSelectorUiState.value.patientViewData!!.logicalId,
                    practitioner = authenticatedUserInfo?.keyclockuuid!!
                  )
                }

              if (measureReport.type == MeasureReport.MeasureReportType.INDIVIDUAL) {
                val population: MeasureReport.MeasureReportGroupPopulationComponent? =
                  measureReport.group.first().population.find { it.id == NUMERATOR }
                measureReportIndividualResult.value =
                  MeasureReportIndividualResult(
                    status = if (population != null && population.count > 0) "True" else "False"
                  )
              }
            } else if (reportTypeSelectorUiState.value.patientViewData == null &&
                !individualEvaluation
            ) {
              evaluatePopulationMeasure(measureUrl, startDateFormatted, endDateFormatted)
            }
          }
          .onSuccess {
            toggleProgressIndicatorVisibility(false)
            // Show results of measure report for individual/population
            navController.navigate(MeasureReportNavigationScreen.MeasureReportResult.route) {
              launchSingleTop = true
            }
          }
          .onFailure { toggleProgressIndicatorVisibility(false) }
      }
    }
  }

  private suspend fun evaluatePopulationMeasure(
    measureUrl: String,
    startDateFormatted: String,
    endDateFormatted: String
  ) {
    val measureReport =
      withContext(dispatcherProvider.io()) {
        fhirOperator.evaluateMeasure(
          url = measureUrl,
          start = startDateFormatted,
          end = endDateFormatted,
          reportType = POPULATION,
          subject = null,
          practitioner = authenticatedUserInfo?.keyclockuuid!!
        )
      }

    measureReportPopulationResults.value = formatPopulationMeasureReport(measureReport)
  }

  private fun toggleProgressIndicatorVisibility(showProgressIndicator: Boolean = false) {
    reportTypeSelectorUiState.value =
      reportTypeSelectorUiState.value.copy(showProgressIndicator = showProgressIndicator)
  }

  fun formatPopulationMeasureReport(
    measureReport: MeasureReport
  ): List<MeasureReportPopulationResult> {
    return measureReport.group.flatMap { reportGroup: MeasureReport.MeasureReportGroupComponent ->
      reportGroup.stratifier.map { stratifier ->
        val resultItems: List<MeasureReportIndividualResult> =
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
            MeasureReportIndividualResult(
              title = text,
              percentage = percentage.toString(),
              count = count
            )
          }
        MeasureReportPopulationResult(
          title = stratifier.id.replace("-", " ").uppercase(Locale.getDefault()),
          dataList = resultItems
        )
      }
    }
  }

  // TODO: Enhancement - use FhirPathEngine evaluator for data extraction
  private fun MeasureReport.StratifierGroupComponent.findPopulation(
    id: String
  ): MeasureReport.StratifierGroupPopulationComponent? {
    return this.population.find { it.hasId() && it.id.equals(id, ignoreCase = true) }
  }

  fun resetState() {
    reportTypeSelectorUiState.value = ReportTypeSelectorUiState()
    dateRange.value = defaultDateRangeState()
    reportTypeState.value = MeasureReport.MeasureReportType.SUMMARY
    searchTextState.value = TextFieldValue("")
  }

  companion object {
    const val DATE_RANGE_DATE_FORMAT = "d MMM, yyyy"
    const val MEASURE_REPORT_DATE_FORMAT = "yyyy-MM-dd"
    const val NUMERATOR = "numerator"
    const val DENOMINATOR = "denominator"
    const val SUBJECT = "subject"
    const val POPULATION = "population"
  }
}
