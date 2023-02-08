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

package org.smartregister.fhircore.quest.ui.appsetting

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

@HiltAndroidTest
class AppSettingActivityTest : RobolectricTest() {
  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
  @Inject lateinit var sharedPreferencesHelper: SharedPreferencesHelper

  @BindValue
  val configurationRegistry: ConfigurationRegistry = Faker.buildTestConfigurationRegistry()

  private var activityScenario: ActivityScenario<AppSettingActivity>? = null

  @Before
  fun setUp() {
    hiltRule.inject()
    ApplicationProvider.getApplicationContext<Context>().apply { setTheme(R.style.AppTheme) }
  }

  @After
  override fun tearDown() {
    activityScenario?.close()
    super.tearDown()
  }

  private fun appSettingActivityIntent() =
    Intent(ApplicationProvider.getApplicationContext(), AppSettingActivity::class.java)

  @Test
  fun testThatConfigsAreLoadedWhenAppSettingsIsLaunched() {
    // ConfigurationRegistry loads app configs when it is initialized
    activityScenario =
      ActivityScenario.launch<AppSettingActivity?>(appSettingActivityIntent()).use {
        it.onActivity { activity ->
          Assert.assertTrue(activity != null)
          Assert.assertTrue(configurationRegistry.configsJsonMap.isNotEmpty())
        }
      }
  }
}
