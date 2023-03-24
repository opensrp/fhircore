/*
 * Copyright 2021-2023 Ona Systems, Inc
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

package org.smartregister.fhircore.quest.ui.usersetting

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.mockk.mockk
import io.mockk.spyk
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceService
import org.smartregister.fhircore.engine.launchFragmentInHiltContainer
import org.smartregister.fhircore.engine.sync.SyncBroadcaster
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.quest.app.AppConfigService
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.robolectric.RobolectricTest
import org.smartregister.fhircore.quest.ui.login.AccountAuthenticator

@HiltAndroidTest
class UserSettingFragmentTest : RobolectricTest() {

  @get:Rule(order = 0) var hiltRule = HiltAndroidRule(this)
  @BindValue var configurationRegistry = Faker.buildTestConfigurationRegistry()
  lateinit var userSettingViewModel: UserSettingViewModel
  lateinit var accountAuthenticator: AccountAuthenticator
  lateinit var secureSharedPreference: SecureSharedPreference
  var sharedPreferencesHelper: SharedPreferencesHelper
  private var configService: ConfigService
  private val navController = TestNavHostController(ApplicationProvider.getApplicationContext())
  private val context = ApplicationProvider.getApplicationContext<HiltTestApplication>()
  private val resourceService: FhirResourceService = mockk()
  private var fhirResourceDataSource: FhirResourceDataSource
  private lateinit var syncBroadcaster: SyncBroadcaster
  private val application: Context = ApplicationProvider.getApplicationContext()

  init {
    sharedPreferencesHelper = SharedPreferencesHelper(context = context, gson = mockk())
    configService = AppConfigService(context = context)
    fhirResourceDataSource = spyk(FhirResourceDataSource(resourceService))
  }

  @Before
  fun setUp() {
    hiltRule.inject()
    accountAuthenticator = mockk()
    secureSharedPreference = mockk()
    sharedPreferencesHelper = mockk()
    syncBroadcaster =
      SyncBroadcaster(
        configurationRegistry,
        fhirEngine = mockk(),
        dispatcherProvider = coroutineTestRule.testDispatcherProvider,
        syncListenerManager = mockk(relaxed = true),
        context = application
      )

    userSettingViewModel =
      UserSettingViewModel(
        fhirEngine = mockk(),
        syncBroadcaster = syncBroadcaster,
        accountAuthenticator = accountAuthenticator,
        secureSharedPreference = secureSharedPreference,
        sharedPreferencesHelper = sharedPreferencesHelper,
        configurationRegistry = configurationRegistry,
        workManager = mockk(relaxed = true),
        dispatcherProvider = coroutineTestRule.testDispatcherProvider
      )
  }

  @Test
  fun assertGetUserSettingViewModelReturnsCorrectViewModelInstance() {
    launchFragmentInHiltContainer<UserSettingFragment>(Bundle(), R.style.AppTheme, navController) {
      Assert.assertNotNull(this)
      Assert.assertNotNull((this as UserSettingFragment).userSettingViewModel)
      Assert.assertEquals(userSettingViewModel, userSettingViewModel)
    }
  }

  @Test
  fun testOnCreateViewRendersUserSettingFragmentCorrectly() {
    launchFragmentInHiltContainer<UserSettingFragment>(Bundle(), R.style.AppTheme, navController) {
      this.view!!.findViewWithTag<View>(USER_SETTING_ROW_LOGOUT)?.let {
        Assert.assertTrue(it.isVisible)
        Assert.assertTrue(it.isShown)
      }
    }
  }
}
