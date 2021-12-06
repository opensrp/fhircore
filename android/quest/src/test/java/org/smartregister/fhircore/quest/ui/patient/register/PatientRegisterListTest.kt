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

package org.smartregister.fhircore.quest.ui.patient.register

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.hasAnyChild
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import androidx.paging.LoadState
import io.mockk.every
import io.mockk.spyk
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.quest.data.patient.model.PatientItem
import org.smartregister.fhircore.quest.robolectric.RobolectricTest
import org.smartregister.fhircore.quest.ui.patient.register.components.PATIENT_BIO
import org.smartregister.fhircore.quest.ui.patient.register.components.PatientRegisterList
import org.smartregister.fhircore.quest.ui.patient.register.components.dummyPatientPagingList

class PatientRegisterListTest : RobolectricTest() {
  @get:Rule val composeRule = createComposeRule()

  @Test
  fun testPatientRegisterListShouldHaveAllItemWithCorrectData() {
    composeRule.runOnIdle {
      composeRule.setContent {
        val pagingItemsSpy = spyk(dummyPatientPagingList())

        every { pagingItemsSpy.loadState.append } returns LoadState.NotLoading(true)
        every { pagingItemsSpy.loadState.refresh } returns LoadState.NotLoading(true)

        PatientRegisterList(pagingItems = pagingItemsSpy, clickListener = { _, _ -> })
      }

      composeRule.onAllNodesWithTag(PATIENT_BIO).assertCountEquals(2)

      composeRule.onAllNodesWithTag(PATIENT_BIO, true)[0]
        .assertHasClickAction()
        .assert(hasAnyChild(hasText("John Doe, 27y")))
        .assert(hasAnyChild(hasText("Male")))

      composeRule.onAllNodesWithTag(PATIENT_BIO, true)[1]
        .assertHasClickAction()
        .assert(hasAnyChild(hasText("Jane Doe, 20y")))
        .assert(hasAnyChild(hasText("Female")))
    }
  }

  @Test
  fun testPatientRegisterListItemShouldCallItemClickListener() {
    val clickedItemList = mutableListOf<PatientItem>()

    composeRule.runOnIdle {
      composeRule.setContent {
        val pagingItemsSpy = spyk(dummyPatientPagingList())

        every { pagingItemsSpy.loadState.append } returns LoadState.NotLoading(true)
        every { pagingItemsSpy.loadState.refresh } returns LoadState.NotLoading(true)

        PatientRegisterList(
          pagingItems = pagingItemsSpy,
          clickListener = { i, p ->
            // click intent should be open profile
            Assert.assertEquals(OpenPatientProfile, i)

            clickedItemList.add(p)
          }
        )
      }

      composeRule.onAllNodesWithTag(PATIENT_BIO).assertCountEquals(2)
      composeRule.onAllNodesWithTag(PATIENT_BIO)[0].performClick()
      composeRule.onAllNodesWithTag(PATIENT_BIO)[1].performClick()

      Assert.assertEquals("John Doe", clickedItemList[0].name)
      Assert.assertEquals("Jane Doe", clickedItemList[1].name)
    }
  }
}
