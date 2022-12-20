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

package org.smartregister.fhircore.engine.data.local.register.dao

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.android.fhir.FhirEngine
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Group
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.app.fakes.Faker
import org.smartregister.fhircore.engine.app.fakes.Faker.buildPatient
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.rule.CoroutineTestRule
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
class AppointmentRegisterDaoTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val instantTaskExecutorRule = InstantTaskExecutorRule()

  @get:Rule(order = 2) val coroutineTestRule = CoroutineTestRule()

  private lateinit var appointmentRegisterDao: AppointmentRegisterDao

  private val fhirEngine: FhirEngine = mockk()

  var defaultRepository: DefaultRepository =
    DefaultRepository(fhirEngine = fhirEngine, dispatcherProvider = DefaultDispatcherProvider())

  var configurationRegistry: ConfigurationRegistry = Faker.buildTestConfigurationRegistry(mockk())

  @Before
  fun setUp() {
    hiltRule.inject()

    coEvery { fhirEngine.get(ResourceType.Patient, "1234") } returns
      buildPatient("1", "doe", "john", 50)

    coEvery { fhirEngine.search<Group>(any()) } returns emptyList()

    coEvery { configurationRegistry.retrieveDataFilterConfiguration(any()) } returns emptyList()

    appointmentRegisterDao =
      AppointmentRegisterDao(
        fhirEngine = fhirEngine,
        defaultRepository = defaultRepository,
        configurationRegistry = configurationRegistry,
        dispatcherProvider = DefaultDispatcherProvider()
      )
  }

  @Test
  fun testLoadProfileData() {
    val data = runBlocking {
      appointmentRegisterDao.loadProfileData(appFeatureName = "HIV", resourceId = "1234")
    }
    // TODO update this test once AppointmentRegisterDao
    //  implements load Patient and Profile Data
    Assert.assertNull(data)
  }

  @Test
  fun testCountRegisterData() {
    val count = runBlocking { appointmentRegisterDao.countRegisterData("1234") }
    Assert.assertTrue(count >= 0)
  }

  @Test
  fun testGetRegisterDataFilters() {
    Assert.assertNotNull(appointmentRegisterDao.getRegisterDataFilters())
  }
}
