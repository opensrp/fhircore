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

package org.smartregister.fhircore.anc.ui.details

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Looper
import android.view.MenuInflater
import android.widget.TextView
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.FhirEngine
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.verify
import java.util.Date
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.jupiter.api.DisplayName
import org.robolectric.Robolectric
import org.robolectric.Shadows
import org.robolectric.fakes.RoboMenu
import org.robolectric.fakes.RoboMenuItem
import org.robolectric.shadows.ShadowAlertDialog
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.anc.AncApplication
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.coroutine.CoroutineTestRule
import org.smartregister.fhircore.anc.data.model.PatientDetailItem
import org.smartregister.fhircore.anc.data.model.PatientItem
import org.smartregister.fhircore.anc.data.patient.DeletionReason
import org.smartregister.fhircore.anc.data.patient.PatientRepository
import org.smartregister.fhircore.anc.robolectric.ActivityRobolectricTest
import org.smartregister.fhircore.anc.ui.anccare.details.AncDetailsViewModel
import org.smartregister.fhircore.anc.ui.anccare.encounters.EncounterListActivity
import org.smartregister.fhircore.anc.ui.details.bmicompute.BmiQuestionnaireActivity
import org.smartregister.fhircore.anc.ui.details.form.FormConfig
import org.smartregister.fhircore.anc.ui.family.form.FamilyQuestionnaireActivity
import org.smartregister.fhircore.engine.ui.base.AlertDialogue
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.util.extension.plusYears
import org.smartregister.fhircore.engine.util.extension.toAgeDisplay

@ExperimentalCoroutinesApi
@HiltAndroidTest
internal class PatientDetailsActivityTest : ActivityRobolectricTest() {

  @get:Rule(order = 0) var hiltRule = HiltAndroidRule(this)
  @get:Rule(order = 1) var instantTaskExecutorRule = InstantTaskExecutorRule()
  @get:Rule(order = 2) var coroutinesTestRule = CoroutineTestRule()

  private val appContext = ApplicationProvider.getApplicationContext<HiltTestApplication>()
  private lateinit var patientDetailsActivity: PatientDetailsActivity
  private lateinit var patientDetailsActivitySpy: PatientDetailsActivity
  private lateinit var fhirEngine: FhirEngine
  private lateinit var patientDetailsViewModel: AncDetailsViewModel
  private lateinit var patientRepository: PatientRepository
  private val patientId = "samplePatientId"

  private val ancPatientDetailItem = spyk<PatientDetailItem>()

  @Before
  fun setUp() {

    hiltRule.inject()
    fhirEngine = mockk(relaxed = true)
    patientRepository = mockk()

    every { ancPatientDetailItem.patientDetails } returns
      PatientItem(patientId, "Mandela Nelson", "Female", "26")
    every { ancPatientDetailItem.patientDetailsHead } returns PatientItem()
    coEvery { patientRepository.fetchDemographics(patientId) } returns ancPatientDetailItem

    patientDetailsViewModel =
      spyk(AncDetailsViewModel(patientRepository, coroutinesTestRule.testDispatcherProvider))

    patientDetailsActivity =
      Robolectric.buildActivity(PatientDetailsActivity::class.java, null).create().get()
    patientDetailsActivitySpy = spyk(objToCopy = patientDetailsActivity)
  }

  @Test
  @DisplayName("Should start patient details activity")
  fun testPatientActivityShouldNotNull() {
    Assert.assertNotNull(patientDetailsActivity)
  }

  @Test
  @DisplayName("Should inflate menu and return true")
  fun testThatMenuIsCreated() {

    val menuInflater = mockk<MenuInflater>()

    every { patientDetailsActivitySpy.menuInflater } returns menuInflater
    every { menuInflater.inflate(any(), any()) } returns Unit

    Assert.assertTrue(patientDetailsActivitySpy.onCreateOptionsMenu(null))
  }

  @Test
  @DisplayName("Should start Encounter List Activity")
  fun testOnClickedPastViewEncounterItemShouldStartEncounterListActivity() {

    val menuItem = RoboMenuItem(R.id.view_past_encounters)
    patientDetailsActivity.onOptionsItemSelected(menuItem)

    val expectedIntent = Intent(patientDetailsActivity, EncounterListActivity::class.java)
    val actualIntent =
      Shadows.shadowOf(ApplicationProvider.getApplicationContext<AncApplication>())
        .nextStartedActivity

    Assert.assertEquals(expectedIntent.component, actualIntent.component)
    Assert.assertFalse(patientDetailsActivity.onOptionsItemSelected(RoboMenuItem(-1)))
  }

  @Test
  fun testMakeAncPatientVisibilityActivity() {
    val menuItem = RoboMenuItem(R.id.anc_enrollment)
    patientDetailsActivity.onOptionsItemSelected(menuItem)
    Assert.assertTrue(menuItem.isVisible)
  }

  @Test
  fun testOnClickedAncEnrollmentItemShouldStartQuestionnaireActivity() {

    val menuItem = RoboMenuItem(R.id.anc_enrollment)
    patientDetailsActivity.onOptionsItemSelected(menuItem)

    val expectedIntent = Intent(patientDetailsActivity, FamilyQuestionnaireActivity::class.java)
    val actualIntent =
      Shadows.shadowOf(ApplicationProvider.getApplicationContext<AncApplication>())
        .nextStartedActivity

    Assert.assertEquals(expectedIntent.component, actualIntent.component)
    Assert.assertFalse(patientDetailsActivity.onOptionsItemSelected(RoboMenuItem(-1)))
  }

  @Test
  fun testOnClickedEditInfoItemShouldStartFamilyQuestionnaireActivity() {

    val menuItem = RoboMenuItem(R.id.edit_info)
    patientDetailsActivity.onOptionsItemSelected(menuItem)

    val expectedIntent = Intent(patientDetailsActivity, FamilyQuestionnaireActivity::class.java)
    val actualIntent =
      Shadows.shadowOf(ApplicationProvider.getApplicationContext<AncApplication>())
        .nextStartedActivity

    Assert.assertEquals(expectedIntent.component, actualIntent.component)
    Assert.assertFalse(patientDetailsActivity.onOptionsItemSelected(RoboMenuItem(-1)))
  }

  @Test
  fun testOnClickedAncEnrollmentItemShouldStartBMIQuestionnaire() {

    val menuItem = RoboMenuItem(R.id.bmi_widget)
    patientDetailsActivity.onOptionsItemSelected(menuItem)

    val expectedIntent = Intent(patientDetailsActivity, BmiQuestionnaireActivity::class.java)
    val actualIntent =
      Shadows.shadowOf(ApplicationProvider.getApplicationContext<AncApplication>())
        .nextStartedActivity

    Assert.assertEquals(expectedIntent.component, actualIntent.component)
    Assert.assertFalse(patientDetailsActivity.onOptionsItemSelected(RoboMenuItem(-1)))
  }

  @Test
  fun testOnClickedVitalSignsShouldShowADialog() {

    val menuItem = RoboMenuItem(R.id.add_vitals)
    patientDetailsActivity.onOptionsItemSelected(menuItem)

    Assert.assertFalse(patientDetailsActivity.onOptionsItemSelected(RoboMenuItem(-1)))
  }

  @Test
  fun testOnClickedLogDeathShouldShowDialog() {
    mockkObject(AlertDialogue)
    every {
      AlertDialogue.showDatePickerAlert(
        any(),
        any(),
        getString(R.string.log_death_button_title),
        any(),
        any(),
        getString(R.string.log_death_confirm_message),
        true
      )
    } returns mockk()

    val menuItem = RoboMenuItem(R.id.log_death)
    patientDetailsActivity.onOptionsItemSelected(menuItem)

    verify {
      AlertDialogue.showDatePickerAlert(
        any(),
        any(),
        getString(R.string.log_death_button_title),
        any(),
        any(),
        getString(R.string.log_death_confirm_message),
        true
      )
    }

    unmockkObject(AlertDialogue)
  }

  @Test
  fun testOnClickedRemovePersonShouldShowDialog() {
    mockkObject(AlertDialogue)
    every {
      AlertDialogue.showConfirmAlert(
        any(),
        R.string.remove_this_person_confirm_message,
        R.string.remove_this_person_confirm_title,
        any(),
        R.string.remove_this_person_button_title,
        any()
      )
    } returns mockk()

    val menuItem = RoboMenuItem(R.id.remove_this_person)
    patientDetailsActivity.onOptionsItemSelected(menuItem)

    verify {
      AlertDialogue.showConfirmAlert(
        any(),
        R.string.remove_this_person_confirm_message,
        R.string.remove_this_person_confirm_title,
        any(),
        R.string.remove_this_person_button_title,
        any()
      )
    }

    unmockkObject(AlertDialogue)
  }

  @Test
  fun testPrepareOptionsMenuShouldShowRelevantOptions() {
    val menu = RoboMenu()
    menu.add(0, R.id.add_vitals, 1, R.string.add_vitals)
    menu.add(0, R.id.log_death, 2, R.string.log_death)
    menu.add(0, R.id.anc_enrollment, 3, R.string.mark_as_anc_client)
    menu.add(0, R.id.remove_this_person, 4, R.string.remove_this_person)
    menu.add(0, R.id.pregnancy_outcome, 5, R.string.pregnancy_outcome)

    patientDetailsActivity.patient =
      MutableLiveData(
        PatientItem(birthDate = Date().plusYears(-21), gender = "Female", isPregnant = true)
      )
    patientDetailsActivity.onPrepareOptionsMenu(menu)

    Assert.assertTrue(menu.findMenuItem(getString(R.string.add_vitals)).isVisible)
    Assert.assertTrue(menu.findMenuItemContaining(getString(R.string.log_death)).isVisible)
    Assert.assertFalse(menu.findMenuItem(getString(R.string.mark_as_anc_client)).isVisible)
    Assert.assertTrue(menu.findMenuItemContaining(getString(R.string.remove_this_person)).isVisible)
    Assert.assertTrue(menu.findMenuItem(getString(R.string.pregnancy_outcome)).isVisible)
  }

  @Test
  fun testHandlePatientDemographics() {

    var patientDetailItem =
      PatientDetailItem(
        PatientItem(patientId, "Mandela Nelson", "Male", "26", isHouseHoldHead = true),
        PatientItem(patientId, "Mandela Nelson", "Male", "26")
      )

    ReflectionHelpers.callInstanceMethod<Any>(
      patientDetailsActivity,
      "handlePatientDemographics",
      ReflectionHelpers.ClassParameter(PatientDetailItem::class.java, patientDetailItem)
    )

    val patientDetails =
      patientDetailItem.patientDetails.name +
        ", " +
        patientDetailItem.patientDetails.gender +
        ", " +
        patientDetailItem.patientDetails.birthDate.toAgeDisplay()
    val patientId =
      patientDetailItem.patientDetails.address +
        " " +
        getString(R.string.bullet_character) +
        " ID: " +
        patientDetailItem.patientDetails.patientIdentifier +
        " " +
        getString(R.string.bullet_character) +
        " " +
        getString(R.string.head_of_household)

    val txtViewPatientDetails =
      patientDetailsActivity.findViewById<TextView>(R.id.txtView_patientDetails)

    val txtViewPatientId = patientDetailsActivity.findViewById<TextView>(R.id.txtView_patientId)

    Assert.assertEquals(patientDetails, txtViewPatientDetails.text.toString())
    Assert.assertEquals(patientId, txtViewPatientId.text.toString())

    patientDetailItem =
      PatientDetailItem(
        PatientItem(
          patientId,
          "Mandela Nelson",
          "fam",
          "Male",
          Date().plusYears(-26),
          isHouseHoldHead = false
        ),
        PatientItem(patientId, "Mandela Nelson", "fam", "Male", Date().plusYears(-26))
      )
    ReflectionHelpers.callInstanceMethod<Any>(
      patientDetailsActivity,
      "handlePatientDemographics",
      ReflectionHelpers.ClassParameter(PatientDetailItem::class.java, patientDetailItem)
    )

    Assert.assertEquals(
      "Mandela Nelson, Male, 26y",
      patientDetailsActivity.findViewById<TextView>(R.id.txtView_patientDetails).text.toString()
    )
    Assert.assertEquals(
      " · ID:  · ID: samplePatientId · Head of household · ",
      patientDetailsActivity.findViewById<TextView>(R.id.txtView_patientId).text.toString()
    )
  }

  @Test
  fun testOnDeleteFamilyMemberRequested() {
    var dialog = mockk<AlertDialog>()

    every { patientDetailsActivitySpy.getSelectedKey(any()) } returns DeletionReason.MOVED_OUT.name
    coEvery { patientDetailsActivitySpy.ancDetailsViewModel.deletePatient(any(), any()) } returns
      MutableLiveData(true)

    ReflectionHelpers.callInstanceMethod<Any>(
      patientDetailsActivitySpy,
      "onDeleteFamilyMemberRequested",
      ReflectionHelpers.ClassParameter(DialogInterface::class.java, dialog)
    )

    verify { patientDetailsActivitySpy.ancDetailsViewModel.deletePatient(any(), any()) }
  }

  @Test
  fun testShowAlertShouldVerifyListeners() {
    ReflectionHelpers.callInstanceMethod<Any>(patientDetailsActivity, "showAlert")
    var dialog = ShadowAlertDialog.getLatestDialog() as AlertDialog
    dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
    Shadows.shadowOf(Looper.getMainLooper()).idle()
    var actualIntent =
      Shadows.shadowOf(ApplicationProvider.getApplicationContext<AncApplication>())
        .nextStartedActivity

    Assert.assertEquals(
      FormConfig.ANC_VITAL_SIGNS_STANDARD,
      actualIntent.getStringExtra(QuestionnaireActivity.QUESTIONNAIRE_ARG_FORM)
    )

    ReflectionHelpers.callInstanceMethod<Any>(patientDetailsActivity, "showAlert")
    dialog = ShadowAlertDialog.getLatestDialog() as AlertDialog
    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick()
    Shadows.shadowOf(Looper.getMainLooper()).idle()
    actualIntent =
      Shadows.shadowOf(ApplicationProvider.getApplicationContext<AncApplication>())
        .nextStartedActivity

    Assert.assertEquals(
      FormConfig.ANC_VITAL_SIGNS_METRIC,
      actualIntent.getStringExtra(QuestionnaireActivity.QUESTIONNAIRE_ARG_FORM)
    )
  }

  override fun getActivity(): Activity {
    return patientDetailsActivity
  }
}
