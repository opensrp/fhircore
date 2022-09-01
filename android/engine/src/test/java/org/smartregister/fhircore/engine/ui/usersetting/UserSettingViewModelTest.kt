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

import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.sync.State
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Shadows
import org.smartregister.fhircore.engine.app.AppConfigService
import org.smartregister.fhircore.engine.app.fakes.Faker
import org.smartregister.fhircore.engine.auth.AccountAuthenticator
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceService
import org.smartregister.fhircore.engine.domain.model.Language
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.rule.CoroutineTestRule
import org.smartregister.fhircore.engine.sync.SyncBroadcaster
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper

@HiltAndroidTest
class UserSettingViewModelTest : RobolectricTest() {

  @get:Rule var hiltRule = HiltAndroidRule(this)

  lateinit var userSettingViewModel: UserSettingViewModel
  lateinit var accountAuthenticator: AccountAuthenticator
  lateinit var secureSharedPreference: SecureSharedPreference
  var sharedPreferencesHelper: SharedPreferencesHelper

  val defaultRepository: DefaultRepository = mockk()
  @BindValue var configurationRegistry = Faker.buildTestConfigurationRegistry(defaultRepository)

  private var configService: ConfigService

  private val sharedSyncStatus: MutableSharedFlow<State> = MutableSharedFlow()
  private var syncBroadcaster: SyncBroadcaster
  private val context = ApplicationProvider.getApplicationContext<HiltTestApplication>()

  private val resourceService: FhirResourceService = mockk()

  private var fhirResourceDataSource: FhirResourceDataSource

  init {
    sharedPreferencesHelper = SharedPreferencesHelper(context)
    configService = AppConfigService(context = context)
    fhirResourceDataSource = spyk(FhirResourceDataSource(resourceService))
    syncBroadcaster =
      SyncBroadcaster(
        configurationRegistry,
        sharedPreferencesHelper,
        configService,
        syncJob = mockk(),
        fhirEngine = mockk(),
        sharedSyncStatus,
        dispatcherProvider = CoroutineTestRule().testDispatcherProvider
      )
  }

  @Before
  fun setUp() {
    hiltRule.inject()
    accountAuthenticator = mockk()
    secureSharedPreference = mockk()
    sharedPreferencesHelper = mockk()
    userSettingViewModel =
      UserSettingViewModel(
        syncBroadcaster,
        accountAuthenticator,
        secureSharedPreference,
        sharedPreferencesHelper,
        configurationRegistry
      )
  }

  @Test
  fun testRunSync() {
    userSettingViewModel.runSync()
  }

  @Test
  fun testRetrieveUsernameShouldReturnDemo() {
    every { secureSharedPreference.retrieveSessionUsername() } returns "demo"

    Assert.assertEquals("demo", userSettingViewModel.retrieveUsername())
    verify { secureSharedPreference.retrieveSessionUsername() }
  }

  @Test
  fun testLogoutUserShouldCallAuthLogoutService() {
    every { accountAuthenticator.logout() } returns Unit

    userSettingViewModel.logoutUser()

    verify(exactly = 1) { accountAuthenticator.logout() }
    Shadows.shadowOf(Looper.getMainLooper()).idle()
    Assert.assertTrue(userSettingViewModel.onLogout.value!!)
  }

  @Test
  fun allowSwitchingLanguagesShouldReturnTrueWhenMultipleLanguagesAreConfigured() {
    val languages = listOf(Language("es", "Spanish"), Language("en", "English"))
    userSettingViewModel = spyk(userSettingViewModel)

    every { userSettingViewModel.languages } returns languages

    Assert.assertTrue(userSettingViewModel.allowSwitchingLanguages())
  }

  @Test
  fun allowSwitchingLanguagesShouldReturnFalseWhenConfigurationIsFalse() {
    val languages = listOf(Language("es", "Spanish"))
    userSettingViewModel = spyk(userSettingViewModel)

    every { userSettingViewModel.languages } returns languages

    Assert.assertFalse(userSettingViewModel.allowSwitchingLanguages())
  }

  @Test
  fun loadSelectedLanguage() {
    every { sharedPreferencesHelper.read(SharedPreferencesHelper.LANG, "en") } returns "fr"

    Assert.assertEquals("French", userSettingViewModel.loadSelectedLanguage())
    verify { sharedPreferencesHelper.read(SharedPreferencesHelper.LANG, "en") }
  }

  @Test
  fun setLanguageShouldCallSharedPreferencesHelperWriteWithSelectedLanguageTagAndPostValue() {
    val language = Language("es", "Spanish")
    var postedValue: Language? = null

    every { sharedPreferencesHelper.write(any(), any<String>()) } just runs

    userSettingViewModel.language.observeForever { postedValue = it }

    userSettingViewModel.setLanguage(language)

    Shadows.shadowOf(Looper.getMainLooper()).idle()

    verify { sharedPreferencesHelper.write(SharedPreferencesHelper.LANG, "es") }
    Assert.assertEquals(language, postedValue!!)
  }

  @Test
  fun fetchLanguagesShouldReturnEnglishAndSwahiliAsModels() = runBlockingTest {
    val languages = userSettingViewModel.languages
    Assert.assertEquals("English", languages[0].displayName)
    Assert.assertEquals("en", languages[0].tag)
    Assert.assertEquals("Swahili", languages[1].displayName)
    Assert.assertEquals("sw", languages[1].tag)
  }

  @Test
  fun languagesLazyPropertyShouldRunFetchLanguagesAndReturnConfiguredLanguages() {
    val languages = userSettingViewModel.languages

    Assert.assertEquals("English", languages[0].displayName)
    Assert.assertEquals("en", languages[0].tag)
    Assert.assertEquals("Swahili", languages[1].displayName)
    Assert.assertEquals("sw", languages[1].tag)
  }
}
