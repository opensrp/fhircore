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

package org.smartregister.fhircore.engine.util.extension

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.LocaleList
import android.os.Parcelable
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.state.ToggleableState
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import java.io.Serializable
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.smartregister.fhircore.engine.datastore.dataFilterLocationIdsProtoStore
import org.smartregister.fhircore.engine.datastore.syncLocationIdsProtoStore
import org.smartregister.fhircore.engine.domain.model.MultiSelectViewAction
import org.smartregister.fhircore.engine.domain.model.SyncLocationState
import org.smartregister.fhircore.engine.ui.theme.DangerColor
import org.smartregister.fhircore.engine.ui.theme.DefaultColor
import org.smartregister.fhircore.engine.ui.theme.InfoColor
import org.smartregister.fhircore.engine.ui.theme.LightColors
import org.smartregister.fhircore.engine.ui.theme.SuccessColor
import org.smartregister.fhircore.engine.ui.theme.WarningColor
import org.smartregister.fhircore.engine.util.annotation.ExcludeFromJacocoGeneratedReport
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

fun Context.setAppLocale(languageTag: String): Configuration {
  val res: Resources = this.resources
  val configuration: Configuration = res.configuration
  try {
    val locale = Locale.forLanguageTag(languageTag)
    configuration.setLocale(locale)
    val localeList = LocaleList(locale)
    LocaleList.setDefault(localeList)
    configuration.setLocales(localeList)
    this.createConfigurationContext(configuration)
  } catch (e: Exception) {
    Timber.e(e)
  }

  return configuration
}

fun <T : Enum<T>> Enum<T>.isIn(vararg values: Enum<T>): Boolean {
  return values.any { this == it }
}

/** Return a pair of application versionCode and versionName e.g. Pair(1, 0.0.1) */
fun Context.appVersion(): Pair<Int, String> =
  Pair(
    this.packageManager.getPackageInfo(this.packageName, 0)?.versionCode ?: 1,
    this.packageManager.getPackageInfo(this.packageName, 0).versionName?.substringBefore("-")
      ?: "0.0.1",
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
  ViewCompat.setOnApplyWindowInsetsListener(this.findViewById(android.R.id.content)) { view, insets,
    ->
    val bottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
    view.updatePadding(bottom = bottom)
    insets
  }
}

/**
 * This function launches another [Activity] on top of the current. The current [Activity] is
 * cleared from the back stack for launching the next activity then the current [Activity] is
 * finished based on [finishLauncherActivity] condition.
 */
inline fun <reified A : Activity> Activity.launchActivityWithNoBackStackHistory(
  finishLauncherActivity: Boolean = true,
  bundle: Bundle = bundleOf(),
) {
  startActivity(
    Intent(this, A::class.java).apply {
      addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
      addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
      addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
      putExtras(bundle)
    },
  )
  if (finishLauncherActivity) finish()
}

/** This function checks if the device is online */
fun Context.isDeviceOnline(): Boolean {
  val connectivityManager =
    this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
  val network = connectivityManager.activeNetwork ?: return false
  val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

  // Device can be connected to the internet through any of these NetworkCapabilities
  val transports: List<Int> =
    listOf(
      NetworkCapabilities.TRANSPORT_ETHERNET,
      NetworkCapabilities.TRANSPORT_CELLULAR,
      NetworkCapabilities.TRANSPORT_WIFI,
      NetworkCapabilities.TRANSPORT_VPN,
    )

  return transports.any { capabilities.hasTransport(it) }
}

/**
 * This function returns the second element of the List. It complements the existing kotlin
 * List.first() and List.last() extensions
 */
fun <T> List<T>.second(): T = this[1]

@ExcludeFromJacocoGeneratedReport
inline fun <reified T : Parcelable> Intent.parcelable(key: String): T? =
  when {
    SDK_INT >= 33 -> getParcelableExtra(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelableExtra(key) as? T
  }

@ExcludeFromJacocoGeneratedReport
inline fun <reified T : Parcelable> Bundle.parcelable(key: String): T? =
  when {
    SDK_INT >= 33 -> getParcelable(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelable(key) as? T
  }

@ExcludeFromJacocoGeneratedReport
inline fun <reified T : Serializable> Intent.serializable(key: String): T? =
  when {
    SDK_INT >= 33 -> getSerializableExtra(key, T::class.java)
    else -> @Suppress("DEPRECATION") getSerializableExtra(key) as? T
  }

@ExcludeFromJacocoGeneratedReport
inline fun <reified T : Parcelable> Intent.parcelableArrayList(key: String): ArrayList<T>? =
  when {
    SDK_INT >= 33 -> getParcelableArrayListExtra(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelableArrayListExtra(key)
  }

suspend fun Context.retrieveRelatedEntitySyncLocationState(
  multiSelectViewAction: MultiSelectViewAction,
  filterToggleableStateOn: Boolean = true,
): List<SyncLocationState> {
  val selectedLocationStateMap =
    withContext(Dispatchers.IO) {
      val context = this@retrieveRelatedEntitySyncLocationState
      when (multiSelectViewAction) {
        MultiSelectViewAction.SYNC_DATA -> context.syncLocationIdsProtoStore.data.firstOrNull()
        MultiSelectViewAction.FILTER_DATA ->
          context.dataFilterLocationIdsProtoStore.data.firstOrNull()
      }
    }
  return if (filterToggleableStateOn) {
    selectedLocationStateMap?.values?.filter {
      it.toggleableState == ToggleableState.On &&
        selectedLocationStateMap[it.parentLocationId]?.toggleableState != ToggleableState.On
    }
  } else {
    selectedLocationStateMap?.values?.toList()
  } ?: emptyList()
}
