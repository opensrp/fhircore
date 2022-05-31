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
import androidx.appcompat.app.AlertDialog
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hl7.fhir.r4.model.BooleanType
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Reference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowAlertDialog
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.coroutine.CoroutineTestRule
import org.smartregister.fhircore.anc.data.family.FamilyRepository
import org.smartregister.fhircore.anc.robolectric.ActivityRobolectricTest
import org.smartregister.fhircore.anc.ui.family.form.FamilyFormConstants
import org.smartregister.fhircore.anc.ui.family.form.FamilyQuestionnaireActivity
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_ARG_FORM
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_ARG_PATIENT_KEY
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_ARG_TYPE
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireType
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireViewModel

@ExperimentalCoroutinesApi
@HiltAndroidTest
internal class FamilyQuestionnaireActivityTest : ActivityRobolectricTest() {

  @get:Rule(order = 0) var hiltRule = HiltAndroidRule(this)
  @get:Rule var coroutinesTestRule = CoroutineTestRule()

  @BindValue lateinit var questionnaireViewModel: QuestionnaireViewModel

  @BindValue lateinit var configurationRegistry: ConfigurationRegistry

  private lateinit var familyQuestionnaireActivity: FamilyQuestionnaireActivity
  private val patientId = "123"

  @Before
  fun setUp() {
    questionnaireViewModel =
      spyk(QuestionnaireViewModel(mockk(), mockk(), mockk(), mockk(), mockk(), mockk(), mockk()))
    configurationRegistry = spyk(ConfigurationRegistry(mockk(), mockk(), mockk()))

    hiltRule.inject()

    coEvery { questionnaireViewModel.libraryEvaluator.initialize() } just runs
  }

  @Test
  fun testActivityShouldNotNull() {
    buildActivityFor(FamilyFormConstants.FAMILY_REGISTER_FORM, false)
    assertNotNull(familyQuestionnaireActivity)
  }

  @Test
  fun testPostSaveSuccessfulWithFamilyRegistrationShouldCallHandlePregnancyForPregnantClient() =
      runBlockingTest {
    buildActivityFor(FamilyFormConstants.FAMILY_REGISTER_FORM, false)

    val questionnaireResponse = buildPregnantQuestionnaireResponse(true)

    familyQuestionnaireActivity.postSaveSuccessful(questionnaireResponse)

    assertAncEnrollmentQuestionnaireActivity()
  }

  @Test
  fun testPostSaveSuccessfulWithFamilyMemberRegistrationShouldCallHandlePregnancyForPregnantClient() =
      runBlockingTest {
    buildActivityFor(FamilyFormConstants.FAMILY_MEMBER_REGISTER_FORM, false, "123")

    val questionnaireResponse = buildPregnantQuestionnaireResponse(true)

    familyQuestionnaireActivity.postSaveSuccessful(questionnaireResponse)

    assertAncEnrollmentQuestionnaireActivity()
  }

  @Test
  fun testPostSaveSuccessfulWithAncServiceEnrollment() = runBlockingTest {
    buildActivityFor(FamilyFormConstants.ANC_ENROLLMENT_FORM, false, "123")

    val familyRepositoryMockk = mockk<FamilyRepository>()
    familyQuestionnaireActivity.familyRepository = familyRepositoryMockk

    val questionnaireResponse = buildPregnantQuestionnaireResponse(true)

    familyQuestionnaireActivity.postSaveSuccessful(questionnaireResponse)

    assertTrue(familyQuestionnaireActivity.isFinishing)
  }

  private fun assertAncEnrollmentQuestionnaireActivity() {
    val expectedIntent =
      Intent(familyQuestionnaireActivity, FamilyQuestionnaireActivity::class.java)
    val actualIntent =
      Shadows.shadowOf(ApplicationProvider.getApplicationContext<Application>()).nextStartedActivity

    assertEquals(expectedIntent.component, actualIntent.component)
    assertEquals(
      FamilyFormConstants.ANC_ENROLLMENT_FORM,
      actualIntent.getStringExtra(QUESTIONNAIRE_ARG_FORM)
    )
    assertEquals(patientId, actualIntent.getStringExtra(QUESTIONNAIRE_ARG_PATIENT_KEY))
  }

  @Test
  fun testOnBackPressedShouldCallConfirmationDialogue() {
    buildActivityFor(FamilyFormConstants.FAMILY_REGISTER_FORM, false)

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
  }

  @Test
  fun testTextOfSaveButtonForFamilyMemberRegistration() {
    buildActivityFor(FamilyFormConstants.FAMILY_MEMBER_REGISTER_FORM, false)

    assertEquals(
      getString(R.string.family_member_save_label, "Save"),
      familyQuestionnaireActivity.saveBtn.text.toString()
    )

    buildActivityFor(FamilyFormConstants.FAMILY_MEMBER_REGISTER_FORM, true)

    assertEquals(
      getString(R.string.family_member_save_label, "Update"),
      familyQuestionnaireActivity.saveBtn.text.toString()
    )
  }

  @Test
  fun testTextOfSaveButtonForFamilyRegistration() {
    buildActivityFor(FamilyFormConstants.FAMILY_REGISTER_FORM, false)

    assertEquals(
      getString(R.string.family_save_label, "Save"),
      familyQuestionnaireActivity.saveBtn.text.toString()
    )

    buildActivityFor(FamilyFormConstants.FAMILY_REGISTER_FORM, true)

    assertEquals(
      getString(R.string.family_save_label, "Update"),
      familyQuestionnaireActivity.saveBtn.text.toString()
    )
  }

  @Test
  fun testTextOfSavedButtonForAncRegister() {
    buildActivityFor(FamilyFormConstants.ANC_ENROLLMENT_FORM, false)

    assertEquals(
      getString(R.string.mark_as_anc_client),
      familyQuestionnaireActivity.saveBtn.text.toString()
    )

    buildActivityFor(FamilyFormConstants.ANC_ENROLLMENT_FORM, true)

    assertEquals(
      getString(R.string.mark_as_anc_client),
      familyQuestionnaireActivity.saveBtn.text.toString()
    )
  }

  private fun buildActivityFor(form: String, editForm: Boolean, headId: String? = null) {
    coEvery { questionnaireViewModel.loadQuestionnaire(any(), any()) } returns
      Questionnaire().apply {
        name = form
        title = form
        if (form == FamilyFormConstants.FAMILY_MEMBER_REGISTER_FORM)
          addItem().linkId = FamilyQuestionnaireActivity.HEAD_RECORD_ID_KEY
      }
    every { configurationRegistry.appId } returns "anc"

    val intent =
      Intent().apply {
        putExtra(QUESTIONNAIRE_ARG_FORM, form)
        putExtra(
          QUESTIONNAIRE_ARG_TYPE,
          if (editForm) QuestionnaireType.EDIT.name else QuestionnaireType.DEFAULT.name
        )
        putExtra(FamilyQuestionnaireActivity.QUESTIONNAIRE_RELATED_TO_KEY, headId)
      }

    familyQuestionnaireActivity =
      Robolectric.buildActivity(FamilyQuestionnaireActivity::class.java, intent).create().get()
  }

  private fun buildPregnantQuestionnaireResponse(pregnant: Boolean): QuestionnaireResponse {
    return QuestionnaireResponse().apply {
      addItem().apply {
        linkId = "is_pregnant"
        subject = Reference().apply { reference = "Patient/$patientId" }
        addAnswer().apply { value = BooleanType(pregnant) }
      }
    }
  }

  override fun getActivity(): Activity {
    return familyQuestionnaireActivity
  }
}
