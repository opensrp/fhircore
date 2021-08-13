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

package org.smartregister.fhircore.eir.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import ca.uhn.fhir.context.FhirContext
import com.google.android.fhir.search.Search
import com.google.android.fhir.search.StringFilterModifier
import com.google.android.fhir.search.search
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
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
import org.smartregister.fhircore.eir.EirApplication
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceService
import timber.log.Timber

object Utils {

  val gson = Gson()

  private var simpleDateFormat = SimpleDateFormat("MM-dd-yyyy", Locale.getDefault())

  fun getAgeFromDate(dateOfBirth: String, currentDate: ReadablePartial? = null): Int {
    val date: DateTime = DateTime.parse(dateOfBirth)
    val age: Years = Years.yearsBetween(date.toLocalDate(), currentDate ?: LocalDate.now())
    return age.years
  }

  /** Load dynamic config for given key. This would be loaded from FHIR DB later todo */
  fun <T> loadConfig(config: String, configClass: Class<T>, context: Context): T {
    return gson.fromJson(
      context.assets.open(config).bufferedReader().use { it.readText() },
      configClass
    )
  }

  fun addBasePatientFilter(search: Search) {
    search.filter(Patient.ADDRESS_CITY) {
      modifier = StringFilterModifier.CONTAINS
      value = "NAIROBI"
    }
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

  suspend fun getLastSeen(patientId: String, lastUpdated: Date?): String {

    val searchResults: List<Immunization> =
      EirApplication.fhirEngine(EirApplication.getContext()).search {
        filter(Immunization.PATIENT) { value = "Patient/$patientId" }
      }

    return searchResults
      .maxByOrNull { it.protocolApplied.first().doseNumberPositiveIntType.value }
      ?.occurrenceDateTimeType
      ?.toHumanDisplay()
      ?: lastUpdated?.makeItReadable() ?: ""
  }

  fun Date.makeItReadable(): String = simpleDateFormat.format(this)

  fun Int.ordinalOf() =
    "$this" +
      if (this % 100 in 11..13) "th"
      else
        when (this % 10) {
          1 -> "st"
          2 -> "nd"
          3 -> "rd"
          else -> "th"
        }

  fun buildDatasource(
    appContext: Context,
    applicationConfiguration: ApplicationConfiguration
  ): FhirResourceDataSource {
    return FhirResourceDataSource(
      FhirResourceService.create(
        FhirContext.forR4().newJsonParser(),
        appContext,
        applicationConfiguration
      )
    )
  }

  fun buildResourceSyncParams(): Map<ResourceType, Map<String, String>> {
    return mapOf(
      ResourceType.Patient to emptyMap(),
      ResourceType.Immunization to emptyMap(),
      ResourceType.Questionnaire to emptyMap(),
      ResourceType.StructureMap to mapOf(),
      ResourceType.RelatedPerson to mapOf()
    )
  }
}
