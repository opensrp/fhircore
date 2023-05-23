package org.smartregister.fhircore.engine.rulesengine.services

import org.apache.commons.lang3.NotImplementedException
import org.joda.time.LocalDate
import org.smartregister.fhircore.engine.util.extension.SDF_YYYY_MM_DD
import org.smartregister.fhircore.engine.util.extension.formatDate

object DateService {
  fun addOrSubtractYearFromCurrentDate(
      years: Int,
      operation: String,
      dateFormat: String = SDF_YYYY_MM_DD,
  ): String {
    return when (operation) {
      "-" -> LocalDate.now().minusYears(years).toDate().formatDate(dateFormat)
      "+" -> LocalDate.now().plusYears(years).toDate().formatDate(dateFormat)
      else ->
          throw NotImplementedException(
              "Operation not supported. Operations supported operation are '+' or '-'")
    }
  }
}
