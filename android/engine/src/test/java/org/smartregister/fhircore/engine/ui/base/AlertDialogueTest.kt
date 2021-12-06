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

package org.smartregister.fhircore.engine.ui.base

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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
      neutralButtonListener = { neutralCalled.add(true) }
    )

    val dialog = shadowOf(ShadowAlertDialog.getLatestAlertDialog())
    val alertDialog = ReflectionHelpers.getField<AlertDialog>(dialog, "realAlertDialog")

    assertSimpleMessageDialog(
      dialog,
      getString(R.string.questionnaire_alert_invalid_message),
      getString(R.string.questionnaire_alert_invalid_title),
      getString(R.string.questionnaire_alert_confirm_button_title)
    )

    // confirmButton.performClick()
    // Assert.assertTrue(confirmCalled.size > 0)

    // test additional neutral button
    val neutralButton = alertDialog.getButton(DialogInterface.BUTTON_NEUTRAL)
    Assert.assertEquals(
      getString(R.string.questionnaire_alert_ack_button_title),
      neutralButton.text
    )

    // neutralButton.performClick()
    // Assert.assertTrue(neutralCalled.size > 0)
  }

  @Test
  fun testShowProgressAlertShouldShowAlertWithProgress() {
    AlertDialogue.showProgressAlert(context = context, message = R.string.saving_registration)

    val dialog = shadowOf(ShadowAlertDialog.getLatestAlertDialog())
    val alertDialog = ReflectionHelpers.getField<AlertDialog>(dialog, "realAlertDialog")

    Assert.assertNotNull(dialog)
    Assert.assertTrue(alertDialog.isShowing)

    Assert.assertEquals(
      getString(R.string.saving_registration),
      dialog.view.findViewById<TextView>(R.id.tv_alert_message)!!.text
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
      message = R.string.saving_registration,
      title = R.string.questionnaire_alert_invalid_title,
      confirmButtonListener = {},
      confirmButtonText = R.string.submit_button_text
    )

    val dialog = shadowOf(ShadowAlertDialog.getLatestAlertDialog())
    val alertDialog = ReflectionHelpers.getField<AlertDialog>(dialog, "realAlertDialog")

    assertSimpleMessageDialog(
      dialog,
      getString(R.string.saving_registration),
      getString(R.string.questionnaire_alert_invalid_title),
      getString(R.string.submit_button_text)
    )

    // test an additional cancel or neutral button in confirm alert
    val neutralButton = alertDialog.getButton(DialogInterface.BUTTON_NEUTRAL)
    Assert.assertEquals(View.VISIBLE, neutralButton.visibility)
    Assert.assertEquals(
      getString(R.string.questionnaire_alert_neutral_button_title),
      neutralButton.text
    )
  }

  @Test
  fun testShowErrorAlertShouldShowAlertWithCorrectData() {
    AlertDialogue.showErrorAlert(
      context = context,
      message = R.string.error_saving_form,
      title = R.string.default_app_title
    )

    val dialog = shadowOf(ShadowAlertDialog.getLatestAlertDialog())

    assertSimpleMessageDialog(
      dialog,
      getString(R.string.error_saving_form),
      getString(R.string.default_app_title),
      getString(R.string.questionnaire_alert_ack_button_title)
    )
  }

  @Test
  fun testShowInfoAlertShouldShowAlertWithCorrectData() {
    AlertDialogue.showInfoAlert(
      context = context,
      message = "Here is the complete info",
      title = "Info title",
      confirmButtonText = R.string.submit_button_text
    )

    val dialog = shadowOf(ShadowAlertDialog.getLatestAlertDialog())

    assertSimpleMessageDialog(
      dialog,
      "Here is the complete info",
      "Info title",
      getString(R.string.submit_button_text)
    )
  }

  private fun assertSimpleMessageDialog(
    dialog: ShadowAlertDialog,
    message: String,
    title: String,
    confirmButtonTitle: String
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
}
