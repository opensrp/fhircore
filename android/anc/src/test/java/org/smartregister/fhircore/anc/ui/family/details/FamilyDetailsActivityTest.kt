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

package org.smartregister.fhircore.anc.ui.family.details

import android.app.Activity
import android.app.Application
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.mockk
import java.util.Date
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.smartregister.fhircore.anc.app.fakes.FakeModel
import org.smartregister.fhircore.anc.data.family.FamilyDetailRepository
import org.smartregister.fhircore.anc.data.family.model.FamilyMemberItem
import org.smartregister.fhircore.anc.robolectric.ActivityRobolectricTest
import org.smartregister.fhircore.anc.ui.details.PatientDetailsActivity
import org.smartregister.fhircore.anc.ui.family.form.FamilyQuestionnaireActivity
import org.smartregister.fhircore.engine.util.extension.plusYears

@HiltAndroidTest
class FamilyDetailsActivityTest : ActivityRobolectricTest() {

  @BindValue val familyDetailRepository = mockk<FamilyDetailRepository>()

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  private val application = ApplicationProvider.getApplicationContext<Application>()

  private lateinit var familyDetailsActivity: FamilyDetailsActivity

  @Before
  fun setUp() {
    hiltRule.inject()
    coEvery { familyDetailRepository.fetchDemographics(any()) } returns
      FakeModel.buildPatient(id = "1", family = "Mandela", given = "Nelson")
    coEvery { familyDetailRepository.fetchFamilyMembers(any()) } returns listOf()
    coEvery { familyDetailRepository.fetchFamilyCarePlans(any()) } returns
      listOf(FakeModel.buildCarePlan("edd"))
    coEvery { familyDetailRepository.fetchEncounters(any()) } returns
      listOf(FakeModel.getEncounter("1"))
    familyDetailsActivity =
      Robolectric.buildActivity(FamilyDetailsActivity::class.java).create().resume().get()
  }

  @Test
  fun testOnBackIconClickedShouldCallFinish() {
    familyDetailsActivity.familyDetailViewModel.onAppBackClick()
    Assert.assertTrue(familyDetailsActivity.isFinishing)
  }

  @Test
  fun testOnAddNewMemberButtonClickedShouldStartFamilyQuestionnaireActivity() {
    familyDetailsActivity.familyDetailViewModel.onAddMemberItemClicked()
    val expectedIntent = Intent(familyDetailsActivity, FamilyQuestionnaireActivity::class.java)
    val actualIntent = shadowOf(application).nextStartedActivity

    Assert.assertEquals(expectedIntent.component, actualIntent.component)
  }

  @Test
  fun testOnFamilyMemberItemClickedShouldStartAncDetailsActivity() {
    val familyMemberItem =
      FamilyMemberItem("fmname", "fm1", Date().plusYears(-21), "F", true, false)

    familyDetailsActivity.familyDetailViewModel.onMemberItemClick(familyMemberItem)

    val expectedIntent = Intent(familyDetailsActivity, PatientDetailsActivity::class.java)
    val actualIntent = shadowOf(application).nextStartedActivity

    Assert.assertEquals(expectedIntent.component, actualIntent.component)
  }

  override fun getActivity(): Activity {
    return familyDetailsActivity
  }
}
