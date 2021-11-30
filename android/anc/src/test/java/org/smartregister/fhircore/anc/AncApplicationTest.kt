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

package org.smartregister.fhircore.anc

import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkManager
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert
import org.junit.Test
import org.smartregister.fhircore.anc.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.configuration.app.ConfigurableApplication
import org.smartregister.fhircore.engine.data.remote.model.response.UserInfo
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.USER_INFO_SHARED_PREFERENCE_KEY
import org.smartregister.fhircore.engine.util.extension.encodeJson

class AncApplicationTest : RobolectricTest() {

  private val app by lazy { ApplicationProvider.getApplicationContext<AncApplication>() }

  @Test
  fun testConstructFhirEngineShouldReturnNonNull() {
    WorkManager.initialize(AncApplication.getContext(), Configuration.Builder().build())
    Assert.assertNotNull(AncApplication.getContext().fhirEngine)
  }
  @Test
  fun testThatApplicationIsInstanceOfConfigurableApplication() {
    Assert.assertTrue(
      ApplicationProvider.getApplicationContext<AncApplication>() is ConfigurableApplication
    )
  }

  @Test
  fun testResourceSyncParamsShouldHaveAllRequiredEntities() {
    val syncParams = ApplicationProvider.getApplicationContext<AncApplication>().resourceSyncParams
    Assert.assertTrue(syncParams.containsKey(ResourceType.Patient))
    Assert.assertTrue(syncParams.containsKey(ResourceType.Questionnaire))
    Assert.assertTrue(syncParams.containsKey(ResourceType.CarePlan))
    Assert.assertTrue(syncParams.containsKey(ResourceType.Condition))
    Assert.assertTrue(syncParams.containsKey(ResourceType.Observation))
    Assert.assertTrue(syncParams.containsKey(ResourceType.Encounter))
  }

  @Test
  fun testApplyConfigurationShouldLoadConfiguration() {

    val config = app.applicationConfiguration

    Assert.assertEquals(BuildConfig.FHIR_BASE_URL, config.fhirServerBaseUrl)
    Assert.assertEquals(BuildConfig.OAUTH_BASE_URL, config.oauthServerBaseUrl)
    Assert.assertEquals(BuildConfig.OAUTH_CIENT_ID, config.clientId)
    Assert.assertEquals(BuildConfig.OAUTH_CLIENT_SECRET, config.clientSecret)
  }

  @Test
  fun testAuthenticateUserInfoShouldReturnNonNull() {
    mockkObject(SharedPreferencesHelper)

    every { SharedPreferencesHelper.read(USER_INFO_SHARED_PREFERENCE_KEY, null) } returns
      UserInfo("ONA-Systems", "105", "Nairobi").encodeJson()

    Assert.assertEquals(
      "ONA-Systems",
      AncApplication.getContext().authenticatedUserInfo?.questionnairePublisher
    )
    Assert.assertEquals("105", AncApplication.getContext().authenticatedUserInfo?.organization)
    Assert.assertEquals("Nairobi", AncApplication.getContext().authenticatedUserInfo?.location)

    unmockkObject(SharedPreferencesHelper)
  }
}
