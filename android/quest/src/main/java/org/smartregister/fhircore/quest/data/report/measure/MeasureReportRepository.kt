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

package org.smartregister.fhircore.quest.data.report.measure

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.android.fhir.FhirEngine
import javax.inject.Inject
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.report.measure.MeasureReportConfig
import org.smartregister.fhircore.engine.configuration.report.measure.MeasureReportConfiguration
import org.smartregister.fhircore.engine.data.local.register.RegisterRepository
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider

class MeasureReportRepository
@Inject
constructor(
  val fhirEngine: FhirEngine,
  val dispatcherProvider: DefaultDispatcherProvider,
  val configurationRegistry: ConfigurationRegistry,
  val registerRepository: RegisterRepository
) : PagingSource<Int, MeasureReportConfig>() {

  private val measureReportConfiguration by lazy {
    configurationRegistry.retrieveConfiguration<MeasureReportConfiguration>(
      ConfigType.MeasureReport
    )
  }

  override fun getRefreshKey(state: PagingState<Int, MeasureReportConfig>): Int? {
    return state.anchorPosition
  }

  override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MeasureReportConfig> {
    return try {
      LoadResult.Page(data = measureReportConfiguration.reports, prevKey = null, nextKey = null)
    } catch (exception: Exception) {
      LoadResult.Error(exception)
    }
  }

  suspend fun retrievePatients(currentPage: Int): List<ResourceData> {
    return registerRepository.loadRegisterData(
      currentPage = currentPage,
      registerId = measureReportConfiguration.registerId
    )
  }

  /** returns start date of campaign to get the month/year list start month */
  fun getCampaignStartDate(): String {
    return measureReportConfiguration.registerDate
  }

  /** show month/year listing otherwise show date picker */
  fun showFixedRangeSelection() = measureReportConfiguration.showFixedRangeSelection ?: false
}
