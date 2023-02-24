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

package org.smartregister.fhircore.quest.data.report.measure

import androidx.paging.PagingSource
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.quest.robolectric.RobolectricTest
import org.smartregister.fhircore.quest.util.mappers.MeasureReportPatientViewDataMapper

@HiltAndroidTest
class MeasureReportPatientsPagingSourceTest : RobolectricTest() {

  @get:Rule val hiltAndroidRule = HiltAndroidRule(this)

  @Inject lateinit var measureReportPatientViewDataMapper: MeasureReportPatientViewDataMapper

  private val reportRepository = mockk<MeasureReportRepository>()

  private lateinit var reportPatientsPagingSource: MeasureReportPatientsPagingSource

  @Before
  fun setUp() {
    hiltAndroidRule.inject()
    reportPatientsPagingSource =
      MeasureReportPatientsPagingSource(
        reportRepository,
        measureReportPatientViewDataMapper,
        registerConfigs
      )
  }

  @Test
  fun loadShouldReturnResults() {
    coEvery { reportRepository.retrievePatients(0, registerConfiguration) } returns
      listOf(
        ResourceData(
          baseResourceId = "resourceId",
          baseResourceType = ResourceType.Patient,
          computedValuesMap = emptyMap(),
        )
      )

    val loadParams = mockk<PagingSource.LoadParams<Int>>()
    every { loadParams.key } returns null
    runBlocking {
      reportPatientsPagingSource.run {
        val result = load(loadParams)
        Assert.assertNotNull(result)
        Assert.assertEquals(1, (result as PagingSource.LoadResult.Page).data.size)
      }
    }
  }
}
