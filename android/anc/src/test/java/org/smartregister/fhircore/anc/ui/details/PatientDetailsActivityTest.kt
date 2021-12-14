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
import android.content.Intent
import android.os.Looper
import android.view.MenuInflater
import android.widget.TextView
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.FhirEngine
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
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
import org.smartregister.fhircore.anc.data.patient.PatientRepository
import org.smartregister.fhircore.anc.robolectric.ActivityRobolectricTest
import org.smartregister.fhircore.anc.ui.anccare.details.AncDetailsViewModel
import org.smartregister.fhircore.anc.ui.anccare.encounters.EncounterListActivity
import org.smartregister.fhircore.anc.ui.details.bmicompute.BmiQuestionnaireActivity
import org.smartregister.fhircore.anc.ui.details.form.FormConfig
import org.smartregister.fhircore.anc.ui.family.form.FamilyQuestionnaireActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity

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
  fun testHandlePatientDemographics() {

    var patientDetailItem =
      PatientDetailItem(
        PatientItem(patientId, "Mandela Nelson", "Male", "26", isHouseHoldHead = true),
        PatientItem(patientId, "Mandela Nelson", "Male", "26")
      )

    ReflectionHelpers.setField(patientDetailsActivity, "isMale", true)
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
        patientDetailItem.patientDetails.age
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
          "Male",
          "26",
          demographics = "NK",
          isHouseHoldHead = false
        ),
        PatientItem(patientId, "Mandela Nelson", "Male", "26")
      )
    ReflectionHelpers.setField(patientDetailsActivity, "isMale", false)
    ReflectionHelpers.callInstanceMethod<Any>(
      patientDetailsActivity,
      "handlePatientDemographics",
      ReflectionHelpers.ClassParameter(PatientDetailItem::class.java, patientDetailItem)
    )

    Assert.assertEquals(
      "Mandela Nelson, Male, 26",
      patientDetailsActivity.findViewById<TextView>(R.id.txtView_patientDetails).text.toString()
    )
    Assert.assertEquals(
      "NK · ID:  · ID: samplePatientId · Head of household · ",
      patientDetailsActivity.findViewById<TextView>(R.id.txtView_patientId).text.toString()
    )
  }

  @Test
  fun testOnPrepareOptionsMenuShouldVerifySpannableString() {
    val menu = RoboMenu(appContext)
    menu.add(0, R.id.remove_this_person, 0, "remove this person")
    menu.add(0, R.id.anc_enrollment, 0, "anc enrollment")
    menu.add(0, R.id.pregnancy_outcome, 0, "pregnancy outcome")

    ReflectionHelpers.setField(patientDetailsActivity, "isMale", true)

    patientDetailsActivity.onPrepareOptionsMenu(menu)

    Assert.assertEquals(
      "remove this person",
      menu.findItem(R.id.remove_this_person).title.toString()
    )
    Assert.assertFalse(menu.findItem(R.id.pregnancy_outcome).isVisible)
    Assert.assertFalse(menu.findItem(R.id.anc_enrollment).isVisible)
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
