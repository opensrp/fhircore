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
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.Search
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.util.Calendar
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.DateType
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.RelatedPerson
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.Task
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
import org.smartregister.fhircore.engine.domain.model.HealthStatus
import org.smartregister.fhircore.engine.domain.model.ProfileData
import org.smartregister.fhircore.engine.domain.model.RegisterData
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.rule.CoroutineTestRule
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.extension.asReference
import org.smartregister.fhircore.engine.util.extension.referenceValue

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

  private val testTask1 =
    Task().apply {
      this.id = "1"
      this.meta.addTag(
        Coding().apply {
          system = "https://d-tree.org"
          code = "clinic-visit-task-order-3"
        }
      )
    }

  private val testTask2 =
    Task().apply {
      this.id = "2"
      this.meta.addTag(
        Coding().apply {
          system = "https://d-tree.org"
          code = "clinic-visit-task-order-1"
        }
      )
    }

  private val carePlan1 =
    CarePlan().apply {
      id = "CarePlan/cp1"
      status = CarePlan.CarePlanStatus.ACTIVE
      careTeam = listOf(Reference("Ref11"), Reference("Ref12"))
      addActivity(
        CarePlan.CarePlanActivityComponent().apply {
          outcomeReference.add(Reference(testTask1.referenceValue()))
        }
      )
    }

  private val carePlan2 =
    CarePlan().apply {
      id = "CarePlan/cp2"
      status = CarePlan.CarePlanStatus.ACTIVE
      careTeam = listOf(Reference("Ref21"), Reference("Ref22"))
      addActivity(
        CarePlan.CarePlanActivityComponent().apply {
          outcomeReference.add(Reference(testTask2.referenceValue()))
        }
      )
    }

  @Before
  fun setUp() {

    coEvery { fhirEngine.get(ResourceType.Patient, "1") } returns testPatient

    coEvery { fhirEngine.get(ResourceType.Task, testTask1.logicalId) } returns testTask1
    coEvery { fhirEngine.get(ResourceType.Task, testTask2.logicalId) } returns testTask2

    coEvery { fhirEngine.search<Resource>(any()) } answers
      {
        val search = firstArg<Search>()
        when (search.type) {
          ResourceType.Patient -> listOf<Patient>(testPatient, testPatientGenderNull)
          ResourceType.Task -> listOf<Task>(testTask1, testTask2)
          ResourceType.CarePlan -> listOf<CarePlan>(carePlan1, carePlan2)
          else -> emptyList()
        }
      }

    every { configurationRegistry.retrieveDataFilterConfiguration(any()) } returns emptyList()

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
  fun `loadPatient gets patient with id`() = runTest {
    val patientResult = hivRegisterDao.loadPatient(testPatient.id)
    Assert.assertEquals(testPatient, patientResult)
    coVerify(exactly = 1) { fhirEngine.get(ResourceType.Patient, testPatient.id) }
  }

  @Test
  fun testLoadRegisterData() = runTest {
    val data =
      hivRegisterDao.loadRegisterData(currentPage = 0, loadAll = true, appFeatureName = "HIV")

    assertNotNull(data)
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
    assertNotNull(data)
    val hivProfileData = data as ProfileData.HivProfileData
    assertEquals("50y", hivProfileData.age)
    assertEquals("Dist 1 City 1", hivProfileData.address)
    assertEquals("John Doe", hivProfileData.name)
    assertEquals("practitioner/1234", hivProfileData.chwAssigned.reference)
    assertEquals(HealthStatus.EXPOSED_INFANT, hivProfileData.healthStatus)
    assertEquals(Enumerations.AdministrativeGender.MALE, hivProfileData.gender)
  }

  @Test
  fun `loadGuardiansRegisterData returns from relatedPersons`() = runTest {
    val guardianRelatedPerson =
      RelatedPerson().apply {
        id = "22"
        gender = Enumerations.AdministrativeGender.MALE
        birthDate = DateType(Date()).apply { add(Calendar.YEAR, -32) }.dateTimeValue().value
      }
    val guardianReference = guardianRelatedPerson.asReference()
    coEvery { fhirEngine.get(ResourceType.RelatedPerson, guardianRelatedPerson.logicalId) } returns
      guardianRelatedPerson
    testPatient.apply {
      link = mutableListOf(Patient.PatientLinkComponent().apply { other = guardianReference })
    }

    val result = hivRegisterDao.loadGuardiansRegisterData(testPatient).firstOrNull()
    coVerify { fhirEngine.get(ResourceType.RelatedPerson, guardianRelatedPerson.logicalId) }
    assertNotNull(result)
    assertEquals(guardianRelatedPerson.logicalId, result.logicalId)
    assertEquals(HealthStatus.NOT_ON_ART, result.healthStatus)
    assertEquals("Not on ART", result.healthStatus.display)
    assertEquals(HivRegisterDao.ResourceValue.BLANK, result.identifier)
    assertEquals(HivRegisterDao.ResourceValue.BLANK, result.chwAssigned)
  }

  @Test
  fun `loadGuardiansRegisterData returns from patient's link`() = runTest {
    val guardianPatient =
      Patient().apply {
        id = "38"
        gender = Enumerations.AdministrativeGender.MALE
        meta.addTag(
          Coding().apply {
            system = "https://d-tree.org"
            code = "client-already-on-art"
          }
        )
        birthDate = DateType(Date()).apply { add(Calendar.YEAR, 48) }.dateTimeValue().value
      }
    val guardianReference = guardianPatient.asReference()
    coEvery { fhirEngine.get(ResourceType.Patient, guardianPatient.logicalId) } returns
      guardianPatient
    testPatient.apply {
      val patientLink =
        Patient.PatientLinkComponent().apply {
          other = guardianReference
          type = Patient.LinkType.REFER
        }
      link = mutableListOf(patientLink)
    }

    val result = hivRegisterDao.loadGuardiansRegisterData(testPatient).firstOrNull()
    coVerify { fhirEngine.get(ResourceType.Patient, guardianPatient.logicalId) }
    assertNotNull(result)
    assertEquals(guardianPatient.logicalId, result.logicalId)
    assertEquals(HealthStatus.CLIENT_ALREADY_ON_ART, result.healthStatus)
    assertEquals("ART Client", result.healthStatus.display)
  }

  @Test
  fun `loadProfileData loads tasks in specified order from meta tag`() {
    val data = runBlocking {
      hivRegisterDao.loadProfileData(appFeatureName = "HIV", resourceId = "1")
    }
    assertNotNull(data)
    val hivProfileData = data as ProfileData.HivProfileData
    assertNotNull(hivProfileData.tasks)
    assertEquals(2, hivProfileData.tasks.size)
    // assert testTask2 comes before testTest1 according to meta tag
    // 'clinic-visit-task-order-{order_number}'
    assertEquals(testTask2, hivProfileData.tasks[0])
    assertEquals(testTask1, hivProfileData.tasks[1])
  }

  @Test
  fun `test hiv patient identifier to be the 'official' identifier`() = runTest {
    val identifierNumber = "123456"
    val patient =
      Patient().apply {
        identifier.add(Identifier())

        identifier.add(
          Identifier().apply {
            this.use = Identifier.IdentifierUse.OFFICIAL
            this.value = identifierNumber
          }
        )

        identifier.add(
          Identifier().apply {
            this.use = Identifier.IdentifierUse.SECONDARY
            this.value = "149856"
          }
        )
      }
    assertEquals(identifierNumber, hivRegisterDao.hivPatientIdentifier(patient))
  }

  @Test
  fun `test hiv patient identifier to be empty string when no 'official' identifier found`() =
      runTest {
    val identifierNumber = "149856"
    val patient =
      Patient().apply {
        identifier.add(Identifier())

        identifier.add(
          Identifier().apply {
            this.use = Identifier.IdentifierUse.SECONDARY
            this.value = identifierNumber
          }
        )
      }
    assertEquals("", hivRegisterDao.hivPatientIdentifier(patient))
  }

  @Test
  fun testGetRegisterDataFilters() {
    assertNotNull(hivRegisterDao.getRegisterDataFilters("1234"))
  }

  @Test
  fun testCountRegisterData() = runTest {
    val count = hivRegisterDao.countRegisterData("HIV")
    assertEquals(1, count)
  }

  private val testHivPatient =
    buildPatient(
      id = "logicalId",
      family = "doe",
      given = "john",
      age = 50,
      patientType = "exposed-infant",
      practitionerReference = "practitioner/1234"
    )
      .apply {
        active = true
        identifier.add(
          Identifier().apply {
            this.use = Identifier.IdentifierUse.SECONDARY
            this.value = "149856"
          }
        )
        meta.addTag(
          Coding().apply {
            system = "https://d-tree.org"
            code = "exposed-infant"
            display = "Exposed Infant"
          }
        )
      }

  @Test
  fun testTransformChildrenPatientToRegisterData() = runTest {
    val patient = testHivPatient.apply { active = true }
    val childRegisterData = hivRegisterDao.transformChildrenPatientToRegisterData(listOf(patient))
    assertEquals(1, childRegisterData.size)
    val registerDataPatient = childRegisterData[0] as RegisterData.HivRegisterData
    with(registerDataPatient) {
      assertEquals("logicalId", logicalId)
      assertEquals("John Doe", name)
      assertEquals("Dist 1 City 1", address)
      assertEquals("50y", age)
      assertEquals(emptyList(), phoneContacts)
      assertEquals(Enumerations.AdministrativeGender.MALE, gender)
      assertEquals("practitioner/1234", chwAssigned)
      assertEquals("practitioner/1234", practitioners?.get(0)?.reference)
      assertNotEquals(HealthStatus.DEFAULT, healthStatus)
    }
  }
}
