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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.HumanName
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.StringType
import org.junit.Assert
import org.junit.Test
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

class PatientExtensionTest : RobolectricTest() {

  @Test
  fun testExtractAddressShouldReturnFullAddress() {
    val patient =
      Patient().apply {
        addAddress().apply {
          this.addLine("12 B")
          this.addLine("Gulshan")
          this.district = "Karimabad"
          this.state = "Sindh"
        }
      }

    Assert.assertEquals("12 B, Gulshan, Karimabad Sindh", patient.extractAddress())
  }

  @Test
  fun testExtractAddressAttributes() {
    val patient =
      Patient().apply {
        addAddress().apply {
          this.addLine("12 B")
          this.addLine("Gulshan")
          this.district = "Karimabad"
          this.state = "Sindh"
          this.text = "home location Karachi"
        }
      }

    Assert.assertEquals("12 B, Gulshan, Karimabad Sindh", patient.extractAddress())
    Assert.assertEquals("Karimabad", patient.extractAddressDistrict())
    Assert.assertEquals("Sindh", patient.extractAddressState())
    Assert.assertEquals("home location Karachi", patient.extractAddressText())
  }

  @Test
  fun testExtractTelecomShouldReturnTelecom() {
    val patient =
      Patient().apply {
        addTelecom().apply { this.value = "+1234" }
        addTelecom().apply { this.value = "+5678" }
      }
    val expectedList: List<String> = listOf("+1234", "+5678")
    Assert.assertEquals(expectedList, patient.extractTelecom())
    if (!patient.hasManagingOrganization()) {
      Assert.assertEquals("", patient.extractManagingOrganizationReference())
    }
    if (!patient.hasGeneralPractitioner()) {
      Assert.assertEquals("", patient.extractGeneralPractitionerReference())
    }
  }

  @Test
  fun testExtractGeneralPractitionerShouldReturnReference() {
    val patient =
      Patient().apply { addGeneralPractitioner().apply { this.reference = "practitioner/1234" } }
    Assert.assertEquals("practitioner/1234", patient.extractGeneralPractitionerReference())
    if (!patient.hasTelecom()) {
      Assert.assertEquals(null, patient.extractTelecom())
    }
  }

  @Test
  fun testExtractManagingOrganizationShouldReturnReference() {
    val patient =
      Patient().apply { managingOrganization.apply { this.reference = "reference/1234" } }
    Assert.assertEquals("reference/1234", patient.extractManagingOrganizationReference())
  }

  @Test
  fun testIsFamilyHeadShouldReturnTrueWithTagFamily() {
    val patient = Patient().apply { meta.addTag().display = "FamiLy" }

    Assert.assertTrue(patient.isFamilyHead())
  }

  @Test
  fun testIsFamilyHeadShouldReturnTrueWithNoTagAsFamily() {
    val patient = Patient().apply { meta.addTag().display = "Pregnant" }

    Assert.assertFalse(patient.isFamilyHead())
  }

  @Test
  fun testHasActivePregnancyShouldReturnTrueWithActivePregnancyCondition() {
    val conditions =
      listOf(
        Condition().apply {
          this.clinicalStatus.addCoding().code = "L123"
          this.clinicalStatus.addCoding().code = "active"
          this.code.addCoding().display = "OCD"
        },
        Condition().apply {
          this.clinicalStatus.addCoding().code = "L123"
          this.clinicalStatus.addCoding().code = "active"
          this.code.addCoding().display = "preGnant"
        }
      )

    Assert.assertTrue(conditions.hasActivePregnancy())
  }

  @Test
  fun testHasActivePregnancyShouldReturnFalseWithNoActivePregnancyCondition() {
    val conditions =
      listOf(
        Condition().apply {
          this.clinicalStatus.addCoding().code = "L123"
          this.clinicalStatus.addCoding().code = "active"
          this.code.addCoding().display = "OCD"
        },
        Condition().apply {
          this.clinicalStatus.addCoding().code = "L123"
          this.clinicalStatus.addCoding().code = "inactive"
          this.code.addCoding().display = "preGnant"
        }
      )

    Assert.assertFalse(conditions.hasActivePregnancy())
  }

  @Test
  fun testPregnancyConditionShouldReturnPregnancyCondition() {
    val conditions =
      listOf(
        Condition().apply {
          this.clinicalStatus.addCoding().code = "L123"
          this.clinicalStatus.addCoding().code = "active"
          this.code.addCoding().display = "OCD"
        },
        Condition().apply {
          this.clinicalStatus.addCoding().code = "L123"
          this.clinicalStatus.addCoding().code = "active"
          this.code.addCoding().display = "pregnant"
        }
      )

    Assert.assertNotNull(conditions.pregnancyCondition())
  }

  @Test
  fun testPregnancyConditionShouldReturnCondition() {
    val conditions =
      listOf(
        Condition().apply {
          this.clinicalStatus.addCoding().code = "L123"
          this.clinicalStatus.addCoding().code = "active"
          this.code.addCoding().display = "OCD"
        }
      )

    Assert.assertNotNull(conditions.pregnancyCondition())
  }

  private fun getDateFromDaysAgo(daysAgo: Int): Date {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)
    return calendar.time
  }

  @Test
  fun testGetAgeString() {
    val expectedAge = "1y"
    Assert.assertEquals(expectedAge, getAgeStringFromDays(getDateFromDaysAgo(365)))

    val expectedAge2 = "1y 1m"
    // passing days value for 1y 1m 4d
    Assert.assertEquals(expectedAge2, getAgeStringFromDays(getDateFromDaysAgo(399)))

    val expectedAge3 = "1y 1w"
    // passing days value for 1y 1w
    Assert.assertEquals(expectedAge3, getAgeStringFromDays(getDateFromDaysAgo(372)))

    val expectedAge4 = "1m"
    Assert.assertEquals(expectedAge4, getAgeStringFromDays(getDateFromDaysAgo(35)))

    val expectedAge5 = "1m 2w"
    Assert.assertEquals(expectedAge5, getAgeStringFromDays(getDateFromDaysAgo(49)))

    val expectedAge6 = "1w"
    Assert.assertEquals(expectedAge6, getAgeStringFromDays(getDateFromDaysAgo(7)))

    val expectedAge7 = "1w 2d"
    Assert.assertEquals(expectedAge7, getAgeStringFromDays(getDateFromDaysAgo(9)))

    val expectedAge8 = "3d"
    Assert.assertEquals(expectedAge8, getAgeStringFromDays(getDateFromDaysAgo(3)))

    val expectedAge9 = "1y 2m"
    Assert.assertEquals(expectedAge9, getAgeStringFromDays(getDateFromDaysAgo(450)))

    val expectedAge10 = "40y 3m"
    Assert.assertNotEquals(expectedAge10, getAgeStringFromDays(getDateFromDaysAgo(14700)))

    val expectedAge11 = "40y"
    Assert.assertEquals(expectedAge11, getAgeStringFromDays(getDateFromDaysAgo(14700)))

    val expectedAge12 = "0d"
    // if difference b/w current date and DOB is O from extractAge extension
    Assert.assertEquals(expectedAge12, getAgeStringFromDays(getDateFromDaysAgo(0)))
  }

  @Test
  fun testExtractAge() {
    val patient =
      Patient().apply { birthDate = Calendar.getInstance().apply { add(Calendar.YEAR, -19) }.time }

    Assert.assertEquals("19y", patient.extractAge())
  }

  @Test
  fun testExtractFamilyName() {
    val patient =
      Patient().apply {
        addName().apply {
          addGiven("Given Name")
          family = "genealogy"
        }
      }

    Assert.assertEquals("Genealogy Family", patient.extractFamilyName())
  }

  @Test
  fun testExtractFamilyNameShouldReturnEmptyStringWhenFamilyNameIsEmptyAndGivenNameIsProvided() {
    val patient = Patient().apply { addName().apply { addGiven("Given Name") } }

    Assert.assertEquals("", patient.extractFamilyName())
  }

  @Test
  fun testExtractFamilyNameShouldReturnEmptyStringWhenFamilyNameAndGivenNameAreEmpty() {
    val patient = Patient()

    Assert.assertEquals("", patient.extractFamilyName())
  }

  @Test
  fun testExtractFamilyNameShouldReturnEmptyStringWhenFamilyNameAndGivenNameAreEmpty2() {
    val patient = Patient().apply { name = listOf() }

    Assert.assertEquals("", patient.extractFamilyName())
  }

  @Test
  fun testExtractNameShouldReturnNameWithFamilyNameOnly() {
    val patient = Patient().apply { name = listOf(HumanName().apply { family = "Doe" }) }

    Assert.assertEquals("Doe", patient.extractName())
  }

  @Test
  fun testExtractNameShouldReturnNameWithGivenNameOnly() {
    val patient =
      Patient().apply { name = listOf(HumanName().apply { given = listOf(StringType("John")) }) }

    Assert.assertEquals("John", patient.extractName())
  }

  @Test
  fun testExtractNameShouldReturnNameWhenGivenSingleGivenName() {
    val patient =
      Patient().apply {
        name =
          listOf(
            HumanName().apply {
              family = "Doe"
              given = listOf(StringType("John"))
            }
          )
      }

    Assert.assertEquals("John Doe", patient.extractName())
  }

  @Test
  fun testExtractNameShouldReturnNameWhenGivenMultipleGivenNames() {
    val patient =
      Patient().apply {
        name =
          listOf(
            HumanName().apply {
              family = "Doe"
              given = listOf(StringType("John"), StringType("Tom"))
            }
          )
      }

    Assert.assertEquals("John Tom Doe", patient.extractName())
  }

  @Test
  fun testExtractNameShouldReturnEmptyNameWhenPatientNameIsNull() {
    val patient = Patient()

    Assert.assertEquals("", patient.extractName())
  }

  @Test
  fun testTitleCaseShouldConvertStringInLowerCase() {
    val patient =
      Patient().apply { name = listOf(HumanName().apply { given = listOf(StringType("john")) }) }

    Assert.assertEquals("John", patient.extractName())
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

    Assert.assertEquals("", patient.extractAge())
  }

  @Test
  fun testExtractAgeShouldReturnCallGetAgeStringFromDaysWhenPatientHasBirthDate() {
    val calendar =
      Calendar.getInstance().apply { timeInMillis = (timeInMillis - (1L * 365 * 24 * 3600 * 1000)) }

    val patient = Patient().apply { birthDate = calendar.time }

    Assert.assertEquals("1y", patient.extractAge())
  }

  @Test
  fun testAtRiskShouldReturnEmptyStringWhenRiskExtensionDoesNotExist() {
    Assert.assertEquals("", Patient().atRisk())
  }

  @Test
  fun testAtRiskShouldReturnRiskValueWhenPatientHasRiskExtension() {
    val patient =
      Patient().apply { addExtension(Extension("covid-risk", StringType("covid-risk"))) }
    Assert.assertEquals("covid-risk", patient.atRisk())
  }

  @Test
  fun testGetLastSeen() {
    val calendar1YearAgo =
      Calendar.getInstance().apply { timeInMillis = (timeInMillis - (1L * 365 * 24 * 3600 * 1000)) }
    val timeNow = Calendar.getInstance().time
    val immunizations =
      listOf(
        Immunization().apply {
          occurrence = DateTimeType(timeNow)
          protocolAppliedFirstRep.doseNumberPositiveIntType.value = 2
        },
        Immunization().apply {
          occurrence = DateTimeType(calendar1YearAgo.time)
          protocolAppliedFirstRep.doseNumberPositiveIntType.value = 1
        }
      )

    Assert.assertEquals(DateTimeType(timeNow).toDisplay(), Patient().getLastSeen(immunizations))
  }

  @Test
  fun testFormatLastSeen() {
    val date: Date = SimpleDateFormat("dd/MM/yyyy").parse("31/12/2021")

    Assert.assertEquals("12-31-2021", date.lastSeenFormat())
  }

  @Test
  fun testExtractDeathDate() {
    val date: Date = SimpleDateFormat("dd/MM/yyyy").parse("31/12/2021")
    val patient = Patient().apply { deceased = DateTimeType(date) }

    Assert.assertEquals(date, patient.extractDeathDate())
  }

  @Test
  fun testExtractOfficialIdentifierReturnsValuewhenUseIsOfficial() {
    val patient =
      Patient().apply {
        identifier =
          mutableListOf(
            Identifier().apply {
              use = Identifier.IdentifierUse.OFFICIAL
              value = "immunization"
            }
          )
      }
    Assert.assertEquals("immunization", patient.extractOfficialIdentifier())
  }

  @Test
  fun testExtractOfficialIdentifierReturnsNullwhenUseIsNotOfficial() {
    val patient =
      Patient().apply {
        identifier =
          mutableListOf(
            Identifier().apply {
              use = Identifier.IdentifierUse.USUAL
              value = "immunization"
            }
          )
      }
    Assert.assertNull(patient.extractOfficialIdentifier())
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
