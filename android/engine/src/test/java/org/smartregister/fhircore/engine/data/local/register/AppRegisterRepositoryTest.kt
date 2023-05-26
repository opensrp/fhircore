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
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.Patient
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.smartregister.fhircore.engine.appfeature.model.HealthModule
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.data.local.register.dao.HivRegisterDao
import org.smartregister.fhircore.engine.data.local.register.dao.RegisterDaoFactory
import org.smartregister.fhircore.engine.domain.model.ProfileData
import org.smartregister.fhircore.engine.domain.model.RegisterData
import org.smartregister.fhircore.engine.domain.repository.RegisterDao
import org.smartregister.fhircore.engine.trace.FakePerformanceReporter
import org.smartregister.fhircore.engine.trace.PerformanceReporter
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider

@OptIn(ExperimentalCoroutinesApi::class)
class AppRegisterRepositoryTest {

  private lateinit var repository: AppRegisterRepository
  private val fhirEngine: FhirEngine = mockk()
  private val dispatcherProvider: DefaultDispatcherProvider = mockk()
  private val registerDaoFactory: RegisterDaoFactory = mockk()
  private val tracer: PerformanceReporter = FakePerformanceReporter()

  @Before
  fun setUp() {
    repository = AppRegisterRepository(fhirEngine, dispatcherProvider, registerDaoFactory, tracer)
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
    val healthModule: HealthModule = mockk()
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
    val healthModule: HealthModule = mockk()
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
    val healthModule: HealthModule = mockk()
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
    val healthModule: HealthModule = mockk()
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
    val healthModule: HealthModule = mockk()
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
}
