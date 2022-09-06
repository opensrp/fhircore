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

package org.smartregister.fhircore.engine.util

import org.hl7.fhir.r4.model.DateTimeType
import org.junit.Assert
import org.junit.Test
import org.smartregister.fhircore.engine.util.DateUtils.getDate
import org.smartregister.fhircore.engine.util.DateUtils.isToday
import org.smartregister.fhircore.engine.util.extension.makeItReadable

class DateUtilsTest {

  @Test
  fun `isToday returns true for today`() {
    Assert.assertTrue(DateUtils.today().isToday())
  }

  @Test
  fun `isToday returns false for yesterday`() {
    Assert.assertFalse(DateUtils.yesterday().isToday())
  }

  @Test
  fun `addDays() should return correct date when given initialDate and daysToAdd`() {
    val finalDate = DateUtils.addDays("2020-04-05", 12)

    Assert.assertEquals("4-17-2020", finalDate)
  }

  @Test
  fun `addDays() should return correct date when given initialDate, daysToAdd and pattern`() {
    val finalDate =
      DateUtils.addDays("2020-03-10 01:23:00 AM", 12, dateTimeFormat = "yyyy-MM-dd h:mm:ss a")

    Assert.assertEquals("3-22-2020", finalDate)
  }

  @Test
  fun `addDays() should return same date when given initialDate, has default 0 daysToAdd and pattern`() {
    val finalDate =
      DateUtils.addDays("2020-03-10 01:23:00 AM", dateTimeFormat = "yyyy-MM-dd h:mm:ss a")
    Assert.assertEquals("3-10-2020", finalDate)
  }

  @Test
  fun `hasPastDays() should return true when given valid initialDate is 3 days in the past`() {
    val initialDate = DateTimeType("2022-01-11T15:30:10.222")
    val hasPastDays = DateUtils.hasPastDays(initialDate, -3)
    Assert.assertTrue(hasPastDays)
  }

  @Test
  fun `hasPastDays() should return true when given valid initialDate and has default 0 days`() {
    val initialDate = DateTimeType("2022-01-11T15:30:10.222")
    val hasPastDays = DateUtils.hasPastDays(initialDate)
    Assert.assertTrue(hasPastDays)
  }

  @Test
  fun `String#getDate() should return correct date when given string format expected`() {
    Assert.assertEquals("10-Mar-2020", "2020-03-10".getDate("yyyy-MM-dd").makeItReadable())
  }
}
