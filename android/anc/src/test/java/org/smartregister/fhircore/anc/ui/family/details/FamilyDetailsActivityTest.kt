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
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.anc.activity.ActivityRobolectricTest
import org.smartregister.fhircore.anc.data.family.model.FamilyMemberItem
import org.smartregister.fhircore.anc.shadow.AncApplicationShadow
import org.smartregister.fhircore.anc.ui.details.PatientDetailsActivity
import org.smartregister.fhircore.anc.ui.family.form.FamilyQuestionnaireActivity

@Config(shadows = [AncApplicationShadow::class])
class FamilyDetailsActivityTest : ActivityRobolectricTest() {

  private lateinit var activity: FamilyDetailsActivity

  @Before
  fun setUp() {
    mockkObject(FamilyDetailViewModel.Companion)

    val viewModel = mockk<FamilyDetailViewModel>()

    every { FamilyDetailViewModel.get(any(), any(), any()) } returns viewModel
    with(viewModel) {
      every { setAppBackClickListener(any()) } returns Unit
      every { setMemberItemClickListener(any()) } returns Unit
      every { setAddMemberItemClickListener(any()) } returns Unit
      every { setSeeAllEncounterClickListener(any()) } returns Unit
      every { setEncounterItemClickListener(any()) } returns Unit
    }

    activity = Robolectric.buildActivity(FamilyDetailsActivity::class.java).create().get()

    verify(exactly = 1) {
      viewModel.setAppBackClickListener(any())
      viewModel.setMemberItemClickListener(any())
      viewModel.setAddMemberItemClickListener(any())
      viewModel.setSeeAllEncounterClickListener(any())
      viewModel.setEncounterItemClickListener(any())
    }
  }

  @Test
  fun testOnBackIconClickedShouldCallFinish() {
    val spyActivity = spyk(activity)
    every { spyActivity.finish() } returns Unit

    ReflectionHelpers.callInstanceMethod<Any>(spyActivity, "onBackIconClicked")
    verify(exactly = 1) { spyActivity.finish() }
  }

  @Test
  fun testOnAddNewMemberButtonClickedShouldStartFamilyQuestionnaireActivity() {
    ReflectionHelpers.callInstanceMethod<Any>(activity, "onAddNewMemberButtonClicked")

    val expectedIntent = Intent(activity, FamilyQuestionnaireActivity::class.java)
    val actualIntent =
      shadowOf(ApplicationProvider.getApplicationContext<Application>()).nextStartedActivity

    Assert.assertEquals(expectedIntent.component, actualIntent.component)
  }

  @Test
  fun testOnFamilyMemberItemClickedShouldStartAncDetailsActivity() {
    val familyMemberItem = FamilyMemberItem("fmname", "fm1", "21", "F", true)

    ReflectionHelpers.callInstanceMethod<Any>(
      activity,
      "onFamilyMemberItemClicked",
      ReflectionHelpers.ClassParameter.from(FamilyMemberItem::class.java, familyMemberItem)
    )

    val expectedIntent = Intent(activity, PatientDetailsActivity::class.java)
    val actualIntent =
      shadowOf(ApplicationProvider.getApplicationContext<Application>()).nextStartedActivity

    Assert.assertEquals(expectedIntent.component, actualIntent.component)
  }

  override fun getActivity(): Activity {
    return activity
  }
}
