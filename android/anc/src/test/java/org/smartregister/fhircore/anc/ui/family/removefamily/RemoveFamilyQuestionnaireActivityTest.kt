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
import android.app.AlertDialog
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.hl7.fhir.r4.model.Questionnaire
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
import org.smartregister.fhircore.anc.robolectric.ActivityRobolectricTest
import org.smartregister.fhircore.anc.ui.family.form.FamilyFormConstants
import org.smartregister.fhircore.quest.ui.family.form.FamilyQuestionnaireActivity
import org.smartregister.fhircore.anc.ui.family.register.FamilyRegisterActivity
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
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

  @Before
  fun setUp() {
    hiltRule.inject()
    coEvery { questionnaireViewModel.libraryEvaluator.initialize() } just runs
  }

  @Test
  fun testActivityShouldNotNull() {
    buildActivityFor(FamilyFormConstants.REMOVE_FAMILY, false)
    assertNotNull(removeFamilyQuestionnaireActivity)
  }

  @Test
  fun testOnBackPressedShouldCallConfirmationDialogue() {
    buildActivityFor(FamilyFormConstants.REMOVE_FAMILY, false)
    removeFamilyQuestionnaireActivity.onBackPressed()

    val dialog = Shadows.shadowOf(ShadowAlertDialog.getLatestDialog())
    val alertDialog = ReflectionHelpers.getField<AlertDialog>(dialog, "realDialog")

    assertNotNull(alertDialog)
    assertEquals("", alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).text)
    assertEquals(
      getString(R.string.questionnaire_alert_back_pressed_button_title),
      alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).text
    )
  }

  @Test
  fun testOnRemoveFamilyShouldCallExpectedDialog() {
    buildActivityFor(FamilyFormConstants.REMOVE_FAMILY, false)

    ReflectionHelpers.callInstanceMethod<Void>(
      removeFamilyQuestionnaireActivity,
      "removeFamilyMember",
      ReflectionHelpers.ClassParameter.from(String::class.java, "1234"),
      ReflectionHelpers.ClassParameter.from(String::class.java, "Test FamilyName")
    )

    val dialog = Shadows.shadowOf(ShadowAlertDialog.getLatestDialog())
    val alertDialog = ReflectionHelpers.getField<AlertDialog>(dialog, "realDialog")

    assertNotNull(alertDialog)
    assertEquals("", alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).text)
    assertEquals(
      getString(R.string.family_register_ok_title),
      alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).text
    )
  }

  @Test
  fun testRemovingFamilyMoveToHomeAndFinishActivity() {
    buildActivityFor(FamilyFormConstants.REMOVE_FAMILY, false)
    removeFamilyQuestionnaireActivity.moveToHomePage()
    val expectedIntent =
      Intent(removeFamilyQuestionnaireActivity, FamilyRegisterActivity::class.java)
    val actualIntent =
      Shadows.shadowOf(ApplicationProvider.getApplicationContext<HiltTestApplication>())
        .nextStartedActivity
    assertEquals(expectedIntent.component, actualIntent.component)
    assertTrue(removeFamilyQuestionnaireActivity.isFinishing)
  }

  @Test
  fun testDiscardRemovingFinishActivity() {
    buildActivityFor(FamilyFormConstants.REMOVE_FAMILY, false)
    removeFamilyQuestionnaireActivity.discardRemovingAncBackToFamilyDetailPage()
    assertTrue(removeFamilyQuestionnaireActivity.isFinishing)
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
          addItem().linkId = org.smartregister.fhircore.quest.ui.family.form.FamilyQuestionnaireActivity.HEAD_RECORD_ID_KEY
      }
    every { configurationRegistry.appId } returns "anc"

    val intent =
      Intent().apply {
        putExtra(QUESTIONNAIRE_ARG_FORM, form)
        putExtra(
          QUESTIONNAIRE_ARG_TYPE,
          if (editForm) QuestionnaireType.EDIT.name else QuestionnaireType.DEFAULT.name
        )
        putExtra(org.smartregister.fhircore.quest.ui.family.form.FamilyQuestionnaireActivity.QUESTIONNAIRE_RELATED_TO_KEY, headId)
      }

    removeFamilyQuestionnaireActivity =
      Robolectric.buildActivity(RemoveFamilyQuestionnaireActivity::class.java, intent)
        .create()
        .get()
  }
}
