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

package org.smartregister.fhircore.quest.ui.report.indicator

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.uhn.fhir.rest.gclient.StringClientParam
import com.google.android.fhir.search.Order
import com.google.android.fhir.search.Search
import com.google.android.fhir.search.search
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Encounter
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.report.indicator.ReportIndicator
import org.smartregister.fhircore.engine.configuration.report.indicator.ReportIndicatorConfiguration
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.util.extension.SDF_MMMM
import org.smartregister.fhircore.engine.util.extension.SDF_YYYY
import org.smartregister.fhircore.engine.util.extension.firstDayOfMonth
import org.smartregister.fhircore.engine.util.extension.formatDate
import org.smartregister.fhircore.engine.util.extension.plusMonths
import org.smartregister.fhircore.quest.ui.report.models.ReportRangeSelectionData
import timber.log.Timber

@HiltViewModel
class ReportIndicatorViewModel @Inject constructor(val defaultRepository: DefaultRepository) :
  ViewModel() {

  val reportIndicatorsMap: SnapshotStateMap<String, Pair<ReportIndicator, Long>> =
    SnapshotStateMap()

  val reportPeriodRange: MutableState<Map<String, List<ReportRangeSelectionData>>> =
    mutableStateOf(emptyMap())

  val isLoading: MutableState<Boolean> = mutableStateOf(false)
  val selectedDateRange: MutableState<Pair<String, String>?> = mutableStateOf(null)
  val selectedPeriodLabel: MutableState<String?> = mutableStateOf(null)

  suspend fun retrieveIndicators(
    reportId: String,
    startDate: String,
    endDate: String,
    periodLabel: String,
  ) {
    isLoading.value = true
    reportIndicatorsMap.clear()

    try {
      val reportIndicatorConfiguration =
        defaultRepository.configurationRegistry.retrieveConfiguration<ReportIndicatorConfiguration>(
          ConfigType.ReportIndicator,
          reportId,
        )
      val rules =
        defaultRepository.configRulesExecutor.generateRules(
          reportIndicatorConfiguration.configRules,
        )
      val computedValues =
        defaultRepository.configRulesExecutor
          .computeConfigRules(rules, baseResource = null)
          .toMutableMap()
          .apply {
            put("startDate", startDate)
            put("endDate", endDate)
          }

      with(defaultRepository) {
        reportIndicatorConfiguration.indicators.forEach { config ->
          val resourceConfig = config.resourceConfig
          val search =
            Search(resourceConfig.resource).apply {
              applyConfiguredSortAndFilters(
                resourceConfig = resourceConfig,
                sortData = false,
                configComputedRuleValues = computedValues,
              )
            }
          val count = defaultRepository.count(search)
          if (count > 0) {
            reportIndicatorsMap[config.id] = Pair(config, count)
          }
        }
      }
      selectedDateRange.value = Pair(startDate, endDate)
      selectedPeriodLabel.value = periodLabel
    } catch (e: Exception) {
      Timber.e(e, "Error retrieving indicators")
    } finally {
      isLoading.value = false
    }
  }

  fun loadDateRangeFromEncounters() {
    viewModelScope.launch {
      try {
        val (minDate, maxDate) = getDateRangeFromEncounters()
        val ranges = generateMonthlyRanges(minDate, maxDate)
        reportPeriodRange.value = ranges
      } catch (e: Exception) {
        Timber.e(e, "Error loading date range from encounters")
      }
    }
  }

  private suspend fun getDateRangeFromEncounters(): Pair<Date?, Date?> {
    val clientParam = StringClientParam("date")
    val minEncounter =
      defaultRepository.fhirEngine
        .search<Encounter> {
          sort(clientParam, Order.ASCENDING)
          count = 1
        }
        .firstOrNull()
    val minDate = minEncounter?.resource?.period?.start

    val maxEncounter =
      defaultRepository.fhirEngine
        .search<Encounter> {
          sort(clientParam, Order.DESCENDING)
          count = 1
        }
        .firstOrNull()
    val maxDate = maxEncounter?.resource?.period?.end ?: maxEncounter?.resource?.period?.start

    return Pair(minDate, maxDate)
  }

  private fun generateMonthlyRanges(
    startDate: Date?,
    endDate: Date?,
  ): Map<String, List<ReportRangeSelectionData>> {
    val yearMonths = mutableListOf<ReportRangeSelectionData>()
    val calendar = Calendar.getInstance()

    val currentMonth = calendar.time.firstDayOfMonth()
    val actualEndDate = maxOf(endDate?.firstDayOfMonth() ?: currentMonth, currentMonth)
    val actualStartDate = startDate?.firstDayOfMonth() ?: actualEndDate

    var currentDate = actualEndDate

    while (currentDate >= actualStartDate) {
      yearMonths.add(
        ReportRangeSelectionData(
          currentDate.formatDate(SDF_MMMM),
          currentDate.formatDate(SDF_YYYY),
          currentDate,
        ),
      )
      currentDate = currentDate.plusMonths(-1)
    }

    return yearMonths
      .groupBy { it.year }
      .toSortedMap(compareByDescending { it })
      .mapValues { (_, months) -> months }
  }
}
