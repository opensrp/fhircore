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

package org.smartregister.fhircore.quest.ui.register

import androidx.compose.runtime.mutableStateOf
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.register.RegisterConfiguration
import org.smartregister.fhircore.engine.data.local.register.RegisterRepository
import org.smartregister.fhircore.engine.domain.model.FhirResourceConfig
import org.smartregister.fhircore.engine.domain.model.ResourceConfig
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

@HiltAndroidTest
class RegisterViewModelTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  private val configurationRegistry: ConfigurationRegistry = Faker.buildTestConfigurationRegistry()

  private lateinit var registerViewModel: RegisterViewModel

  private lateinit var registerRepository: RegisterRepository

  private lateinit var sharedPreferencesHelper: SharedPreferencesHelper

  private val registerId = "register101"

  private val screenTitle = "Register 101"

  @Before
  fun setUp() {
    hiltRule.inject()
    registerRepository = mockk()
    sharedPreferencesHelper = mockk()
    registerViewModel =
      spyk(
        RegisterViewModel(
          registerRepository = registerRepository,
          configurationRegistry = configurationRegistry,
          sharedPreferencesHelper = sharedPreferencesHelper,
          dispatcherProvider = coroutineTestRule.testDispatcherProvider
        )
      )

    every { registerViewModel.retrieveRegisterConfiguration(any()) } returns
      RegisterConfiguration(
        appId = "app",
        id = registerId,
        fhirResource =
          FhirResourceConfig(
            baseResource = ResourceConfig(resource = "Patient"),
          ),
        pageSize = 10
      )
    every {
      sharedPreferencesHelper.read(SharedPreferenceKey.LAST_SYNC_TIMESTAMP.name, null)
    } returns "Mar 20, 03:01PM"
  }

  @Test
  fun testPaginateRegisterData() {
    registerViewModel.paginateRegisterData(registerId, false)
    val paginatedRegisterData = registerViewModel.paginatedRegisterData.value
    Assert.assertNotNull(paginatedRegisterData)
    Assert.assertTrue(registerViewModel.pagesDataCache.isEmpty())
  }

  @Test
  fun testRetrieveRegisterUiState() = runTest {
    every { registerViewModel.paginateRegisterData(any(), any()) } just runs
    coEvery { registerRepository.countRegisterData(any()) } returns 200
    registerViewModel.retrieveRegisterUiState(registerId = registerId, screenTitle = screenTitle)
    val registerUiState = registerViewModel.registerUiState.value
    Assert.assertNotNull(registerUiState)
    Assert.assertEquals(registerId, registerUiState.registerId)
    Assert.assertFalse(registerUiState.isFirstTimeSync)
    Assert.assertEquals(screenTitle, registerUiState.screenTitle)
    val registerConfiguration = registerUiState.registerConfiguration
    Assert.assertNotNull(registerConfiguration)
    Assert.assertEquals("app", registerConfiguration?.appId)
    Assert.assertEquals(200, registerUiState.totalRecordsCount)
    Assert.assertEquals(20, registerUiState.pagesCount)
  }

  @Test
  fun testOnEventSearchRegister() {
    every { registerViewModel.registerUiState } returns
      mutableStateOf(RegisterUiState(registerId = registerId))
    // Search with empty string should paginate the data
    registerViewModel.onEvent(RegisterEvent.SearchRegister(""))
    verify { registerViewModel.paginateRegisterData(any(), any()) }

    // Search for the word 'Khan' should call the filterRegisterData function
    registerViewModel.onEvent(RegisterEvent.SearchRegister("Khan"))
    verify { registerViewModel.filterRegisterData(any()) }
  }

  @Test
  fun testOnEventMoveToNextPage() {
    every { registerViewModel.registerUiState } returns
      mutableStateOf(RegisterUiState(registerId = registerId))
    registerViewModel.currentPage.value = 1
    every { registerViewModel.paginateRegisterData(any(), any()) } just runs
    registerViewModel.onEvent(RegisterEvent.MoveToNextPage)
    Assert.assertEquals(2, registerViewModel.currentPage.value)
    verify { registerViewModel.paginateRegisterData(any(), any()) }
  }

  @Test
  fun testOnEventMoveToPreviousPage() {
    every { registerViewModel.registerUiState } returns
      mutableStateOf(RegisterUiState(registerId = registerId))
    registerViewModel.currentPage.value = 2
    every { registerViewModel.paginateRegisterData(any(), any()) } just runs
    registerViewModel.onEvent(RegisterEvent.MoveToPreviousPage)
    Assert.assertEquals(1, registerViewModel.currentPage.value)
    verify { registerViewModel.paginateRegisterData(any(), any()) }
  }
}
