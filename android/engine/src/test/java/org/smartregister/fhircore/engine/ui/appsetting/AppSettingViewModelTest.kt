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

package org.smartregister.fhircore.engine.ui.appsetting

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.spyk
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class AppSettingViewModelTest {

  @get:Rule(order = 1) var instantTaskExecutorRule = InstantTaskExecutorRule()

  private val appSettingViewModel = spyk(AppSettingViewModel())

  @Test
  fun testOnApplicationIdChanged() {
    appSettingViewModel.onApplicationIdChanged("appId")
    Assert.assertNotNull(appSettingViewModel.appId.value)
    Assert.assertEquals("appId", appSettingViewModel.appId.value)
  }

  @Test
  fun testOnCompositionIdChanged() {
    appSettingViewModel.onCompositionIdChanged("1234")
    Assert.assertNotNull(appSettingViewModel.compositionId.value)
    Assert.assertEquals("1234", appSettingViewModel.compositionId.value)
  }

  @Test
  fun testOnRememberAppChecked() {
    appSettingViewModel.onRememberAppChecked(true)
    Assert.assertNotNull(appSettingViewModel.rememberApp.value)
    Assert.assertEquals(true, appSettingViewModel.rememberApp.value)
  }

  @Test
  fun testLoadConfigurations() {
    appSettingViewModel.loadConfigurations(true)
    Assert.assertNotNull(appSettingViewModel.loadConfigs.value)
    Assert.assertEquals(true, appSettingViewModel.loadConfigs.value)
  }
}
