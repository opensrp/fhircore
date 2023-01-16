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
import android.content.ContextWrapper
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.os.Build
import android.os.LocaleList
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import java.util.Locale
import org.smartregister.fhircore.engine.ui.theme.DangerColor
import org.smartregister.fhircore.engine.ui.theme.DefaultColor
import org.smartregister.fhircore.engine.ui.theme.InfoColor
import org.smartregister.fhircore.engine.ui.theme.LightColors
import org.smartregister.fhircore.engine.ui.theme.SuccessColor
import org.smartregister.fhircore.engine.ui.theme.WarningColor
import timber.log.Timber

const val ERROR_COLOR = "errorColor"
const val PRIMARY_COLOR = "primaryColor"
const val PRIMARY_VARIANT_COLOR = "primaryVariantColor"
const val DEFAULT_COLOR = "defaultColor"
const val SUCCESS_COLOR = "successColor"
const val WARNING_COLOR = "warningColor"
const val DANGER_COLOR = "dangerColor"
const val INFO_COLOR = "infoColor"

fun Context.showToast(message: String, toastLength: Int = Toast.LENGTH_LONG) =
  Toast.makeText(this, message, toastLength).show()

fun Activity.refresh() {
  finish()
  startActivity(Intent(this, this.javaClass))
  finishAffinity()
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

/** Return a pair of application versionCode and versionName e.g. Pair(1, 0.0.1) */
fun Context.appVersion(): Pair<Int, String> =
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
 * Parse this [String] to a color code to be used in compose. Color code must either a). begin with
 * pound sign ('#') and should be of 6 valid characters or b). be equal to 'primaryColor',
 * 'primaryVariantColor' or 'errorColor'
 */
fun String?.parseColor(): androidx.compose.ui.graphics.Color {
  if (this.isNullOrEmpty()) {
    return ComposeColor.Unspecified
  } else if (this.startsWith("#")) {
    return ComposeColor(Color.parseColor(this))
  } else {
    when {
      this.equals(PRIMARY_COLOR, ignoreCase = true) -> return LightColors.primary
      this.equals(PRIMARY_VARIANT_COLOR, ignoreCase = true) -> return LightColors.primaryVariant
      this.equals(ERROR_COLOR, ignoreCase = true) -> return LightColors.error
      this.equals(DANGER_COLOR, ignoreCase = true) -> return DangerColor
      this.equals(WARNING_COLOR, ignoreCase = true) -> return WarningColor
      this.equals(INFO_COLOR, ignoreCase = true) -> return InfoColor
      this.equals(SUCCESS_COLOR, ignoreCase = true) -> return SuccessColor
      this.equals(DEFAULT_COLOR, ignoreCase = true) -> return DefaultColor
    }
  }
  return ComposeColor.Unspecified
}

fun Context.getActivity(): AppCompatActivity? =
  when (this) {
    is AppCompatActivity -> this
    is ContextWrapper -> baseContext.getActivity()
    else -> null
  }

/**
 * This is required to fix keyboard overlapping content in a Composable screen. This functionality
 * is applied after the setContent function of the activity is called.
 */
fun Activity.applyWindowInsetListener() {
  ViewCompat.setOnApplyWindowInsetsListener(this.findViewById(android.R.id.content)) { view, insets
    ->
    val bottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
    view.updatePadding(bottom = bottom)
    insets
  }
}
