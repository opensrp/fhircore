package org.smartregister.fhircore.util

import org.joda.time.DateTime
import org.joda.time.LocalDate
import org.joda.time.Years

object Utils {

    fun getAgeFromDate (dateOfBirth : String) : Int {
        val date: DateTime = DateTime.parse(dateOfBirth)
        val age: Years = Years.yearsBetween(date.toLocalDate(), LocalDate.now())
        return age.getYears()
    }
}