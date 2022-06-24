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
import com.google.android.fhir.logicalId
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.app.fakes.Faker
import org.smartregister.fhircore.engine.app.fakes.Faker.buildPatient
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.WorkflowPoint
import org.smartregister.fhircore.engine.configuration.app.AppConfigClassification
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.configuration.app.applicationConfigurationOf
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.data.local.register.dao.HivRegisterDao
import org.smartregister.fhircore.engine.domain.model.HealthStatus
import org.smartregister.fhircore.engine.domain.model.ProfileData
import org.smartregister.fhircore.engine.domain.model.RegisterData
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.rule.CoroutineTestRule
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider

@OptIn(ExperimentalCoroutinesApi::class)
class HivRegisterDaoTest : RobolectricTest() {

  @get:Rule(order = 1) val instantTaskExecutorRule = InstantTaskExecutorRule()

  @get:Rule(order = 2) val coroutineTestRule = CoroutineTestRule()

  private lateinit var hivRegisterDao: HivRegisterDao

  private val fhirEngine: FhirEngine = mockk()

  val defaultRepository: DefaultRepository =
    DefaultRepository(fhirEngine = fhirEngine, dispatcherProvider = DefaultDispatcherProvider())

  val configurationRegistry: ConfigurationRegistry = Faker.buildTestConfigurationRegistry(mockk())

  private val testPatient =
    buildPatient(
      id = "1",
      family = "doe",
      given = "john",
      age = 50,
      patientType = "exposed-infant",
      practitionerReference = "practitioner/1234"
    )
      .apply { active = true }

  private val testPatientGenderNull =
    buildPatient(
      id = "3",
      family = "doe",
      given = "jane",
      gender = null,
      patientType = "exposed-infant"
    )
      .apply { active = true }

  @Before
  fun setUp() {

    coEvery { fhirEngine.get(ResourceType.Patient, "1") } returns testPatient

    coEvery { fhirEngine.search<Patient>(any()) } returns listOf(testPatient, testPatientGenderNull)

    every { configurationRegistry.retrieveDataFilterConfiguration(any()) } returns emptyList()

    // Faker.loadTestConfigurationRegistryData(defaultRepository, configurationRegistry)

    val workflowPoint = mockk<WorkflowPoint>()
    every { configurationRegistry.workflowPointsMap[any()] } returns workflowPoint
    every { configurationRegistry.configurationsMap[any()] } returns
      applicationConfigurationOf(patientTypeFilterTagViaMetaCodingSystem = "https://d-tree.org")

    every {
      configurationRegistry.retrieveConfiguration<ApplicationConfiguration>(
        AppConfigClassification.APPLICATION
      )
    } returns
      applicationConfigurationOf(patientTypeFilterTagViaMetaCodingSystem = "https://d-tree.org")

    hivRegisterDao =
      HivRegisterDao(
        fhirEngine = fhirEngine,
        defaultRepository = defaultRepository,
        configurationRegistry = configurationRegistry
      )
  }

  @Test
  fun testLoadRegisterData() = runTest {
    val data =
      hivRegisterDao.loadRegisterData(currentPage = 0, loadAll = true, appFeatureName = "HIV")
    Assert.assertNotNull(data)
    val hivRegisterData = data[0] as RegisterData.HivRegisterData
    assertEquals(expected = "50y", actual = hivRegisterData.age)
    assertEquals(expected = "Dist 1 City 1", actual = hivRegisterData.address)
    assertEquals(expected = "John Doe", actual = hivRegisterData.name)
    assertEquals(expected = HealthStatus.EXPOSED_INFANT, actual = hivRegisterData.healthStatus)
    assertEquals(expected = Enumerations.AdministrativeGender.MALE, actual = hivRegisterData.gender)
  }

  @Test
  fun `loadRegisterData excludes Patients with gender null`() = runTest {
    val result =
      hivRegisterDao.loadRegisterData(currentPage = 0, loadAll = true, appFeatureName = "HIV")
    assertTrue {
      result.all {
        (it as RegisterData.HivRegisterData).gender != null &&
          it.logicalId != testPatientGenderNull.logicalId
      }
    }
  }

  @Test
  fun testLoadProfileData() {
    val data = runBlocking {
      hivRegisterDao.loadProfileData(appFeatureName = "HIV", resourceId = "1")
    }
    Assert.assertNotNull(data)
    val hivProfileData = data as ProfileData.HivProfileData
    Assert.assertEquals("50y", hivProfileData.age)
    Assert.assertEquals("Dist 1 City 1", hivProfileData.address)
    Assert.assertEquals("John Doe", hivProfileData.name)
    Assert.assertEquals("practitioner/1234", hivProfileData.chwAssigned.reference)
    Assert.assertEquals(HealthStatus.EXPOSED_INFANT, hivProfileData.healthStatus)
    Assert.assertEquals(Enumerations.AdministrativeGender.MALE, hivProfileData.gender)
  }

  @Test
  fun testGetRegisterDataFilters() {
    Assert.assertNotNull(hivRegisterDao.getRegisterDataFilters("1234"))
  }

  @Test
  fun testCountRegisterData() = runTest {
    val count = hivRegisterDao.countRegisterData("HIV")
    assertEquals(1, count)
  }
}
