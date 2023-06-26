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

package org.smartregister.fhircore.engine.data.local.register

import com.google.android.fhir.FhirEngine
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import java.util.Date
import javax.inject.Inject
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.Patient
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.app.fakes.Faker
import org.smartregister.fhircore.engine.appfeature.model.HealthModule
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.data.local.AppointmentRegisterFilter
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.data.local.TracingRegisterFilter
import org.smartregister.fhircore.engine.data.local.register.dao.AppointmentRegisterDao
import org.smartregister.fhircore.engine.data.local.register.dao.HivRegisterDao
import org.smartregister.fhircore.engine.data.local.register.dao.HomeTracingRegisterDao
import org.smartregister.fhircore.engine.data.local.register.dao.PhoneTracingRegisterDao
import org.smartregister.fhircore.engine.data.local.register.dao.RegisterDaoFactory
import org.smartregister.fhircore.engine.domain.model.ProfileData
import org.smartregister.fhircore.engine.domain.model.RegisterData
import org.smartregister.fhircore.engine.domain.repository.RegisterDao
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.trace.FakePerformanceReporter
import org.smartregister.fhircore.engine.trace.PerformanceReporter
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
class AppRegisterRepositoryTest : RobolectricTest() {
  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
  private lateinit var repository: AppRegisterRepository
  private val fhirEngine: FhirEngine = mockk()
  private val dispatcherProvider: DefaultDispatcherProvider = mockk()
  private val registerDaoFactory: RegisterDaoFactory = mockk()
  private val tracer: PerformanceReporter = FakePerformanceReporter()
  @BindValue val sharedPreferencesHelper = mockk<SharedPreferencesHelper>(relaxed = true)
  private val configurationRegistry = Faker.buildTestConfigurationRegistry()
  @Inject lateinit var configService: ConfigService
  @Before
  fun setUp() {
    hiltRule.inject()
    repository =
      AppRegisterRepository(
        fhirEngine,
        dispatcherProvider,
        sharedPreferencesHelper,
        configurationRegistry,
        registerDaoFactory,
        configService,
        tracer
      )
    mockkConstructor(DefaultRepository::class)
    mockkStatic("kotlinx.coroutines.DispatchersKt")
    every { anyConstructed<DefaultRepository>().fhirEngine } returns fhirEngine
    every { anyConstructed<DefaultRepository>().dispatcherProvider } returns dispatcherProvider
    every { dispatcherProvider.io() } returns Dispatchers.Unconfined
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `loadRegisterData should call appropriate dao method and return result`() = runTest {
    val healthModule: HealthModule = HealthModule.HIV
    val currentPage = 1
    val appFeatureName: String? = null
    val registerDataList: List<RegisterData> = listOf(mockk())
    val registerDao: RegisterDao = mockk(relaxed = true)
    val registerDaoMap: MutableMap<HealthModule, RegisterDao> =
      mutableMapOf(healthModule to registerDao)

    every { registerDaoFactory.registerDaoMap } returns registerDaoMap
    coEvery { registerDao.loadRegisterData(currentPage, any(), appFeatureName) } returns
      registerDataList

    val result = repository.loadRegisterData(currentPage, false, appFeatureName, healthModule)

    coVerify { registerDao.loadRegisterData(currentPage, any(), appFeatureName) }
    assertEquals(registerDataList, result)
  }

  @Test
  fun `searchByName should call appropriate dao method and return result`() = runTest {
    val healthModule: HealthModule = HealthModule.HIV
    val nameQuery = "John"
    val currentPage = 1
    val appFeatureName: String? = null
    val registerDataList: List<RegisterData> = listOf(mockk())
    val registerDao: RegisterDao = mockk(relaxed = true)
    val registerDaoMap: MutableMap<HealthModule, RegisterDao> =
      mutableMapOf(healthModule to registerDao)

    every { registerDaoFactory.registerDaoMap } returns registerDaoMap
    coEvery { registerDao.searchByName(nameQuery, currentPage, appFeatureName) } returns
      registerDataList

    val result = repository.searchByName(nameQuery, currentPage, appFeatureName, healthModule)

    coVerify { registerDao.searchByName(nameQuery, currentPage, appFeatureName) }
    assertEquals(registerDataList, result)
  }

  @Test
  fun `countRegisterData should call appropriate dao method and return result`() = runTest {
    val healthModule: HealthModule = HealthModule.HIV
    val appFeatureName: String? = null
    val count: Long = 10
    val registerDao: RegisterDao = mockk(relaxed = true)
    val registerDaoMap: MutableMap<HealthModule, RegisterDao> =
      mutableMapOf(healthModule to registerDao)

    every { registerDaoFactory.registerDaoMap } returns registerDaoMap
    coEvery { registerDao.countRegisterData(appFeatureName) } returns count

    val result = repository.countRegisterData(appFeatureName, healthModule)

    coVerify { registerDao.countRegisterData(appFeatureName) }
    assertEquals(count, result)
  }

  @Test
  fun `loadPatientProfileData should call appropriate dao method and return result`() = runTest {
    val healthModule: HealthModule = HealthModule.HIV
    val appFeatureName: String? = null
    val patientId = "123"
    val profileData: ProfileData = mockk()
    val registerDao: RegisterDao = mockk(relaxed = true)
    val registerDaoMap: MutableMap<HealthModule, RegisterDao> =
      mutableMapOf(healthModule to registerDao)

    every { registerDaoFactory.registerDaoMap } returns registerDaoMap
    coEvery { registerDao.loadProfileData(appFeatureName, patientId) } returns profileData

    val result = repository.loadPatientProfileData(appFeatureName, healthModule, patientId)

    coVerify { registerDao.loadProfileData(appFeatureName, patientId) }
    assertEquals(profileData, result)
  }

  @Test
  fun `loadChildrenRegisterData should call appropriate dao method and return result`() = runTest {
    val healthModule: HealthModule = HealthModule.HIV
    val otherPatientResource: List<Patient> = listOf(mockk())
    val registerDataList: List<RegisterData> = listOf(mockk())
    val hivRegisterDao: HivRegisterDao = mockk(relaxed = true)
    val registerDaoMap: MutableMap<HealthModule, RegisterDao> =
      mutableMapOf(healthModule to hivRegisterDao)

    every { registerDaoFactory.registerDaoMap } returns registerDaoMap
    coEvery { hivRegisterDao.transformChildrenPatientToRegisterData(any()) } returns
      registerDataList

    val result = repository.loadChildrenRegisterData(healthModule, otherPatientResource)

    coVerify { hivRegisterDao.transformChildrenPatientToRegisterData(otherPatientResource) }
    assertEquals(registerDataList, result)
  }

  @Test
  fun `loadRegisterFiltered should call correct appointment dao method`() = runTest {
    val healthModule = HealthModule.APPOINTMENT
    val appointmentRegisterDao =
      mockk<AppointmentRegisterDao>(relaxed = true) {
        coEvery { loadRegisterFiltered(any(), any(), any(), any()) } returns
          listOf(mockk<RegisterData.AppointmentRegisterData>())
      }

    every { registerDaoFactory.registerDaoMap } returns
      mapOf(healthModule to appointmentRegisterDao)
    val today = Date()
    repository.loadRegisterFiltered(
      1,
      false,
      null,
      healthModule,
      AppointmentRegisterFilter(today, true, null, null)
    )
    coVerify {
      appointmentRegisterDao.loadRegisterFiltered(
        1,
        false,
        null,
        AppointmentRegisterFilter(today, true, null, null)
      )
    }
  }

  @Test
  fun `loadRegisterFiltered should call correct tracing dao method`() = runTest {
    val healthModule = HealthModule.PHONE_TRACING
    val phoneTracingRegisterDao =
      mockk<PhoneTracingRegisterDao>(relaxed = true) {
        coEvery { loadRegisterFiltered(any(), any(), any(), any()) } returns
          listOf(mockk<RegisterData.TracingRegisterData>())
      }

    every { registerDaoFactory.registerDaoMap } returns
      mapOf(healthModule to phoneTracingRegisterDao)
    repository.loadRegisterFiltered(
      1,
      false,
      null,
      healthModule,
      TracingRegisterFilter(true, null, null, null)
    )
    coVerify {
      phoneTracingRegisterDao.loadRegisterFiltered(
        1,
        false,
        null,
        TracingRegisterFilter(true, null, null, null)
      )
    }
  }

  @Test
  fun `loadRegisterFiltered returns empty for dao not in registerDaoMap`() = runTest {
    val healthModule = HealthModule.APPOINTMENT
    val appointmentRegisterDao =
      mockk<AppointmentRegisterDao>(relaxed = true) {
        coEvery { loadRegisterFiltered(any(), any(), any(), any()) } returns
          listOf(mockk<RegisterData.AppointmentRegisterData>())
      }

    every { registerDaoFactory.registerDaoMap } returns mapOf()
    val today = Date()
    val data =
      repository.loadRegisterFiltered(
        0,
        healthModule = healthModule,
        filters = AppointmentRegisterFilter(today, true, null, null)
      )
    Assert.assertTrue(data.isEmpty())
    coVerify(exactly = 0) {
      appointmentRegisterDao.loadRegisterFiltered(
        1,
        false,
        null,
        AppointmentRegisterFilter(today, true, null, null)
      )
    }
  }

  @Test
  fun `countRegisterFiltered should call correct tracing dao method`() = runTest {
    val healthModule = HealthModule.HOME_TRACING
    val homeTracingRegisterDao =
      mockk<HomeTracingRegisterDao>(relaxed = true) {
        coEvery { countRegisterFiltered(any(), any()) } returns 0
      }

    every { registerDaoFactory.registerDaoMap } returns
      mapOf(healthModule to homeTracingRegisterDao)
    repository.countRegisterFiltered(
      null,
      healthModule,
      TracingRegisterFilter(true, null, null, null)
    )
    coVerify {
      homeTracingRegisterDao.countRegisterFiltered(
        null,
        TracingRegisterFilter(true, null, null, null)
      )
    }
  }

  @Test
  fun `countRegisterFiltered should call correct appointment dao method`() = runTest {
    val healthModule = HealthModule.APPOINTMENT
    val appointmentRegisterDao =
      mockk<AppointmentRegisterDao>(relaxed = true) {
        coEvery { countRegisterFiltered(any(), any()) } returns 0
      }

    every { registerDaoFactory.registerDaoMap } returns
      mapOf(healthModule to appointmentRegisterDao)
    val today = Date()
    repository.countRegisterFiltered(
      null,
      healthModule,
      AppointmentRegisterFilter(today, true, null, null)
    )
    coVerify {
      appointmentRegisterDao.countRegisterFiltered(
        null,
        AppointmentRegisterFilter(today, true, null, null)
      )
    }
  }
}
