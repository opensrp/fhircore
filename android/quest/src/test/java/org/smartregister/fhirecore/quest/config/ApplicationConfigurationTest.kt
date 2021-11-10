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

package org.smartregister.fhirecore.quest.config

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runBlockingTest
import org.hl7.fhir.r4.model.Binary
import org.junit.Assert.assertEquals
import org.junit.Test
import org.robolectric.annotation.Config
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.configuration.app.applicationConfigurationOf
import org.smartregister.fhircore.engine.configuration.app.loadApplicationConfiguration
import org.smartregister.fhircore.engine.util.extension.encodeJson
import org.smartregister.fhircore.engine.util.extension.loadBinaryResourceConfiguration
import org.smartregister.fhircore.quest.BuildConfig
import org.smartregister.fhircore.quest.QuestApplication
import org.smartregister.fhirecore.quest.robolectric.RobolectricTest
import org.smartregister.fhirecore.quest.shadow.QuestApplicationShadow

@Config(shadows = [QuestApplicationShadow::class])
class ApplicationConfigurationTest : RobolectricTest() {

  @Test
  fun testLoadRegisterViewConfigShouldReturnValidConfig() {
    val context = ApplicationProvider.getApplicationContext<Application>()
    val result = context.loadApplicationConfiguration("quest-app")

    assertEquals("quest-app", result.id)
    assertEquals("QuestDefault", result.theme)
    assertEquals(2, result.languages.size)
    assertEquals("en", result.languages[0])
    assertEquals("sw", result.languages[1])
  }

  @Test
  fun testLoadBinaryResourceConfigurationShouldReturnValidConfig() = runBlockingTest {
    val expectedConfig =
      applicationConfigurationOf(
        id = "quest-app",
        theme = "QuestDefault",
        languages = listOf("en", "sw"),
        syncInterval = 30
      )

    val context = ApplicationProvider.getApplicationContext<QuestApplication>()
    context.fhirEngine.save(
      Binary().apply {
        id = "quest-app"
        data = expectedConfig.encodeJson().encodeToByteArray()
      }
    )

    val result = context.loadBinaryResourceConfiguration<ApplicationConfiguration>("quest-app")!!

    assertEquals("quest-app", result.id)
    assertEquals("QuestDefault", result.theme)
    assertEquals("en", result.languages[0])
    assertEquals(30, result.syncInterval)
  }

  @Test
  fun testShouldVerifyAllDefinedAppPropertiesConfig() {

    val appConfig =
      ApplicationConfiguration(
        "quest-app",
        "QuestDefault",
        BuildConfig.OAUTH_BASE_URL,
        BuildConfig.FHIR_BASE_URL,
        BuildConfig.OAUTH_CIENT_ID,
        BuildConfig.OAUTH_CLIENT_SECRET,
        BuildConfig.OAUTH_SCOPE,
        listOf("en", "sw"),
        40
      )

    assertEquals("quest-app", appConfig.id)
    assertEquals("QuestDefault", appConfig.theme)
    assertEquals(BuildConfig.OAUTH_BASE_URL, appConfig.oauthServerBaseUrl)
    assertEquals(BuildConfig.FHIR_BASE_URL, appConfig.fhirServerBaseUrl)
    assertEquals(BuildConfig.OAUTH_CIENT_ID, appConfig.clientId)
    assertEquals(BuildConfig.OAUTH_CLIENT_SECRET, appConfig.clientSecret)
    assertEquals(BuildConfig.OAUTH_SCOPE, appConfig.scope)
    assertEquals("en", appConfig.languages[0])
    assertEquals("sw", appConfig.languages[1])
    assertEquals(40, appConfig.syncInterval)
  }

  @Test
  fun testShouldVerifyAllDefaultAppPropertiesConfig() {

    val appConfig = ApplicationConfiguration()

    assertEquals("", appConfig.id)
    assertEquals("", appConfig.theme)
    assertEquals("", appConfig.oauthServerBaseUrl)
    assertEquals("", appConfig.fhirServerBaseUrl)
    assertEquals("", appConfig.clientId)
    assertEquals("", appConfig.clientSecret)
    assertEquals("openid", appConfig.scope)
    assertEquals("en", appConfig.languages[0])
    assertEquals(30, appConfig.syncInterval)
  }
}
