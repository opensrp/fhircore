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

package org.smartregister.fhircore.quest.ui.main

import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.mockk
import java.time.OffsetDateTime
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.appfeature.AppFeature
import org.smartregister.fhircore.engine.appfeature.AppFeatureManager
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.util.APP_ID_CONFIG
import org.smartregister.fhircore.engine.util.LAST_SYNC_TIMESTAMP
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.navigation.SideMenuOptionFactory
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

@HiltAndroidTest
class AppMainViewModelTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @BindValue val sharedPreferencesHelper: SharedPreferencesHelper = mockk(relaxed = true)

  @BindValue val secureSharedPreference: SecureSharedPreference = mockk()

  @BindValue val sideMenuOptionFactory: SideMenuOptionFactory = mockk()

  @BindValue val appFeatureManager: AppFeatureManager = mockk()

  @BindValue var defaultRepository: DefaultRepository = mockk()

  @BindValue
  var configurationRegistry: ConfigurationRegistry =
    Faker.buildTestConfigurationRegistry("g6pd", defaultRepository)

  @Inject lateinit var configService: ConfigService

  private lateinit var viewModel: AppMainViewModel

  @Before
  fun setUp() = runBlocking {
    hiltRule.inject()

    Faker.loadTestConfigurationRegistryData("g6pd", defaultRepository, configurationRegistry)

    every { configurationRegistry.appId } returns "g6pd"

    every { sharedPreferencesHelper.write(APP_ID_CONFIG, "default/debug") }
    every { sharedPreferencesHelper.read(any(), any<String>()) } answers
      {
        if (firstArg<String>() == LAST_SYNC_TIMESTAMP) {
          ""
        } else {
          "1234"
        }
      }

    every { secureSharedPreference.retrieveSessionUsername() } returns "demo"

    every { sideMenuOptionFactory.retrieveSideMenuOptions() } returns emptyList()

    every { appFeatureManager.isFeatureActive(AppFeature.DeviceToDeviceSync) } returns true
    every { appFeatureManager.isFeatureActive(AppFeature.InAppReporting) } returns false

    viewModel =
      AppMainViewModel(
        accountAuthenticator = mockk(),
        syncBroadcaster = mockk(),
        sideMenuOptionFactory = sideMenuOptionFactory,
        secureSharedPreference = secureSharedPreference,
        sharedPreferencesHelper = sharedPreferencesHelper,
        configurationRegistry = configurationRegistry,
        configService = configService,
        appFeatureManager = appFeatureManager,
      )
  }

  @Test
  fun testLoadCurrentLanguage() {
    Assert.assertNotNull(viewModel.loadCurrentLanguage())
  }

  @Test
  fun testUpdateAndRetrieveLastSyncTimestamp() {
    val odt = OffsetDateTime.now()
    val formattedOdt = viewModel.formatLastSyncTimestamp(OffsetDateTime.now())
    viewModel.updateLastSyncTimestamp(odt)
    Assert.assertNotNull(viewModel.retrieveLastSyncTimestamp())
    Assert.assertEquals(formattedOdt, viewModel.formatLastSyncTimestamp(odt))
  }

  @Test
  fun testRetrieveAppMainUiState() {
    val expectedAppMainUiState =
      appMainUiStateOf(
        "G6PD Test Reader",
        "demo",
        enableReports = false,
        enableDeviceToDeviceSync = true
      )
    viewModel.retrieveAppMainUiState()
    val resultAppMainUiState = viewModel.appMainUiState.value
    Assert.assertNotNull(resultAppMainUiState)
    Assert.assertEquals(expectedAppMainUiState.appTitle, resultAppMainUiState.appTitle)
    Assert.assertEquals(expectedAppMainUiState.username, resultAppMainUiState.username)
    Assert.assertEquals(
      expectedAppMainUiState.enableDeviceToDeviceSync,
      resultAppMainUiState.enableDeviceToDeviceSync
    )
    Assert.assertEquals(expectedAppMainUiState.enableReports, resultAppMainUiState.enableReports)
  }
}
