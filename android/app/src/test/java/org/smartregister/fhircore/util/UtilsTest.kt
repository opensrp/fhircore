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

import java.text.SimpleDateFormat
import java.util.Date
import org.apache.commons.lang3.time.DateUtils
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Immunization
import org.joda.time.DateTime
import org.junit.Assert
import org.junit.Test
import org.smartregister.fhircore.fragment.PatientListFragment
import org.smartregister.fhircore.util.Utils.isOverdue

class UtilsTest {

  @Test
  fun `getAgeFromDate calculates correct age when current date is not null`() {
    Assert.assertEquals(
      1,
      Utils.getAgeFromDate("2020-01-01", DateTime.parse("2021-01-01").toLocalDate())
    )
  }

  @Test
  fun `getAgeFromDate calculates correct age when current date is NULL`() {

    val date: Date = DateUtils.addYears(Date(), -4)
    val sdf = SimpleDateFormat("yyyy-MM-dd")
    val fourYearsAgo = sdf.format(date)

    Assert.assertEquals(4, Utils.getAgeFromDate(fourYearsAgo, null))
  }

  @Test
  fun testVerifyImmunizationOverdueStatus() {
    val immunization = getImmunization()
    Assert.assertTrue(immunization.isOverdue(PatientListFragment.SECOND_DOSE_OVERDUE_DAYS))
  }

  @Test
  fun testAddDaysShouldReturnValid() {
    val immunization = getImmunization()
    val vaccineDate = immunization.occurrenceDateTimeType.toHumanDisplay()
    val nextVaccineDate = Utils.addDays(vaccineDate, 28)
    Assert.assertEquals("1-29-2021", nextVaccineDate)
  }

  private fun getImmunization(): Immunization {
    val date = SimpleDateFormat("MM-dd-yyyy").parse("01-01-2021")

    return Immunization().apply {
      recorded = date
      occurrence = DateTimeType.today().setValue(date)
    }
  }
}
