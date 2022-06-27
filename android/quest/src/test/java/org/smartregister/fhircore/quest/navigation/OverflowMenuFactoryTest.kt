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

package org.smartregister.fhircore.quest.navigation

import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

@HiltAndroidTest
class OverflowMenuFactoryTest : RobolectricTest() {

  @get:Rule val hiltRule = HiltAndroidRule(this)

  @Inject lateinit var overflowMenuFactory: OverflowMenuFactory

  @Before
  fun setup() {
    hiltRule.inject()
  }

  @Test
  fun testRetrieveOverflowMenuItems() {
    val uiProfileNewlyDiagnosed =
      overflowMenuFactory.retrieveOverflowMenuItems(OverflowMenuHost.NEWLY_DIAGNOSED_PROFILE)
    Assert.assertNotNull(uiProfileNewlyDiagnosed)
    Assert.assertEquals(6, uiProfileNewlyDiagnosed.size)

    val uiProfileAlreadyOnART =
      overflowMenuFactory.retrieveOverflowMenuItems(OverflowMenuHost.ART_CLIENT_PROFILE)
    Assert.assertNotNull(uiProfileAlreadyOnART)
    Assert.assertEquals(6, uiProfileAlreadyOnART.size)

    val uiProfileExposedInfant =
      overflowMenuFactory.retrieveOverflowMenuItems(OverflowMenuHost.EXPOSED_INFANT_PROFILE)
    Assert.assertNotNull(uiProfileExposedInfant)
    Assert.assertEquals(5, uiProfileExposedInfant.size)

    val uiProfileChildContact =
      overflowMenuFactory.retrieveOverflowMenuItems(OverflowMenuHost.CHILD_CONTACT_PROFILE)
    Assert.assertNotNull(uiProfileChildContact)
    Assert.assertEquals(4, uiProfileChildContact.size)

    val uiProfileSexualContact =
      overflowMenuFactory.retrieveOverflowMenuItems(OverflowMenuHost.SEXUAL_CONTACT_PROFILE)
    Assert.assertNotNull(uiProfileSexualContact)
    Assert.assertEquals(3, uiProfileSexualContact.size)

    val uiProfileHivPositive =
      overflowMenuFactory.retrieveOverflowMenuItems(OverflowMenuHost.COMMUNITY_POSITIVE_PROFILE)
    Assert.assertNotNull(uiProfileHivPositive)
    Assert.assertEquals(4, uiProfileHivPositive.size)

    val uiProfileFamily =
      overflowMenuFactory.retrieveOverflowMenuItems(OverflowMenuHost.FAMILY_PROFILE)
    Assert.assertNotNull(uiProfileFamily)
    Assert.assertEquals(5, uiProfileFamily.size)

    val uiProfilePatient =
      overflowMenuFactory.retrieveOverflowMenuItems(OverflowMenuHost.PATIENT_PROFILE)
    Assert.assertNotNull(uiProfilePatient)
    Assert.assertEquals(4, uiProfilePatient.size)
  }
}
