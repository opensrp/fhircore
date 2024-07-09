/*
 * Copyright 2021-2024 Ona Systems, Inc
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

package org.smartregister.fhircore.quest.ui.geowidget

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.every
import io.mockk.mockk
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.smartregister.fhircore.engine.configuration.geowidget.GeoWidgetConfiguration
import org.smartregister.fhircore.engine.configuration.register.NoResultsConfig
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.rulesengine.ResourceDataRulesExecutor
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.geowidget.model.ServicePointType
import org.smartregister.fhircore.geowidget.screens.GeoWidgetViewModel
import org.smartregister.fhircore.quest.ui.launcher.GeoWidgetLauncherViewModel

@ExperimentalCoroutinesApi
class GeoWidgetLauncherViewModelTest {

  @get:Rule var rule: TestRule = InstantTaskExecutorRule()

  private lateinit var viewModel: GeoWidgetLauncherViewModel
  private lateinit var defaultRepository: DefaultRepository
  private lateinit var mockDispatcherProvider: DispatcherProvider
  private lateinit var sharedPreferencesHelper: SharedPreferencesHelper
  private lateinit var resourceDataRulesExecutor: ResourceDataRulesExecutor

  @Before
  fun setUp() {
    defaultRepository = mockk()
    mockDispatcherProvider = mockk()
    sharedPreferencesHelper = mockk()
    resourceDataRulesExecutor = mockk()
    viewModel =
      GeoWidgetLauncherViewModel(
        defaultRepository,
        mockDispatcherProvider,
        sharedPreferencesHelper,
        resourceDataRulesExecutor,
      )
  }

  @Test
  fun test_getServicePointKeyToType() {
    val viewModel = GeoWidgetViewModel(mockDispatcherProvider)

    val servicePointMap = viewModel.getServicePointKeyToType()

    assertEquals(ServicePointType.EPP, servicePointMap["epp"])
    assertEquals(ServicePointType.CEG, servicePointMap["ceg"])
    assertEquals(ServicePointType.CHRD1, servicePointMap["chrd1"])
  }

  @Test
  fun `showNoLocationDialog() should not set noLocationFoundDialog value when noResults in geoWidgetConfiguration is null`() {
    val geoWidgetConfiguration: GeoWidgetConfiguration = mockk(relaxed = true)

    every { geoWidgetConfiguration.noResults } returns null

    viewModel.showNoLocationDialog(geoWidgetConfiguration)

    val value = viewModel.noLocationFoundDialog.value
    assertNull(value)
  }

  @Test
  fun `showNoLocationDialog() should set noLocationFoundDialog value when noResults in geoWidgetConfiguration is not null`() {
    val geoWidgetConfiguration: GeoWidgetConfiguration = mockk(relaxed = true)

    every { geoWidgetConfiguration.noResults } returns NoResultsConfig()

    viewModel.showNoLocationDialog(geoWidgetConfiguration)

    val value = viewModel.noLocationFoundDialog.value
    assertNotNull(value)
    assertTrue { value!! }
  }
}
