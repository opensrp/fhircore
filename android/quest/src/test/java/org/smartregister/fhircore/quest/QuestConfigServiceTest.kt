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

@file:Suppress("UNUSED", "INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package org.smartregister.fhircore.quest

import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

@HiltAndroidTest
class QuestConfigServiceTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltAndroidRule = HiltAndroidRule(this)

  @Inject lateinit var configService: QuestConfigService

  @Before
  fun setUp() {
    hiltAndroidRule.inject()
  }

  @Test
  fun testProvideAuthConfigurationShouldReturnConfigs() {

    val authConfiguration = configService.provideAuthConfiguration()

    Assert.assertNotNull(authConfiguration)
    Assert.assertEquals(BuildConfig.FHIR_BASE_URL, authConfiguration.fhirServerBaseUrl)
    Assert.assertEquals(BuildConfig.OAUTH_BASE_URL, authConfiguration.oauthServerBaseUrl)
    Assert.assertEquals(BuildConfig.OAUTH_CIENT_ID, authConfiguration.clientId)
    Assert.assertEquals(BuildConfig.OAUTH_CLIENT_SECRET, authConfiguration.clientSecret)
    Assert.assertEquals(
      InstrumentationRegistry.getInstrumentation().targetContext.packageName,
      authConfiguration.accountType
    )
  }
}
