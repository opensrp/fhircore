/*
 * Copyright 2021 Ona Systems Inc
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

import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import com.google.android.fhir.search.search
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Immunization
import org.joda.time.DateTime
import org.joda.time.LocalDate
import org.joda.time.ReadablePartial
import org.joda.time.Years
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import org.smartregister.fhircore.FhirApplication

object Utils {

  fun getAgeFromDate(dateOfBirth: String, currentDate: ReadablePartial? = null): Int {
    val date: DateTime = DateTime.parse(dateOfBirth)
    val age: Years = Years.yearsBetween(date.toLocalDate(), currentDate ?: LocalDate.now())
    return age.getYears()
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

  fun Immunization.isOverdue(daysToAdd: Int): Boolean {
    val cal: Calendar = Calendar.getInstance().apply { add(Calendar.DATE, daysToAdd) }
    return recorded.before(cal.time)
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
}
