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
import com.google.android.fhir.SearchResult
import com.google.android.fhir.db.ResourceNotFoundException
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.Search
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.Appointment
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.ListResource
import org.hl7.fhir.r4.model.Meta
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Period
import org.hl7.fhir.r4.model.Practitioner
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
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.data.local.TracingAgeFilterEnum
import org.smartregister.fhircore.engine.data.local.TracingRegisterFilter
import org.smartregister.fhircore.engine.data.local.tracing.TracingRepository
import org.smartregister.fhircore.engine.domain.model.ProfileData
import org.smartregister.fhircore.engine.domain.model.RegisterData
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.rule.CoroutineTestRule
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.LOGGED_IN_PRACTITIONER
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.asReference
import org.smartregister.fhircore.engine.util.extension.referenceValue

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
class TracingRegisterDaoTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
  @get:Rule(order = 1) val instantTaskExecutorRule = InstantTaskExecutorRule()
  @get:Rule(order = 2) val coroutineTestRule = CoroutineTestRule()

  @BindValue val sharedPreferencesHelper = mockk<SharedPreferencesHelper>(relaxed = true)

  private val fhirEngine = mockk<FhirEngine>()
  private val tracingRepository = spyk(TracingRepository(fhirEngine))
  private val dispatcherProvider = DefaultDispatcherProvider()
  private lateinit var defaultRepository: DefaultRepository
  private val configurationRegistry = Faker.buildTestConfigurationRegistry()
  private lateinit var tracingRegisterDao: TracingRegisterDao
  @get:Rule(order = 2) var coroutineRule = CoroutineTestRule()

  @Inject lateinit var configService: ConfigService
  @Before
  fun setUp() {
    hiltRule.inject()
    defaultRepository =
      spyk(
        DefaultRepository(
          fhirEngine = fhirEngine,
          dispatcherProvider = coroutineRule.testDispatcherProvider,
          sharedPreferencesHelper = sharedPreferencesHelper,
          configurationRegistry = configurationRegistry,
          configService = configService
        )
      )
    coEvery { configurationRegistry.retrieveDataFilterConfiguration(any()) } returns emptyList()

    every {
      sharedPreferencesHelper.read<Practitioner>(LOGGED_IN_PRACTITIONER, decodeWithGson = true)
    } returns Practitioner().apply { id = "123" }

    tracingRegisterDao =
      HomeTracingRegisterDao(
        fhirEngine = fhirEngine,
        tracingRepository = tracingRepository,
        defaultRepository = defaultRepository,
        configurationRegistry = configurationRegistry,
        dispatcherProvider = dispatcherProvider,
        sharedPreferencesHelper = sharedPreferencesHelper
      )
  }

  @Test
  fun countRegisterDataReturnsExpectedCount() = runTest {
    val patient0 = Patient().apply { id = "patient0" }
    val task1 =
      Task().apply {
        status = Task.TaskStatus.READY
        executionPeriod = Period().apply { start = Date() }
        `for` = patient0.asReference()
      }
    coEvery { fhirEngine.search<Resource>(any()) } answers
      {
        val searchObj = firstArg<Search>()
        when (searchObj.type) {
          ResourceType.Patient ->
            listOf(SearchResult(patient0, included = null, revIncluded = null))
          ResourceType.Task -> listOf(SearchResult(task1, included = null, revIncluded = null))
          else -> emptyList()
        }
      }
    Assert.assertEquals(1, tracingRegisterDao.countRegisterData(appFeatureName = null))
  }

  @Test
  fun loadRegisterDataReturnsTracingRegisterData() = runTest {
    val patient0 =
      Faker.buildPatient("patient0", "doe", "john", 30, patientType = "client-already-on-art")

    val task1 =
      Task().apply {
        status = Task.TaskStatus.READY
        executionPeriod = Period().apply { start = Date() }
        `for` = patient0.asReference()
        reasonCode =
          CodeableConcept(
            Coding().apply {
              system = "https://dtree.org"
              code = "linkage"
            }
          )
      }

    coEvery { fhirEngine.search<Resource>(any()) } answers
      {
        val searchObj = firstArg<Search>()
        when {
          searchObj.type == ResourceType.Patient ->
            listOf(SearchResult(patient0, included = null, revIncluded = null))
          searchObj.type == ResourceType.Task ->
            listOf(SearchResult(task1, included = null, revIncluded = null))
          searchObj.type == ResourceType.Condition -> emptyList()
          searchObj.type == ResourceType.List && searchObj.count == 1 -> emptyList()
          else -> emptyList()
        }
      }

    val data = tracingRegisterDao.loadRegisterData(0, loadAll = false, appFeatureName = null)
    Assert.assertEquals(1, data.size)
    Assert.assertTrue(data.all { it is RegisterData.TracingRegisterData })
  }

  @Test
  fun countRegisterFilteredReturnsExpectedCount() = runTest {
    val patient0 =
      Faker.buildPatient("patient0", "doe", "john", 19, patientType = "client-already-on-art")
    val patient1 =
      Faker.buildPatient(
        "patient1",
        "doe",
        "jane",
        16,
        gender = Enumerations.AdministrativeGender.FEMALE,
        patientType = "community-positive"
      )
    val patient2 = Faker.buildPatient("patient2", "doe", "jimmy", 1, patientType = "exposed-infant")
    val task1 =
      Task().apply {
        status = Task.TaskStatus.READY
        executionPeriod = Period().apply { start = Date() }
        `for` = patient0.asReference()
        meta =
          Meta().apply {
            addTag(
              Coding().apply {
                system = "https://dtree.org"
                code = "home-tracing"
                display = "Home Tracing"
              }
            )
          }
        reasonCode =
          CodeableConcept(
            Coding().apply {
              system = "https://dtree.org"
              code = "miss-routine"
            }
          )
        reasonReference = Reference().apply { reference = "Questionnaire/art-tracing-outcome" }
      }
    val task2 =
      Task().apply {
        status = Task.TaskStatus.READY
        executionPeriod = Period().apply { start = Date() }
        `for` = patient1.asReference()
        meta =
          Meta().apply {
            addTag(
              Coding().apply {
                system = "https://dtree.org"
                code = "home-tracing"
                display = "Home Tracing"
              }
            )
          }
        reasonCode =
          CodeableConcept(
            Coding().apply {
              system = "https://dtree.org"
              code = "dual-referral"
            }
          )
        reasonReference = Reference().apply { reference = "Questionnaire/art-tracing-outcome" }
      }
    val task3 =
      Task().apply {
        status = Task.TaskStatus.READY
        executionPeriod = Period().apply { start = Date() }
        `for` = patient2.asReference()
        meta =
          Meta().apply {
            addTag(
              Coding().apply {
                system = "https://dtree.org"
                code = "home-tracing"
                display = "Home Tracing"
              }
            )
          }
        reasonCode =
          CodeableConcept(
            Coding().apply {
              system = "https://dtree.org"
              code = "hvl"
            }
          )
        reasonReference = Reference().apply { reference = "Questionnaire/art-tracing-outcome" }
      }

    coEvery { fhirEngine.search<Resource>(any()) } answers
      {
        val searchObj = firstArg<Search>()
        when (searchObj.type) {
          ResourceType.Patient ->
            listOf(
              SearchResult(patient0, included = null, revIncluded = null),
              SearchResult(patient1, included = null, revIncluded = null),
              SearchResult(patient2, included = null, revIncluded = null)
            )
          ResourceType.Task ->
            listOf(
              SearchResult(task1, included = null, revIncluded = null),
              SearchResult(task2, included = null, revIncluded = null),
              SearchResult(task3, included = null, revIncluded = null)
            )
          else -> emptyList()
        }
      }

    Assert.assertEquals(
      1,
      tracingRegisterDao.countRegisterFiltered(
        appFeatureName = null,
        filters =
          TracingRegisterFilter(
            isAssignedToMe = false,
            patientCategory = null,
            reasonCode = "hvl",
            age = TracingAgeFilterEnum.ZERO_TO_2
          )
      )
    )
    Assert.assertEquals(
      1,
      tracingRegisterDao.countRegisterFiltered(
        appFeatureName = null,
        filters =
          TracingRegisterFilter(
            isAssignedToMe = false,
            patientCategory = null,
            reasonCode = "dual-referral",
            age = TracingAgeFilterEnum.ZERO_TO_18
          )
      )
    )
    Assert.assertEquals(
      1,
      tracingRegisterDao.countRegisterFiltered(
        appFeatureName = null,
        filters =
          TracingRegisterFilter(
            isAssignedToMe = false,
            patientCategory = null,
            reasonCode = "miss-routine",
            age = TracingAgeFilterEnum.`18_PLUS`
          )
      )
    )
  }

  @Test
  fun loadRegisterFilteredReturnsExpectedRegisterData() = runTest {
    val patient0 =
      Faker.buildPatient("patient0", "doe", "john", 19, patientType = "client-already-on-art")
    val patient1 =
      Faker.buildPatient(
        "patient1",
        "doe",
        "jane",
        16,
        gender = Enumerations.AdministrativeGender.FEMALE,
        patientType = "community-positive"
      )
    val task0 =
      Task().apply {
        id = "task0"
        status = Task.TaskStatus.READY
        executionPeriod = Period().apply { start = Date() }
        `for` = patient0.asReference()
        meta =
          Meta().apply {
            addTag(
              Coding().apply {
                system = "https://dtree.org"
                code = "home-tracing"
                display = "Home Tracing"
              }
            )
          }
        reasonCode =
          CodeableConcept(
            Coding().apply {
              system = "https://dtree.org"
              code = "miss-routine"
            }
          )
        reasonReference = Reference().apply { reference = "Questionnaire/art-tracing-outcome" }
      }
    val task1 =
      Task().apply {
        id = "task1"
        status = Task.TaskStatus.READY
        executionPeriod = Period().apply { start = Date() }
        `for` = patient1.asReference()
        meta =
          Meta().apply {
            addTag(
              Coding().apply {
                system = "https://dtree.org"
                code = "home-tracing"
                display = "Home Tracing"
              }
            )
          }
        reasonCode =
          CodeableConcept(
            Coding().apply {
              system = "https://dtree.org"
              code = "dual-referral"
            }
          )
        reasonReference = Reference().apply { reference = "Questionnaire/art-tracing-outcome" }
      }
    val enc0 =
      Encounter().apply {
        id = "enc0"
        status = Encounter.EncounterStatus.FINISHED
        class_ =
          Coding().apply {
            system = "https://d-tree.org"
            code = "IMP"
            display = "inpatient encounter"
          }
        serviceType =
          CodeableConcept(
            Coding().apply {
              system = "https://d-tree.org"
              code = "home-tracing-outcome"
            }
          )
        subject = patient0.asReference()
        period = Period().apply { start = Date() }
        addReasonCode().apply {
          addCoding().apply {
            system = "https://d-tree.org"
            code = "miss-routine"
            display = "Missed Routine"
          }
          text = "Missed Routine"
        }
      }
    val obs0 =
      Observation().apply {
        id = "obs0"
        encounter = enc0.asReference()
        code =
          CodeableConcept().apply {
            addCoding().apply {
              system = "https://d-tree.org"
              code = "tracing-outcome-conducted"
            }
          }
        value = CodeableConcept().apply { text = "" }
      }
    val list0 =
      ListResource().apply {
        id = "list0"
        status = ListResource.ListStatus.CURRENT
        title = "Tracing Encounter List_1"
        orderedBy =
          CodeableConcept(
              Coding().apply {
                system = "https://d-tree.org"
                code = "1"
              }
            )
            .apply { text = "1" }
        subject = patient0.asReference()
        addEntry().apply {
          flag =
            CodeableConcept(
                Coding().apply {
                  system = "https://d-tree.org"
                  code = "tracing-task"
                }
              )
              .apply { text = task0.referenceValue() }
          item = task0.asReference()
        }
        addEntry().apply {
          flag = CodeableConcept(Coding()).apply {}
          item = enc0.asReference()
        }
      }

    coEvery { fhirEngine.search<Resource>(any()) } answers
      {
        val searchObj = firstArg<Search>()
        when {
          searchObj.type == ResourceType.Patient ->
            listOf(
              SearchResult(patient0, included = null, revIncluded = null),
              SearchResult(patient1, included = null, revIncluded = null)
            )
          searchObj.type == ResourceType.Task ->
            listOf(
              SearchResult(task0, included = null, revIncluded = null),
              SearchResult(task1, included = null, revIncluded = null)
            )
          searchObj.type == ResourceType.Condition -> emptyList()
          searchObj.type == ResourceType.List ->
            listOf(SearchResult(list0, included = null, revIncluded = null))
          searchObj.type == ResourceType.Observation && searchObj.count == 1 ->
            listOf(SearchResult(obs0, included = null, revIncluded = null))
          else -> emptyList()
        }
      }

    coEvery { fhirEngine.get(ResourceType.Encounter, "enc0") } returns enc0

    val data =
      tracingRegisterDao.loadRegisterFiltered(
        0,
        loadAll = false,
        appFeatureName = null,
        filters =
          TracingRegisterFilter(
            isAssignedToMe = false,
            patientCategory = null,
            reasonCode = null,
            age = null
          )
      )

    Assert.assertEquals(2, data.size)
    Assert.assertEquals(patient1.logicalId, data.elementAt(0).logicalId)
    Assert.assertEquals(0, (data.elementAt(0) as RegisterData.TracingRegisterData).attempts)

    Assert.assertEquals(patient0.logicalId, data.elementAt(1).logicalId)
    Assert.assertEquals(1, (data.elementAt(1) as RegisterData.TracingRegisterData).attempts)
  }

  @Test
  fun loadProfileDataReturnsExpectedProfileData() = runTest {
    val practitioner =
      sharedPreferencesHelper.read<Practitioner>(LOGGED_IN_PRACTITIONER, decodeWithGson = true)!!
    val guardian0 = RelatedPerson().apply { id = "guardian0" }
    val guardian1 = Faker.buildPatient("guardian1")

    val patient0 =
      Faker.buildPatient("patient0", "doe", "little-john", 1, patientType = "exposed-infant")
        .apply {
          addIdentifier().apply {
            use = Identifier.IdentifierUse.OFFICIAL
            value = "123456432"
          }
          addGeneralPractitioner(practitioner.asReference())
          addLink().apply { other = guardian0.asReference() }
          addLink().apply {
            type = Patient.LinkType.REFER
            other = guardian1.asReference()
          }
        }
    val carePlan0 =
      CarePlan().apply {
        status = CarePlan.CarePlanStatus.ACTIVE
        subject = patient0.asReference()
      }
    val task0 =
      Task().apply {
        id = "task0"
        status = Task.TaskStatus.READY
        executionPeriod = Period().apply { start = Date() }
        `for` = patient0.asReference()
        meta =
          Meta().apply {
            addTag(
              Coding().apply {
                system = "https://dtree.org"
                code = "home-tracing"
                display = "Home Tracing"
              }
            )
          }
        reasonCode =
          CodeableConcept(
            Coding().apply {
              system = "https://dtree.org"
              code = "hvl"
            }
          )
        reasonReference = Reference().apply { reference = "Questionnaire/art-tracing-outcome" }
      }
    val appointment0 =
      Appointment().apply {
        status = Appointment.AppointmentStatus.BOOKED
        addParticipant().apply { actor = patient0.asReference() }
        start = Date()
      }

    coEvery { fhirEngine.get(ResourceType.Patient, any()) } answers
      {
        when (secondArg<String>()) {
          "patient0" -> patient0
          "guardian1" -> guardian1
          else -> throw ResourceNotFoundException("Not Found", secondArg())
        }
      }
    coEvery { fhirEngine.get(ResourceType.RelatedPerson, "guardian0") } returns guardian0
    coEvery { fhirEngine.search<Resource>(any()) } answers
      {
        val searchObj = firstArg<Search>()
        when {
          searchObj.type == ResourceType.Task ->
            listOf(SearchResult(task0, included = null, revIncluded = null))
          searchObj.type == ResourceType.List && searchObj.count == 1 -> emptyList()
          searchObj.type == ResourceType.Appointment && searchObj.count == 1 ->
            listOf(SearchResult(appointment0, included = null, revIncluded = null))
          searchObj.type == ResourceType.CarePlan ->
            listOf(SearchResult(carePlan0, included = null, revIncluded = null))
          searchObj.type == ResourceType.Condition -> emptyList()
          else -> emptyList()
        }
      }
    coEvery { fhirEngine.get(ResourceType.Practitioner, practitioner.logicalId) } returns
      practitioner
    val profileData =
      tracingRegisterDao.loadProfileData(appFeatureName = null, resourceId = patient0.logicalId) as
        ProfileData.TracingProfileData
    Assert.assertNotNull(profileData)
    Assert.assertEquals(patient0.logicalId, profileData.logicalId)
    Assert.assertEquals("123456432", profileData.identifier)
    Assert.assertTrue(carePlan0 in profileData.services)
    Assert.assertTrue(task0 in profileData.tasks)
    Assert.assertTrue(guardian0 in profileData.guardians)
    Assert.assertTrue(guardian1 in profileData.guardians)
    Assert.assertTrue(practitioner in profileData.practitioners)
  }
}
