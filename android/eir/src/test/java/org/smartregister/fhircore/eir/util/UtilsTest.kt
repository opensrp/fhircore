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
import android.content.Intent
import android.os.Build
import android.view.MotionEvent
import android.widget.EditText
import com.google.android.fhir.search.Search
import com.google.android.fhir.search.StringFilter
import com.google.android.fhir.search.StringFilterModifier
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.slot
import io.mockk.verify
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.time.DateUtils
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.PositiveIntType
import org.hl7.fhir.r4.model.ResourceType
import org.joda.time.DateTime
import org.junit.Assert
import org.junit.Test
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter.from
import org.smartregister.fhircore.eir.EirApplication
import org.smartregister.fhircore.eir.RobolectricTest
import org.smartregister.fhircore.eir.shadow.FhirApplicationShadow
import org.smartregister.fhircore.eir.util.Utils.makeItReadable
import org.smartregister.fhircore.eir.util.Utils.ordinalOf

@Config(shadows = [FhirApplicationShadow::class])
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
    Assert.assertEquals(StringFilterModifier.CONTAINS, filterList[0].modifier)
    Assert.assertEquals("NAIROBI", filterList[0].value)
  }

  @Test
  fun testSetAppLocaleShouldReturnUpdatedConfiguration() {

    val swConfig = Utils.setAppLocale(EirApplication.getContext(), "sw")

    Assert.assertNotNull(swConfig)
    Assert.assertEquals("sw", swConfig!!.locales[0].language)

    ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", 23)
    val enConfig = Utils.setAppLocale(EirApplication.getContext(), "en")

    Assert.assertNotNull(enConfig)
    Assert.assertEquals("en", enConfig!!.locales[0].language)

    val config = Utils.setAppLocale(EirApplication.getContext(), null)
    Assert.assertEquals("en", enConfig!!.locales[0].language)
  }

  @Test
  fun testisDrawableClickedShouldVerifyScenarios() {

    val motionEvent = mockk<MotionEvent>()
    every { motionEvent.rawX } returns 0f

    val view =
      mockk<EditText> {
        every { compoundDrawables } returns
          arrayOf(
            mockk { every { bounds } returns mockk { every { width() } returns 0 } },
            mockk(),
            mockk { every { bounds } returns mockk { every { width() } returns 0 } },
            mockk()
          )
        every { right } returns 0
      }

    val instance = ReflectionHelpers.getStaticField<Utils>(Utils::class.java, "INSTANCE")
    Assert.assertTrue(
      ReflectionHelpers.callInstanceMethod(
        instance,
        "isDrawableClicked",
        from(Utils.DrawablePosition::class.java, Utils.DrawablePosition.DRAWABLE_RIGHT),
        from(MotionEvent::class.java, motionEvent),
        from(EditText::class.java, view)
      )
    )
    Assert.assertTrue(
      ReflectionHelpers.callInstanceMethod(
        instance,
        "isDrawableClicked",
        from(Utils.DrawablePosition::class.java, Utils.DrawablePosition.DRAWABLE_LEFT),
        from(MotionEvent::class.java, motionEvent),
        from(EditText::class.java, view)
      )
    )
    Assert.assertFalse(
      ReflectionHelpers.callInstanceMethod(
        instance,
        "isDrawableClicked",
        from(Utils.DrawablePosition::class.java, Utils.DrawablePosition.DRAWABLE_TOP),
        from(MotionEvent::class.java, motionEvent),
        from(EditText::class.java, view)
      )
    )
  }

  @Test
  fun testGetLastSeenShouldReturnExpectedDate() {

    val patientId = "0"
    val patientRegisteredDate = Calendar.getInstance().apply { add(Calendar.DATE, -50) }.time
    runBlocking {
      Assert.assertEquals(
        patientRegisteredDate.makeItReadable(),
        Utils.getLastSeen(patientId, patientRegisteredDate)
      )
    }
    val immunization =
      Immunization().apply {
        id = "Patient/0"
        occurrence = DateTimeType("2021-07-30")
        protocolApplied =
          listOf(Immunization.ImmunizationProtocolAppliedComponent(PositiveIntType(1)))
      }

    runBlocking {
      EirApplication.fhirEngine(EirApplication.getContext()).save(immunization)
      Assert.assertEquals(
        immunization.occurrenceDateTimeType.toHumanDisplay(),
        Utils.getLastSeen(patientId, Date())
      )
    }

    immunization.occurrence = DateTimeType("2021-07-30")
    runBlocking {
      EirApplication.fhirEngine(EirApplication.getContext()).save(immunization)
      Assert.assertEquals(
        immunization.occurrenceDateTimeType.toHumanDisplay(),
        Utils.getLastSeen(patientId, Date())
      )
      EirApplication.fhirEngine(EirApplication.getContext())
        .remove(Immunization::class.java, patientId)
      EirApplication.fhirEngine(EirApplication.getContext())
        .remove(Immunization::class.java, patientId)
    }
  }

  @Test
  fun testIntOrdinalConversion() {
    Assert.assertEquals("1st", 1.ordinalOf())
    Assert.assertEquals("2nd", 2.ordinalOf())
    Assert.assertEquals("3rd", 3.ordinalOf())
    Assert.assertEquals("4th", 4.ordinalOf())
    Assert.assertEquals("13th", 13.ordinalOf())
    Assert.assertEquals("22nd", 22.ordinalOf())
    Assert.assertEquals("23rd", 23.ordinalOf())
    Assert.assertEquals("11th", 11.ordinalOf())
    Assert.assertEquals("20th", 20.ordinalOf())
  }
}
