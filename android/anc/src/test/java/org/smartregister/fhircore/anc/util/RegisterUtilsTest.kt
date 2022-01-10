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

package org.smartregister.fhircore.anc.util

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.IParser
import com.google.android.fhir.datacapture.common.datatype.asStringValue
import java.util.Date
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.EpisodeOfCare
import org.hl7.fhir.r4.model.Flag
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.utils.StructureMapUtilities
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test
import org.smartregister.fhircore.anc.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.extension.makeItReadable
import org.smartregister.fhircore.engine.util.extension.setPropertySafely

class RegisterUtilsTest : RobolectricTest() {

  @Test
  fun testLoadRegisterConfig() {
    val config =
      ApplicationProvider.getApplicationContext<Application>()
        .loadRegisterConfig(RegisterType.ANC_REGISTER_ID)

    assertEquals(Enumerations.SearchParamType.TOKEN, config.primaryFilter!!.filterType)
    assertEquals(Enumerations.DataType.CODEABLECONCEPT, config.primaryFilter!!.valueType)
    assertEquals("code", config.primaryFilter!!.key)
    assertEquals("LA15173-0", config.primaryFilter!!.valueCoding!!.code)

    assertEquals(Enumerations.SearchParamType.TOKEN, config.secondaryFilter!!.filterType)
    assertEquals(Enumerations.DataType.CODEABLECONCEPT, config.secondaryFilter!!.valueType)
    assertEquals("clinical-status", config.secondaryFilter!!.key)
    assertEquals("active", config.secondaryFilter!!.valueCoding!!.code)
  }

  @Test
  fun testFamilyRegistrationStructureMapExtraction() {
    val structureMap = "questionnaires/family-registration/structure-map.txt".readFile()
    val response = "questionnaires/family-registration/questionnaire-response.json".readFile()
    val scu = buildStructureMapUtils()

    val targetResource = transform(scu, structureMap, response, "PatientRegistration")

    assertEquals(2, targetResource.entry.size)

    val patient = targetResource.entry[0].resource as Patient

    assertEquals("Arazi", patient.nameFirstRep.given.first().value)
    assertEquals("Rahi", patient.nameFirstRep.family)
    assertEquals("06-Jan-1994", patient.birthDate.makeItReadable())
    assertEquals("45 y", patient.addressFirstRep.line.first().value)
    assertEquals("4578", patient.identifierFirstRep.value)
    assertEquals(Enumerations.AdministrativeGender.FEMALE, patient.gender)
    assertEquals(true, patient.active)
    assertEquals("35359004", patient.meta.tagFirstRep.code)
    assertEquals("Family", patient.meta.tagFirstRep.display)
    assertEquals("http://hl7.org/fhir/StructureDefinition/flag-detail", patient.extension[0].url)
    assertEquals("Family", patient.extension[0].value.asStringValue())

    val flag = targetResource.entry[1].resource as Flag
    assertEquals(Flag.FlagStatus.ACTIVE, flag.status)
    assertEquals(Date().makeItReadable(), flag.period.start.makeItReadable())
    assertEquals("Patient/${patient.id}", flag.subject.reference)
    assertEquals("35359004", flag.code.codingFirstRep.code)
    assertEquals("Family", flag.code.codingFirstRep.display)
  }

  @Test
  fun testFamilyMemberRegistrationStructureMapExtraction() {
    val structureMap = "questionnaires/family-member-registration/structure-map.txt".readFile()
    val response =
      "questionnaires/family-member-registration/questionnaire-response.json".readFile()
    val scu = buildStructureMapUtils()

    val targetResource = transform(scu, structureMap, response, "PatientRegistration")

    assertEquals(1, targetResource.entry.size)

    val patient = targetResource.entry[0].resource as Patient

    assertEquals("Reem", patient.nameFirstRep.given.first().value)
    assertEquals("Hayat", patient.nameFirstRep.family)
    assertEquals("02-Nov-2021", patient.birthDate.makeItReadable())
    assertEquals(Enumerations.AdministrativeGender.FEMALE, patient.gender)
    assertEquals(true, patient.active)
    assertEquals(0, patient.meta.tag.size)
    assertEquals(0, patient.extension.size)
  }

  @Test
  fun testAncServiceEnrollmentStructureMapExtraction() {
    val structureMap = "questionnaires/anc-service-enrollment/structure-map.txt".readFile()
    val response = "questionnaires/anc-service-enrollment/questionnaire-response.json".readFile()
    val scu = buildStructureMapUtils()

    val targetResource = transform(scu, structureMap, response, "ANCServiceEnrollment")

    assertEquals(5, targetResource.entry.size)

    val condition = targetResource.entry[0].resource as Condition
    val sampleCondition = "questionnaires/anc-service-enrollment/sample/condition.json".parseSampleResource() as Condition

    assertResourceContent(sampleCondition, condition)

    val episode = targetResource.entry[0].resource as EpisodeOfCare
    val sampleEpisode = "questionnaires/anc-service-enrollment/sample/episode.json".parseSampleResource() as EpisodeOfCare

    assertResourceContent(sampleEpisode, episode)

    val encounter = targetResource.entry[0].resource as Encounter
    val sampleEncounter = "questionnaires/anc-service-enrollment/sample/encounter.json".parseSampleResource() as Encounter

    assertResourceContent(sampleEncounter, encounter)

    assertEquals(Encounter.EncounterStatus.INPROGRESS, encounter.status)
    assertEquals("HH", encounter.class_.code)
    assertEquals("home health", encounter.class_.display)
    assertEquals("Antenatal", encounter.serviceType.text)
    assertEquals("249", encounter.serviceType.codingFirstRep.code)
    assertEquals("Antenatal care contact", encounter.typeFirstRep.text)
    assertEquals("anc-contact", encounter.typeFirstRep.codingFirstRep.code)
    assertEquals(Date().makeItReadable(), encounter.period.start.makeItReadable())
    assertEquals("Patient/1234", encounter.subject.reference)

    val lmp = targetResource.entry[1].resource as Observation

  //  validateObsBasic(lmp, encounter)

    assertEquals("LMP", lmp.code.text)
    assertEquals("21840007", lmp.code.codingFirstRep.code)
    assertEquals("Date of last menstrual period", lmp.code.codingFirstRep.display)
    assertEquals("02-Jul-2021", lmp.valueDateTimeType.value.makeItReadable())

    val edd = targetResource.entry[2].resource as Observation

   // validateObsBasic(edd, encounter)

    assertEquals("EDD", edd.code.text)
    assertEquals("161714006", edd.code.codingFirstRep.code)
    assertEquals("Estimated date of delivery", edd.code.codingFirstRep.display)
    assertEquals("02-Dec-2021", edd.valueDateTimeType.value.makeItReadable())

    val gravida = targetResource.entry[3].resource as Observation

   // validateObsBasic(gravida, encounter)

    assertEquals("Gravida", gravida.code.text)
    assertEquals("246211005", gravida.code.codingFirstRep.code)
    assertEquals("Number of previous pregnancies", gravida.code.codingFirstRep.display)
    assertEquals(5, gravida.valueIntegerType.value)

    val liveDelivery = targetResource.entry[4].resource as Observation

  //  validateObsBasic(liveDelivery, encounter)

    assertEquals("Live Deliveries", liveDelivery.code.text)
    assertEquals("248991006", liveDelivery.code.codingFirstRep.code)
    assertEquals("Number of live deliveries", liveDelivery.code.codingFirstRep.display)
    assertEquals(4, liveDelivery.valueIntegerType.value)
  }

  fun assertResourceContent(expected: Resource, actual: Resource){
    // replace properties generating dynamically
    actual.setPropertySafely("id", expected.idElement)

    if (expected.resourceType == ResourceType.Observation)
      actual.setPropertySafely("encounter", expected.getNamedProperty("encounter").values[0])

    val expectedStr = expected.convertToString(true)
    val actualStr = actual.convertToString(true)

    System.out.println(expectedStr)
    System.out.println(actualStr)

    assertEquals(expectedStr, actualStr)
  }
}
