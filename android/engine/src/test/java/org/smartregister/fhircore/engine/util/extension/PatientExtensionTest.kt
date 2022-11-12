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

package org.smartregister.fhircore.engine.util.extension

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import java.util.Calendar
import java.util.Date
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Patient
import org.junit.Assert
import org.junit.Test
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

class PatientExtensionTest : RobolectricTest() {

  private val context = InstrumentationRegistry.getInstrumentation().context

  private fun getDateFromDaysAgo(daysAgo: Int): Date {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)
    return calendar.time
  }

  @Test
  fun testGetAgeString() {
    val expectedAge = "1y"
    Assert.assertEquals(expectedAge, calculateAge(getDateFromDaysAgo(365), context))

    val expectedAge2 = "1y 1m"
    // passing days value for 1y 1m 4d
    Assert.assertEquals(expectedAge2, calculateAge(getDateFromDaysAgo(399), context))

    val expectedAge3 = "1y 1w"
    // passing days value for 1y 1w
    Assert.assertEquals(expectedAge3, calculateAge(getDateFromDaysAgo(372), context))

    val expectedAge4 = "1m"
    Assert.assertEquals(expectedAge4, calculateAge(getDateFromDaysAgo(35), context))

    val expectedAge5 = "1m 2w"
    Assert.assertEquals(expectedAge5, calculateAge(getDateFromDaysAgo(49), context))

    val expectedAge6 = "1w"
    Assert.assertEquals(expectedAge6, calculateAge(getDateFromDaysAgo(7), context))

    val expectedAge7 = "1w 2d"
    Assert.assertEquals(expectedAge7, calculateAge(getDateFromDaysAgo(9), context))

    val expectedAge8 = "3d"
    Assert.assertEquals(expectedAge8, calculateAge(getDateFromDaysAgo(3), context))

    val expectedAge9 = "1y 2m"
    Assert.assertEquals(expectedAge9, calculateAge(getDateFromDaysAgo(450), context))

    val expectedAge10 = "40y 3m"
    Assert.assertNotEquals(expectedAge10, calculateAge(getDateFromDaysAgo(14700), context))

    val expectedAge11 = "40y"
    Assert.assertEquals(expectedAge11, calculateAge(getDateFromDaysAgo(14700), context))

    val expectedAge12 = "0d"
    // if difference b/w current date and DOB is O from extractAge extension
    Assert.assertEquals(expectedAge12, calculateAge(getDateFromDaysAgo(0), context))
  }

  @Test
  fun testExtractAge() {
    val patient =
      Patient().apply { birthDate = Calendar.getInstance().apply { add(Calendar.YEAR, -19) }.time }

    Assert.assertEquals("19y", patient.extractAge(context))
  }

  @Test
  fun testExtractGenderShouldReturnMaleStringWhenPatientGenderIsMale() {
    val patient = Patient().apply { gender = Enumerations.AdministrativeGender.MALE }

    Assert.assertEquals(
      (ApplicationProvider.getApplicationContext() as Application).getString(R.string.male),
      patient.extractGender(ApplicationProvider.getApplicationContext())
    )
  }

  @Test
  fun testExtractGenderShouldReturnFemaleStringWhenPatientGenderIsFemale() {
    val patient = Patient().apply { gender = Enumerations.AdministrativeGender.FEMALE }

    Assert.assertEquals(
      (ApplicationProvider.getApplicationContext() as Application).getString(R.string.female),
      patient.extractGender(ApplicationProvider.getApplicationContext())
    )
  }

  @Test
  fun testExtractGenderShouldReturnOtherStringWhenPatientGenderIsOther() {
    val patient = Patient().apply { gender = Enumerations.AdministrativeGender.OTHER }

    val applicationContext = (ApplicationProvider.getApplicationContext() as Application)

    Assert.assertEquals(
      applicationContext.getString(R.string.other),
      patient.extractGender(applicationContext)
    )
  }

  @Test
  fun testExtractGenderShouldReturnUnknownStringWhenPatientGenderIsUnknown() {
    val patient = Patient().apply { gender = Enumerations.AdministrativeGender.UNKNOWN }

    Assert.assertEquals(
      (ApplicationProvider.getApplicationContext() as Application).getString(R.string.unknown),
      patient.extractGender(ApplicationProvider.getApplicationContext())
    )
  }

  @Test
  fun testExtractGenderShouldReturnAnEmptyStringWhenPatientGenderIsNull() {
    val patient = Patient().apply { gender = Enumerations.AdministrativeGender.NULL }

    Assert.assertEquals("", patient.extractGender(ApplicationProvider.getApplicationContext()))
  }

  @Test
  fun testExtractAgeShouldReturnAnEmptyStringWhenPatientDoesNotHaveBirthDate() {
    val patient = Patient()

    Assert.assertEquals("", patient.extractAge(context))
  }

  @Test
  fun testExtractAgeShouldReturnCallGetAgeStringFromDaysWhenPatientHasBirthDate() {
    val calendar =
      Calendar.getInstance().apply { timeInMillis = (timeInMillis - (1L * 365 * 24 * 3600 * 1000)) }

    val patient = Patient().apply { birthDate = calendar.time }

    Assert.assertEquals("1y", patient.extractAge(context))
  }

  @Test
  fun testTranslateMaleGender() {
    val patient = Patient().apply { gender = Enumerations.AdministrativeGender.MALE }
    Assert.assertEquals(
      "Male",
      patient.gender.translateGender(ApplicationProvider.getApplicationContext())
    )
  }

  @Test
  fun testTranslateFemaleGender() {
    val patient = Patient().apply { gender = Enumerations.AdministrativeGender.FEMALE }
    Assert.assertEquals(
      "Female",
      patient.gender.translateGender(ApplicationProvider.getApplicationContext())
    )
  }

  @Test
  fun testTranslateGenderReturnsUnknownWhenValeIsNotMaleOrFemale() {
    val patient = Patient().apply { gender = Enumerations.AdministrativeGender.OTHER }
    Assert.assertEquals(
      "Unknown",
      patient.gender.translateGender(ApplicationProvider.getApplicationContext())
    )
  }
}
