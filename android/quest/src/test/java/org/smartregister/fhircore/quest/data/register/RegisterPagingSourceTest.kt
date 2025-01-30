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

package org.smartregister.fhircore.quest.data.register

import androidx.paging.PagingSource
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.Task
import org.jeasy.rules.api.Rules
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.data.local.register.RegisterRepository
import org.smartregister.fhircore.engine.domain.model.FhirResourceConfig
import org.smartregister.fhircore.engine.domain.model.RepositoryResourceData
import org.smartregister.fhircore.engine.domain.model.ResourceConfig
import org.smartregister.fhircore.engine.rulesengine.RulesExecutor
import org.smartregister.fhircore.engine.util.extension.asReference
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.data.register.model.RegisterPagingSourceState
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

@HiltAndroidTest
class RegisterPagingSourceTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltAndroidRule = HiltAndroidRule(this)

  @Inject lateinit var rulesExecutor: RulesExecutor
  private val registerRepository = mockk<RegisterRepository>()
  private lateinit var registerPagingSource: RegisterPagingSource
  private val registerId = "registerId"

  @Before
  fun setUp() {
    hiltAndroidRule.inject()
  }

  @Test
  fun testLoadWithNullFhirResourceConfigShouldReturnResults() {
    registerPagingSource =
      RegisterPagingSource(
        registerRepository = registerRepository,
        fhirResourceConfig = null,
        actionParameters = emptyMap(),
        rulesExecutor = rulesExecutor,
        registerPagingSourceState =
          RegisterPagingSourceState(
            registerId = registerId,
            currentPage = 0,
            loadAll = false,
            rules = Rules(setOf()),
          ),
      )
    coEvery { registerRepository.loadRegisterData(0, registerId) } returns
      listOf(RepositoryResourceData(resource = Faker.buildPatient()))

    val loadParams = mockk<PagingSource.LoadParams<Int>>()
    every { loadParams.key } returns null
    runBlocking {
      registerPagingSource.run {
        val result = load(loadParams)
        Assert.assertNotNull(result)
        Assert.assertEquals(1, (result as PagingSource.LoadResult.Page).data.size)
      }
    }
  }

  @Test
  fun testLoadWithProvidedFhirResourceConfigShouldReturnResults() {
    val baseResource = Faker.buildPatient()
    val relatedResources =
      listOf(
        Task().apply {
          id = "hiv-test-task"
          status = Task.TaskStatus.READY
          description = "Test patient HIV status"
          `for` = baseResource.asReference()
        },
      )

    val fhirResourceConfig =
      FhirResourceConfig(
        baseResource =
          ResourceConfig(
            id = ResourceType.Patient.name,
            resource = ResourceType.Patient,
          ),
        relatedResources =
          listOf(
            ResourceConfig(
              id = ResourceType.Task.name,
              resource = ResourceType.Task,
            ),
          ),
      )
    registerPagingSource =
      RegisterPagingSource(
        registerRepository = registerRepository,
        fhirResourceConfig = fhirResourceConfig,
        actionParameters = emptyMap(),
        registerPagingSourceState =
          RegisterPagingSourceState(
            registerId = registerId,
            currentPage = 0,
            loadAll = false,
            Rules(
              setOf(),
            ),
          ),
        rulesExecutor = rulesExecutor,
      )
    coEvery {
      registerRepository.loadRegisterData(
        currentPage = 0,
        registerId = registerId,
        fhirResourceConfig = fhirResourceConfig,
      )
    } returns
      listOf(
        RepositoryResourceData(
          resource = baseResource,
          relatedResourcesMap =
            relatedResources.groupBy { it.resourceType.name }.toMap(ConcurrentHashMap()),
        ),
      )

    val loadParams = mockk<PagingSource.LoadParams<Int>>()
    every { loadParams.key } returns null
    runBlocking {
      registerPagingSource.run {
        val result = load(loadParams)
        Assert.assertNotNull(result)
        Assert.assertEquals(1, (result as PagingSource.LoadResult.Page).data.size)
      }
    }
  }

  @Test
  fun testLoadWithEmptyRegisterDataShouldReturnEmptyResults() {
    registerPagingSource =
      RegisterPagingSource(
        registerRepository = registerRepository,
        fhirResourceConfig = null,
        actionParameters = emptyMap(),
        registerPagingSourceState =
          RegisterPagingSourceState(
            registerId = registerId,
            currentPage = 0,
            loadAll = false,
            rules =
              Rules(
                setOf(),
              ),
          ),
        rulesExecutor = rulesExecutor,
      )
    coEvery { registerRepository.loadRegisterData(0, registerId) } returns emptyList()
    val loadParams = mockk<PagingSource.LoadParams<Int>>()
    every { loadParams.key } returns null
    runBlocking {
      registerPagingSource.run {
        val result = load(loadParams)
        Assert.assertNotNull(result)
        Assert.assertTrue((result as PagingSource.LoadResult.Page).data.isEmpty())
      }
    }
  }

  @Test
  fun testLoadWithNonZeroInitialPageShouldReturnResults() {
    registerPagingSource =
      RegisterPagingSource(
        registerRepository = registerRepository,
        fhirResourceConfig = null,
        actionParameters = emptyMap(),
        registerPagingSourceState =
          RegisterPagingSourceState(
            registerId = registerId,
            currentPage = 2,
            loadAll = false,
            rules =
              Rules(
                setOf(),
              ),
          ),
        rulesExecutor = rulesExecutor,
      )
    coEvery { registerRepository.loadRegisterData(2, registerId) } returns
      listOf(RepositoryResourceData(resource = Faker.buildPatient()))
    val loadParams = mockk<PagingSource.LoadParams<Int>>()
    every { loadParams.key } returns 2
    runBlocking {
      registerPagingSource.run {
        val result = load(loadParams)
        Assert.assertNotNull(result)
        Assert.assertEquals(1, (result as PagingSource.LoadResult.Page).data.size)
      }
    }
  }

  @Test
  fun testLoadWithMultiplePagesShouldReturnPaginatedResults() {
    registerPagingSource =
      RegisterPagingSource(
        registerRepository = registerRepository,
        fhirResourceConfig = null,
        actionParameters = emptyMap(),
        registerPagingSourceState =
          RegisterPagingSourceState(
            registerId = registerId,
            currentPage = 0,
            loadAll = true,
            rules =
              Rules(
                setOf(),
              ),
          ),
        rulesExecutor = rulesExecutor,
      )
    coEvery { registerRepository.loadRegisterData(0, registerId) } returns
      listOf(RepositoryResourceData(resource = Faker.buildPatient()))
    coEvery { registerRepository.loadRegisterData(1, registerId) } returns
      listOf(RepositoryResourceData(resource = Faker.buildPatient()))
    val loadParams = mockk<PagingSource.LoadParams<Int>>()
    every { loadParams.key } returns null
    runBlocking {
      registerPagingSource.run {
        var result = load(loadParams)
        Assert.assertNotNull(result)
        Assert.assertEquals(1, (result as PagingSource.LoadResult.Page).data.size)
        every { loadParams.key } returns 1
        result = load(loadParams)
        Assert.assertNotNull(result)
        Assert.assertEquals(1, (result as PagingSource.LoadResult.Page).data.size)
      }
    }
  }

  @Test
  fun testLoadWithNonEmptyActionParametersShouldReturnResults() = runTest {
    val actionParameters = mapOf("param1" to "value1")
    registerPagingSource =
      RegisterPagingSource(
        registerRepository = registerRepository,
        fhirResourceConfig = null,
        actionParameters = actionParameters,
        registerPagingSourceState =
          RegisterPagingSourceState(
            registerId = registerId,
            currentPage = 0,
            loadAll = false,
            rules = Rules(setOf()),
          ),
        rulesExecutor = rulesExecutor,
      )
    coEvery {
      registerRepository.loadRegisterData(
        currentPage = 0,
        registerId = registerId,
        paramsMap = actionParameters,
      )
    } returns listOf(RepositoryResourceData(resource = Faker.buildPatient()))
    val loadParams = mockk<PagingSource.LoadParams<Int>>()
    every { loadParams.key } returns null
    registerPagingSource.run {
      val result = load(loadParams)
      Assert.assertNotNull(result)
      Assert.assertEquals(1, (result as PagingSource.LoadResult.Page).data.size)
    }
  }
}
