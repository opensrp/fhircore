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

package org.smartregister.fhircore.quest.data.register

import androidx.paging.PagingSource
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.smartregister.fhircore.engine.data.local.register.RegisterRepository
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.quest.data.register.model.RegisterPagingSourceState

class RegisterPagingSourceTest {

  private val registerRepository = mockk<RegisterRepository>()

  private lateinit var registerPagingSource: RegisterPagingSource

  private val registerId = "registerId"

  @Before
  fun setUp() {
    registerPagingSource = RegisterPagingSource(registerRepository)
  }

  @Test
  fun testLoadShouldReturnResults() {
    coEvery { registerRepository.loadRegisterData(0, registerId) } returns
      listOf(
        ResourceData(
          baseResourceId = "resourceId",
          baseResourceType = ResourceType.Patient,
          computedValuesMap = emptyMap(),
          listResourceDataMap = emptyMap(),
        )
      )

    val loadParams = mockk<PagingSource.LoadParams<Int>>()
    every { loadParams.key } returns null
    runBlocking {
      registerPagingSource.run {
        setPatientPagingSourceState(
          RegisterPagingSourceState(registerId = registerId, currentPage = 0, loadAll = false)
        )
        val result = load(loadParams)
        Assert.assertNotNull(result)
        Assert.assertEquals(1, (result as PagingSource.LoadResult.Page).data.size)
      }
    }
  }
}
