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

package org.smartregister.fhircore.engine.util.extension

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import android.widget.Toast
import androidx.compose.ui.graphics.Color as ComposeColor
import java.util.Locale
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireType
import org.smartregister.fhircore.engine.ui.theme.DefaultColor
import timber.log.Timber

fun Context.showToast(message: String, toastLength: Int = Toast.LENGTH_LONG) =
  Toast.makeText(this, message, toastLength).show()

fun Activity.refresh() {
  startActivity(Intent(this, this.javaClass))
  finish()
}

fun Context.setAppLocale(languageTag: String): Configuration? {
  val res: Resources = this.resources
  val configuration: Configuration = res.configuration
  try {
    val locale = Locale.forLanguageTag(languageTag)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      configuration.setLocale(locale)
      val localeList = LocaleList(locale)
      LocaleList.setDefault(localeList)
      configuration.setLocales(localeList)
      this.createConfigurationContext(configuration)
    } else {
      configuration.locale = locale
      res.updateConfiguration(configuration, res.displayMetrics)
    }
  } catch (e: Exception) {
    Timber.e(e)
  }

  if (Build.VERSION.SDK_INT <= 23) {
    Locale.setDefault(Locale(languageTag))
  }

  return configuration
}

fun <T : Enum<T>> Enum<T>.isIn(vararg values: Enum<T>): Boolean {
  return values.any { this == it }
}

inline fun <reified Q : QuestionnaireActivity> Context.launchQuestionnaire(
  questionnaireId: String,
  clientIdentifier: String? = null,
  groupIdentifier: String? = null,
  questionnaireType: QuestionnaireType = QuestionnaireType.DEFAULT,
  intentBundle: Bundle = Bundle.EMPTY
) {
  this.startActivity(
    Intent(this, Q::class.java)
      .putExtras(intentBundle)
      .putExtras(
        QuestionnaireActivity.intentArgs(
          clientIdentifier = clientIdentifier,
          groupIdentifier = groupIdentifier,
          formName = questionnaireId,
          questionnaireType = questionnaireType
        )
      )
  )
}

inline fun <reified Q : QuestionnaireActivity> Context.launchQuestionnaireForResult(
  questionnaireId: String,
  clientIdentifier: String? = null,
  questionnaireType: QuestionnaireType = QuestionnaireType.DEFAULT,
  backReference: String? = null,
  intentBundle: Bundle = Bundle.EMPTY
) {
  (this as Activity).startActivityForResult(
    Intent(this, Q::class.java)
      .putExtras(intentBundle)
      .putExtras(
        QuestionnaireActivity.intentArgs(
          clientIdentifier = clientIdentifier,
          formName = questionnaireId,
          questionnaireType = questionnaireType,
          backReference = backReference
        )
      ),
    0
  )
}

/** Return a pair of application versionCode and versionName e.g. Pair(1, 0.0.1) */
fun Context.appVersion() =
  Pair(
    this.packageManager.getPackageInfo(this.packageName, 0).versionCode,
    this.packageManager.getPackageInfo(this.packageName, 0).versionName.substringBefore("-")
  )

fun Context.retrieveResourceId(resourceName: String?, resourceType: String = "drawable"): Int? {
  if (resourceName.isNullOrEmpty()) return null
  val resourceId = this.resources.getIdentifier(resourceName, resourceType, this.packageName)
  return if (resourceId != 0) resourceId else null
}

/**
 * Parse this [String] to a color code to be used in compose. Color code must begin with pound sign
 * ('#') and should be of 6 valid characters
 */
fun String?.parseColor() =
  if (this.isNullOrEmpty()) DefaultColor else ComposeColor(Color.parseColor(this))
