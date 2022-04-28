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
import android.app.AlertDialog
import android.app.Application
import android.content.DialogInterface
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.verify
import java.util.Date
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.anc.app.fakes.FakeModel
import org.smartregister.fhircore.anc.data.family.FamilyDetailRepository
import org.smartregister.fhircore.anc.data.family.model.FamilyMemberItem
import org.smartregister.fhircore.anc.robolectric.ActivityRobolectricTest
import org.smartregister.fhircore.anc.ui.details.PatientDetailsActivity
import org.smartregister.fhircore.anc.ui.family.form.FamilyQuestionnaireActivity
import org.smartregister.fhircore.quest.ui.family.removefamily.RemoveFamilyQuestionnaireActivity
import org.smartregister.fhircore.engine.ui.base.AlertDialogue
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
    familyDetailsActivity.familyName = "Test Family"
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
  fun testOnChangeFamilyHeadButtonClickedShouldShowAlert() = runBlockingTest {
    mockkObject(AlertDialogue)

    every { AlertDialogue.showConfirmAlert(any(), any(), any(), any(), any(), any()) } returns
      mockk()

    familyDetailsActivity.familyDetailViewModel.familyMembers.value =
      listOf(FamilyMemberItem("F1", "1", Date(), "M", false, false))

    familyDetailsActivity.familyDetailViewModel.onChangeHeadClicked()

    verify(timeout = 2000) {
      AlertDialogue.showConfirmAlert(any(), any(), any(), any(), any(), any())
    }

    unmockkObject(AlertDialogue)
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

  @Test
  fun testOnChangeFamilyHeadRequestedShouldCallRepository() {
    var dialog = mockk<AlertDialog>()

    val familyDetailsActivitySpy = spyk(familyDetailsActivity)

    every { dialog.dismiss() } answers {}
    every { familyDetailsActivitySpy.getSelectedKey(any()) } returns "111"
    coEvery {
      familyDetailsActivitySpy.familyDetailViewModel.changeFamilyHead(any(), any())
    } returns MutableLiveData(true)

    ReflectionHelpers.callInstanceMethod<Any>(
      familyDetailsActivitySpy,
      "onFamilyHeadChangeRequested",
      ReflectionHelpers.ClassParameter(DialogInterface::class.java, dialog)
    )

    verify { familyDetailsActivitySpy.familyDetailViewModel.changeFamilyHead(any(), any()) }
  }

  @Test
  fun testRemoveShouldShowRemoveFamilyConfirmationDialogue() {
    familyDetailsActivity.familyDetailViewModel.onRemoveFamilyMenuItemClicked()
    val expectedIntent =
      Intent(familyDetailsActivity, org.smartregister.fhircore.quest.ui.family.removefamily.RemoveFamilyQuestionnaireActivity::class.java)
    val actualIntent = shadowOf(application).nextStartedActivity
    Assert.assertEquals(expectedIntent.component, actualIntent.component)
  }

  override fun getActivity(): Activity {
    return familyDetailsActivity
  }
}
