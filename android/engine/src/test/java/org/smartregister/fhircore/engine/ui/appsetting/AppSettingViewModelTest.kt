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
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import kotlinx.coroutines.test.runBlockingTest
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Composition
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class AppSettingViewModelTest {

  @get:Rule var instantTaskExecutorRule = InstantTaskExecutorRule()

  private val appSettingViewModel = spyk(AppSettingViewModel(mockk(), mockk()))

  @Test
  fun testOnApplicationIdChanged() {
    appSettingViewModel.onApplicationIdChanged("appId")
    Assert.assertNotNull(appSettingViewModel.appId.value)
    Assert.assertEquals("appId", appSettingViewModel.appId.value)
  }

  @Test
  fun testOnRememberAppChecked() {
    appSettingViewModel.onRememberAppChecked(true)
    Assert.assertNotNull(appSettingViewModel.rememberApp.value)
    Assert.assertEquals(true, appSettingViewModel.rememberApp.value)
  }

  @Test
  fun testLoadConfigurations() = runBlockingTest {
    coEvery { appSettingViewModel.fhirResourceDataSource.loadData(any()) } returns
      Bundle().apply { addEntry().resource = Composition() }
    coEvery { appSettingViewModel.defaultRepository.save(any()) } just runs

    appSettingViewModel.loadConfigurations("appId")
    Assert.assertNotNull(appSettingViewModel.loadConfigs.value)
    Assert.assertEquals(true, appSettingViewModel.loadConfigs.value)
  }
}
