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
import com.google.android.fhir.search.Search
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.Appointment
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Practitioner
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.app.fakes.Faker
import org.smartregister.fhircore.engine.app.fakes.Faker.buildPatient
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.data.local.AppointmentRegisterFilter
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.domain.model.HealthStatus
import org.smartregister.fhircore.engine.domain.model.RegisterData
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.rule.CoroutineTestRule
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.LOGGED_IN_PRACTITIONER
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
class AppointmentRegisterDaoTest : RobolectricTest() {

  @BindValue val sharedPreferencesHelper: SharedPreferencesHelper = mockk(relaxed = true)
  @get:Rule(order = 2) var coroutineRule = CoroutineTestRule()

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val instantTaskExecutorRule = InstantTaskExecutorRule()

  @get:Rule(order = 2) val coroutineTestRule = CoroutineTestRule()

  private lateinit var appointmentRegisterDao: AppointmentRegisterDao

  private val fhirEngine: FhirEngine = mockk()

  lateinit var defaultRepository: DefaultRepository
  @Inject lateinit var configService: ConfigService

  var configurationRegistry: ConfigurationRegistry = Faker.buildTestConfigurationRegistry()

  @Before
  fun setUp() {
    hiltRule.inject()
    defaultRepository =
      DefaultRepository(
        fhirEngine = fhirEngine,
        dispatcherProvider = coroutineRule.testDispatcherProvider,
        sharedPreferencesHelper = sharedPreferencesHelper,
        configurationRegistry = configurationRegistry,
        configService = configService
      )
    coEvery { fhirEngine.get(ResourceType.Patient, "1234") } returns
      buildPatient("1", "doe", "john", 10, patientType = "exposed-infant")

    coEvery { configurationRegistry.retrieveDataFilterConfiguration(any()) } returns emptyList()

    every {
      sharedPreferencesHelper.read<Practitioner>(LOGGED_IN_PRACTITIONER, decodeWithGson = true)
    } returns Practitioner().apply { id = "123" }

    appointmentRegisterDao =
      AppointmentRegisterDao(
        fhirEngine = fhirEngine,
        defaultRepository = defaultRepository,
        configurationRegistry = configurationRegistry,
        dispatcherProvider = DefaultDispatcherProvider(),
        sharedPreferencesHelper = sharedPreferencesHelper
      )
  }

  @Test(expected = NotImplementedError::class)
  fun testLoadProfileData() = runTest {
    appointmentRegisterDao.loadProfileData(appFeatureName = "HIV", resourceId = "1234")
  }

  @Test
  fun testCountRegisterData() = runTest {
    val search =
      Search(ResourceType.Appointment).apply {
        filter(Appointment.STATUS, { value = of(Appointment.AppointmentStatus.BOOKED.toCode()) })
        filter(Appointment.DATE, { value = of(DateTimeType.today()) })
      }
    val appointments =
      listOf(
        SearchResult(
          Appointment().apply {
            status = Appointment.AppointmentStatus.BOOKED
            start = Date()
            addParticipant().apply { actor = Reference().apply { reference = "Patient/random" } }

            addParticipant().apply {
              actor = Reference().apply { reference = "Practitioner/random" }
            }
          },
          included = null,
          revIncluded = null
        ),
        SearchResult(
          Appointment().apply {
            status = Appointment.AppointmentStatus.BOOKED
            start = Date()
          },
          included = null,
          revIncluded = null
        )
      )
    coEvery { fhirEngine.search<Appointment>(search) } returns appointments

    val count = appointmentRegisterDao.countRegisterData("1234")
    Assert.assertEquals(1, count)
  }

  @Test
  fun testCountRegisterFiltered() = runTest {
    val startDate = Date()
    val appointment1 =
      SearchResult(
        Appointment().apply {
          status = Appointment.AppointmentStatus.BOOKED
          start = startDate

          addParticipant().apply { actor = Reference().apply { reference = "Practitioner/123" } }
          addParticipant().apply { actor = Reference().apply { reference = "Patient/1234" } }
          addReasonCode(CodeableConcept(Coding().apply { code = "Milestone" }))
        },
        included = null,
        revIncluded = null
      )
    val appointment2 =
      SearchResult(
        Appointment().apply {
          status = Appointment.AppointmentStatus.BOOKED
          start = startDate
          addParticipant().apply { actor = Reference().apply { reference = "Practitioner/123" } }
          addParticipant().apply { actor = Reference().apply { reference = "Patient/1234" } }
          addReasonCode(CodeableConcept(Coding().apply { code = "ICT" }))
        },
        included = null,
        revIncluded = null
      )
    coEvery { fhirEngine.search<Appointment>(any<Search>()) } returns
      listOf(appointment1, appointment2)
    val registerFilter =
      AppointmentRegisterFilter(
        startDate,
        myPatients = true,
        patientCategory = null,
        reasonCode = "ICT"
      )
    val counts = appointmentRegisterDao.countRegisterFiltered(filters = registerFilter)
    Assert.assertEquals(1, counts)
  }

  @Test
  fun loadRegisterDataReturnsAppointmentRegisterData() = runTest {
    val startDate = Date()
    val appointment =
      SearchResult(
        Appointment().apply {
          status = Appointment.AppointmentStatus.BOOKED
          start = startDate

          addParticipant().apply { actor = Reference().apply { reference = "Practitioner/123" } }
          addParticipant().apply { actor = Reference().apply { reference = "Patient/1234" } }
          addReasonCode(CodeableConcept(Coding().apply { code = "Milestone" }))
        },
        included = null,
        revIncluded = null
      )

    coEvery { fhirEngine.search<Appointment>(Search(ResourceType.Appointment)) } returns
      listOf(appointment)
    coEvery { fhirEngine.search<Condition>(Search(ResourceType.Condition)) } returns listOf()
    val data = appointmentRegisterDao.loadRegisterData(0, loadAll = true, appFeatureName = null)
    Assert.assertTrue(data.isNotEmpty())
    Assert.assertTrue(data.all { it is RegisterData.AppointmentRegisterData })
  }

  @Test
  fun loadRegisterFilteredReturnsDataFiltered() = runTest {
    val startDate = Date()
    val patient1 =
      buildPatient("12345-3214", "doe", "june", 10, patientType = "exposed-infant").apply {
        addIdentifier(
          Identifier().apply {
            use = Identifier.IdentifierUse.OFFICIAL
            value = "3214"
          }
        )
      }
    coEvery { fhirEngine.get(ResourceType.Patient, "12345-3214") } returns patient1
    val appointment1 =
      SearchResult(
        Appointment().apply {
          id = "appointment1"
          status = Appointment.AppointmentStatus.BOOKED
          start = startDate

          addParticipant().apply { actor = Reference().apply { reference = "Practitioner/123" } }
          addParticipant().apply { actor = Reference().apply { reference = "Patient/12345-3214" } }
          addReasonCode(CodeableConcept(Coding().apply { code = "Milestone" }))
        },
        included = null,
        revIncluded = null
      )
    val patient2 =
      buildPatient("1234-4321-3214", "doedoe", "janen", 10, patientType = "exposed-infant")
    coEvery { fhirEngine.get(ResourceType.Patient, "1234-4321-3214") } returns patient2
    val appointment2 =
      SearchResult(
        Appointment().apply {
          id = "appointment2"
          status = Appointment.AppointmentStatus.BOOKED
          start = startDate
          addParticipant().apply { actor = Reference().apply { reference = "Practitioner/123" } }
          addParticipant().apply {
            actor = Reference().apply { reference = "Patient/1234-4321-3214" }
          }
          addReasonCode(CodeableConcept(Coding().apply { code = "ICT" }))
        },
        included = null,
        revIncluded = null
      )
    coEvery { fhirEngine.search<Condition>(Search(ResourceType.Condition)) } returns listOf()
    coEvery { fhirEngine.search<Appointment>(Search(ResourceType.Appointment)) } returns
      listOf(appointment1, appointment2)
    val data =
      appointmentRegisterDao.loadRegisterFiltered(
        0,
        loadAll = false,
        filters =
          AppointmentRegisterFilter(
            startDate,
            myPatients = true,
            patientCategory = listOf(HealthStatus.EXPOSED_INFANT),
            reasonCode = null
          )
      )
    Assert.assertEquals(2, data.size)
    Assert.assertEquals("appointment2", data.elementAt(0).logicalId)
    Assert.assertEquals("appointment1", data.elementAt(1).logicalId)
  }

  @Test
  fun testGetRegisterDataFilters() {
    Assert.assertNotNull(appointmentRegisterDao.getRegisterDataFilters())
  }
}
