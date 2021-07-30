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
import android.widget.TextView
import androidx.core.content.ContextCompat
import ca.uhn.fhir.context.FhirContext
import com.google.android.fhir.search.Search
import com.google.android.fhir.search.StringFilterModifier
import com.google.android.fhir.search.search
import java.text.SimpleDateFormat
import com.google.gson.Gson
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.ResourceType
import org.joda.time.DateTime
import org.joda.time.LocalDate
import org.joda.time.ReadablePartial
import org.joda.time.Years
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import org.smartregister.fhircore.FhirApplication
import org.smartregister.fhircore.api.HapiFhirService
import org.smartregister.fhircore.data.HapiFhirResourceDataSource
import org.smartregister.fhircore.model.PatientItem
import timber.log.Timber

object Utils {
  private val gson = Gson()

  fun getAgeFromDate(dateOfBirth: String, currentDate: ReadablePartial? = null): Int {
    val date: DateTime = DateTime.parse(dateOfBirth)
    val age: Years = Years.yearsBetween(date.toLocalDate(), currentDate ?: LocalDate.now())
    return age.years
  }

  /** Load dynamic config for given key. This would be loaded from FHIR DB later todo */
  fun <T> loadConfig(config: String, clz: Class<T>, context: Context): T {
    return gson.fromJson(context.assets.open(config).bufferedReader().use { it.readText() }, clz)
  }

  fun addBasePatientFilter(search: Search) {
    search.filter(Patient.ADDRESS_CITY) {
      modifier = StringFilterModifier.CONTAINS
      value = "NAIROBI"
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

  fun addDays(initialDate: String, daysToAdd: Int = 0, returnDateFormat: String = "M-d-Y"): String {
    val fmt: DateTimeFormatter = DateTimeFormat.forPattern(returnDateFormat)
    val date: DateTime = DateTime.parse(initialDate)
    return date.plusDays(daysToAdd).toString(fmt)
  }

  fun hasPastDays(initialDate: DateTimeType, days: Int = 0): Boolean {
    val copy = initialDate.copy()
    copy.add(Calendar.DATE, days)
    return copy.after(DateTimeType.now())
  }

  fun setBgColor(view: View, color: Int) {
    view.setBackgroundColor(ContextCompat.getColor(view.context, color))
  }

  fun setTextColor(view: TextView, color: Int) {
    view.setTextColor(ContextCompat.getColor(view.context, color))
  }

  data class PatientAgeGender(val age: Int, val genderAbbr: Char)
  fun getPatientAgeGender(patientItem: PatientItem): PatientAgeGender {
    val age = getAgeFromDate(patientItem.dob)
    val gender = if (patientItem.gender == "male") 'M' else 'F'
    return PatientAgeGender(age, gender)
  }

  fun getLastSeen(patientId: String, lastUpdated: Date?): String {

    var lastSeenDate = lastUpdated?.makeItReadable() ?: ""

    var searchResults = listOf<Immunization>()
    runBlocking {
      searchResults =
        FhirApplication.fhirEngine(FhirApplication.getContext()).search {
          filter(Immunization.PATIENT) { value = "Patient/$patientId" }
        }
    }

    if (searchResults.isNotEmpty()) {
      lastSeenDate = searchResults[searchResults.size.minus(1)].recorded.makeItReadable()
    }

    return lastSeenDate
  }

  fun Date.makeItReadable(): String {
    return SimpleDateFormat("MM-dd-yyyy").format(this)
  }

  fun buildDatasource(appContext: Context): HapiFhirResourceDataSource {
    return HapiFhirResourceDataSource(
      HapiFhirService.create(FhirContext.forR4().newJsonParser(), appContext)
    )
  }

  fun buildResourceSyncParams(): Map<ResourceType, Map<String, String>> {
    return mapOf(
      ResourceType.Patient to emptyMap<String, String>(),
      ResourceType.Immunization to emptyMap(),
      ResourceType.Questionnaire to emptyMap()
    )
  }
}
