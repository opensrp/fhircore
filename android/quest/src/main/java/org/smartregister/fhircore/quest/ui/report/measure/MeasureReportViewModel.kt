/*
 * Copyright 2021-2023 Ona Systems, Inc
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

import android.annotation.SuppressLint
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
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.search
import com.google.android.fhir.workflow.FhirOperator
import com.google.android.material.datepicker.MaterialDatePicker
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.Group
import org.hl7.fhir.r4.model.MeasureReport
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.Resource
import org.opencds.cqf.cql.evaluator.measure.common.MeasurePopulationType
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.report.measure.MeasureReportConfig
import org.smartregister.fhircore.engine.configuration.report.measure.MeasureReportConfiguration
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.data.local.register.RegisterRepository
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.SDF_D_MMM_YYYY_WITH_COMA
import org.smartregister.fhircore.engine.util.extension.SDF_MMMM
import org.smartregister.fhircore.engine.util.extension.SDF_YYYY
import org.smartregister.fhircore.engine.util.extension.SDF_YYYY_MM_DD
import org.smartregister.fhircore.engine.util.extension.asReference
import org.smartregister.fhircore.engine.util.extension.codingOf
import org.smartregister.fhircore.engine.util.extension.encodeResourceToString
import org.smartregister.fhircore.engine.util.extension.extractId
import org.smartregister.fhircore.engine.util.extension.extractLogicalIdUuid
import org.smartregister.fhircore.engine.util.extension.extractType
import org.smartregister.fhircore.engine.util.extension.findPercentage
import org.smartregister.fhircore.engine.util.extension.findPopulation
import org.smartregister.fhircore.engine.util.extension.findRatio
import org.smartregister.fhircore.engine.util.extension.firstDayOfMonth
import org.smartregister.fhircore.engine.util.extension.formatDate
import org.smartregister.fhircore.engine.util.extension.lastDayOfMonth
import org.smartregister.fhircore.engine.util.extension.loadCqlLibraryBundle
import org.smartregister.fhircore.engine.util.extension.parseDate
import org.smartregister.fhircore.engine.util.extension.plusMonths
import org.smartregister.fhircore.engine.util.extension.retrievePreviouslyGeneratedMeasureReports
import org.smartregister.fhircore.engine.util.extension.valueCode
import org.smartregister.fhircore.quest.data.report.measure.MeasureReportPatientsPagingSource
import org.smartregister.fhircore.quest.data.report.measure.MeasureReportRepository
import org.smartregister.fhircore.quest.navigation.MeasureReportNavigationScreen
import org.smartregister.fhircore.quest.navigation.NavigationArg
import org.smartregister.fhircore.quest.ui.report.measure.models.MeasureReportIndividualResult
import org.smartregister.fhircore.quest.ui.report.measure.models.MeasureReportPopulationResult
import org.smartregister.fhircore.quest.ui.report.measure.models.ReportRangeSelectionData
import org.smartregister.fhircore.quest.ui.shared.models.MeasureReportPatientViewData
import org.smartregister.fhircore.quest.util.mappers.MeasureReportPatientViewDataMapper
import timber.log.Timber

@SuppressLint("DiscouragedPrivateApi")
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
  val measureReportPatientViewDataMapper: MeasureReportPatientViewDataMapper,
  val defaultRepository: DefaultRepository
) : ViewModel() {

  val measureReportConfigList: MutableList<MeasureReportConfig> = mutableListOf()

  val measureReportIndividualResult: MutableState<MeasureReportIndividualResult?> =
    mutableStateOf(null)

  val measureReportPopulationResults: MutableState<List<MeasureReportPopulationResult>?> =
    mutableStateOf(null)

  private val _measureReportPopulationResultList: MutableList<MeasureReportPopulationResult> =
    mutableListOf()

  val reportTypeState: MutableState<MeasureReport.MeasureReportType> =
    mutableStateOf(MeasureReport.MeasureReportType.SUMMARY)

  val dateRange: MutableState<androidx.core.util.Pair<Long, Long>> =
    mutableStateOf(defaultDateRangeState())

  val reportTypeSelectorUiState: MutableState<ReportTypeSelectorUiState> =
    mutableStateOf(ReportTypeSelectorUiState())

  val searchTextState: MutableState<TextFieldValue> = mutableStateOf(TextFieldValue())

  val patientsData: MutableStateFlow<Flow<PagingData<MeasureReportPatientViewData>>> =
    MutableStateFlow(emptyFlow())

  private val practitionerId: String? by lazy {
    sharedPreferencesHelper
      .read(key = SharedPreferenceKey.PRACTITIONER_ID.name, null)
      ?.extractLogicalIdUuid()
  }

  fun defaultDateRangeState() =
    androidx.core.util.Pair(
      MaterialDatePicker.thisMonthInUtcMilliseconds(),
      MaterialDatePicker.todayInUtcMilliseconds()
    )

  fun reportMeasuresList(reportId: String): Flow<PagingData<MeasureReportConfig>> {
    val measureReportConfiguration = retrieveMeasureReportConfiguration(reportId)
    return Pager(PagingConfig(pageSize = DEFAULT_PAGE_SIZE)) {
        MeasureReportRepository(measureReportConfiguration, registerRepository)
      }
      .flow
      .cachedIn(viewModelScope)
  }

  private fun retrieveMeasureReportConfiguration(reportId: String): MeasureReportConfiguration =
    configurationRegistry.retrieveConfiguration(
      configType = ConfigType.MeasureReport,
      configId = reportId
    )

  fun onEvent(event: MeasureReportEvent, selectedDate: Date? = null) {

    when (event) {
      is MeasureReportEvent.OnSelectMeasure -> {
        event.measureReportConfig?.let {
          measureReportConfigList.clear()
          measureReportConfigList.addAll(it)
        }
        event.navController.navigate(
          MeasureReportNavigationScreen.ReportTypeSelector.route +
            NavigationArg.bindArgumentsOf(
              Pair(NavigationArg.SCREEN_TITLE, measureReportConfigList.firstOrNull()?.module ?: "")
            )
        )
      }
      is MeasureReportEvent.GenerateReport -> {
        if (selectedDate != null) {
          reportTypeState.value = MeasureReport.MeasureReportType.SUMMARY
          reportTypeSelectorUiState.value =
            reportTypeSelectorUiState.value.copy(
              startDate = selectedDate.firstDayOfMonth().formatDate(SDF_D_MMM_YYYY_WITH_COMA),
              endDate = selectedDate.lastDayOfMonth().formatDate(SDF_D_MMM_YYYY_WITH_COMA)
            )
        }
        refereshData()
        evaluateMeasure(event.navController)
      }
      is MeasureReportEvent.OnDateRangeSelected -> {
        //  Update dateRange and format start/end dates e.g 16 Nov, 2020 - 29 Oct, 2021
        dateRange.value = event.newDateRange
        reportTypeSelectorUiState.value =
          reportTypeSelectorUiState.value.copy(
            startDate = Date(dateRange.value.first).formatDate(SDF_D_MMM_YYYY_WITH_COMA),
            endDate = Date(dateRange.value.second).formatDate(SDF_D_MMM_YYYY_WITH_COMA)
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
      is MeasureReportEvent.OnPatientSelected -> // Reset previously selected patient
        //  Update dateRange and format start/end dates e.g 16 Nov, 2020 - 29 Oct, 2021
      {
        reportTypeSelectorUiState.value =
          reportTypeSelectorUiState.value.copy(patientViewData = event.patientViewData)
      }
      is MeasureReportEvent.OnSearchTextChanged -> // Reset previously selected patient
        //  Update dateRange and format start/end dates e.g 16 Nov, 2020 - 29 Oct, 2021
      {
        patientsData.value =
          retrievePatients(event.reportId).map {
            pagingData: PagingData<MeasureReportPatientViewData> ->
            pagingData.filter { it.name.contains(event.searchText, ignoreCase = true) }
          }
      }
    }
  }

  private fun refereshData() {
    _measureReportPopulationResultList.clear()
    measureReportPopulationResults.value = _measureReportPopulationResultList
  }

  fun retrievePatients(reportId: String): Flow<PagingData<MeasureReportPatientViewData>> {
    val measureReportConfig = retrieveMeasureReportConfiguration(reportId)
    patientsData.value =
      Pager(
          config = PagingConfig(pageSize = DEFAULT_PAGE_SIZE),
          pagingSourceFactory = {
            MeasureReportPatientsPagingSource(
              MeasureReportRepository(measureReportConfig, registerRepository),
              measureReportPatientViewDataMapper
            )
          }
        )
        .flow
        .cachedIn(viewModelScope)
    return patientsData.value
  }

  // TODO: Enhancement - use FhirPathEngine evaluator for data extraction
  fun evaluateMeasure(navController: NavController) {
    // Run evaluate measure only for existing report
    if (measureReportConfigList.isNotEmpty()) {
      // Retrieve and parse dates to  (2020-11-16)
      val startDateFormatted =
        reportTypeSelectorUiState
          .value
          .startDate
          .parseDate(SDF_D_MMM_YYYY_WITH_COMA)
          ?.formatDate(SDF_YYYY_MM_DD)!!

      val endDateFormatted =
        reportTypeSelectorUiState
          .value
          .endDate
          .parseDate(SDF_D_MMM_YYYY_WITH_COMA)
          ?.formatDate(SDF_YYYY_MM_DD)!!

      viewModelScope.launch {
        kotlin
          .runCatching {
            // Show Progress indicator while evaluating measure
            toggleProgressIndicatorVisibility(true)
            val result =
              measureReportConfigList.flatMap { config ->
                val existing =
                  retrievePreviouslyGeneratedMeasureReports<MeasureReport>(
                    fhirEngine,
                    startDateFormatted,
                    endDateFormatted,
                    config.url
                  )

                // if report is of current month or does not exist generate a new one and replace
                // existing
                if (startDateFormatted.contentEquals(
                    Date().firstDayOfMonth().formatDate(SDF_YYYY_MM_DD)
                  ) ||
                    endDateFormatted.contentEquals(
                      Date().lastDayOfMonth().formatDate(SDF_YYYY_MM_DD)
                    ) ||
                    existing.isEmpty()
                ) {
                  withContext(dispatcherProvider.io()) {
                    fhirEngine.loadCqlLibraryBundle(fhirOperator, config.url)
                  }
                  evaluatePopulationMeasure(
                    config.url,
                    startDateFormatted,
                    endDateFormatted,
                    config.subjectXFhirQuery,
                    existing
                  )
                } else existing
              }

            _measureReportPopulationResultList.addAll(
              formatPopulationMeasureReports(result, measureReportConfigList)
            )
          }
          .onSuccess {
            measureReportPopulationResults.value = _measureReportPopulationResultList
            Timber.w("measureReportPopulationResults${measureReportPopulationResults.value}")
            toggleProgressIndicatorVisibility(false)
            // Show results of measure report for individual/population
            navController.navigate(MeasureReportNavigationScreen.MeasureReportResult.route) {
              launchSingleTop = true
            }
          }
          .onFailure {
            Timber.w(it)
            toggleProgressIndicatorVisibility(false)
          }
      }
    }
  }

  private suspend fun evaluatePopulationMeasure(
    measureUrl: String,
    startDateFormatted: String,
    endDateFormatted: String,
    subjectXFhirQuery: String?,
    existing: List<MeasureReport>
  ): List<MeasureReport> {
    val measureReport = mutableListOf<MeasureReport>()
    withContext(dispatcherProvider.io()) {
      if (subjectXFhirQuery?.isNotEmpty() == true) {
        fhirEngine
          .search(subjectXFhirQuery)
          .map {
            val subject = "${it.resourceType}/${it.logicalId}"
            // TODO a hack to prevent missing subject in case of Group based reports where
            // MeasureEvaluator looks for Group members and skips the Group itself
            if (it is Group && !it.hasMember()) {
              it.addMember(Group.GroupMemberComponent(it.asReference()))
              fhirEngine.update(it)
            }
            runMeasureReport(measureUrl, SUBJECT, startDateFormatted, endDateFormatted, subject)
          }
          .forEach { measureReport.add(it) }
      } else
        runMeasureReport(measureUrl, POPULATION, startDateFormatted, endDateFormatted, null).also {
          measureReport.add(it)
        }

      measureReport.forEach { report ->
        // if report exists override instead of creating a new one
        existing
          .find {
            it.measure == report.measure &&
              (!it.hasSubject() || it.subject.reference == report.subject.reference)
          }
          ?.let { existing -> report.id = existing.id }
        defaultRepository.addOrUpdate(resource = report)
      }
    }
    return measureReport
  }

  /**
   * Run and generate MeasureReport for given measure and subject.
   * @param measureUrl url of measure to generate report for
   * @param reportType type of report (population | subject)
   * @param startDateFormatted start date of measure period with format yyyy-MM-dd
   * @param endDateFormatted end date of measure period with format yyyy-MM-dd
   * @param subject the individual subject reference (ResourceType/id) to run report for
   */
  fun runMeasureReport(
    measureUrl: String,
    reportType: String,
    startDateFormatted: String,
    endDateFormatted: String,
    subject: String?
  ): MeasureReport {
    return fhirOperator.evaluateMeasure(
      measureUrl = measureUrl,
      start = startDateFormatted,
      end = endDateFormatted,
      reportType = reportType,
      subject = subject,
      practitioner = null
      /* TODO DO NOT pass this id to MeasureProcessor as this is treated as subject if subject is null.
      practitionerId?.asReference(ResourceType.Practitioner)?.reference*/ ,
      lastReceivedOn = null // Non-null value not supported yet
    )
  }

  fun toggleProgressIndicatorVisibility(showProgressIndicator: Boolean = false) {
    reportTypeSelectorUiState.value =
      reportTypeSelectorUiState.value.copy(showProgressIndicator = showProgressIndicator)
  }

  suspend fun formatPopulationMeasureReports(
    measureReports: List<MeasureReport>,
    indicators: List<MeasureReportConfig> = listOf(),
  ): List<MeasureReportPopulationResult> {
    val data = mutableListOf<MeasureReportPopulationResult>()

    require(measureReports.distinctBy { it.type }.size <= 1) {
      "All Measure Reports in module should have same type"
    }

    measureReports
      .takeIf { it.all { it.type == MeasureReport.MeasureReportType.INDIVIDUAL } }
      ?.groupBy { it.subject.reference }
      ?.forEach {
        val reference = Reference().apply { reference = it.key }
        val subject =
          fhirEngine.get(reference.extractType()!!, reference.extractId()).let {
            when (it) {
              is Patient -> it.nameFirstRep.nameAsSingleString
              is Group -> it.name
              else ->
                throw UnsupportedOperationException(
                  "${it.resourceType} as individual subject not allowed"
                )
            }
          }

        val indicators =
          it.value.flatMap { report ->
            val formatted = formatSupplementalData(report.contained, report.type)
            val title = indicators.find { it.url == report.measure }?.title ?: ""
            if (formatted.isEmpty())
              listOf(MeasureReportIndividualResult(title = title, count = "0"))
            else if (formatted.size == 1)
              listOf(
                MeasureReportIndividualResult(
                  title = title,
                  count = formatted.first().measureReportDenominator?.toString() ?: "0"
                )
              )
            else
              formatted.map {
                MeasureReportIndividualResult(
                  title = it.title,
                  count = it.measureReportDenominator.toString()
                )
              }
          }

        data.add(
          MeasureReportPopulationResult(
            title = subject,
            indicatorTitle = subject,
            measureReportDenominator =
              if (indicators.size == 1) indicators.first().count.toInt() else null,
            dataList = if (indicators.size > 1) indicators else emptyList()
          )
        )
      }

    measureReports
      .takeIf { it.all { it.type != MeasureReport.MeasureReportType.INDIVIDUAL } }
      ?.forEach { report ->
        Timber.d(report.encodeResourceToString())

        data.addAll(formatSupplementalData(report.contained, report.type))

        report
          .group
          .map { group ->
            val denominator = group.findPopulation(MeasurePopulationType.NUMERATOR)?.count
            group to
              group
                .stratifier
                .flatMap { it.stratum }
                .filter { it.hasValue() && it.value.hasText() }
                .map { stratifier ->
                  stratifier.findPopulation(MeasurePopulationType.NUMERATOR)!!.let {
                    MeasureReportIndividualResult(
                      title = stratifier.value.text,
                      percentage = stratifier.findPercentage(denominator!!).toString(),
                      count = stratifier.findRatio(denominator),
                      description = stratifier.id?.replace("-", " ")?.uppercase() ?: ""
                    )
                  }
                }
          }
          .mapNotNull {
            it.first.findPopulation(MeasurePopulationType.NUMERATOR)?.let { count ->
              MeasureReportPopulationResult(
                title = it.first.id.replace("-", " "),
                indicatorTitle = indicators.find { it.url == report.measure }?.title ?: "",
                measureReportDenominator = count.count
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
   * @param list the list of resources output into MeasureReport.contained
   * @param type type of report for which supplemental data was output
   */
  fun formatSupplementalData(
    list: List<Resource>,
    type: MeasureReport.MeasureReportType
  ): List<MeasureReportPopulationResult> {
    // handle extracted supplemental data for values
    return list
      .filterIsInstance<Observation>()
      .let {
        // the observations would have key as coding.code=populationId and value as codeableConcept
        it
          .groupBy { it.codingOf(POPULATION_OBS_URL)?.display }
          .filter { it.key.isNullOrBlank().not() }
          .map {
            it.key!! to
              if (type == MeasureReport.MeasureReportType.INDIVIDUAL)
              // for subject specific reports it is key value map with exact value
              it.value.joinToString { it.valueCode() ?: "" }
              // for multiple subjects it is a number for each which should be counted by entries
              else it.value.count().toString()
          }
      }
      .map {
        MeasureReportPopulationResult(
          title = it.first,
          indicatorTitle = it.first,
          measureReportDenominator = it.second.toBigDecimal().toInt()
        )
      }
  }

  /** This function @returns a map of year-month for all months falling in given measure period */
  fun getReportGenerationRange(
    reportId: String,
    startDate: Date? = null
  ): Map<String, List<ReportRangeSelectionData>> {

    val reportConfiguration = retrieveMeasureReportConfiguration(reportId)
    val yearMonths = mutableListOf<ReportRangeSelectionData>()
    val endDate = Calendar.getInstance().time.formatDate(SDF_YYYY_MM_DD).parseDate(SDF_YYYY_MM_DD)
    var lastDate = endDate?.firstDayOfMonth()

    while (lastDate!!.after(
      startDate ?: reportConfiguration.registerDate?.parseDate(SDF_YYYY_MM_DD)
    )) {
      yearMonths.add(
        ReportRangeSelectionData(
          lastDate.formatDate(SDF_MMMM),
          lastDate.formatDate(SDF_YYYY),
          lastDate
        )
      )

      lastDate = lastDate.plusMonths(-1)
    }
    return yearMonths.toList().groupBy { it.year }
  }

  /** This function lists the fixed range selection in months for the entire year */
  fun showFixedRangeSelection(reportId: String) =
    retrieveMeasureReportConfiguration(reportId).showFixedRangeSelection == true

  fun resetState() {
    reportTypeSelectorUiState.value = ReportTypeSelectorUiState()
    dateRange.value = defaultDateRangeState()
    reportTypeState.value = MeasureReport.MeasureReportType.SUMMARY
    searchTextState.value = TextFieldValue("")
  }

  companion object {
    private const val SUBJECT = "subject"
    const val POPULATION = "population"
    private const val POPULATION_OBS_URL = "populationId"
    private const val DEFAULT_PAGE_SIZE = 20
  }
}
