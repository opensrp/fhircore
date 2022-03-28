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

package org.smartregister.fhircore.anc.ui.family.removememberfamily

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.mockk
import org.hl7.fhir.r4.model.HumanName
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.StringType
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.anc.coroutine.CoroutineTestRule
import org.smartregister.fhircore.anc.data.family.FamilyDetailRepository
import org.smartregister.fhircore.anc.data.patient.DeletionReason
import org.smartregister.fhircore.anc.robolectric.RobolectricTest
import org.smartregister.fhircore.anc.ui.family.removefamilymember.RemoveFamilyMemberQuestionnaireViewModel

@HiltAndroidTest
class RemoveFamilyMemberQuestionnaireViewModelTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) var instantTaskExecutorRule = InstantTaskExecutorRule()

  @get:Rule(order = 2) var coroutineRule = CoroutineTestRule()

  private val familyDetailRepository: FamilyDetailRepository = mockk(relaxed = true)

  private lateinit var viewModel: RemoveFamilyMemberQuestionnaireViewModel

  @Before
  fun setUp() {
    hiltRule.inject()
    viewModel =
      RemoveFamilyMemberQuestionnaireViewModel(
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        mockk()
      )

    coEvery { familyDetailRepository.fetchDemographics("111") } returns getPatient()
  }

  private fun getPatient(): Patient {
    val patient =
      Patient().apply {
        id = "111"
        name =
          listOf(
            HumanName().apply {
              given = listOf(StringType("john"))
              family = "doe"
            }
          )
      }
    return patient
  }

  @Test
  fun testGetReasonRemove() {
    val deletionReason = viewModel.getReasonRemove("Moved away")
    Assert.assertEquals(deletionReason, DeletionReason.MOVED_AWAY)
    val deletionReasonTwo = viewModel.getReasonRemove("Other")
    Assert.assertEquals(deletionReasonTwo, DeletionReason.OTHER)
    val deletionReasonThree = viewModel.getReasonRemove("Died")
    Assert.assertEquals(deletionReasonThree, DeletionReason.DIED)
    val deletionReasonEmpty = viewModel.getReasonRemove("")
    Assert.assertEquals(deletionReasonEmpty, DeletionReason.OTHER)
  }
}
