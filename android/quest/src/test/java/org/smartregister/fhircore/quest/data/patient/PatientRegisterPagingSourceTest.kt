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

package org.smartregister.fhircore.quest.data.patient

import android.content.Context
import androidx.paging.PagingSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.smartregister.fhircore.engine.appfeature.model.HealthModule
import org.smartregister.fhircore.engine.data.local.register.PatientRegisterRepository
import org.smartregister.fhircore.engine.domain.model.RegisterData
import org.smartregister.fhircore.quest.data.patient.model.PatientPagingSourceState
import org.smartregister.fhircore.quest.ui.shared.models.RegisterViewData
import org.smartregister.fhircore.quest.util.mappers.RegisterViewDataMapper

@OptIn(ExperimentalCoroutinesApi::class)
class PatientRegisterPagingSourceTest {

  val patientRegisterRepository = mockk<PatientRegisterRepository>()
  val registerViewDataMapper = spyk(RegisterViewDataMapper(mockk<Context>()))
  val hivRegisterData = mockk<RegisterData.HivRegisterData>()
  val hivRegisterViewData = mockk<RegisterViewData>()

  @Before
  fun setUp() {
    coEvery {
      patientRegisterRepository.loadRegisterData(
        any<Int>(),
        any<Boolean>(),
        any<String>(),
        any<HealthModule>()
      )
    } returns listOf(hivRegisterData)

    every { registerViewDataMapper.transformInputToOutputModel(eq(hivRegisterData)) } returns
      hivRegisterViewData
  }

  @Test
  fun loadReturnsPageOnSuccess() = runTest {
    val pagingSource =
      PatientRegisterPagingSource(patientRegisterRepository, registerViewDataMapper)
    val patientPagingSourceState = PatientPagingSourceState(loadAll = true)
    pagingSource.setPatientPagingSourceState(patientPagingSourceState)
    val loadPageResult: PagingSource.LoadResult<Int, RegisterViewData> =
      pagingSource.load(
        PagingSource.LoadParams.Refresh(key = null, loadSize = 2, placeholdersEnabled = false)
      )
    val expectedData =
      listOf(hivRegisterData).map { registerViewDataMapper.transformInputToOutputModel(it) }
    assertEquals(
      expected = PagingSource.LoadResult.Page(data = expectedData, prevKey = null, nextKey = 1),
      actual = loadPageResult
    )
    coVerify {
      patientRegisterRepository.loadRegisterData(
        currentPage = eq(patientPagingSourceState.currentPage),
        appFeatureName = isNull(),
        healthModule = eq(patientPagingSourceState.healthModule),
        loadAll = eq(patientPagingSourceState.loadAll)
      )
    }
  }
}
