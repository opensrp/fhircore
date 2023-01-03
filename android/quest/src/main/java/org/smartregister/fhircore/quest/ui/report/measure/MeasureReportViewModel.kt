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

import android.annotation.SuppressLint
import android.database.CursorWindow
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
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Group
import org.hl7.fhir.r4.model.Group.GroupType
import org.hl7.fhir.r4.model.MeasureReport
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.opencds.cqf.cql.evaluator.measure.common.MeasurePopulationType
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.report.measure.MeasureReportConfig
import org.smartregister.fhircore.engine.configuration.report.measure.MeasureReportConfigType
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
import org.smartregister.fhircore.engine.util.extension.encodeResourceToString
import org.smartregister.fhircore.engine.util.extension.extractLogicalIdUuid
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

  // TODO remove this code once SDK fixes cursor size issue
  init {
    try {
      val field = CursorWindow::class.java.getDeclaredField("sCursorWindowSize")
      field.isAccessible = true
      field.set(null, 10 * 1024 * 1024) // the 10MB is the new size
    } catch (e: Exception) {
      Timber.e(e)
    }
  }

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

      val individualEvaluation = reportTypeState.value == MeasureReport.MeasureReportType.INDIVIDUAL

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
            measureReportConfigList.forEach {
              val result =
                retrievePreviouslyGeneratedMeasureReports<MeasureReport>(
                  fhirEngine,
                  startDateFormatted,
                  endDateFormatted,
                  it.url
                )
              if (result?.isEmpty() == true) {
                withContext(dispatcherProvider.io()) {
                  fhirEngine.loadCqlLibraryBundle(fhirOperator, it.url)
                }
                if (reportTypeSelectorUiState.value.patientViewData != null && individualEvaluation
                ) {
                  val measureReport: MeasureReport? =
                    try {
                      withContext(dispatcherProvider.io()) {
                        fhirOperator.evaluateMeasure(
                          measureUrl = it.url,
                          start = startDateFormatted,
                          end = endDateFormatted,
                          reportType = SUBJECT,
                          subject = reportTypeSelectorUiState.value.patientViewData!!.logicalId,
                          practitioner =
                            practitionerId?.asReference(ResourceType.Practitioner)?.reference,
                          lastReceivedOn = null // Non-null value not supported yet
                        )
                      }
                    } catch (exception: IllegalArgumentException) {
                      Timber.e(exception)
                      null
                    }

                  if (measureReport?.type == MeasureReport.MeasureReportType.INDIVIDUAL) {
                    val population: MeasureReport.MeasureReportGroupPopulationComponent? =
                      measureReport.group.first().findPopulation(MeasurePopulationType.NUMERATOR)
                    measureReportIndividualResult.value =
                      MeasureReportIndividualResult(
                        status = if (population != null && population.count > 0) "True" else "False"
                      )
                  }
                } else if (reportTypeSelectorUiState.value.patientViewData == null &&
                    !individualEvaluation
                ) {
                  evaluatePopulationMeasure(
                    it.url,
                    startDateFormatted,
                    endDateFormatted,
                    it.title,
                    it.type
                  )
                }
              } else {
                result
                  ?.let { it1 -> formatPopulationMeasureReports(it1, it.title, it.type) }
                  ?.let { it2 -> _measureReportPopulationResultList.addAll(it2) }
              }
            }
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
    indicatorTitle: String,
    type: MeasureReportConfigType
  ) {
    val measureReport: List<MeasureReport> =
      if (type == MeasureReportConfigType.STOCK) {
        fhirEngine
          .search<Group> {
            filter(
              Group.TYPE,
              {
                value =
                  of(
                    Coding(
                      GroupType.MEDICATION.system,
                      GroupType.MEDICATION.toCode(),
                      GroupType.MEDICATION.display
                    )
                  )
              }
            )
          }
          .map {
            // TODO a hack to prevent missing subject in case of Stock (Group) report where
            // MeasureEvaluator looks for Group members and skips the Group itself
            if (!it.hasMember()) {
              it.addMember(Group.GroupMemberComponent(it.asReference()))
              fhirEngine.update(it)
            }
            runMeasureReport(measureUrl, SUBJECT, startDateFormatted, endDateFormatted, it.id)
          }
      } else
        listOfNotNull(
          runMeasureReport(measureUrl, POPULATION, startDateFormatted, endDateFormatted, null)
        )

    measureReport.forEach { defaultRepository.addOrUpdate(resource = it) }

    _measureReportPopulationResultList.addAll(
      formatPopulationMeasureReports(measureReport, indicatorTitle, type)
    )
  }

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
    indicatorTitle: String = "",
    type: MeasureReportConfigType
  ): List<MeasureReportPopulationResult> {
    val data = mutableListOf<MeasureReportPopulationResult>()

    measureReports.forEach {
      Timber.d(it.encodeResourceToString())

      if (type == MeasureReportConfigType.STOCK) {
        val group =
          fhirEngine.get(ResourceType.Group, it.subject.reference.extractLogicalIdUuid()) as Group
        data.add(
          MeasureReportPopulationResult(
            title = group.name,
            indicatorTitle = group.name,
            dataList =
              formatSupplementalData(it.contained, type).map {
                MeasureReportIndividualResult(
                  title = it.indicatorTitle,
                  count = it.measureReportDenominator.toString()
                )
              }
          )
        )
      } else data.addAll(formatSupplementalData(it.contained, type))

      it
        .group
        .map { group ->
          val denominator = group.findPopulation(MeasurePopulationType.NUMERATOR)?.count
          group to
            group.stratifier.flatMap { it.stratum }.map { stratifier ->
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
              indicatorTitle = indicatorTitle,
              measureReportDenominator = count.count
            )
          }
        }
        .forEach { data.add(it) }
    }

    return data
  }

  fun formatSupplementalData(
    list: List<Resource>,
    type: MeasureReportConfigType
  ): List<MeasureReportPopulationResult> {
    // handle extracted supplemental data for values
    return list
      .filter { it is Observation }
      .map { it as Observation }
      .let {
        if (type == MeasureReportConfigType.STOCK)
        // for stock or a specific subject it is key value map with exact value
        it.map {
            it.code.coding.find { it.code == POPULATION_OBS_URL }!!.display to
              it.valueCodeableConcept.codingFirstRep.code
          }
        else
        // for all it would be each subject outputting a value for given key which should be handled
        // by the count
        it.groupBy(
              { it.code.coding.find { it.code == POPULATION_OBS_URL }!!.display },
              { it.code.codingFirstRep.code }
            )
            .map { it.key to it.value.count().toString() }
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
