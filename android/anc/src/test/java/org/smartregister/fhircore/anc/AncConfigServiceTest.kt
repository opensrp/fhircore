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

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.smartregister.fhircore.anc.robolectric.RobolectricTest

class AncConfigServiceTest : RobolectricTest() {

  private lateinit var eirConfigService: AncConfigService

  private val context = ApplicationProvider.getApplicationContext<Context>()

  @Before
  fun setUp() {
    eirConfigService = AncConfigService(context)
  }

  @Test
  fun testResourceSyncParamsVariable() {
    val data = eirConfigService.resourceSyncParams
    Assert.assertEquals(data.size, 7)
    Assert.assertTrue(data.containsKey(ResourceType.Patient))
    Assert.assertTrue(data.containsKey(ResourceType.Questionnaire))
    Assert.assertTrue(data.containsKey(ResourceType.Observation))
    Assert.assertTrue(data.containsKey(ResourceType.Encounter))
    Assert.assertTrue(data.containsKey(ResourceType.CarePlan))
    Assert.assertTrue(data.containsKey(ResourceType.Condition))
    Assert.assertTrue(data.containsKey(ResourceType.Task))
  }

  @Test
  fun testProvideAuthConfiguration() {
    val authConfiguration = eirConfigService.provideAuthConfiguration()

    Assert.assertEquals(BuildConfig.FHIR_BASE_URL, authConfiguration.fhirServerBaseUrl)
    Assert.assertEquals(BuildConfig.OAUTH_BASE_URL, authConfiguration.oauthServerBaseUrl)
    Assert.assertEquals(BuildConfig.OAUTH_CIENT_ID, authConfiguration.clientId)
    Assert.assertEquals(BuildConfig.OAUTH_CLIENT_SECRET, authConfiguration.clientSecret)
    Assert.assertEquals(
      context.getString(R.string.authenticator_account_type),
      authConfiguration.accountType
    )
  }
}
