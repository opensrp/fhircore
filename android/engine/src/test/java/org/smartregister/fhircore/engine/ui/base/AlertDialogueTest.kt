/*
 * Copyright 2021-2024 Ona Systems, Inc
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

package org.smartregister.fhircore.engine.ui.base

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.util.Date
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAlertDialog
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.robolectric.ActivityRobolectricTest

class AlertDialogueTest : ActivityRobolectricTest() {
  private lateinit var context: Activity

  @Before
  fun setup() {
    context = Robolectric.buildActivity(AppCompatActivity::class.java).create().get()
    context.setTheme(R.style.AppTheme)
  }

  @Test
  fun testShowAlertShouldShowAlertWithCorrectDetails() {
    val confirmCalled = mutableListOf<Boolean>()
    val neutralCalled = mutableListOf<Boolean>()

    AlertDialogue.showAlert(
      context = context,
      alertIntent = AlertIntent.ERROR,
      message = getString(R.string.questionnaire_alert_invalid_message),
      title = getString(R.string.questionnaire_alert_invalid_title),
      confirmButtonText = R.string.questionnaire_alert_confirm_button_title,
      confirmButtonListener = { confirmCalled.add(true) },
      neutralButtonText = R.string.questionnaire_alert_ack_button_title,
      neutralButtonListener = { neutralCalled.add(true) },
      options = arrayOf(AlertDialogListItem("a", "A"), AlertDialogListItem("b", "B")),
    )

    val dialog = shadowOf(ShadowAlertDialog.getLatestAlertDialog())
    val alertDialog = ReflectionHelpers.getField<AlertDialog>(dialog, "realAlertDialog")

    assertSimpleMessageDialog(
      dialog,
      getString(R.string.questionnaire_alert_invalid_message),
      getString(R.string.questionnaire_alert_invalid_title),
      getString(R.string.questionnaire_alert_confirm_button_title),
    )

    Assert.assertEquals(2, alertDialog.listView.count)

    // TODO: test click
    // confirmButton.performClick()
    // Assert.assertTrue(confirmCalled.size > 0)

    // test additional neutral button
    val neutralButton = alertDialog.getButton(DialogInterface.BUTTON_NEUTRAL)
    Assert.assertEquals(
      getString(R.string.questionnaire_alert_ack_button_title),
      neutralButton.text,
    )

    // TODO: test click
    // neutralButton.performClick()
    // Assert.assertTrue(neutralCalled.size > 0)
  }

  @Test
  fun testShowProgressAlertShouldShowAlertWithProgress() {
    AlertDialogue.showProgressAlert(context = context, message = R.string.form_progress_message)

    val dialog = shadowOf(ShadowAlertDialog.getLatestAlertDialog())
    val alertDialog = ReflectionHelpers.getField<AlertDialog>(dialog, "realAlertDialog")

    Assert.assertNotNull(dialog)
    Assert.assertTrue(alertDialog.isShowing)

    Assert.assertEquals(
      getString(R.string.form_progress_message),
      dialog.view.findViewById<TextView>(R.id.tv_alert_message)!!.text,
    )

    Assert.assertEquals(View.VISIBLE, dialog.view.findViewById<View>(R.id.pr_circular)!!.visibility)

    val confirmButton = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE)
    Assert.assertEquals(View.GONE, confirmButton.visibility)

    val neutralButton = alertDialog.getButton(DialogInterface.BUTTON_NEUTRAL)
    Assert.assertEquals(View.GONE, neutralButton.visibility)
  }

  @Test
  fun testShowConfirmAlertShouldShowAlertWithCorrectData() {
    AlertDialogue.showConfirmAlert(
      context = context,
      message = R.string.form_progress_message,
      title = R.string.questionnaire_alert_invalid_title,
      confirmButtonListener = {},
      confirmButtonText = com.google.android.fhir.datacapture.R.string.submit_questionnaire,
    )

    val dialog = shadowOf(ShadowAlertDialog.getLatestAlertDialog())
    val alertDialog = ReflectionHelpers.getField<AlertDialog>(dialog, "realAlertDialog")

    assertSimpleMessageDialog(
      dialog,
      getString(R.string.form_progress_message),
      getString(R.string.questionnaire_alert_invalid_title),
      getString(com.google.android.fhir.datacapture.R.string.submit_questionnaire),
    )

    // test an additional cancel or neutral button in confirm alert
    val neutralButton = alertDialog.getButton(DialogInterface.BUTTON_NEUTRAL)
    Assert.assertEquals(View.VISIBLE, neutralButton.visibility)
    Assert.assertEquals(
      getString(R.string.questionnaire_alert_neutral_button_title),
      neutralButton.text,
    )
  }

  @Test
  fun testShowCancelAlertShowsWithCorrectData() {
    AlertDialogue.showCancelAlert(
      context = context,
      message = R.string.questionnaire_in_progress_alert_back_pressed_message,
      title = R.string.questionnaire_alert_back_pressed_title,
      confirmButtonListener = {},
      confirmButtonText = R.string.questionnaire_alert_back_pressed_save_draft_button_title,
      neutralButtonListener = {},
      neutralButtonText = R.string.questionnaire_alert_back_pressed_button_title,
      negativeButtonListener = {},
      negativeButtonText = R.string.questionnaire_alert_negative_button_title,
    )
    val dialog = shadowOf(ShadowAlertDialog.getLatestAlertDialog())

    assertSimpleMessageDialog(
      dialog,
      getString(R.string.questionnaire_in_progress_alert_back_pressed_message),
      getString(R.string.questionnaire_alert_back_pressed_title),
      getString(R.string.questionnaire_alert_back_pressed_save_draft_button_title),
    )
    Assert.assertTrue(dialog.isCancelable)
  }

  @Test
  fun testShowErrorAlertShouldShowAlertWithCorrectData() {
    AlertDialogue.showErrorAlert(
      context = context,
      message = R.string.error_saving_form,
      title = R.string.default_app_title,
    )

    val dialog = shadowOf(ShadowAlertDialog.getLatestAlertDialog())

    assertSimpleMessageDialog(
      dialog,
      getString(R.string.error_saving_form),
      getString(R.string.default_app_title),
      getString(R.string.questionnaire_alert_ack_button_title),
    )
  }

  @Test
  fun testShowInfoAlertShouldShowAlertWithCorrectData() {
    AlertDialogue.showInfoAlert(
      context = context,
      message = "Here is the complete info",
      title = "Info title",
      confirmButtonText = com.google.android.fhir.datacapture.R.string.submit_questionnaire,
    )

    val dialog = shadowOf(ShadowAlertDialog.getLatestAlertDialog())

    assertSimpleMessageDialog(
      dialog,
      "Here is the complete info",
      "Info title",
      getString(com.google.android.fhir.datacapture.R.string.submit_questionnaire),
    )
  }

  @Test
  fun testShowDatePromptShouldShowAlertWithCorrectData() {
    AlertDialogue.showDatePickerAlert(
      context = context,
      confirmButtonListener = {},
      confirmButtonText = "Date confirm",
      max = Date(),
      title = "Date title",
    )

    val dialog = shadowOf(ShadowAlertDialog.getLatestAlertDialog())
    val alertDialog = ReflectionHelpers.getField<AlertDialog>(dialog, "realAlertDialog")

    Assert.assertNotNull(dialog)
    Assert.assertTrue(alertDialog.isShowing)

    Assert.assertEquals("Date title", (dialog.customTitleView as TextView).text)

    val confirmButton = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE)
    Assert.assertEquals(View.VISIBLE, confirmButton.visibility)
    Assert.assertEquals("Date confirm", confirmButton.text)
  }

  private fun assertSimpleMessageDialog(
    dialog: ShadowAlertDialog,
    message: String,
    title: String,
    confirmButtonTitle: String,
  ) {
    val alertDialog = ReflectionHelpers.getField<AlertDialog>(dialog, "realAlertDialog")

    Assert.assertNotNull(dialog)
    Assert.assertTrue(alertDialog.isShowing)

    Assert.assertEquals(message, dialog.view.findViewById<TextView>(R.id.tv_alert_message)!!.text)
    Assert.assertEquals(title, dialog.title)

    Assert.assertEquals(View.GONE, dialog.view.findViewById<View>(R.id.pr_circular)!!.visibility)

    val confirmButton = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE)
    Assert.assertEquals(View.VISIBLE, confirmButton.visibility)
    Assert.assertEquals(confirmButtonTitle, confirmButton.text)
  }

  override fun getActivity(): Activity {
    return context
  }

  @Test
  fun testAlertDialogNeutralButtonReturnsCorrectColor() {
    AlertDialogue.showInfoAlert(
      context = context,
      message = "Please confirm that you have all details filled in before submission",
      title = "Submit Details",
      confirmButtonText = R.string.questionnaire_alert_neutral_button_title,
    )

    val dialog = shadowOf(ShadowAlertDialog.getLatestAlertDialog())
    val alertDialog = ReflectionHelpers.getField<AlertDialog>(dialog, "realAlertDialog")

    // test an additional cancel or neutral button in confirm alert
    val neutralButton = alertDialog.getButton(DialogInterface.BUTTON_NEUTRAL)
    Assert.assertEquals(
      ContextCompat.getColor(context, R.color.grey_text_color),
      neutralButton.currentTextColor,
    )
  }
}
