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
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkManager
import com.google.android.fhir.FhirEngine
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowLooper
import org.smartregister.fhircore.engine.HiltActivityForTest
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceService
import org.smartregister.fhircore.engine.domain.model.Language
import org.smartregister.fhircore.engine.sync.SyncBroadcaster
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.isDeviceOnline
import org.smartregister.fhircore.engine.util.extension.launchActivityWithNoBackStackHistory
import org.smartregister.fhircore.quest.app.AppConfigService
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.robolectric.RobolectricTest
import org.smartregister.fhircore.quest.ui.login.AccountAuthenticator
import org.smartregister.fhircore.quest.ui.login.LoginActivity

@HiltAndroidTest
class UserSettingViewModelTest : RobolectricTest() {

  @get:Rule var hiltRule = HiltAndroidRule(this)
  @BindValue var configurationRegistry = Faker.buildTestConfigurationRegistry()
  lateinit var userSettingViewModel: UserSettingViewModel
  lateinit var accountAuthenticator: AccountAuthenticator
  lateinit var secureSharedPreference: SecureSharedPreference
  lateinit var fhirEngine: FhirEngine
  var sharedPreferencesHelper: SharedPreferencesHelper
  private var configService: ConfigService
  private lateinit var syncBroadcaster: SyncBroadcaster
  private val context = ApplicationProvider.getApplicationContext<HiltTestApplication>()
  private val resourceService: FhirResourceService = mockk()
  private val workManager = mockk<WorkManager>(relaxed = true, relaxUnitFun = true)
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
    fhirEngine = mockk(relaxUnitFun = true)
    syncBroadcaster =
      spyk(
        SyncBroadcaster(
          configurationRegistry,
          fhirEngine = mockk(),
          dispatcherProvider = coroutineTestRule.testDispatcherProvider,
          syncListenerManager = mockk(relaxed = true),
          context = context
        )
      )

    userSettingViewModel =
      spyk(
        UserSettingViewModel(
          fhirEngine = fhirEngine,
          syncBroadcaster = syncBroadcaster,
          accountAuthenticator = accountAuthenticator,
          secureSharedPreference = secureSharedPreference,
          sharedPreferencesHelper = sharedPreferencesHelper,
          configurationRegistry = configurationRegistry,
          workManager = workManager,
          dispatcherProvider = coroutineTestRule.testDispatcherProvider
        )
      )
  }

  @Test
  fun testRunSyncWhenDeviceIsOnline() {
    every { syncBroadcaster.runSync(any()) } returns Unit
    userSettingViewModel.onEvent(UserSettingsEvent.SyncData(context))
    verify(exactly = 1) { syncBroadcaster.runSync(any()) }
  }

  @Test
  fun testDoNotRunSyncWhenDeviceIsOffline() {
    mockkStatic(Context::isDeviceOnline)

    val context = mockk<Context> { every { isDeviceOnline() } returns false }

    every { syncBroadcaster.runSync(any()) } returns Unit

    userSettingViewModel.onEvent(UserSettingsEvent.SyncData(context))
    verify(exactly = 0) { syncBroadcaster.runSync(any()) }
  }

  @Test
  fun testRetrieveUsernameShouldReturnDemo() {
    every { secureSharedPreference.retrieveSessionUsername() } returns "demo"

    Assert.assertEquals("demo", userSettingViewModel.retrieveUsername())

    verify { secureSharedPreference.retrieveSessionUsername() }
    Shadows.shadowOf(Looper.getMainLooper()).idle()
  }

  @Test
  fun testLogoutUserShouldCallAuthLogoutService() {
    val activity = mockk<HiltActivityForTest>(relaxed = true)
    every { activity.isDeviceOnline() } returns true
    every { activity.launchActivityWithNoBackStackHistory<LoginActivity>() } just runs
    val userSettingsEvent = UserSettingsEvent.Logout(activity)
    every { accountAuthenticator.logout(any()) } just runs

    userSettingViewModel.onEvent(userSettingsEvent)

    verify(exactly = 1) { accountAuthenticator.logout(any()) }
    Shadows.shadowOf(Looper.getMainLooper()).idle()
  }

  @Test
  fun allowSwitchingLanguagesShouldReturnTrueWhenMultipleLanguagesAreConfigured() {
    val languages = listOf(Language("es", "Spanish"), Language("en", "English"))
    userSettingViewModel = spyk(userSettingViewModel)

    every { userSettingViewModel.languages } returns languages

    Assert.assertTrue(userSettingViewModel.allowSwitchingLanguages())
    Shadows.shadowOf(Looper.getMainLooper()).idle()
  }

  @Test
  fun allowSwitchingLanguagesShouldReturnFalseWhenConfigurationIsFalse() {
    val languages = listOf(Language("es", "Spanish"))
    userSettingViewModel = spyk(userSettingViewModel)

    every { userSettingViewModel.languages } returns languages
    Shadows.shadowOf(Looper.getMainLooper()).idle()

    Assert.assertFalse(userSettingViewModel.allowSwitchingLanguages())
  }

  @Test
  fun loadSelectedLanguage() {
    every { sharedPreferencesHelper.read(SharedPreferenceKey.LANG.name, "en") } returns "fr"
    Assert.assertEquals("French", userSettingViewModel.loadSelectedLanguage())
    Shadows.shadowOf(Looper.getMainLooper()).idle()
    verify { sharedPreferencesHelper.read(SharedPreferenceKey.LANG.name, "en") }
  }

  @Test
  fun setLanguageShouldCallSharedPreferencesHelperWriteWithSelectedLanguageTagAndPostValue() {
    val language = Language("es", "Spanish")
    val userSettingsEvent = UserSettingsEvent.SwitchLanguage(language, context)

    every { sharedPreferencesHelper.write(any(), any<String>()) } just runs

    userSettingViewModel.onEvent(userSettingsEvent)

    Shadows.shadowOf(Looper.getMainLooper()).idle()

    verify { sharedPreferencesHelper.write(SharedPreferenceKey.LANG.name, "es") }
  }

  @Test
  fun languagesLazyPropertyShouldRunFetchLanguagesAndReturnConfiguredLanguages() {
    val languages = userSettingViewModel.languages

    Assert.assertTrue(languages.isNotEmpty())
    val language = languages.find { it.displayName == "English" }
    Assert.assertEquals("English", language?.displayName)
    Assert.assertEquals("en", language?.tag)
  }

  @Test
  fun testShowResetDatabaseConfirmationDialogShouldUpdateFlagCorrectly() {
    Assert.assertEquals(false, userSettingViewModel.showDBResetConfirmationDialog.value)

    val userSettingsEvent = UserSettingsEvent.ShowResetDatabaseConfirmationDialog(true)

    userSettingViewModel.onEvent(userSettingsEvent)

    ShadowLooper.idleMainLooper()
    Assert.assertEquals(true, userSettingViewModel.showDBResetConfirmationDialog.value)
  }

  @Test
  fun testResetDatabaseFlagEventShouldInvokeResetDatabaseMethod() {
    val userSettingViewModelSpy = spyk(userSettingViewModel)
    every { userSettingViewModelSpy.resetAppData(any()) } just runs

    val userSettingsEvent = UserSettingsEvent.ResetDatabaseFlag(true, context)

    userSettingViewModelSpy.onEvent(userSettingsEvent)

    verify { userSettingViewModelSpy.resetAppData(any()) }
  }

  @Test
  fun testShowLoaderViewShouldUpdateShowProgressFlagCorrectly() {
    Assert.assertEquals(false, userSettingViewModel.progressBarState.value?.first)

    val userSettingsEvent = UserSettingsEvent.ShowLoaderView(true, R.string.resetting_app)

    userSettingViewModel.onEvent(userSettingsEvent)

    ShadowLooper.idleMainLooper()
    Assert.assertEquals(true, userSettingViewModel.progressBarState.value?.first)
    Assert.assertEquals(R.string.resetting_app, userSettingViewModel.progressBarState.value?.second)
  }

  @Test
  fun testResetAppDataShouldClearEverything() = runTest {
    userSettingViewModel.resetAppData(context)

    verify { workManager.cancelAllWork() }
    coVerify { fhirEngine.clearDatabase() }
    verify { accountAuthenticator.invalidateSession(any()) }
  }
}
