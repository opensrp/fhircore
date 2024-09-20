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

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.content.DialogInterface
import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import java.util.Calendar
import java.util.Date
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.util.extension.getActivity
import org.smartregister.fhircore.engine.util.extension.hide
import org.smartregister.fhircore.engine.util.extension.show

enum class AlertIntent {
  PROGRESS,
  CONFIRM,
  ERROR,
  INFO,
}

data class AlertDialogListItem(val key: String, val value: String)

object AlertDialogue {
  private val ITEMS_LIST_KEY = "alert_dialog_items_list"

  fun showAlert(
    context: Context,
    alertIntent: AlertIntent,
    message: CharSequence,
    title: String? = null,
    confirmButtonListener: ((d: DialogInterface) -> Unit)? = null,
    @StringRes confirmButtonText: Int = R.string.questionnaire_alert_confirm_button_title,
    neutralButtonListener: ((d: DialogInterface) -> Unit)? = null,
    @StringRes neutralButtonText: Int = R.string.questionnaire_alert_neutral_button_title,
    cancellable: Boolean = false,
    options: Array<AlertDialogListItem>? = null,
  ): AlertDialog {
    val dialog =
      AlertDialog.Builder(context, R.style.AlertDialogTheme)
        .apply {
          val view = LayoutInflater.from(context).inflate(R.layout.alert_dialog, null)
          setView(view)
          title?.let { setTitle(it) }
          setCancelable(cancellable)
          neutralButtonListener?.let {
            setNeutralButton(neutralButtonText) { d, _ -> neutralButtonListener.invoke(d) }
          }
          confirmButtonListener?.let {
            setPositiveButton(confirmButtonText) { d, _ -> confirmButtonListener.invoke(d) }
          }
          options?.run { setSingleChoiceItems(options.map { it.value }.toTypedArray(), -1, null) }
        }
        .show()

    dialog
      .getButton(AlertDialog.BUTTON_NEUTRAL)
      .setTextColor(ContextCompat.getColor(context, R.color.grey_text_color))
    dialog.findViewById<View>(R.id.pr_circular)?.apply {
      if (alertIntent == AlertIntent.PROGRESS) {
        this.show()
      } else {
        this.hide()
      }
    }

    dialog.findViewById<TextView>(R.id.tv_alert_message)?.apply { this.text = message }

    options?.let {
      context.getActivity()?.run {
        intent?.putExtra(ITEMS_LIST_KEY, it)
        dialog.setOwnerActivity(this)
      }
    }

    return dialog
  }

  fun showInfoAlert(
    context: Context,
    message: String,
    title: String? = null,
    confirmButtonListener: ((d: DialogInterface) -> Unit) = { d -> d.dismiss() },
    @StringRes confirmButtonText: Int = R.string.questionnaire_alert_ack_button_title,
  ): AlertDialog {
    return showAlert(
      context = context,
      alertIntent = AlertIntent.INFO,
      message = message,
      title = title,
      confirmButtonListener = confirmButtonListener,
      confirmButtonText = confirmButtonText,
    )
  }

  fun showErrorAlert(context: Context, message: String, title: String? = null): AlertDialog {
    return showAlert(
      context = context,
      alertIntent = AlertIntent.ERROR,
      message = message,
      title = title,
      confirmButtonListener = { d -> d.dismiss() },
      confirmButtonText = R.string.questionnaire_alert_ack_button_title,
    )
  }

  fun showErrorAlert(
    context: Context,
    @StringRes message: Int,
    @StringRes title: Int? = null,
  ): AlertDialog {
    return showErrorAlert(
      context = context,
      message = context.getString(message),
      title = title?.let { context.getString(it) },
    )
  }

  fun showProgressAlert(context: Context, @StringRes message: Int): AlertDialog {
    return showAlert(context, AlertIntent.PROGRESS, context.getString(message))
  }

  fun showConfirmAlert(
    context: Context,
    @StringRes message: Int,
    @StringRes title: Int? = null,
    confirmButtonListener: ((d: DialogInterface) -> Unit),
    @StringRes confirmButtonText: Int,
    options: List<AlertDialogListItem>? = null,
  ): AlertDialog {
    return showAlert(
      context = context,
      alertIntent = AlertIntent.CONFIRM,
      message = context.getString(message),
      title = title?.let { context.getString(it) },
      confirmButtonListener = confirmButtonListener,
      confirmButtonText = confirmButtonText,
      neutralButtonListener = { d -> d.dismiss() },
      neutralButtonText = R.string.questionnaire_alert_neutral_button_title,
      cancellable = false,
      options = options?.toTypedArray(),
    )
  }

  fun showCancelAlert(
    context: Context,
    @StringRes message: Int,
    @StringRes title: Int? = null,
    confirmButtonListener: ((d: DialogInterface) -> Unit),
    @StringRes confirmButtonText: Int,
    neutralButtonListener: ((d: DialogInterface) -> Unit),
    @StringRes neutralButtonText: Int,
    cancellable: Boolean = true,
    options: List<AlertDialogListItem>? = null,
  ): AlertDialog {
    return showAlert(
      context = context,
      alertIntent = AlertIntent.CONFIRM,
      message = context.getString(message),
      title = title?.let { context.getString(it) },
      confirmButtonListener = confirmButtonListener,
      confirmButtonText = confirmButtonText,
      neutralButtonListener = neutralButtonListener,
      neutralButtonText = neutralButtonText,
      cancellable = cancellable,
      options = options?.toTypedArray(),
    )
  }

  fun showDatePickerAlert(
    context: Context,
    confirmButtonListener: (d: Date) -> Unit,
    confirmButtonText: String,
    max: Date?,
    title: String?,
    dangerActionColor: Boolean = true,
  ): DatePickerDialog {
    val dateDialog = DatePickerDialog(context)

    dateDialog
      .apply {
        max?.let { this.datePicker.maxDate = it.time }
        val id = Resources.getSystem().getIdentifier("date_picker_header_date", "id", "android")
        if (id != 0) {
          this.datePicker.findViewById<TextView>(id).textSize = 14f
        }

        title?.let {
          this.setCustomTitle(
            TextView(context).apply {
              this.text = it
              this.setPadding(20)
            },
          )
        }

        this.setButton(DialogInterface.BUTTON_POSITIVE, confirmButtonText) { d, _ ->
          val date =
            Calendar.getInstance().apply {
              (d as DatePickerDialog).datePicker.let { this.set(it.year, it.month, it.dayOfMonth) }
            }
          confirmButtonListener.invoke(date.time)
        }
      }
      .create()

    if (dangerActionColor) {
      dateDialog
        .getButton(DialogInterface.BUTTON_POSITIVE)
        .setTextColor(context.resources.getColor(R.color.colorError))
    }
    dateDialog.show()
    return dateDialog
  }
}
