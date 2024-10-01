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
import androidx.test.core.app.ApplicationProvider
import ca.uhn.fhir.parser.IParser
import com.google.android.fhir.FhirEngine
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.spyk
import java.util.UUID
import javax.inject.Inject
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.rulesengine.ConfigRulesExecutor
import org.smartregister.fhircore.engine.util.DispatcherProvider
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

  private lateinit var defaultRepository: DefaultRepository

  private val fhirEngine = mockk<FhirEngine>()

  private val configRulesExecutor: ConfigRulesExecutor = mockk()

  private lateinit var viewModel: GeoWidgetViewModel

  @Mock private lateinit var dispatcherProvider: DispatcherProvider

  @Before
  fun setUp() {
    MockitoAnnotations.initMocks(this)
    viewModel = GeoWidgetViewModel()
    hiltRule.inject()
    sharedPreferencesHelper = mockk()
    configurationRegistry = mockk()
    defaultRepository =
      spyk(
        DefaultRepository(
          fhirEngine = fhirEngine,
          dispatcherProvider = dispatcherProvider,
          sharedPreferencesHelper = sharedPreferencesHelper,
          configurationRegistry = configurationRegistry,
          configService = configService,
          configRulesExecutor = configRulesExecutor,
          fhirPathDataExtractor = fhirPathDataExtractor,
          parser = parser,
          context = ApplicationProvider.getApplicationContext(),
        ),
      )
    geoWidgetViewModel = spyk(GeoWidgetViewModel())

    coEvery { defaultRepository.create(any()) } returns emptyList()
  }

  fun testMappingServicePointKeysToTypes() {
    val expectedMap = mutableMapOf<String, ServicePointType>()
    ServicePointType.entries.forEach { expectedMap[it.name.lowercase()] = it }

    val result = viewModel.getServicePointKeyToType()

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
    geoWidgetViewModel.submitFeatures(geoJsonFeatures)

    Assert.assertEquals(geoWidgetViewModel.features.value!!.size, geoJsonFeatures.size)
  }

  @Test
  fun testThatMapOfServicePointTypeReturnsEnumValuesBasedOnTheirLowercaseNames() {
    val result = geoWidgetViewModel.getServicePointKeyToType()
    val expectedMap =
      mapOf(
        "epp" to ServicePointType.EPP,
        "ceg" to ServicePointType.CEG,
        "chrd1" to ServicePointType.CHRD1,
        "chrd2" to ServicePointType.CHRD2,
        "drsp" to ServicePointType.DRSP,
        "msp" to ServicePointType.MSP,
        "sdsp" to ServicePointType.SDSP,
        "csb1" to ServicePointType.CSB1,
        "csb2" to ServicePointType.CSB2,
        "chrr" to ServicePointType.CHRR,
        "warehouse" to ServicePointType.WAREHOUSE,
        "water_point" to ServicePointType.WATER_POINT,
        "presco" to ServicePointType.PRESCO,
        "meah" to ServicePointType.MEAH,
        "dreah" to ServicePointType.DREAH,
        "mppspf" to ServicePointType.MPPSPF,
        "drppspf" to ServicePointType.DRPPSPF,
        "ngo_partner" to ServicePointType.NGO_PARTNER,
        "site_communautaire" to ServicePointType.SITE_COMMUNAUTAIRE,
        "drjs" to ServicePointType.DRJS,
        "instat" to ServicePointType.INSTAT,
        "bsd" to ServicePointType.BSD,
        "men" to ServicePointType.MEN,
        "dren" to ServicePointType.DREN,
        "district_ppspf" to ServicePointType.DISTRICT_PPSPF,
        "mairie" to ServicePointType.MAIRIE,
        "ecole_communautaire" to ServicePointType.ECOLE_COMMUNAUTAIRE,
        "ecole_privé" to ServicePointType.ECOLE_PRIVÉ,
        "lycée" to ServicePointType.LYCÉE,
      )
    Assert.assertEquals(expectedMap, result)
  }
}
