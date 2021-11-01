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

import org.junit.Assert
import org.junit.Test
import org.smartregister.fhircore.engine.util.DateUtils.getDate
import org.smartregister.fhircore.engine.util.DateUtils.makeItReadable

/** Created by Ephraim Kigamba - nek.eam@gmail.com on 24-09-2021. */
class DateUtilsTest {

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
  fun testDateToStringFunction() {
    Assert.assertEquals("2020-03-10".getDate("yyyy-MM-dd").makeItReadable(), "10-Mar-2020")
  }
}
