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

package org.smartregister.fhircore.quest.ui.usersetting

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.FhirEngine
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
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
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceService
import org.smartregister.fhircore.engine.domain.model.Language
import org.smartregister.fhircore.engine.sync.SyncBroadcaster
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.quest.app.AppConfigService
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.robolectric.RobolectricTest
import org.smartregister.fhircore.quest.ui.login.AccountAuthenticator

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

  private val application: Context = ApplicationProvider.getApplicationContext()

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
    fhirEngine = mockk()
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
        fhirEngine = fhirEngine,
        syncBroadcaster,
        accountAuthenticator,
        secureSharedPreference,
        sharedPreferencesHelper,
        configurationRegistry
      )
  }

  @Test
  fun testRunSync() {
    userSettingViewModel.onEvent(UserSettingsEvent.SyncData)
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
    val userSettingsEvent = UserSettingsEvent.Logout
    every { accountAuthenticator.logout(any()) } returns Unit

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
    userSettingViewModelSpy.dispatcherProvider = coroutineTestRule.testDispatcherProvider
    every { userSettingViewModelSpy.resetDatabase(any()) } just runs

    val userSettingsEvent = UserSettingsEvent.ResetDatabaseFlag(true)

    userSettingViewModelSpy.onEvent(userSettingsEvent)

    verify { userSettingViewModelSpy.resetDatabase(any()) }
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
  fun testResetDatabaseInvokesClearDatabase() = runTest {
    coEvery { fhirEngine.clearDatabase() } just runs
    coEvery { accountAuthenticator.invalidateSession() } just runs
    coEvery { sharedPreferencesHelper.resetSharedPrefs() } just runs
    coEvery { secureSharedPreference.resetSharedPrefs() } just runs
    coEvery { accountAuthenticator.invalidateSession() } just runs

    userSettingViewModel.resetDatabase(coroutineTestRule.testDispatcherProvider.io())

    coVerify { fhirEngine.clearDatabase() }
  }

  @Test
  fun testResetDatabaseInvokesResetSharedPrefs() = runTest {
    coEvery { fhirEngine.clearDatabase() } just runs
    coEvery { accountAuthenticator.invalidateSession() } just runs
    coEvery { sharedPreferencesHelper.resetSharedPrefs() } just runs
    coEvery { secureSharedPreference.resetSharedPrefs() } just runs
    coEvery { accountAuthenticator.invalidateSession() } just runs

    userSettingViewModel.resetDatabase(coroutineTestRule.testDispatcherProvider.io())

    coVerify { sharedPreferencesHelper.resetSharedPrefs() }
  }

  @Test
  fun testResetDatabaseInvokesResetSecuredSharedPrefs() = runTest {
    coEvery { fhirEngine.clearDatabase() } just runs
    coEvery { accountAuthenticator.invalidateSession() } just runs
    coEvery { sharedPreferencesHelper.resetSharedPrefs() } just runs
    coEvery { accountAuthenticator.invalidateSession() } just runs

    userSettingViewModel.resetDatabase(coroutineTestRule.testDispatcherProvider.io())

    coVerify { sharedPreferencesHelper.resetSharedPrefs() }
  }

  @Test
  fun testResetDatabaseInvokesAccountAuthenticatorLocalLogout() = runTest {
    coEvery { fhirEngine.clearDatabase() } just runs
    coEvery { accountAuthenticator.invalidateSession() } just runs
    coEvery { sharedPreferencesHelper.resetSharedPrefs() } just runs
    coEvery { secureSharedPreference.resetSharedPrefs() } just runs
    coEvery { accountAuthenticator.invalidateSession() } just runs

    userSettingViewModel.resetDatabase(coroutineTestRule.testDispatcherProvider.io())

    coVerify { accountAuthenticator.invalidateSession() }
  }

  @Test
  fun testResetDatabaseInvokesAccountAuthenticatorLaunchScreen() = runTest {
    coEvery { fhirEngine.clearDatabase() } just runs
    coEvery { accountAuthenticator.launchScreen(any()) } just runs
    coEvery { sharedPreferencesHelper.resetSharedPrefs() } just runs
    coEvery { secureSharedPreference.resetSharedPrefs() } just runs
    coEvery { accountAuthenticator.invalidateSession() } just runs

    userSettingViewModel.resetDatabase(coroutineTestRule.testDispatcherProvider.io())

    coVerify { accountAuthenticator.launchScreen(any()) }
  }
}
