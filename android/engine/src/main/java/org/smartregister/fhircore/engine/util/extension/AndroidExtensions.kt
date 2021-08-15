package org.smartregister.fhircore.engine.util.extension

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import android.view.View
import android.widget.Toast
import java.util.Locale
import org.smartregister.fhircore.engine.configuration.app.ConfigurableApplication
import timber.log.Timber

fun Context.showToast(message: String, toastLength: Int = Toast.LENGTH_LONG) =
  Toast.makeText(this, message, toastLength).show()

fun Activity.refresh() = startActivity(Intent(this, this.javaClass))

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
  return configuration
}

fun View.toggleVisibility(show: Boolean) =
  if (show) this.visibility = View.VISIBLE else this.visibility = View.GONE

fun Application.assertIsConfigurable() {
  if (this !is ConfigurableApplication)
    throw (IllegalStateException("Application MUST implement ConfigurableApplication interface"))
  else return
}
