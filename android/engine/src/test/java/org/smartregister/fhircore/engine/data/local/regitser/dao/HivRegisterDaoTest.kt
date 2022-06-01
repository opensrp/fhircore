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
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Group
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.app.fakes.Faker
import org.smartregister.fhircore.engine.app.fakes.Faker.buildPatient
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.AppConfigClassification
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.configuration.app.applicationConfigurationOf
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.data.local.register.dao.HivRegisterDao
import org.smartregister.fhircore.engine.domain.model.PatientType
import org.smartregister.fhircore.engine.domain.model.ProfileData
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.rule.CoroutineTestRule
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider

@HiltAndroidTest
internal class HivRegisterDaoTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val instantTaskExecutorRule = InstantTaskExecutorRule()

  @get:Rule(order = 2) val coroutineTestRule = CoroutineTestRule()

  private lateinit var hivRegisterDao: HivRegisterDao

  private val fhirEngine: FhirEngine = mockk()

  var defaultRepository: DefaultRepository =
    DefaultRepository(fhirEngine = fhirEngine, dispatcherProvider = DefaultDispatcherProvider())

  var configurationRegistry: ConfigurationRegistry = Faker.buildTestConfigurationRegistry(mockk())

  @Before
  fun setUp() {
    hiltRule.inject()

    val testPatient: Patient =
      buildPatient(
        id = "1",
        family = "doe",
        given = "john",
        age = 50,
        patientType = "exposed-infant",
        practitionerReference = "practitioner/1234"
      )

    coEvery { fhirEngine.get(ResourceType.Patient, "1234") } returns testPatient

    coEvery { fhirEngine.search<Patient>(any()) } returns listOf(testPatient)

    coEvery { fhirEngine.search<Group>(any()) } returns emptyList()

    coEvery { configurationRegistry.retrieveDataFilterConfiguration(any()) } returns emptyList()

    // Faker.loadTestConfigurationRegistryData(defaultRepository, configurationRegistry)

    coEvery {
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
  fun testLoadRegisterData() {
    val data = runBlocking {
      hivRegisterDao.loadRegisterData(currentPage = 0, loadAll = true, appFeatureName = "HIV")
    }
    Assert.assertNotNull(data)
    /* Todo fix this test for coverage
    val hivRegisterData = data[0] as RegisterData.HivRegisterData
    Assert.assertEquals("50y", hivRegisterData.age)
    Assert.assertEquals("Dist 1 City 1", hivRegisterData.address)
    Assert.assertEquals("John Doe", hivRegisterData.name)
    Assert.assertEquals(PatientType.EXPOSED_INFANT, hivRegisterData.patientType)
    Assert.assertEquals(Enumerations.AdministrativeGender.MALE, hivRegisterData.gender)
     */
  }

  @Ignore("will need to fix mocking appConfigRegistration in HivRegisterDao")
  @Test
  fun testLoadProfileData() {
    val data = runBlocking {
      hivRegisterDao.loadProfileData(appFeatureName = "HIV", resourceId = "1234")
    }
    Assert.assertNotNull(data)
    val hivProfileData = data as ProfileData.HivProfileData
    Assert.assertEquals("50y", hivProfileData.age)
    Assert.assertEquals("Dist 1 City 1", hivProfileData.address)
    Assert.assertEquals("John Doe", hivProfileData.name)
    Assert.assertEquals("practitioner/1234", hivProfileData.chwAssigned)
    Assert.assertEquals(PatientType.EXPOSED_INFANT, hivProfileData.patientType)
    Assert.assertEquals(Enumerations.AdministrativeGender.MALE, hivProfileData.gender)
  }

  @Test
  fun testGetRegisterDataFilters() {
    Assert.assertNotNull(hivRegisterDao.getRegisterDataFilters("1234"))
  }

  @Test
  fun testCountRegisterData() {
    val count = runBlocking { hivRegisterDao.countRegisterData("1234") }
    Assert.assertTrue(count >= 0)
  }
}
