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

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import javax.inject.Inject
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.DecimalType
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
import org.smartregister.fhircore.engine.rulesengine.RulesExecutor
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

@ExperimentalCoroutinesApi
@HiltAndroidTest
class GeoWidgetLauncherViewModelTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltAndroidRule = HiltAndroidRule(this)

  @Inject lateinit var defaultRepository: DefaultRepository

  @Inject lateinit var dispatcherProvider: DefaultDispatcherProvider

  @Inject lateinit var sharedPreferencesHelper: SharedPreferencesHelper

  @Inject lateinit var rulesExecutor: RulesExecutor

  private lateinit var applicationContext: Context

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
    applicationContext = ApplicationProvider.getApplicationContext<HiltTestApplication>()
    viewModel =
      GeoWidgetLauncherViewModel(
        defaultRepository = defaultRepository,
        dispatcherProvider = dispatcherProvider,
        sharedPreferencesHelper = sharedPreferencesHelper,
        rulesExecutor = rulesExecutor,
        configurationRegistry = configurationRegistry,
        context = applicationContext,
      )
    runBlocking { defaultRepository.addOrUpdate(resource = location) }
  }

  @Test
  fun testShowNoLocationDialogShouldNotSetLiveDataValueWhenConfigIsNull() = runTest {
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
  fun testShowNoLocationDialogShouldSetLiveDataValueWhenConfigIsPresent() = runTest {
    viewModel.showNoLocationDialog(geoWidgetConfiguration)
    val value = viewModel.noLocationFoundDialog.value
    assertNotNull(value)
    assertTrue(value!!)
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
