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

package org.smartregister.fhircore.quest.ui.report.other

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.workflow.FhirOperator
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.*
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.report.other.OtherReportConfiguration
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.data.local.register.RegisterRepository
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.rulesengine.ResourceDataRulesExecutor
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.SDF_YYYY_MM_DD
import org.smartregister.fhircore.engine.util.extension.formatDate

@HiltViewModel
class OtherReportViewModel
@Inject
constructor(
  val fhirEngine: FhirEngine,
  val fhirOperator: FhirOperator,
  val sharedPreferencesHelper: SharedPreferencesHelper,
  val dispatcherProvider: DefaultDispatcherProvider,
  val configurationRegistry: ConfigurationRegistry,
  val registerRepository: RegisterRepository,
  val defaultRepository: DefaultRepository,
  val resourceDataRulesExecutor: ResourceDataRulesExecutor,
) : ViewModel() {
  val dateRange: MutableState<androidx.core.util.Pair<Long?, Long?>> =
    mutableStateOf(defaultDateRangeState())
  val otherReportUiState = mutableStateOf(OtherReportUiState())

  private fun defaultDateRangeState() =
    androidx.core.util.Pair<Long?, Long?>(
      null, // MaterialDatePicker.thisMonthInUtcMilliseconds(),
      null, // MaterialDatePicker.todayInUtcMilliseconds(),
    )

  suspend fun retrieveReportUiState(reportId: String) {
    val otherReportConfiguration = retrieveOtherReportConfiguration(reportId)
    val repositoryResourceData =
      registerRepository.loadReportData(
        reportId = reportId,
        startDateFormatted =
          dateRange.value.first?.let { Date(dateRange.value.first!!).formatDate(SDF_YYYY_MM_DD) },
        endDateFormatted =
          dateRange.value.second?.let { Date(dateRange.value.second!!).formatDate(SDF_YYYY_MM_DD) },
        resourceConfigs = otherReportConfiguration.resources,
      )
    val computedValuesMap =
      resourceDataRulesExecutor.computeResourceDataRules(
        otherReportConfiguration.rules,
        repositoryResourceData,
      )

    otherReportUiState.value =
      OtherReportUiState(
        reportId = reportId,
        resourceData = ResourceData("", ResourceType.MeasureReport, computedValuesMap),
        otherReportConfiguration = otherReportConfiguration,
        showDataLoadProgressIndicator = false,
      )
  }

  fun onEvent(event: OtherReportEvent) {
    when (event) {
      is OtherReportEvent.OnDateRangeSelected -> {
        dateRange.value = event.newDateRange
        OtherReportUiState(reportId = otherReportUiState.value.reportId).let {
          otherReportUiState.value = it
          viewModelScope.launch { retrieveReportUiState(it.reportId) }
        }
      }
    }
  }

  private fun retrieveOtherReportConfiguration(reportId: String): OtherReportConfiguration =
    configurationRegistry.retrieveConfiguration(
      configType = ConfigType.OtherReport,
      configId = reportId,
    )
}
