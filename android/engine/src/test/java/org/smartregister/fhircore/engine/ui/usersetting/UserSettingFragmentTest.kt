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

package org.smartregister.fhircore.engine.ui.usersetting

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
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
import org.smartregister.fhircore.engine.app.AppConfigService
import org.smartregister.fhircore.engine.app.fakes.Faker
import org.smartregister.fhircore.engine.auth.AccountAuthenticator
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceService
import org.smartregister.fhircore.engine.launchFragmentInHiltContainer
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper

@HiltAndroidTest
class UserSettingFragmentTest : RobolectricTest() {

  @get:Rule(order = 0) var hiltRule = HiltAndroidRule(this)

  @BindValue var configurationRegistry = Faker.buildTestConfigurationRegistry()

  lateinit var userSettingViewModel: UserSettingViewModel

  lateinit var accountAuthenticator: AccountAuthenticator

  lateinit var secureSharedPreference: SecureSharedPreference

  var sharedPreferencesHelper: SharedPreferencesHelper

  private var configService: ConfigService

  private val context = ApplicationProvider.getApplicationContext<HiltTestApplication>()

  private val resourceService: FhirResourceService = mockk()

  private var fhirResourceDataSource: FhirResourceDataSource

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
    userSettingViewModel =
      UserSettingViewModel(
        fhirEngine = mockk(),
        accountAuthenticator,
        secureSharedPreference,
        sharedPreferencesHelper,
        configurationRegistry
      )
  }

  @Test
  fun assertGetUserSettingViewModelReturnsCorrectViewModelInstance() {
    launchFragmentInHiltContainer<UserSettingFragment>(Bundle(), R.style.AppTheme) {
      Assert.assertNotNull(this)
      Assert.assertNotNull((this as UserSettingFragment).userSettingViewModel)
      Assert.assertEquals(UserTestingFragmentTest@ userSettingViewModel, userSettingViewModel)
    }
  }

  @Test
  fun testOnCreateViewRendersUserSettingFragmentCorrectly() {
    launchFragmentInHiltContainer<UserSettingFragment>(Bundle(), R.style.AppTheme) {
      this.view!!.findViewWithTag<View>(USER_SETTING_ROW_LOGOUT)?.let {
        Assert.assertTrue(it.isVisible)
        Assert.assertTrue(it.isShown)
      }
    }
  }
}
