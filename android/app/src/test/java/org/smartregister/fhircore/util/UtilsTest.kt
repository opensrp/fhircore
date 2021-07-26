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
import java.util.Date
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.time.DateUtils
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.ResourceType
import org.joda.time.DateTime
import org.junit.Assert
import org.junit.Test
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter.from
import org.smartregister.fhircore.FhirApplication
import org.smartregister.fhircore.RobolectricTest
import org.smartregister.fhircore.shadow.FhirApplicationShadow
import org.smartregister.fhircore.util.Utils.makeItReadable

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

    val swConfig = Utils.setAppLocale(FhirApplication.getContext(), "sw")

    Assert.assertNotNull(swConfig)
    Assert.assertEquals("sw", swConfig!!.locales[0].language)

    ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", 23)
    val enConfig = Utils.setAppLocale(FhirApplication.getContext(), "en")

    Assert.assertNotNull(enConfig)
    Assert.assertEquals("en", enConfig!!.locales[0].language)

    val config = Utils.setAppLocale(FhirApplication.getContext(), null)
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
    val immunization =
      Immunization().apply {
        id = "Patient/0"
        recorded = Date()
      }

    runBlocking { FhirApplication.fhirEngine(FhirApplication.getContext()).save(immunization) }

    Assert.assertEquals(Date().makeItReadable(), Utils.getLastSeen("0", Date()))

    runBlocking {
      FhirApplication.fhirEngine(FhirApplication.getContext()).remove(Immunization::class.java, "0")
    }
  }
}
