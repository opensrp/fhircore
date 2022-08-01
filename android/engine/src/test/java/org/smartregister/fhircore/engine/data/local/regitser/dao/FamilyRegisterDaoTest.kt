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

package org.smartregister.fhircore.engine.data.local.regitser.dao

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.android.fhir.FhirEngine
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Group
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.app.fakes.Faker
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.data.local.register.dao.FamilyRegisterDao
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.rule.CoroutineTestRule
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider

@HiltAndroidTest
internal class FamilyRegisterDaoTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val instantTaskExecutorRule = InstantTaskExecutorRule()

  @get:Rule(order = 2) val coroutineTestRule = CoroutineTestRule()

  private lateinit var familyRegisterDao: FamilyRegisterDao

  private val fhirEngine: FhirEngine = mockk()

  var defaultRepository: DefaultRepository =
    DefaultRepository(fhirEngine = fhirEngine, dispatcherProvider = DefaultDispatcherProvider())

  var configurationRegistry: ConfigurationRegistry = Faker.buildTestConfigurationRegistry(mockk())

  @Before
  fun setUp() {
    hiltRule.inject()

    coEvery { fhirEngine.get(ResourceType.Patient, any()) } returns
      Faker.buildPatient("1", "doe", "john", 50)

    coEvery { fhirEngine.search<Group>(any()) } returns
      listOf(
        Group().apply {
          id = "12"
          active = true
          name = "ABC Family"
          addMember().apply { this.entity.reference = "Patient/123" }
        }
      )

    coEvery { configurationRegistry.retrieveDataFilterConfiguration(any()) } returns emptyList()

    familyRegisterDao =
      spyk(
        FamilyRegisterDao(
          fhirEngine = fhirEngine,
          defaultRepository = defaultRepository,
          configurationRegistry = configurationRegistry
        )
      )

    coEvery { familyRegisterDao.loadMemberCondition(any()) } returns emptyList()
    coEvery { familyRegisterDao.loadMemberCarePlan(any()) } returns emptyList()
  }

  @Test
  fun testLoadRegisterData() {
    val data = runBlocking {
      familyRegisterDao.loadRegisterData(currentPage = 0, loadAll = true, appFeatureName = "FAMILY")
    }
    Assert.assertEquals(1, data.size)
  }
}
