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
import android.content.Intent
import ca.uhn.fhir.rest.param.ParamPrefixEnum
import com.google.android.fhir.search.Search
import com.google.android.fhir.search.StringFilter
import io.mockk.mockkClass
import io.mockk.slot
import io.mockk.verify
import java.text.SimpleDateFormat
import java.util.Date
import org.apache.commons.lang3.time.DateUtils
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.ResourceType
import org.joda.time.DateTime
import org.junit.Assert
import org.junit.Test
import org.smartregister.fhircore.FhirApplication
import org.smartregister.fhircore.RobolectricTest

class UtilsTest : RobolectricTest() {

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
  fun `test refreshActivity invokes startActivity with intent param`() {

    val activity: Activity = mockkClass(type = Activity::class, relaxed = true)

    val slot = slot<Intent>()

    Utils.refreshActivity(activity)

    verify { activity.startActivity(capture(slot)) }

    Assert.assertNotNull(slot.captured)
  }

  @Test
  fun testAddBasePatientFilterShouldVerifyFilterParams() {
    val search = Search(ResourceType.Patient)
    Utils.addBasePatientFilter(search)

    val field = search.javaClass.getDeclaredField("stringFilters")
    field.isAccessible = true
    val filterList = field.get(search) as MutableList<StringFilter>

    Assert.assertEquals(1, filterList.size)
    Assert.assertEquals(Patient.ADDRESS_CITY, filterList[0].parameter)
    /*Assert.assertEquals(ParamPrefixEnum.EQUAL, filterList[0].prefix)
    Assert.assertEquals("NAIROBI", filterList[0].value)*/
  }

  @Test
  fun testSetAppLocaleShouldReturnUpdatedConfiguration() {
    val config = Utils.setAppLocale(FhirApplication.getContext(), "sw")

    Assert.assertNotNull(config)
    Assert.assertEquals("sw", config!!.locales[0].language)
  }
}
