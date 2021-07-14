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

package org.smartregister.fhircore.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import com.google.android.fhir.search.Search
import com.google.android.fhir.search.StringFilterModifier
import java.util.Locale
import org.hl7.fhir.r4.model.Patient
import org.joda.time.DateTime
import org.joda.time.LocalDate
import org.joda.time.ReadablePartial
import org.joda.time.Years
import timber.log.Timber

object Utils {

  fun getAgeFromDate(dateOfBirth: String, currentDate: ReadablePartial? = null): Int {
    if (dateOfBirth.isNullOrEmpty()) return 0

    val date: DateTime = DateTime.parse(dateOfBirth)
    val age: Years = Years.yearsBetween(date.toLocalDate(), currentDate ?: LocalDate.now())
    return age.getYears()
  }

  fun addBasePatientFilter(search: Search) {
    search.filter(Patient.ADDRESS_CITY) {
      value = "NAIROBI"
      modifier = StringFilterModifier.MATCHES_EXACTLY
    }
  }

  fun EditText.addOnDrawableClickedListener(
    drawablePosition: DrawablePosition,
    onClicked: () -> Unit
  ) {
    this.setOnTouchListener(
      object : View.OnTouchListener {

        override fun onTouch(v: View?, event: MotionEvent?): Boolean {
          if (event!!.action == MotionEvent.ACTION_UP &&
              isDrawableClicked(drawablePosition, event, v as EditText)
          ) {
            onClicked()
            return true
          }
          return false
        }
      }
    )
  }

  fun setAppLocale(context: Context, languageTag: String?): Configuration? {
    val res: Resources = context.resources
    val configuration: Configuration = res.configuration
    try {
      val locale = Locale.forLanguageTag(languageTag)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        configuration.setLocale(locale)
        val localeList = LocaleList(locale)
        LocaleList.setDefault(localeList)
        configuration.setLocales(localeList)
        context.createConfigurationContext(configuration)
      } else {
        configuration.locale = locale
        res.updateConfiguration(configuration, res.displayMetrics)
      }
    } catch (e: Exception) {
      Timber.e(e)
    }
    return configuration
  }

  private fun isDrawableClicked(
    drawablePosition: DrawablePosition,
    event: MotionEvent?,
    view: EditText
  ): Boolean {
    return when (drawablePosition) {
      DrawablePosition.DRAWABLE_RIGHT ->
        event!!.rawX >=
          (view.right - view.compoundDrawables[drawablePosition.position].bounds.width())
      DrawablePosition.DRAWABLE_LEFT ->
        event!!.rawX <= (view.compoundDrawables[drawablePosition.position].bounds.width())
      else -> {
        return false
      }
    }
  }

  fun refreshActivity(activity: Activity) {
    val intent = Intent(activity, activity.javaClass)
    activity.startActivity(intent)
  }

  enum class DrawablePosition(val position: Int) {
    DRAWABLE_LEFT(0),
    DRAWABLE_TOP(1),
    DRAWABLE_RIGHT(2),
    DRAWABLE_BOTTOM(3)
  }
}
