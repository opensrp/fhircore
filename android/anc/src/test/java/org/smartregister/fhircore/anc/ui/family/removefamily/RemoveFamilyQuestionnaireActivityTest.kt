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

package org.smartregister.fhircore.anc.ui.family.removefamily

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AlertDialog
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.hl7.fhir.r4.model.Questionnaire
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowAlertDialog
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.coroutine.CoroutineTestRule
import org.smartregister.fhircore.anc.robolectric.ActivityRobolectricTest
import org.smartregister.fhircore.anc.ui.family.form.FamilyFormConstants
import org.smartregister.fhircore.anc.ui.family.form.FamilyQuestionnaireActivity
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.ui.base.AlertDialogue
import org.smartregister.fhircore.engine.ui.base.AlertIntent
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_ARG_FORM
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_ARG_TYPE
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireType
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireViewModel

@ExperimentalCoroutinesApi
@HiltAndroidTest
internal class RemoveFamilyQuestionnaireActivityTest : ActivityRobolectricTest() {

  @get:Rule(order = 0) var hiltRule = HiltAndroidRule(this)
  @get:Rule var coroutinesTestRule = CoroutineTestRule()

  @BindValue
  val questionnaireViewModel: QuestionnaireViewModel =
    spyk(QuestionnaireViewModel(mockk(), mockk(), mockk(), mockk(), mockk(), mockk(), mockk()))

  @BindValue
  val configurationRegistry: ConfigurationRegistry =
    spyk(ConfigurationRegistry(mockk(), mockk(), mockk()))

  private lateinit var removeFamilyQuestionnaireActivity: RemoveFamilyQuestionnaireActivity
  private lateinit var removeFamilyQuestionnaireActivitySpy: RemoveFamilyQuestionnaireActivity

  private val patientId = "123"

  @Before
  fun setUp() {
    hiltRule.inject()
    coEvery { questionnaireViewModel.libraryEvaluator.initialize() } just runs
    buildActivityFor(FamilyFormConstants.REMOVE_FAMILY, false)
    removeFamilyQuestionnaireActivitySpy =
      spyk(removeFamilyQuestionnaireActivity, recordPrivateCalls = true)
    every { removeFamilyQuestionnaireActivitySpy.finish() } returns Unit
  }

  @Test
  fun testActivityShouldNotNull() {
    buildActivityFor(FamilyFormConstants.REMOVE_FAMILY, false)
    assertNotNull(removeFamilyQuestionnaireActivity)
  }

  @Ignore("fails locally")
  @Test
  fun testOnBackPressedShouldCallConfirmationDialogue() {
    buildActivityFor(FamilyFormConstants.REMOVE_FAMILY, false)

    removeFamilyQuestionnaireActivity.onBackPressed()

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

  @Ignore("fails locally")
  @Test
  fun testOnRemoveFamilyShouldCallExpectedDialog() {
    buildActivityFor(FamilyFormConstants.REMOVE_FAMILY, false)

    ReflectionHelpers.callInstanceMethod<Void>(
      removeFamilyQuestionnaireActivitySpy,
      "removeFamilyMember"
    )

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

  @Ignore("fails locally")
  @Test
  fun showErrorAlertShouldShowDialogWithErrorTitleAndMessage() {
    mockkObject(AlertDialogue)

    every {
      AlertDialogue.showAlert(
        context = any(),
        alertIntent = AlertIntent.CONFIRM,
        message = any(),
        title = any(),
        confirmButtonListener = any(),
        confirmButtonText = any(),
        neutralButtonListener = any(),
        neutralButtonText = any()
      )
    } returns mockk()

    ReflectionHelpers.callInstanceMethod<Void>(
      removeFamilyQuestionnaireActivitySpy,
      "removeFamilyMember",
      ReflectionHelpers.ClassParameter.from(String::class.java, "123")
    )

    verify {
      AlertDialogue.showAlert(
        context = removeFamilyQuestionnaireActivitySpy,
        alertIntent = AlertIntent.CONFIRM,
        message = any(),
        title = any(),
        confirmButtonListener = any(),
        confirmButtonText = android.R.string.ok,
        neutralButtonListener = any(),
        neutralButtonText = android.R.string.cancel
      )
    }

    unmockkObject(AlertDialogue)
  }

  override fun getActivity(): Activity {
    return removeFamilyQuestionnaireActivity
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

    removeFamilyQuestionnaireActivity =
      Robolectric.buildActivity(RemoveFamilyQuestionnaireActivity::class.java, intent)
        .create()
        .get()
  }
}
