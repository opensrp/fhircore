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

package org.smartregister.fhircore.geowidget.screens

import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import ca.uhn.fhir.parser.IParser
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.mockk.mockk
import java.util.UUID
import javax.inject.Inject
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.fhirpath.FhirPathDataExtractor
import org.smartregister.fhircore.geowidget.model.GeoJsonFeature
import org.smartregister.fhircore.geowidget.model.Geometry
import org.smartregister.fhircore.geowidget.model.ServicePointType

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1], application = HiltTestApplication::class)
@HiltAndroidTest
class GeoWidgetViewModelTest {

  @get:Rule(order = 0) var hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) var instantTaskExecutorRule = InstantTaskExecutorRule()

  @Inject lateinit var configService: ConfigService

  @Inject lateinit var fhirPathDataExtractor: FhirPathDataExtractor

  @Inject lateinit var parser: IParser

  private lateinit var configurationRegistry: ConfigurationRegistry
  private lateinit var sharedPreferencesHelper: SharedPreferencesHelper
  private lateinit var geoWidgetViewModel: GeoWidgetViewModel

  @Before
  fun setUp() {
    MockitoAnnotations.initMocks(this)
    geoWidgetViewModel = GeoWidgetViewModel()
    hiltRule.inject()
    sharedPreferencesHelper = mockk()
    configurationRegistry = mockk()
  }

  fun testMappingServicePointKeysToTypes() {
    val expectedMap = mutableMapOf<String, ServicePointType>()
    ServicePointType.entries.forEach { expectedMap[it.name.lowercase()] = it }

    val result = geoWidgetViewModel.getServicePointKeyToType()

    Assert.assertEquals(expectedMap.size, result.size)
    expectedMap.forEach { (key, expectedValue) ->
      val actualValue = result[key]
      Assert.assertEquals(expectedValue.name, actualValue?.name)
    }
  }

  @Test
  fun testAddGeoJsonFeaturesToLiveData() {
    val serverVersion = (1..10).random()
    val geoJsonFeatures =
      listOf(
        GeoJsonFeature(
          id = UUID.randomUUID().toString(),
          geometry =
            Geometry(
              coordinates = listOf(34.76, 68.23),
            ),
          properties = mapOf(),
          serverVersion = serverVersion,
        ),
        GeoJsonFeature(
          id = UUID.randomUUID().toString(),
          geometry =
            Geometry(
              coordinates = listOf(34.76, 68.23),
            ),
          properties = mapOf(),
          serverVersion = serverVersion,
        ),
      )
    geoWidgetViewModel.updateMapFeatures(geoJsonFeatures)

    Assert.assertEquals(geoWidgetViewModel.mapFeatures.size, geoJsonFeatures.size)
  }

  @Test
  fun testThatMapOfServicePointTypeReturnsEnumValuesBasedOnTheirLowercaseNames() {
    val servicePointTypeMap = geoWidgetViewModel.getServicePointKeyToType()

    ServicePointType.entries.forEach { servicePointType ->
      val expectedValue = servicePointType.name.lowercase()
      val actualValue = servicePointTypeMap[expectedValue]
      Assert.assertEquals(servicePointType, actualValue)
    }
  }
}
