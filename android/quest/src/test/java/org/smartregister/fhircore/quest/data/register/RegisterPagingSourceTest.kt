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

package org.smartregister.fhircore.quest.data.register

import androidx.paging.PagingSource
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.data.local.register.RegisterRepository
import org.smartregister.fhircore.engine.domain.model.RepositoryResourceData
import org.smartregister.fhircore.engine.rulesengine.RulesExecutor
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.data.register.model.RegisterPagingSourceState
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

@HiltAndroidTest
class RegisterPagingSourceTest : RobolectricTest() {

  @get:Rule val hiltAndroidRule = HiltAndroidRule(this)
  @Inject lateinit var rulesExecutor: RulesExecutor
  private val registerRepository = mockk<RegisterRepository>()
  private lateinit var registerPagingSource: RegisterPagingSource
  private val registerId = "registerId"

  @Before
  fun setUp() {
    hiltAndroidRule.inject()
    registerPagingSource = RegisterPagingSource(registerRepository, rulesExecutor, listOf())
  }

  @Test
  fun testLoadShouldReturnResults() {
    coEvery { registerRepository.loadRegisterData(0, registerId) } returns
      listOf(RepositoryResourceData(resource = Faker.buildPatient()))

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
