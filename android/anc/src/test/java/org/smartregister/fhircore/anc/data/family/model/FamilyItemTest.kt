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

package org.smartregister.fhircore.anc.data.family.model

import dagger.hilt.android.testing.HiltAndroidTest
import java.util.Date
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.smartregister.fhircore.anc.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.extension.plusYears
import org.smartregister.fhircore.engine.util.extension.toAgeDisplay

@HiltAndroidTest
class FamilyItemTest : RobolectricTest() {

  private lateinit var familyItem: FamilyItem

  @Before
  fun setUp() {
    familyItem =
      FamilyItem(
        id = "1",
        identifier = "1",
        name = "Eve",
        address = "",
        head = FamilyMemberItem("Eve", "1", Date().plusYears(-27), "F", false, false),
        members = listOf(),
        servicesDue = 0,
        servicesOverdue = 0
      )
  }

  @Test
  fun testExtractDemographicsShouldReturnFlatDemographic() {
    Assert.assertEquals(
      "${familyItem.name}, ${familyItem.head.gender}, ${familyItem.head.birthdate.toAgeDisplay()}",
      familyItem.extractDemographics()
    )
  }
}
