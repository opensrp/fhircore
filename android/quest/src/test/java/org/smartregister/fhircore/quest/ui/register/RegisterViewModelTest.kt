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

import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import javax.inject.Inject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.data.local.register.RegisterRepository
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

@HiltAndroidTest
class RegisterViewModelTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
  @Inject lateinit var configurationRegistry: ConfigurationRegistry
  private lateinit var registerViewModel: RegisterViewModel
  lateinit var registerRepository: RegisterRepository
  private lateinit var sharedPreferencesHelper: SharedPreferencesHelper
  private lateinit var registerViewModelMock: RegisterViewModel

  @Before
  fun setUp() {
    hiltRule.inject()
    registerRepository = mockk()
    sharedPreferencesHelper = mockk()
    registerViewModelMock = mockk()
    registerViewModel =
      RegisterViewModel(
        registerRepository = registerRepository,
        configurationRegistry = configurationRegistry,
        sharedPreferencesHelper = sharedPreferencesHelper,
        dispatcherProvider = coroutineTestRule.testDispatcherProvider
      )
  }

  @Test
  fun testPaginateRegisterData() {
    val registerId = "12727277171"
    every { registerViewModelMock.paginateRegisterData(any(), any()) } just runs
    registerViewModelMock.paginateRegisterData(registerId, false)
    verify { registerViewModelMock.paginateRegisterData(registerId, false) }
  }
}
