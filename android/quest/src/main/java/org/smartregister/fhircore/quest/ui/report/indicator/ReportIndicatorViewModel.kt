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

import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.lifecycle.ViewModel
import com.google.android.fhir.search.Search
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.report.indicator.ReportIndicator
import org.smartregister.fhircore.engine.configuration.report.indicator.ReportIndicatorConfiguration
import org.smartregister.fhircore.engine.data.local.DefaultRepository

@HiltViewModel
class ReportIndicatorViewModel @Inject constructor(val defaultRepository: DefaultRepository) :
  ViewModel() {

  val reportIndicatorsMap: SnapshotStateMap<String, Pair<ReportIndicator, Long>> =
    SnapshotStateMap()

  suspend fun retrieveIndicators(reportId: String) {
    val reportIndicatorConfiguration =
      defaultRepository.configurationRegistry.retrieveConfiguration<ReportIndicatorConfiguration>(
        ConfigType.ReportIndicator,
        reportId,
      )
    val rules =
      defaultRepository.configRulesExecutor.generateRules(reportIndicatorConfiguration.configRules)
    val computedValues =
      defaultRepository.configRulesExecutor.computeConfigRules(rules, baseResource = null)

    with(defaultRepository) {
      reportIndicatorConfiguration.indicators.forEach { config ->
        val resourceConfig = config.resourceConfig
        val count =
          defaultRepository.count(
            Search(resourceConfig.resource).apply {
              applyConfiguredSortAndFilters(
                resourceConfig = resourceConfig,
                sortData = false,
                configComputedRuleValues = computedValues,
              )
            },
          )
        reportIndicatorsMap[config.id] = Pair(config, count)
      }
    }
  }
}
