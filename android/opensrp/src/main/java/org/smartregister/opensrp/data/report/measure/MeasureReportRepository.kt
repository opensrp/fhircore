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

package org.smartregister.opensrp.data.report.measure

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.android.fhir.search.search
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.register.RegisterConfiguration
import org.smartregister.fhircore.engine.configuration.report.measure.MeasureReportConfig
import org.smartregister.fhircore.engine.configuration.report.measure.MeasureReportConfiguration
import org.smartregister.fhircore.engine.data.local.register.RegisterRepository
import org.smartregister.fhircore.engine.domain.model.RepositoryResourceData
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.rulesengine.ResourceDataRulesExecutor

class MeasureReportRepository(
  private val measureReportConfiguration: MeasureReportConfiguration,
  private val registerConfiguration: RegisterConfiguration,
  private val registerRepository: RegisterRepository,
  private val resourceDataRulesExecutor: ResourceDataRulesExecutor
) : PagingSource<Int, MeasureReportConfig>() {

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

  suspend fun retrieveSubjects(count: Int): List<ResourceData> {
    val xFhirQuery =
      measureReportConfiguration.reports.firstOrNull()?.subjectXFhirQuery
        ?: ResourceType.Patient.name
    return registerRepository.fhirEngine.search(xFhirQuery).map {
      resourceDataRulesExecutor.processResourceData(
        repositoryResourceData =
          RepositoryResourceData(resourceRulesEngineFactId = it.resourceType.name, resource = it),
        ruleConfigs = registerConfiguration.registerCard.rules,
        params = emptyMap()
      )
    }
  }
}
