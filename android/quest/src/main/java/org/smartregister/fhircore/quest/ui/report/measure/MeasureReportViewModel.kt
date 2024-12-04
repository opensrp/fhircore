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
import java.util.*
import javax.inject.Inject
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Group
import org.hl7.fhir.r4.model.MeasureReport
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Practitioner
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.Resource
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.register.RegisterConfiguration
import org.smartregister.fhircore.engine.configuration.report.measure.MeasureReportConfiguration
import org.smartregister.fhircore.engine.configuration.report.measure.ReportConfiguration
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.data.local.register.RegisterRepository
import org.smartregister.fhircore.engine.rulesengine.RulesExecutor
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.SDF_D_MMM_YYYY_WITH_COMA
import org.smartregister.fhircore.engine.util.extension.SDF_MMMM
import org.smartregister.fhircore.engine.util.extension.SDF_YYYY
import org.smartregister.fhircore.engine.util.extension.SDF_YYYY_MMM
import org.smartregister.fhircore.engine.util.extension.SDF_YYYY_MM_DD
import org.smartregister.fhircore.engine.util.extension.codingOf
import org.smartregister.fhircore.engine.util.extension.encodeResourceToString
import org.smartregister.fhircore.engine.util.extension.extractId
import org.smartregister.fhircore.engine.util.extension.extractType
import org.smartregister.fhircore.engine.util.extension.findCount
import org.smartregister.fhircore.engine.util.extension.findPercentage
import org.smartregister.fhircore.engine.util.extension.findRatio
import org.smartregister.fhircore.engine.util.extension.firstDayOfMonth
import org.smartregister.fhircore.engine.util.extension.formatDate
import org.smartregister.fhircore.engine.util.extension.lastDayOfMonth
import org.smartregister.fhircore.engine.util.extension.loadCqlLibraryBundle
import org.smartregister.fhircore.engine.util.extension.parseDate
import org.smartregister.fhircore.engine.util.extension.plusMonths
import org.smartregister.fhircore.engine.util.extension.retrievePreviouslyGeneratedMeasureReports
import org.smartregister.fhircore.engine.util.extension.rounding
import org.smartregister.fhircore.engine.util.extension.valueCode
import org.smartregister.fhircore.quest.data.report.measure.MeasureReportPagingSource
import org.smartregister.fhircore.quest.data.report.measure.MeasureReportRepository
import org.smartregister.fhircore.quest.data.report.measure.MeasureReportSubjectsPagingSource
import org.smartregister.fhircore.quest.navigation.MeasureReportNavigationScreen
import org.smartregister.fhircore.quest.ui.report.measure.models.MeasureReportIndividualResult
import org.smartregister.fhircore.quest.ui.report.measure.models.MeasureReportPopulationResult
import org.smartregister.fhircore.quest.ui.report.measure.models.ReportRangeSelectionData
import org.smartregister.fhircore.quest.ui.shared.models.MeasureReportSubjectViewData
import org.smartregister.fhircore.quest.util.mappers.MeasureReportSubjectViewDataMapper
import org.smartregister.fhircore.quest.util.nonNullGetOrDefault
import timber.log.Timber

@HiltViewModel
class MeasureReportViewModel
@Inject
constructor(
  val fhirEngine: FhirEngine,
  val fhirOperator: FhirOperator,
  val sharedPreferencesHelper: SharedPreferencesHelper,
  val dispatcherProvider: DefaultDispatcherProvider,
  val configurationRegistry: ConfigurationRegistry,
  val registerRepository: RegisterRepository,
  val measureReportSubjectViewDataMapper: MeasureReportSubjectViewDataMapper,
  val defaultRepository: DefaultRepository,
  val rulesExecutor: RulesExecutor,
  private val measureReportRepository: MeasureReportRepository,
) : ViewModel() {
  private val _measureReportPopulationResultList: MutableList<MeasureReportPopulationResult> =
    mutableListOf()
  val dateRange: MutableState<androidx.core.util.Pair<Long, Long>> =
    mutableStateOf(defaultDateRangeState())
  val measureReportIndividualResult: MutableState<MeasureReportIndividualResult?> =
    mutableStateOf(null)
  val measureReportPopulationResults: MutableState<List<MeasureReportPopulationResult>?> =
    mutableStateOf(null)
  val reportConfigurations: MutableList<ReportConfiguration> = mutableListOf()
  val reportTypeSelectorUiState: MutableState<ReportTypeSelectorUiState> =
    mutableStateOf(ReportTypeSelectorUiState())
  val reportTypeState: MutableState<MeasureReport.MeasureReportType> =
    mutableStateOf(MeasureReport.MeasureReportType.SUMMARY)
  val searchTextState: MutableState<TextFieldValue> = mutableStateOf(TextFieldValue())
  val subjectData: MutableStateFlow<Flow<PagingData<MeasureReportSubjectViewData>>> =
    MutableStateFlow(emptyFlow())

  fun defaultDateRangeState() =
    androidx.core.util.Pair(
      MaterialDatePicker.thisMonthInUtcMilliseconds(),
      MaterialDatePicker.todayInUtcMilliseconds(),
    )

  fun reportMeasuresList(reportId: String): Flow<PagingData<ReportConfiguration>> {
    val measureReportConfiguration = retrieveMeasureReportConfiguration(reportId)
    val registerConfiguration =
      configurationRegistry.retrieveConfiguration<RegisterConfiguration>(
        ConfigType.Register,
        measureReportConfiguration.registerId,
      )
    return Pager(PagingConfig(DEFAULT_PAGE_SIZE)) {
        MeasureReportPagingSource(
          measureReportConfiguration = measureReportConfiguration,
          registerConfiguration = registerConfiguration,
          registerRepository = registerRepository,
          rulesExecutor = rulesExecutor,
        )
      }
      .flow
      .cachedIn(viewModelScope)
  }

  private fun retrieveMeasureReportConfiguration(reportId: String): MeasureReportConfiguration =
    configurationRegistry.retrieveConfiguration(
      configType = ConfigType.MeasureReport,
      configId = reportId,
    )

  fun onEvent(event: MeasureReportEvent, selectedDate: Date? = null) {
    when (event) {
      is MeasureReportEvent.OnSelectMeasure -> {
        event.reportConfigurations?.let {
          reportConfigurations.clear()
          reportConfigurations.addAll(it)
        }

        // generate report
        if (selectedDate != null) {
          reportTypeState.value = MeasureReport.MeasureReportType.SUMMARY
          reportTypeSelectorUiState.value =
            reportTypeSelectorUiState.value.copy(
              startDate = selectedDate.firstDayOfMonth().formatDate(SDF_D_MMM_YYYY_WITH_COMA),
              endDate = selectedDate.lastDayOfMonth().formatDate(SDF_D_MMM_YYYY_WITH_COMA),
            )
        }
        refreshData()
        event.practitionerId?.let {
          viewModelScope.launch { evaluateMeasure(event.navController, practitionerId = it) }
        }
      }
      is MeasureReportEvent.OnDateSelected -> {
        if (selectedDate != null) {
          reportTypeState.value = MeasureReport.MeasureReportType.SUMMARY
          reportTypeSelectorUiState.value =
            reportTypeSelectorUiState.value.copy(
              startDate = selectedDate.firstDayOfMonth().formatDate(SDF_D_MMM_YYYY_WITH_COMA),
              endDate = selectedDate.lastDayOfMonth().formatDate(SDF_D_MMM_YYYY_WITH_COMA),
            )
        }
        event.navController.navigate(MeasureReportNavigationScreen.MeasureReportModule.route)
      }
      is MeasureReportEvent.OnDateRangeSelected -> {
        //  Update dateRange and format start/end dates e.g 16 Nov, 2020 - 29 Oct, 2021
        dateRange.value = event.newDateRange
        reportTypeSelectorUiState.value =
          reportTypeSelectorUiState.value.copy(
            startDate = Date(dateRange.value.first).formatDate(SDF_D_MMM_YYYY_WITH_COMA),
            endDate = Date(dateRange.value.second).formatDate(SDF_D_MMM_YYYY_WITH_COMA),
          )
      }
      is MeasureReportEvent.OnReportTypeChanged -> {
        with(event.measureReportType) {
          reportTypeState.value = this
          if (this == MeasureReport.MeasureReportType.INDIVIDUAL) {
            event.navController.navigate(MeasureReportNavigationScreen.SubjectsList.route)
          } else {
            // Reset previously selected subject
            reportTypeSelectorUiState.value =
              reportTypeSelectorUiState.value.copy(subjectViewData = mutableSetOf())
          }
        }
      }
      is MeasureReportEvent.OnSubjectSelected -> // Reset previously selected subject
        //  Update dateRange and format start/end dates e.g 16 Nov, 2020 - 29 Oct, 2021
      {
        reportTypeSelectorUiState.value =
          reportTypeSelectorUiState.value.copy().apply {
            subjectViewData.add(event.subjectViewData)
          }
      }
      is MeasureReportEvent.OnSubjectRemoved -> {
        reportTypeSelectorUiState.value =
          reportTypeSelectorUiState.value.copy().apply {
            subjectViewData.remove(event.subjectViewData)
          }
      }
      is MeasureReportEvent.OnSearchTextChanged -> // Reset previously selected subject
        //  Update dateRange and format start/end dates e.g 16 Nov, 2020 - 29 Oct, 2021
      {
        subjectData.value =
          retrieveSubjects(event.reportId).map {
            pagingData: PagingData<MeasureReportSubjectViewData> ->
            pagingData.filter { it.display.contains(event.searchText, ignoreCase = true) }
          }
      }
    }
  }

  private fun refreshData() {
    _measureReportPopulationResultList.clear()
    measureReportPopulationResults.value = _measureReportPopulationResultList
  }

  fun retrieveSubjects(reportId: String): Flow<PagingData<MeasureReportSubjectViewData>> {
    val measureReportConfig = retrieveMeasureReportConfiguration(reportId)
    val registerConfiguration =
      configurationRegistry.retrieveConfiguration<RegisterConfiguration>(
        ConfigType.Register,
        measureReportConfig.registerId,
      )
    subjectData.value =
      Pager(
          config = PagingConfig(DEFAULT_PAGE_SIZE),
          pagingSourceFactory = {
            MeasureReportSubjectsPagingSource(
              MeasureReportPagingSource(
                measureReportConfiguration = measureReportConfig,
                registerConfiguration = registerConfiguration,
                registerRepository = registerRepository,
                rulesExecutor = rulesExecutor,
              ),
              measureReportSubjectViewDataMapper,
            )
          },
        )
        .flow
        .cachedIn(viewModelScope)
    return subjectData.value
  }

  private val exceptionHandler = CoroutineExceptionHandler { _, throwable -> println(throwable) }

  // TODO: Enhancement - use FhirPathEngine evaluator for data extraction
  suspend fun evaluateMeasure(navController: NavController, practitionerId: String? = null) {
    // Run evaluate measure only for existing report
    if (reportConfigurations.isNotEmpty()) {
      // Retrieve and parse dates to  (2020-11-16)
      val startDateFormatted =
        reportTypeSelectorUiState.value.startDate
          .parseDate(SDF_D_MMM_YYYY_WITH_COMA)
          ?.formatDate(SDF_YYYY_MM_DD)!!

      val endDateFormatted =
        reportTypeSelectorUiState.value.endDate
          .parseDate(SDF_D_MMM_YYYY_WITH_COMA)
          ?.formatDate(SDF_YYYY_MM_DD)!!

      try {
        // Show Progress indicator while evaluating measure
        toggleProgressIndicatorVisibility(true)
        val result =
          reportConfigurations.flatMap { config ->
            val subjects = mutableListOf<String>()
            subjects.addAll(measureReportRepository.fetchSubjects(config))

            // If a practitioner Id is available and if the subjects list is empty, add it to the
            // list of subjects
            if (practitionerId?.isNotBlank() == true && subjects.isEmpty()) {
              subjects.add("${Practitioner().resourceType.name}/$practitionerId")
            }

            val existingReports =
              fhirEngine.retrievePreviouslyGeneratedMeasureReports(
                startDateFormatted = startDateFormatted,
                endDateFormatted = endDateFormatted,
                measureUrl = config.url,
                subjects = listOf(),
              )

            val existingValidReports = mutableListOf<MeasureReport>()

            existingReports
              .groupBy { it.subject.reference }
              .forEach { entry ->
                if (
                  entry.value.size > 1 &&
                    entry.value.distinctBy { it.measure }.size > 1 &&
                    entry.value.distinctBy { it.type }.size > 1
                ) {
                  return@forEach
                } else {
                  existingValidReports.addAll(entry.value)
                }
              }

            // if report is of current month or does not exist generate a new one and replace
            // existing
            if (
              endDateFormatted
                .parseDate(SDF_YYYY_MM_DD)!!
                .formatDate(SDF_YYYY_MMM)
                .contentEquals(Date().formatDate(SDF_YYYY_MMM)) ||
                existingValidReports.isEmpty() ||
                existingValidReports.size != subjects.size
            ) {
              fhirEngine.loadCqlLibraryBundle(fhirOperator, config.url)

              measureReportRepository.evaluatePopulationMeasure(
                measureUrl = config.url,
                startDateFormatted = startDateFormatted,
                endDateFormatted = endDateFormatted,
                subjects = subjects,
                existing = existingValidReports,
                practitionerId = practitionerId,
              )
            } else {
              existingValidReports
            }
          }

        val measureReportPopulationResultList =
          formatPopulationMeasureReports(result, reportConfigurations)
        _measureReportPopulationResultList.addAll(
          measureReportPopulationResultList,
        )

        // On success
        measureReportPopulationResults.value = _measureReportPopulationResultList
        // Timber.w("measureReportPopulationResults${measureReportPopulationResults.value}")

        // Show results of measure report for individual/population
        navController.navigate(MeasureReportNavigationScreen.MeasureReportResult.route) {
          launchSingleTop = true
        }
      } catch (e: Exception) {
        Timber.e(e)
      } finally {
        toggleProgressIndicatorVisibility(false)
      }
    }
  }

  fun toggleProgressIndicatorVisibility(showProgressIndicator: Boolean = false) {
    reportTypeSelectorUiState.value =
      reportTypeSelectorUiState.value.copy(showProgressIndicator = showProgressIndicator)
  }

  suspend fun formatPopulationMeasureReports(
    measureReports: List<MeasureReport>,
    indicators: List<ReportConfiguration> = listOf(),
  ): List<MeasureReportPopulationResult> {
    val data = mutableListOf<MeasureReportPopulationResult>()

    require(measureReports.distinctBy { it.type }.size <= 1) {
      "All Measure Reports in module should have same type"
    }

    val indicatorUrlToConfigMap = indicators.associateBy({ it.url }, { it })

    measureReports
      .takeIf {
        it.all { measureReport -> measureReport.type == MeasureReport.MeasureReportType.INDIVIDUAL }
      }
      ?.groupBy { it.subject.reference }
      ?.forEach { entry ->
        val reference = Reference().apply { reference = entry.key }
        val subject =
          fhirEngine.get(reference.extractType()!!, reference.extractId()).let { resource ->
            when (resource) {
              is Patient -> resource.nameFirstRep.nameAsSingleString
              is Group -> resource.name
              is Practitioner -> resource.nameFirstRep.nameAsSingleString
              else ->
                throw UnsupportedOperationException(
                  "${resource.resourceType} as individual subject not allowed",
                )
            }
          }

        val theIndicators =
          entry.value.flatMap { report ->
            val reportConfig = nonNullGetOrDefault(indicatorUrlToConfigMap, report.measure, null)
            val formatted = formatSupplementalData(report.contained, report.type, reportConfig)
            val title = reportConfig?.title ?: ""
            if (formatted.isEmpty()) {
              listOf(MeasureReportIndividualResult(title = title, count = "0"))
            } else if (formatted.size == 1) {
              listOf(
                MeasureReportIndividualResult(
                  title = title,
                  count = formatted.first().measureReportDenominator,
                ),
              )
            } else {
              formatted.map {
                MeasureReportIndividualResult(
                  title = it.title,
                  count = it.measureReportDenominator,
                )
              }
            }
          }

        data.add(
          MeasureReportPopulationResult(
            title = subject,
            indicatorTitle = subject,
            measureReportDenominator =
              if (theIndicators.size == 1) theIndicators.first().count else "0",
            dataList = if (theIndicators.size > 1) theIndicators else emptyList(),
          ),
        )
      }

    measureReports
      .takeIf {
        it.all { measureReport -> measureReport.type != MeasureReport.MeasureReportType.INDIVIDUAL }
      }
      ?.forEach { report ->
        Timber.d(report.encodeResourceToString())

        val reportConfig = nonNullGetOrDefault(indicatorUrlToConfigMap, report.measure, null)
        data.addAll(formatSupplementalData(report.contained, report.type, reportConfig))

        report.group
          .mapNotNull { group ->
            val denominator = group.findCount()
            group to
              group.stratifier
                .asSequence()
                .flatMap { it.stratum }
                .filter { it.hasValue() && it.value.hasText() }
                .map { stratifier ->
                  MeasureReportIndividualResult(
                    title = stratifier.value.text,
                    percentage =
                      stratifier.findPercentage(
                        denominator!!.count,
                        reportConfig,
                      ),
                    count = stratifier.findRatio(denominator.count),
                    description = stratifier.id?.replace("-", " ")?.uppercase() ?: "",
                  )
                }
          }
          .mapNotNull {
            it.first?.let { group ->
              MeasureReportPopulationResult(
                title = it.first.id.replace("-", " "),
                indicatorTitle = reportConfig?.title ?: "",
                measureReportDenominator = group.findCount()?.count.toString(),
                dataList = it.second.toList(),
              )
            }
          }
          .forEach { data.add(it) }
      }

    return data
  }

  /**
   * Formats the supplementalData field of Measure output as contained Observation resources into
   * MeasureReport. The Observations output the observations would have key as
   * coding.code=populationId and value as codeableConcept. For individual i.e. subject specific
   * reports it is key value map with exact value of variable. For multiple subjects it is a number
   * for each subject which should be counted by for each entry for given code.
   *
   * @param list the list of resources output into MeasureReport.contained
   * @param type type of report for which supplemental data was output
   */
  private fun formatSupplementalData(
    list: List<Resource>,
    type: MeasureReport.MeasureReportType,
    reportConfig: ReportConfiguration?,
  ): List<MeasureReportPopulationResult> {
    // handle extracted supplemental data for values
    return list
      .filterIsInstance<Observation>()
      .let { observations ->
        // the observations would have key as coding.code=populationId and value as codeableConcept
        observations
          .groupBy { it.codingOf(POPULATION_OBS_URL)?.display }
          .filter { it.key.isNullOrBlank().not() }
          .map { entry ->
            entry.key!! to
              if (type == MeasureReport.MeasureReportType.INDIVIDUAL) {
                // for subject specific reports it is key value map with exact value
                entry.value.joinToString { it.valueCode() ?: "" }
              } // for multiple subjects it is a number for each which should be counted by entries
              else {
                entry.value.count().toString()
              }
          }
      }
      .map {
        MeasureReportPopulationResult(
          title = it.first,
          indicatorTitle = it.first,
          measureReportDenominator =
            it.second
              .toBigDecimal()
              .rounding(
                reportConfig?.roundingStrategy ?: ReportConfiguration.DEFAULT_ROUNDING_STRATEGY,
                reportConfig?.roundingPrecision ?: ReportConfiguration.DEFAULT_ROUNDING_PRECISION,
              ),
        )
      }
  }

  /** This function @returns a map of year-month for all months falling in given measure period */
  fun getReportGenerationRange(
    reportId: String,
    startDate: Date? = null,
  ): Map<String, List<ReportRangeSelectionData>> {
    val reportConfiguration = retrieveMeasureReportConfiguration(reportId)
    val yearMonths = mutableListOf<ReportRangeSelectionData>()
    val endDate = Calendar.getInstance().time.formatDate(SDF_YYYY_MM_DD).parseDate(SDF_YYYY_MM_DD)
    var lastDate = endDate?.firstDayOfMonth()

    while (
      lastDate!!.after(startDate ?: reportConfiguration.startPeriod?.parseDate(SDF_YYYY_MM_DD))
    ) {
      yearMonths.add(
        ReportRangeSelectionData(
          lastDate.formatDate(SDF_MMMM),
          lastDate.formatDate(SDF_YYYY),
          lastDate,
        ),
      )

      lastDate = lastDate.plusMonths(-1)
    }
    return yearMonths.toList().groupBy { it.year }
  }

  /** This function lists the fixed range selection in months for the entire year */
  fun showFixedRangeSelection(reportId: String) =
    retrieveMeasureReportConfiguration(reportId).showFixedRangeSelection == true

  /** This function lists the subject selection in report selector */
  fun showSubjectSelection(reportId: String) =
    retrieveMeasureReportConfiguration(reportId).showSubjectSelection == true

  fun resetState() {
    reportTypeSelectorUiState.value = ReportTypeSelectorUiState()
    dateRange.value = defaultDateRangeState()
    reportTypeState.value = MeasureReport.MeasureReportType.SUMMARY
    searchTextState.value = TextFieldValue("")
  }

  companion object {
    const val SUBJECT = "subject"
    const val POPULATION = "population"
    private const val POPULATION_OBS_URL = "populationId"
    private const val DEFAULT_PAGE_SIZE = 20
  }
}
