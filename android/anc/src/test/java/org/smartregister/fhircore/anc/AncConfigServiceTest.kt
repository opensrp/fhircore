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

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Binary
import org.hl7.fhir.r4.model.Composition
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.anc.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.data.remote.model.response.UserInfo
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.encodeJson

@HiltAndroidTest
class AncConfigServiceTest : RobolectricTest() {

  @BindValue val sharedPreferencesHelper: SharedPreferencesHelper = mockk()

  @get:Rule val hiltRule = HiltAndroidRule(this)

  @Inject lateinit var configurationRegistry: ConfigurationRegistry
  @BindValue val repository: DefaultRepository = mockk()

  private lateinit var configService: AncConfigService

  @Before
  fun setUp() {
    hiltRule.inject()

    coEvery { repository.searchCompositionByIdentifier(any()) } returns
      "/configs/anc/config_composition.json".parseSampleResource() as Composition

    coEvery { repository.getBinary(any()) } returns Binary()
    coEvery { repository.getBinary("71729") } returns
      Binary().apply { content = "/configs/config_sync.json".readFile().toByteArray() }

    runBlocking { configurationRegistry.loadConfigurations("anc", {}) }

    configService =
      AncConfigService(
        context = ApplicationProvider.getApplicationContext(),
        configurationRegistry,
        sharedPreferencesHelper
      )
  }

  @Test
  fun testResourceSyncParam_shouldHaveResourceTypes() {
    every { sharedPreferencesHelper.read(any(), null) } returns
      UserInfo("ONA-Systems", "105", "Nairobi").encodeJson()

    val syncParam = configService.resourceSyncParams
    Assert.assertTrue(syncParam.isNotEmpty())
    Assert.assertTrue(syncParam.containsKey(ResourceType.Binary))
    Assert.assertTrue(syncParam.containsKey(ResourceType.QuestionnaireResponse))
    Assert.assertTrue(syncParam.containsKey(ResourceType.Questionnaire))
    Assert.assertTrue(syncParam.containsKey(ResourceType.Patient))
    Assert.assertTrue(syncParam.containsKey(ResourceType.Condition))
    Assert.assertTrue(syncParam.containsKey(ResourceType.Observation))
    Assert.assertTrue(syncParam.containsKey(ResourceType.Encounter))

    Assert.assertTrue(syncParam[ResourceType.Binary]!!.isEmpty())
    Assert.assertTrue(syncParam[ResourceType.QuestionnaireResponse]!!.isNotEmpty())
    Assert.assertTrue(syncParam[ResourceType.Questionnaire]!!.isNotEmpty())
    Assert.assertTrue(syncParam[ResourceType.Condition]!!.isNotEmpty())
    Assert.assertTrue(syncParam[ResourceType.Observation]!!.isNotEmpty())
    Assert.assertTrue(syncParam[ResourceType.Encounter]!!.isNotEmpty())
  }

  @Test
  fun testResourceSyncParam_organizationNull_shouldHaveEmptyMapForOrganizationBasedResources() {
    every { sharedPreferencesHelper.read(any(), null) } returns
      UserInfo("ONA-Systems", null, "Nairobi").encodeJson()

    val syncParam = configService.resourceSyncParams
    Assert.assertTrue(syncParam.isNotEmpty())
    Assert.assertTrue(syncParam.containsKey(ResourceType.Binary))
    Assert.assertTrue(syncParam.containsKey(ResourceType.QuestionnaireResponse))
    Assert.assertTrue(syncParam.containsKey(ResourceType.Questionnaire))
    Assert.assertTrue(syncParam.containsKey(ResourceType.Patient))
    Assert.assertTrue(syncParam.containsKey(ResourceType.Condition))
    Assert.assertTrue(syncParam.containsKey(ResourceType.Observation))
    Assert.assertTrue(syncParam.containsKey(ResourceType.Encounter))

    Assert.assertTrue(syncParam[ResourceType.Binary]!!.isEmpty())
    Assert.assertTrue(syncParam[ResourceType.QuestionnaireResponse]!!.isEmpty())
    Assert.assertTrue(syncParam[ResourceType.Questionnaire]!!.isNotEmpty())
    Assert.assertTrue(syncParam[ResourceType.Condition]!!.isEmpty())
    Assert.assertTrue(syncParam[ResourceType.Observation]!!.isEmpty())
    Assert.assertTrue(syncParam[ResourceType.Encounter]!!.isEmpty())
  }

  @Test
  fun testResourceSyncParam_publisherNull_shouldHaveEmptyMapForQuestionnaire() {
    every { sharedPreferencesHelper.read(any(), null) } returns
      UserInfo(null, "105", "Nairobi").encodeJson()

    val syncParam = configService.resourceSyncParams
    Assert.assertTrue(syncParam.isNotEmpty())
    Assert.assertTrue(syncParam.containsKey(ResourceType.Binary))
    Assert.assertTrue(syncParam.containsKey(ResourceType.QuestionnaireResponse))
    Assert.assertTrue(syncParam.containsKey(ResourceType.Questionnaire))
    Assert.assertTrue(syncParam.containsKey(ResourceType.Patient))
    Assert.assertTrue(syncParam.containsKey(ResourceType.Condition))
    Assert.assertTrue(syncParam.containsKey(ResourceType.Observation))
    Assert.assertTrue(syncParam.containsKey(ResourceType.Encounter))

    Assert.assertTrue(syncParam[ResourceType.Binary]!!.isEmpty())
    Assert.assertTrue(syncParam[ResourceType.QuestionnaireResponse]!!.isNotEmpty())
    Assert.assertTrue(syncParam[ResourceType.Questionnaire]!!.isEmpty())
    Assert.assertTrue(syncParam[ResourceType.Condition]!!.isNotEmpty())
    Assert.assertTrue(syncParam[ResourceType.Observation]!!.isNotEmpty())
    Assert.assertTrue(syncParam[ResourceType.Encounter]!!.isNotEmpty())
  }

  @Test
  fun testResourceSyncParam_WithNullExpressionValue_ShouldReturnEmptyMap() {
    every { sharedPreferencesHelper.read(any(), null) } returns
      UserInfo(null, null, null).encodeJson()

    val syncParam = configService.resourceSyncParams

    Assert.assertTrue(syncParam[ResourceType.Binary]!!.isEmpty())
    Assert.assertTrue(syncParam[ResourceType.QuestionnaireResponse]!!.isEmpty())
    Assert.assertTrue(syncParam[ResourceType.Questionnaire]!!.isEmpty())
    Assert.assertTrue(syncParam[ResourceType.Condition]!!.isEmpty())
    Assert.assertTrue(syncParam[ResourceType.Observation]!!.isEmpty())
    Assert.assertTrue(syncParam[ResourceType.Encounter]!!.isEmpty())
  }

  @Test
  fun testProvideAuthConfiguration() {
    val authConfiguration = configService.provideAuthConfiguration()

    Assert.assertEquals(BuildConfig.FHIR_BASE_URL, authConfiguration.fhirServerBaseUrl)
    Assert.assertEquals(BuildConfig.OAUTH_BASE_URL, authConfiguration.oauthServerBaseUrl)
    Assert.assertEquals(BuildConfig.OAUTH_CIENT_ID, authConfiguration.clientId)
    Assert.assertEquals(BuildConfig.OAUTH_CLIENT_SECRET, authConfiguration.clientSecret)
    Assert.assertEquals(
      ApplicationProvider.getApplicationContext<Application>()
        .getString(R.string.authenticator_account_type),
      authConfiguration.accountType
    )
  }
}
