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

package org.smartregister.fhircore.engine.configuration.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.domain.model.Code
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.sync.SyncStrategy
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper

@HiltAndroidTest
class ApplicationConfigurationTest : RobolectricTest() {

  @get:Rule var hiltRule = HiltAndroidRule(this)

  lateinit var appConfig: ApplicationConfiguration
  var application: Context = ApplicationProvider.getApplicationContext()
  @Inject lateinit var sharedPreferenceHelper: SharedPreferencesHelper
  @Inject lateinit var configurationRegistry: ConfigurationRegistry

  @Before
  fun setUp() {
    hiltRule.inject()
    appConfig =
      ApplicationConfiguration(
        appId = "ancApp",
        configType = "classification",
        theme = "dark theme",
        languages = listOf("en"),
        syncInterval = 15,
        syncStrategy =
          listOf(
            Code(system = "https://smartregister.org/", display = SyncStrategy.CARE_TEAM.value),
            Code(system = "https://smartregister.org/", display = SyncStrategy.LOCATION.value),
            Code(system = "https://smartregister.org/", display = SyncStrategy.ORGANIZATION.value),
            Code(system = "https://smartregister.org/", display = SyncStrategy.PRACTITIONER.value),
          ),
        appTitle = "Test App",
        remoteSyncPageSize = 100
      )
  }

  @Test
  fun appConfigProperties() {
    Assert.assertEquals("ancApp", appConfig.appId)
    Assert.assertEquals("classification", appConfig.configType)
    Assert.assertEquals("dark theme", appConfig.theme)
    Assert.assertEquals(15, appConfig.syncInterval)
    Assert.assertEquals("Test App", appConfig.appTitle)
    Assert.assertEquals(100, appConfig.remoteSyncPageSize)
    Assert.assertTrue(
      appConfig.syncStrategy.map { it.display }.containsAll(SyncStrategy.values().map { it.value })
    )
  }

  @Test
  fun getMandatoryTags() {
    val careTeamIds = listOf("948", "372")
    sharedPreferenceHelper.write(SyncStrategy.CARE_TEAM.value, careTeamIds)
    val organizationIds = listOf("400", "105")
    sharedPreferenceHelper.write(SyncStrategy.ORGANIZATION.value, organizationIds)
    val locationIds = listOf("728", "899")
    sharedPreferenceHelper.write(SyncStrategy.LOCATION.value, locationIds)

    runBlocking {
      configurationRegistry.loadConfigurations("app/debug", application)

      val mandatoryTags = appConfig.getMandatoryTags(sharedPreferenceHelper)

      Assert.assertEquals(
        careTeamIds,
        mandatoryTags.filter { it.display == SyncStrategy.CARE_TEAM.value }.map { it.code }
      )
      Assert.assertEquals(
        organizationIds,
        mandatoryTags.filter { it.display == SyncStrategy.ORGANIZATION.value }.map { it.code }
      )
      Assert.assertEquals(
        locationIds,
        mandatoryTags.filter { it.display == SyncStrategy.LOCATION.value }.map { it.code }
      )
    }
  }
}
