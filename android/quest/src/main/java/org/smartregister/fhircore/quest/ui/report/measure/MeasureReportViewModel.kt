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

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.workflow.FhirOperator
import com.google.android.material.datepicker.MaterialDatePicker
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.MeasureReport
import org.hl7.fhir.r4.model.Observation
import org.smartregister.fhircore.engine.configuration.report.measure.MeasureReportConfig
import org.smartregister.fhircore.engine.domain.util.PaginationConstant
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.asMmmYyyy
import org.smartregister.fhircore.engine.util.extension.asYyyy
import org.smartregister.fhircore.engine.util.extension.encodeResourceToString
import org.smartregister.fhircore.engine.util.extension.firstDayOfMonth
import org.smartregister.fhircore.engine.util.extension.loadCqlLibraryBundle
import org.smartregister.fhircore.engine.util.extension.plusMonths
import org.smartregister.fhircore.engine.util.extension.valueToString
import org.smartregister.fhircore.quest.data.report.measure.MeasureReportPatientsPagingSource
import org.smartregister.fhircore.quest.data.report.measure.MeasureReportRepository
import org.smartregister.fhircore.quest.navigation.MeasureReportNavigationScreen
import org.smartregister.fhircore.quest.navigation.NavigationArg
import org.smartregister.fhircore.quest.ui.report.measure.models.MeasureReportIndividualResult
import org.smartregister.fhircore.quest.ui.report.measure.models.MeasureReportPopulationResult
import org.smartregister.fhircore.quest.ui.shared.models.MeasureReportPatientViewData
import org.smartregister.fhircore.quest.util.mappers.MeasureReportPatientViewDataMapper
import org.smartregister.model.practitioner.PractitionerDetails
import timber.log.Timber

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

  val measureReportConfig: MutableState<MeasureReportConfig?> = mutableStateOf(null)

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

  val patientsData: MutableStateFlow<Flow<PagingData<MeasureReportPatientViewData>>> by lazy {
    MutableStateFlow(retrieveAncPatients())
  }

  private val practitionerDetails by lazy {
    sharedPreferencesHelper.read<PractitionerDetails>(
        key = SharedPreferenceKey.PRACTITIONER_DETAILS_USER_DETAIL.name,
      )
      ?.fhirPractitionerDetails
  }

  fun defaultDateRangeState() =
    androidx.core.util.Pair(
      MaterialDatePicker.thisMonthInUtcMilliseconds(),
      MaterialDatePicker.todayInUtcMilliseconds()
    )

  fun reportMeasuresList(): Flow<PagingData<MeasureReportConfig>> =
    Pager(PagingConfig(pageSize = PaginationConstant.DEFAULT_PAGE_SIZE)) { measureReportRepository }
      .flow
      .cachedIn(viewModelScope)

  fun onEvent(event: MeasureReportEvent) {
    when (event) {
      is MeasureReportEvent.OnSelectMeasure -> {
        measureReportConfig.value = event.measureReportConfig
        event.navController.navigate(
          MeasureReportNavigationScreen.ReportTypeSelector.route +
            NavigationArg.bindArgumentsOf(
              Pair(NavigationArg.SCREEN_TITLE, event.measureReportConfig.title)
            )
        ) { launchSingleTop = true }
      }
      is MeasureReportEvent.GenerateReport -> evaluateMeasure(event.navController)
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
      is MeasureReportEvent.OnPatientSelected ->
        reportTypeSelectorUiState.value =
          reportTypeSelectorUiState.value.copy(patientViewData = event.patientViewData)
      is MeasureReportEvent.OnSearchTextChanged ->
        patientsData.value =
          retrieveAncPatients().map { pagingData: PagingData<MeasureReportPatientViewData> ->
            pagingData.filter { it.name.contains(event.searchText, ignoreCase = true) }
          }
    }
  }

  private fun retrieveAncPatients(): Flow<PagingData<MeasureReportPatientViewData>> =
    Pager(
        config = PagingConfig(pageSize = PaginationConstant.DEFAULT_PAGE_SIZE),
        pagingSourceFactory = {
          MeasureReportPatientsPagingSource(
            measureReportRepository,
            measureReportPatientViewDataMapper
          )
        }
      )
      .flow
      .cachedIn(viewModelScope)

  // TODO: Enhancement - use FhirPathEngine evaluator for data extraction
  fun evaluateMeasure(navController: NavController) {
    // Run evaluate measure only for existing report
    if (measureReportConfig.value != null) {
      val measureUrl = measureReportConfig.value!!.url
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
              fhirEngine.loadCqlLibraryBundle(fhirOperator, measureUrl)
            }

            if (reportTypeSelectorUiState.value.patientViewData != null && individualEvaluation) {
              val measureReport =
                withContext(dispatcherProvider.io()) {
                  fhirOperator.evaluateMeasure(
                    measureUrl = measureUrl,
                    start = startDateFormatted,
                    end = endDateFormatted,
                    reportType = SUBJECT,
                    subject = reportTypeSelectorUiState.value.patientViewData!!.logicalId,
                    practitioner = practitionerDetails?.id,
                    lastReceivedOn = null // Non-null value not supported yet
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
          kotlin.runCatching {
            fhirOperator.evaluateMeasure(
              measureUrl = measureUrl,
              start = startDateFormatted,
              end = endDateFormatted,
              reportType = POPULATION,
              subject = null,
              practitioner = null /*practitionerDetails?.id*/,
              lastReceivedOn = null // Non-null value not supported yet
            )
          }
        }
        .onFailure { Timber.e(it) }
        .getOrThrow()

    measureReportPopulationResults.value = formatPopulationMeasureReport(measureReport)
  }

  fun toggleProgressIndicatorVisibility(showProgressIndicator: Boolean = false) {
    reportTypeSelectorUiState.value =
      reportTypeSelectorUiState.value.copy(showProgressIndicator = showProgressIndicator)
  }

  fun formatPopulationMeasureReport(
    measureReport: MeasureReport
  ): List<MeasureReportPopulationResult> {
    return measureReport
      .also { Timber.w(it.encodeResourceToString()) }
      .group
      .flatMap { reportGroup: MeasureReport.MeasureReportGroupComponent ->
        // Measure Report :
        // group[]
        // group.population[]
        // group.stratifier[]
        // group.stratifier.stratum[]
        // group.stratifier.stratum.population[]

        // report group is stratifier/stratum denominator
        val denominator = reportGroup.findPopulation(NUMERATOR)?.count ?: 0

        val stratifierItems: List<List<MeasureReportIndividualResult>> =
          if (reportGroup.code.coding.any { it.code.contains("month", true) })
            measureReport.period.let {
              val list = mutableListOf<MeasureReportIndividualResult>()
              var currentDate = it.copy().start.firstDayOfMonth()

              while (currentDate.before(it.end)) {
                val currentMonth = MONTH_NAMES.elementAt(currentDate.month)

                val stats =
                  reportGroup
                    .stratifier
                    .flatMap { it.stratum }
                    .find {
                      it.hasValue() &&
                        it.value.text.contains(currentDate.asYyyy()) &&
                        it.value.text.contains(currentMonth)
                    }
                    ?.let { it.findRatio(denominator) to it.findPercentage(denominator) }
                MeasureReportIndividualResult(
                    title = currentDate.asMmmYyyy(),
                    percentage = stats?.second?.toString() ?: "0",
                    count = stats?.first ?: "0/$denominator"
                  )
                  .also {
                    currentDate = currentDate.plusMonths(1)
                    list.add(it)
                  }
              }
              listOf(list.toList())
            }
          else
            reportGroup.stratifier.map { stratifier ->
              stratifier.stratum.filter { it.hasValue() }.map { stratum ->
                val text =
                  when {
                    stratum.value.hasText() -> stratum.value.text
                    stratum.value.hasCoding() -> stratum.value.coding.last().display
                    else -> "N/A"
                  }
                MeasureReportIndividualResult(
                  title = text,
                  percentage = stratum.findPercentage(denominator).toString(),
                  count = stratum.findRatio(denominator),
                  description = stratifier.id?.replace("-", " ")?.uppercase() ?: ""
                )
              }
            }
        // if each stratum evaluated to single item, display all under one group else for each add a
        // separate group
        if (stratifierItems.all { it.count() <= 1 })
          listOf(
            MeasureReportPopulationResult(
              title = reportGroup.id,
              count = reportGroup.findRatio(),
              dataList = stratifierItems.flatMap { it }
            )
          )
        else
          stratifierItems.map {
            MeasureReportPopulationResult(
              title = "${reportGroup.id} - ${it.first().description}",
              count = reportGroup.findRatio(),
              dataList = it
            )
          }
      }
      .toMutableList()
      .apply {
        measureReport
          .contained
          .groupBy {
            it as Observation
            it.extension
              .flatMap { it.extension }
              .firstOrNull { it.url == POPULATION_OBS_URL }
              ?.value
              ?.valueToString()
          }
          .map { it.key to it.value.map { it as Observation } }
          .map { group ->
            group
              .second
              .distinctBy { it.code.codingFirstRep.code }
              .count { it.code.codingFirstRep.code.isNotBlank() }
              .let { group.first to it }
          }
          .filter { it.first?.isNotBlank() == true }
          .distinctBy { it.first }
          .forEach {
            this.add(
              0,
              MeasureReportPopulationResult(title = it.first ?: "", count = it.second.toString())
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

  private fun MeasureReport.MeasureReportGroupComponent.findPopulation(
    id: String
  ): MeasureReport.MeasureReportGroupPopulationComponent? {
    return this.population.find { it.hasId() && it.id.equals(id, ignoreCase = true) }
  }

  private fun MeasureReport.MeasureReportGroupComponent.findRatio(): String {
    return "${this.findPopulation(NUMERATOR)?.count}/${this.findPopulation(DENOMINATOR)?.count}"
  }

  private fun MeasureReport.StratifierGroupComponent.findRatio(denominator: Int): String {
    return "${this.findPopulation(NUMERATOR)?.count}/$denominator"
  }

  private fun MeasureReport.StratifierGroupComponent.findPercentage(denominator: Int): Int {
    return if (denominator == 0) 0
    else findPopulation(NUMERATOR)?.count?.times(100)?.div(denominator) ?: 0
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
    const val POPULATION_OBS_URL = "populationId"
    val MONTH_NAMES =
      listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
  }
}
