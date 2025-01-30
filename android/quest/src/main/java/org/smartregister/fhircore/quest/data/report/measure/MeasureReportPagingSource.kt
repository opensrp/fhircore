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

package org.smartregister.fhircore.quest.data.report.measure

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.android.fhir.search.search
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.register.RegisterConfiguration
import org.smartregister.fhircore.engine.configuration.report.measure.MeasureReportConfiguration
import org.smartregister.fhircore.engine.configuration.report.measure.ReportConfiguration
import org.smartregister.fhircore.engine.data.local.register.RegisterRepository
import org.smartregister.fhircore.engine.domain.model.RepositoryResourceData
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.rulesengine.RulesExecutor

class MeasureReportPagingSource(
  private val measureReportConfiguration: MeasureReportConfiguration,
  private val registerConfiguration: RegisterConfiguration,
  private val registerRepository: RegisterRepository,
  private val rulesExecutor: RulesExecutor,
) : PagingSource<Int, ReportConfiguration>() {

  override fun getRefreshKey(state: PagingState<Int, ReportConfiguration>): Int? {
    return state.anchorPosition
  }

  override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ReportConfiguration> {
    return try {
      LoadResult.Page(data = measureReportConfiguration.reports, prevKey = null, nextKey = null)
    } catch (exception: Exception) {
      LoadResult.Error(exception)
    }
  }

  suspend fun retrieveSubjects(): List<ResourceData> {
    val xFhirQuery =
      measureReportConfiguration.reports.firstOrNull()?.subjectXFhirQuery
        ?: ResourceType.Patient.name
    val rules = rulesExecutor.rulesFactory.generateRules(registerConfiguration.registerCard.rules)
    return registerRepository.fhirEngine.search(xFhirQuery).map {
      rulesExecutor.processResourceData(
        repositoryResourceData =
          RepositoryResourceData(
            resourceConfigId = it.resource.resourceType.name,
            resource = it.resource,
          ),
        rules = rules,
        params = emptyMap(),
      )
    }
  }
}
