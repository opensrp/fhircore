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

package org.smartregister.fhircore.quest.ui.login

import android.content.Intent
import androidx.compose.material.ExperimentalMaterialApi
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowIntent
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.robolectric.RobolectricTest
import org.smartregister.fhircore.quest.ui.main.AppMainActivity
import org.smartregister.p2p.P2PLibrary

@HiltAndroidTest
class QuestLoginServiceTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @Inject lateinit var questLoginService: QuestLoginService

  @Inject lateinit var configurationRegistry: ConfigurationRegistry

  @BindValue
  val secureSharedPreference: SecureSharedPreference =
    spyk(SecureSharedPreference(ApplicationProvider.getApplicationContext()))

  lateinit var loginActivity: LoginActivity

  private lateinit var loginService: QuestLoginService

  @Before
  fun setUp() {
    hiltRule.inject()
    runBlocking {
      configurationRegistry.loadConfigurations(
        context = InstrumentationRegistry.getInstrumentation().targetContext,
        appId = APP_DEBUG
      ) {}
    }
    loginService = spyk(questLoginService)
    loginActivity = Robolectric.buildActivity(LoginActivity::class.java).get()
    loginService.loginActivity = loginActivity
  }

  @After
  override fun tearDown() {
    super.tearDown()
    loginActivity.finish()
  }

  @OptIn(ExperimentalMaterialApi::class)
  @Test
  fun testNavigateToHomeShouldInitP2PAndNavigateToRegisterScreen() {
    mockkObject(P2PLibrary)
    every { P2PLibrary.init(any()) } returns mockk()

    secureSharedPreference.saveCredentials(Faker.authCredentials)
    loginService.navigateToHome()

    val startedIntent: Intent = shadowOf(loginActivity).nextStartedActivity
    val shadowIntent: ShadowIntent = shadowOf(startedIntent)
    Assert.assertEquals(AppMainActivity::class.java, shadowIntent.intentClass)
  }
}
