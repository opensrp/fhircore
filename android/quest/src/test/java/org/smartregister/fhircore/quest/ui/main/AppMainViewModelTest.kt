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

import android.app.Activity
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.gson.Gson
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.verify
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.auth.AccountAuthenticator
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.data.local.register.RegisterRepository
import org.smartregister.fhircore.engine.domain.model.Language
import org.smartregister.fhircore.engine.sync.SyncBroadcaster
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

@HiltAndroidTest
class AppMainViewModelTest : RobolectricTest() {

  @get:Rule val hiltRule = HiltAndroidRule(this)

  lateinit var accountAuthenticator: AccountAuthenticator
  lateinit var syncBroadcaster: SyncBroadcaster
  lateinit var secureSharedPreference: SecureSharedPreference
  lateinit var sharedPreferencesHelper: SharedPreferencesHelper
  @Inject lateinit var configurationRegistry: ConfigurationRegistry
  lateinit var configService: ConfigService
  lateinit var registerRepository: RegisterRepository
  lateinit var dispatcherProvider: DefaultDispatcherProvider

  val application: Context = ApplicationProvider.getApplicationContext()

  @Inject lateinit var gson: Gson

  lateinit var appMainViewModel: AppMainViewModel

  @Before
  fun setUp() {
    hiltRule.inject()

    accountAuthenticator = mockk(relaxed = true)
    syncBroadcaster = mockk(relaxed = true)
    secureSharedPreference = mockk()
    sharedPreferencesHelper = SharedPreferencesHelper(application, gson)
    configService = mockk()
    registerRepository = mockk()
    dispatcherProvider = DefaultDispatcherProvider()

    appMainViewModel =
      AppMainViewModel(
        accountAuthenticator,
        syncBroadcaster,
        secureSharedPreference,
        sharedPreferencesHelper,
        configurationRegistry,
        configService,
        registerRepository,
        dispatcherProvider
      )
  }

  @Test
  fun onEventLogout() {
    val appMainEvent = AppMainEvent.Logout

    appMainViewModel.onEvent(appMainEvent)

    verify { accountAuthenticator.logout() }
  }

  @Test
  fun onEventSwitchLanguage() {
    val appMainEvent =
      AppMainEvent.SwitchLanguage(
        Language("en", "English"),
        mockkClass(Activity::class, relaxed = true)
      )

    appMainViewModel.onEvent(appMainEvent)

    Assert.assertEquals("en", sharedPreferencesHelper.read(SharedPreferenceKey.LANG.name, ""))
  }

  @Test
  fun onEventSyncData() {
    val appMainEvent = AppMainEvent.SyncData

    every { secureSharedPreference.retrieveSessionUsername() } returns "demo"

    runBlocking {
      configurationRegistry.loadConfigurations("app/debug", application)
      appMainViewModel.onEvent(appMainEvent)
    }

    verify { syncBroadcaster.runSync() }
    verify { appMainViewModel.retrieveAppMainUiState() }
  }

  @Test
  fun onEventRegisterNewClient() {
    val context = mockkClass(Activity::class, relaxed = true)
    val appMainEvent = AppMainEvent.RegisterNewClient(context, QuestionnaireConfig(id = "123"))

    runBlocking { appMainViewModel.onEvent(appMainEvent) }

    verify { context.startActivity(any()) }
  }
}
