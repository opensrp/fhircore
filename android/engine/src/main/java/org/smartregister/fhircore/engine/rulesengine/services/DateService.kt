/*
 * Copyright 2021-2023 Ona Systems, Inc
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

package org.smartregister.fhircore.engine.rulesengine.services

import org.apache.commons.lang3.NotImplementedException
import org.joda.time.LocalDate
import org.smartregister.fhircore.engine.util.extension.SDF_YYYY_MM_DD
import org.smartregister.fhircore.engine.util.extension.formatDate

object DateService {
  @JvmOverloads
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
          "Operation not supported. Operations supported operation are '+' or '-'",
        )
    }
  }

  @JvmOverloads
  fun addOrSubtractTimeUnitFromCurrentDate(
    timeUnitCount: Int,
    operation: String,
    timeUnit: String = TimeUnit.YEAR.name,
    dateFormat: String = SDF_YYYY_MM_DD,
  ): String {
    when (TimeUnit.valueOf(timeUnit)) {
      TimeUnit.DAY ->
        return when (operation) {
          "-" -> LocalDate.now().minusDays(timeUnitCount).toDate().formatDate(dateFormat)
          "+" -> LocalDate.now().plusDays(timeUnitCount).toDate().formatDate(dateFormat)
          else ->
            throw NotImplementedException(
              "Operation not supported. Operations supported operation are '+' or '-'"
            )
        }
      TimeUnit.WEEK ->
        return when (operation) {
          "-" -> LocalDate.now().minusWeeks(timeUnitCount).toDate().formatDate(dateFormat)
          "+" -> LocalDate.now().plusWeeks(timeUnitCount).toDate().formatDate(dateFormat)
          else ->
            throw NotImplementedException(
              "Operation not supported. Operations supported operation are '+' or '-'"
            )
        }
      TimeUnit.MONTH ->
        return when (operation) {
          "-" -> LocalDate.now().minusMonths(timeUnitCount).toDate().formatDate(dateFormat)
          "+" -> LocalDate.now().plusMonths(timeUnitCount).toDate().formatDate(dateFormat)
          else ->
            throw NotImplementedException(
              "Operation not supported. Operations supported operation are '+' or '-'"
            )
        }
      TimeUnit.YEAR ->
        return when (operation) {
          "-" -> LocalDate.now().minusYears(timeUnitCount).toDate().formatDate(dateFormat)
          "+" -> LocalDate.now().plusYears(timeUnitCount).toDate().formatDate(dateFormat)
          else ->
            throw NotImplementedException(
              "Operation not supported. Operations supported operation are '+' or '-'"
            )
        }
    }
  }

  enum class TimeUnit {
    DAY,
    WEEK,
    MONTH,
    YEAR
  }
}
