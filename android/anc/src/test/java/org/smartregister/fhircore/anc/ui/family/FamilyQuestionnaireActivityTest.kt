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

package org.smartregister.fhircore.anc.ui.family

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Looper
import androidx.appcompat.app.AlertDialog
import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.sync.Sync
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.spyk
import io.mockk.unmockkObject
import java.time.OffsetDateTime
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.BooleanType
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowAlertDialog
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.data.family.FamilyRepository
import org.smartregister.fhircore.anc.robolectric.ActivityRobolectricTest
import org.smartregister.fhircore.anc.ui.family.details.FamilyDetailsActivity
import org.smartregister.fhircore.anc.ui.family.form.FamilyFormConstants
import org.smartregister.fhircore.anc.ui.family.form.FamilyQuestionnaireActivity
import org.smartregister.fhircore.anc.ui.family.form.FamilyQuestionnaireActivity.Companion.QUESTIONNAIRE_CALLING_ACTIVITY
import org.smartregister.fhircore.anc.ui.family.register.FamilyRegisterActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_ARG_FORM
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_ARG_PATIENT_KEY
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireConfig

@HiltAndroidTest
internal class FamilyQuestionnaireActivityTest : ActivityRobolectricTest() {

  @get:Rule(order = 0) var hiltRule = HiltAndroidRule(this)

  private lateinit var familyQuestionnaireActivity: FamilyQuestionnaireActivity

  private lateinit var familyQuestionnaireActivitySpy: FamilyQuestionnaireActivity

  @Before
  fun setUp() {
    hiltRule.inject()
    mockkObject(Sync)
    every { Sync.basicSyncJob(any()).stateFlow() } returns flowOf()
    every { Sync.basicSyncJob(any()).lastSyncTimestamp() } returns OffsetDateTime.now()

    val intent =
      Intent().apply {
        putExtra(QUESTIONNAIRE_ARG_FORM, FamilyFormConstants.FAMILY_REGISTER_FORM)
        putExtra(FamilyQuestionnaireActivity.QUESTIONNAIRE_RELATED_TO_KEY, "Patient/1")
      }

    familyQuestionnaireActivity =
      Robolectric.buildActivity(FamilyQuestionnaireActivity::class.java, intent).create().get()
    familyQuestionnaireActivitySpy = spyk(objToCopy = familyQuestionnaireActivity)
  }

  @After
  fun cleanup() {
    unmockkObject(Sync)
  }

  @Test
  fun testActivityShouldNotNull() {
    assertNotNull(familyQuestionnaireActivity)
  }

  @Test
  fun testHandleFamilyRegistrationShouldCallPostProcessFamilyHead() {
    val familyRepository = mockk<FamilyRepository>()
    coEvery { familyRepository.postProcessFamilyHead(any(), any()) } returns "1832"

    val fhirEngine: FhirEngine = mockk()

    coEvery { fhirEngine.save(any()) } answers {}

    runBlocking { fhirEngine.save(Questionnaire().apply { id = "1832" }) }

    familyQuestionnaireActivity.questionnaireConfig =
      QuestionnaireConfig(
        appId = "appId",
        form = FamilyFormConstants.FAMILY_REGISTER_FORM,
        title = "Add Family",
        identifier = "1832"
      )

    familyQuestionnaireActivity.familyRepository = familyRepository

    ReflectionHelpers.setField(familyQuestionnaireActivity, "questionnaire", Questionnaire())

    familyQuestionnaireActivity.handleQuestionnaireResponse(QuestionnaireResponse())

    coVerify(timeout = 2000) { familyRepository.postProcessFamilyHead(any(), any()) }
  }

  @Test
  fun testEndActivityWithFamilyRegisterCallingActivityShouldReloadRegisterList() {
    familyQuestionnaireActivity.intent.putExtra(
      QUESTIONNAIRE_CALLING_ACTIVITY,
      FamilyRegisterActivity::class.java.name
    )
    ReflectionHelpers.callInstanceMethod<FamilyQuestionnaireActivity>(
      familyQuestionnaireActivity,
      "endActivity"
    )

    val expectedIntent = Intent(familyQuestionnaireActivity, FamilyRegisterActivity::class.java)
    val actualIntent =
      Shadows.shadowOf(ApplicationProvider.getApplicationContext<Application>()).nextStartedActivity

    assertEquals(expectedIntent.component, actualIntent.component)
  }

  @Test
  fun testHandleFamilyMemberRegistrationShouldCallPostProcessFamilyMember() {
    val familyRepository = mockk<FamilyRepository>()
    coEvery { familyRepository.postProcessFamilyMember(any(), any(), any()) } returns "1832"

    val fhirEngine: FhirEngine = mockk()

    coEvery { fhirEngine.save(any()) } answers {}

    runBlocking { fhirEngine.save(Questionnaire().apply { id = "1832" }) }

    familyQuestionnaireActivity.questionnaireConfig =
      QuestionnaireConfig(
        appId = "appId",
        form = FamilyFormConstants.FAMILY_MEMBER_REGISTER_FORM,
        title = "Add Family Member",
        identifier = "1832"
      )

    familyQuestionnaireActivity.familyRepository = familyRepository

    ReflectionHelpers.setField(familyQuestionnaireActivity, "questionnaire", Questionnaire())

    familyQuestionnaireActivity.handleQuestionnaireResponse(QuestionnaireResponse())

    coVerify(timeout = 2000) { familyRepository.postProcessFamilyMember(any(), any(), any()) }
  }

  @Test
  fun testHandlePregnancyWithPregnantShouldCallAncEnrollment() {
    val questionnaireResponse =
      QuestionnaireResponse().apply {
        addItem().apply {
          linkId = "is_pregnant"
          addAnswer().apply { value = BooleanType(true) }
        }
      }
    ReflectionHelpers.callInstanceMethod<FamilyQuestionnaireActivity>(
      familyQuestionnaireActivity,
      "handlePregnancy",
      ReflectionHelpers.ClassParameter(String::class.java, "1111"),
      ReflectionHelpers.ClassParameter(QuestionnaireResponse::class.java, questionnaireResponse),
      ReflectionHelpers.ClassParameter(String::class.java, FamilyFormConstants.ANC_ENROLLMENT_FORM)
    )

    val expectedIntent =
      Intent(familyQuestionnaireActivity, FamilyQuestionnaireActivity::class.java)
    val actualIntent =
      Shadows.shadowOf(ApplicationProvider.getApplicationContext<Application>()).nextStartedActivity

    assertEquals(expectedIntent.component, actualIntent.component)
    assertEquals(
      FamilyFormConstants.ANC_ENROLLMENT_FORM,
      actualIntent.getStringExtra(QUESTIONNAIRE_ARG_FORM)
    )
  }

  @Test
  fun testHandleAncEnrollmentShouldCallEnrollIntoAnc() {
    val familyRepository = mockk<FamilyRepository>()
    coEvery { familyRepository.enrollIntoAnc(any(), any(), any()) } just runs

    val fhirEngine: FhirEngine = mockk()

    coEvery { fhirEngine.save(any()) } answers {}

    runBlocking { fhirEngine.save(Questionnaire().apply { id = "1832" }) }

    familyQuestionnaireActivity.questionnaireConfig =
      QuestionnaireConfig(
        appId = "appId",
        form = FamilyFormConstants.ANC_ENROLLMENT_FORM,
        title = "Enroll into ANC",
        identifier = "1832"
      )

    familyQuestionnaireActivity.familyRepository = familyRepository
    familyQuestionnaireActivity.intent.putExtra(QUESTIONNAIRE_ARG_PATIENT_KEY, "123456")

    ReflectionHelpers.setField(familyQuestionnaireActivity, "questionnaire", Questionnaire())

    familyQuestionnaireActivity.handleQuestionnaireResponse(QuestionnaireResponse())

    coVerify(timeout = 2000) { familyRepository.enrollIntoAnc(any(), any(), any()) }
  }

  @Test
  fun testOnBackPressedShouldCallConfirmationDialogue() {
    familyQuestionnaireActivity.onBackPressed()

    val dialog = Shadows.shadowOf(ShadowAlertDialog.getLatestDialog())
    val alertDialog = ReflectionHelpers.getField<AlertDialog>(dialog, "realDialog")

    assertNotNull(alertDialog)
    assertEquals(
      getString(R.string.unsaved_changes_neg),
      alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).text
    )
    assertEquals(
      getString(R.string.unsaved_changes_pos),
      alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).text
    )

    val config = QuestionnaireConfig("", FamilyFormConstants.FAMILY_MEMBER_REGISTER_FORM, "", "")
    ReflectionHelpers.setField(familyQuestionnaireActivity, "questionnaireConfig", config)

    alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick()
    Shadows.shadowOf(Looper.getMainLooper()).idle()

    val expectedIntent = Intent(familyQuestionnaireActivity, FamilyDetailsActivity::class.java)
    val actualIntent =
      Shadows.shadowOf(ApplicationProvider.getApplicationContext<Application>()).nextStartedActivity

    assertEquals(expectedIntent.component, actualIntent.component)
  }

  override fun getActivity(): Activity {
    return familyQuestionnaireActivity
  }
}
