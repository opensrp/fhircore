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

package org.smartregister.fhircore.engine.app

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.gson.Gson
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.spyk
import javax.inject.Inject
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper

@HiltAndroidTest
class ConfigServiceTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
  @Inject lateinit var gson: Gson

  private val application = ApplicationProvider.getApplicationContext<Application>()

  private lateinit var sharedPreferencesHelper: SharedPreferencesHelper

  private val configService = spyk(AppConfigService(ApplicationProvider.getApplicationContext()))

  @Before
  fun setUp() {
    hiltRule.inject()
    sharedPreferencesHelper = SharedPreferencesHelper(application, gson)
  }

  @Test
  fun testProvideSyncTagsShouldHaveOrganizationId() {
    val practitionerId = "practitioner-id"
    sharedPreferencesHelper.write(SharedPreferenceKey.PRACTITIONER_ID.name, practitionerId)

    val resourceTags = configService.provideResourceTags(sharedPreferencesHelper)
    val practitionerTag =
      resourceTags.firstOrNull { it.system == AppConfigService.PRACTITIONER_SYSTEM }

    Assert.assertEquals(practitionerId, practitionerTag?.code)
  }

  @Test
  fun testProvideSyncTagsShouldHaveLocationIds() {
    val locationId1 = "location-id1"
    val locationId2 = "location-id2"
    sharedPreferencesHelper.write(ResourceType.Location.name, listOf(locationId1, locationId2))

    val resourceTags = configService.provideResourceTags(sharedPreferencesHelper)
    val locationTags = resourceTags.filter { it.system == AppConfigService.LOCATION_SYSTEM }

    Assert.assertTrue(locationTags.any { it.code == locationId1 })
    Assert.assertTrue(locationTags.any { it.code == locationId2 })
  }

  @Test
  fun testProvideSyncTagsShouldHaveOrganizationIds() {
    val organizationId1 = "organization-id1"
    val organizationId2 = "organization-id2"
    sharedPreferencesHelper.write(
      ResourceType.Organization.name,
      listOf(organizationId1, organizationId2)
    )

    val resourceTags = configService.provideResourceTags(sharedPreferencesHelper)
    val organizationTags = resourceTags.filter { it.system == AppConfigService.ORGANIZATION_SYSTEM }

    Assert.assertTrue(organizationTags.any { it.code == organizationId1 })
    Assert.assertTrue(organizationTags.any { it.code == organizationId2 })
  }

  @Test
  fun testProvideSyncTagsShouldHaveCareTeamIds() {
    val careTeamId1 = "careteam-id1"
    val careTeamId2 = "careteam-id2"
    sharedPreferencesHelper.write(ResourceType.CareTeam.name, listOf(careTeamId1, careTeamId2))

    val resourceTags = configService.provideResourceTags(sharedPreferencesHelper)
    val organizationTags = resourceTags.filter { it.system == AppConfigService.CARETEAM_SYSTEM }

    Assert.assertTrue(organizationTags.any { it.code == careTeamId1 })
    Assert.assertTrue(organizationTags.any { it.code == careTeamId2 })
  }
}
