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

package org.smartregister.fhircore.eir

import androidx.test.core.app.ApplicationProvider
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.smartregister.fhircore.eir.robolectric.RobolectricTest

class EirConfigServiceTest : RobolectricTest() {
/*
  private lateinit var eirConfigService: EirConfigService

  @Before
  fun setUp() {
    eirConfigService = EirConfigService(ApplicationProvider.getApplicationContext())
  }

  @Test
  fun testResourceSyncParamsVariable() {
    val data = eirConfigService.resourceSyncParams
    Assert.assertEquals(data.size, 5)
    Assert.assertTrue(data.containsKey(ResourceType.Patient))
    Assert.assertTrue(data.containsKey(ResourceType.Immunization))
    Assert.assertTrue(data.containsKey(ResourceType.Questionnaire))
    Assert.assertTrue(data.containsKey(ResourceType.StructureMap))
    Assert.assertTrue(data.containsKey(ResourceType.RelatedPerson))
  }

  @Test
  fun testProvideAuthConfiguration() {
    val authConfiguration = eirConfigService.provideAuthConfiguration()

    Assert.assertEquals(BuildConfig.FHIR_BASE_URL, authConfiguration.fhirServerBaseUrl)
    Assert.assertEquals(BuildConfig.OAUTH_BASE_URL, authConfiguration.oauthServerBaseUrl)
    Assert.assertEquals(BuildConfig.OAUTH_CIENT_ID, authConfiguration.clientId)
    Assert.assertEquals(BuildConfig.OAUTH_CLIENT_SECRET, authConfiguration.clientSecret)
    Assert.assertEquals("org.smartregister.fhircore.eir", authConfiguration.accountType)
  }*/
}
