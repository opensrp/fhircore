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

import androidx.compose.ui.graphics.Color
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.domain.model.OverflowMenuItem
import org.smartregister.fhircore.quest.R
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
    Assert.assertEquals(9, uiProfileNewlyDiagnosed.size)

    val uiProfileAlreadyOnART =
      overflowMenuFactory.retrieveOverflowMenuItems(OverflowMenuHost.ART_CLIENT_PROFILE)
    Assert.assertNotNull(uiProfileAlreadyOnART)
    Assert.assertEquals(8, uiProfileAlreadyOnART.size)

    val uiProfileExposedInfant =
      overflowMenuFactory.retrieveOverflowMenuItems(OverflowMenuHost.EXPOSED_INFANT_PROFILE)
    Assert.assertNotNull(uiProfileExposedInfant)
    Assert.assertEquals(6, uiProfileExposedInfant.size)

    val uiProfileChildContact =
      overflowMenuFactory.retrieveOverflowMenuItems(OverflowMenuHost.CHILD_CONTACT_PROFILE)
    Assert.assertNotNull(uiProfileChildContact)
    Assert.assertEquals(5, uiProfileChildContact.size)

    val uiProfileSexualContact =
      overflowMenuFactory.retrieveOverflowMenuItems(OverflowMenuHost.SEXUAL_CONTACT_PROFILE)
    Assert.assertNotNull(uiProfileSexualContact)
    Assert.assertEquals(5, uiProfileSexualContact.size)

    val uiProfileHivPositive =
      overflowMenuFactory.retrieveOverflowMenuItems(OverflowMenuHost.COMMUNITY_POSITIVE_PROFILE)
    Assert.assertNotNull(uiProfileHivPositive)
    Assert.assertEquals(5, uiProfileHivPositive.size)

    val uiProfileFamily =
      overflowMenuFactory.retrieveOverflowMenuItems(OverflowMenuHost.FAMILY_PROFILE)
    Assert.assertNotNull(uiProfileFamily)
    Assert.assertEquals(6, uiProfileFamily.size)

    val uiProfilePatient =
      overflowMenuFactory.retrieveOverflowMenuItems(OverflowMenuHost.PATIENT_PROFILE)
    Assert.assertNotNull(uiProfilePatient)
    Assert.assertEquals(5, uiProfilePatient.size)

    val uiProfileSiblingContact =
      overflowMenuFactory.retrieveOverflowMenuItems(OverflowMenuHost.SEXUAL_CONTACT_PROFILE)
    Assert.assertNotNull(uiProfileSiblingContact)
    Assert.assertEquals(5, uiProfileSiblingContact.size)

    val uiProfileSocialNetworkContact =
      overflowMenuFactory.retrieveOverflowMenuItems(OverflowMenuHost.SOCIAL_NETWORK_CONTACT_PROFILE)
    Assert.assertNotNull(uiProfileSocialNetworkContact)
    Assert.assertEquals(5, uiProfileSocialNetworkContact.size)

    val uiProfileBiologicalParentContact =
      overflowMenuFactory.retrieveOverflowMenuItems(
        OverflowMenuHost.BIOLOGICAL_PARENT_CONTACT_PROFILE
      )
    Assert.assertNotNull(uiProfileBiologicalParentContact)
    Assert.assertEquals(5, uiProfileBiologicalParentContact.size)
  }

  @Test
  fun `client_visit menu items should be hidden for new diagnosed and art clients`() {
    val newlyDiagnosedClientVisitMenuItem =
      overflowMenuFactory.retrieveOverflowMenuItems(OverflowMenuHost.NEWLY_DIAGNOSED_PROFILE)
        .first { it.id == R.id.client_visit }
    val artClientClientVisitMenuItem =
      overflowMenuFactory.retrieveOverflowMenuItems(OverflowMenuHost.ART_CLIENT_PROFILE).first {
        it.id == R.id.client_visit
      }
    Assert.assertTrue(newlyDiagnosedClientVisitMenuItem.hidden)
    Assert.assertTrue(artClientClientVisitMenuItem.hidden)
  }

  @Test
  fun `first newly_diagnosed_profile menuitem should be client_visit`() {
    val firstMenuItem =
      overflowMenuFactory
        .retrieveOverflowMenuItems(OverflowMenuHost.NEWLY_DIAGNOSED_PROFILE)
        .first()
    Assert.assertEquals(
      OverflowMenuItem(R.id.client_visit, R.string.client_visit).apply { hidden = true },
      firstMenuItem
    )
  }

  @Test
  fun testOverflowMenuItemConstructor() {
    val overflowItem =
      OverflowMenuItem(R.id.client_visit, R.string.client_visit).apply { hidden = true }
    Assert.assertEquals(R.id.client_visit, overflowItem.id)
    Assert.assertEquals(R.string.client_visit, overflowItem.titleResource)
    Assert.assertEquals(false, overflowItem.confirmAction)
    Assert.assertEquals(true, overflowItem.hidden)
    Assert.assertEquals(Color.Black.copy(alpha = 0.7f), overflowItem.titleColor)
  }
}
