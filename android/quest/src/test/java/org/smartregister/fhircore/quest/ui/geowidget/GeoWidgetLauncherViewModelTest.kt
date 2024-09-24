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

import com.google.android.fhir.datacapture.extensions.logicalId
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import javax.inject.Inject
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.DecimalType
import org.hl7.fhir.r4.model.IdType
import org.hl7.fhir.r4.model.Location
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.configuration.geowidget.GeoWidgetConfiguration
import org.smartregister.fhircore.engine.configuration.register.NoResultsConfig
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.domain.model.FhirResourceConfig
import org.smartregister.fhircore.engine.domain.model.ResourceConfig
import org.smartregister.fhircore.engine.domain.model.SnackBarMessageConfig
import org.smartregister.fhircore.engine.rulesengine.ResourceDataRulesExecutor
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.geowidget.model.GeoJsonFeature
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

@ExperimentalCoroutinesApi
@HiltAndroidTest
class GeoWidgetLauncherViewModelTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltAndroidRule = HiltAndroidRule(this)

  @Inject lateinit var defaultRepository: DefaultRepository

  @Inject lateinit var dispatcherProvider: DefaultDispatcherProvider

  @Inject lateinit var sharedPreferencesHelper: SharedPreferencesHelper

  @Inject lateinit var resourceDataRulesExecutor: ResourceDataRulesExecutor

  private val configurationRegistry = Faker.buildTestConfigurationRegistry()
  private lateinit var viewModel: GeoWidgetLauncherViewModel
  private val geoWidgetConfiguration =
    GeoWidgetConfiguration(
      appId = "appId",
      id = "id",
      registrationQuestionnaire =
        QuestionnaireConfig(
          id = "id",
        ),
      resourceConfig =
        FhirResourceConfig(baseResource = ResourceConfig(resource = ResourceType.Location)),
      servicePointConfig = null,
      noResults =
        NoResultsConfig(
          title = "Message Title",
          message = "Message text",
        ),
    )

  private val location =
    Location().apply {
      id = "loc1"
      name = "Root Location"
      position = Location.LocationPositionComponent(DecimalType(-10.05), DecimalType(5.55))
    }

  @Before
  fun setUp() {
    hiltAndroidRule.inject()
    viewModel =
      GeoWidgetLauncherViewModel(
        defaultRepository = defaultRepository,
        dispatcherProvider = dispatcherProvider,
        sharedPreferencesHelper = sharedPreferencesHelper,
        resourceDataRulesExecutor = resourceDataRulesExecutor,
        configurationRegistry = configurationRegistry,
      )

    runBlocking { defaultRepository.addOrUpdate(resource = location) }
  }

  @Test
  fun testShowNoLocationDialogShouldNotSetLiveDataValueWhenConfigIsNull() {
    val geoWidgetConfiguration =
      GeoWidgetConfiguration(
        appId = "appId",
        id = "id",
        registrationQuestionnaire =
          QuestionnaireConfig(
            id = "id",
          ),
        resourceConfig =
          FhirResourceConfig(baseResource = ResourceConfig(resource = ResourceType.Location)),
        servicePointConfig = null,
      )

    viewModel.showNoLocationDialog(geoWidgetConfiguration)

    val value = viewModel.noLocationFoundDialog.value
    assertNull(value)
  }

  @Test
  fun testShowNoLocationDialogShouldSetLiveDataValueWhenConfigIsPresent() {
    viewModel.showNoLocationDialog(geoWidgetConfiguration)
    val value = viewModel.noLocationFoundDialog.value
    assertNotNull(value)
    assertTrue(value!!)
  }

  @Test
  fun testRetrieveLocationsShouldReturnGeoJsonFeatureList() {
    runTest {
      viewModel.retrieveLocations(geoWidgetConfiguration, null)
      assertTrue(viewModel.geoJsonFeatures.value.isNotEmpty())
      assertEquals("loc1", viewModel.geoJsonFeatures.value.first().id)
    }
  }

  @Test
  @Ignore("Investigate why this test is not running")
  fun testOnQuestionnaireSubmission() = runTest {
    val emitFeature: (GeoJsonFeature) -> Unit = spyk({})
    val extractedResourceIds = listOf(IdType(ResourceType.Location.name, location.logicalId))

    viewModel.onQuestionnaireSubmission(
      extractedResourceIds = extractedResourceIds,
      emitFeature = emitFeature,
    )
    val geoJsonFeatureSlot = slot<GeoJsonFeature>()
    verify { emitFeature(capture(geoJsonFeatureSlot)) }

    val geoJsonFeature = geoJsonFeatureSlot.captured
    assertEquals(
      location.position.longitude.toDouble(),
      geoJsonFeature.geometry?.coordinates?.first(),
    )
    assertEquals(
      location.position.latitude.toDouble(),
      geoJsonFeature.geometry?.coordinates?.last(),
    )
  }

  @Test
  @Ignore("Fix kotlinx.coroutines.test.UncompletedCoroutinesError")
  fun testEmitSnackBarState() {
    runTest {
      val barMessageConfig = SnackBarMessageConfig(message = "message")
      val deferred = async { viewModel.snackBarStateFlow.first() }
      viewModel.emitSnackBarState(barMessageConfig)
      assertEquals(barMessageConfig.message, deferred.await().message)
    }
  }
}
