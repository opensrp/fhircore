package org.smartregister.fhircore.quest

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.IParser
import org.hl7.fhir.r4.context.SimpleWorkerContext
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Parameters
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.utilities.npm.FilesystemPackageCacheManager
import org.hl7.fhir.utilities.npm.ToolsVersion
import org.junit.Assert
import org.junit.Test
import org.smartregister.fhircore.engine.util.extension.setPropertySafely
import org.smartregister.fhircore.engine.util.helper.TransformSupportServices
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

class RegisterContentTest: RobolectricTest() {

    @Test
    fun testG6pdTestResultsExtraction() {
        val structureMap = "test-results-questionnaire/structure-map.txt".readFile()
        val response = "test-results-questionnaire/questionnaire-response.json".readFile()

        val scu = buildStructureMapUtils()
        val targetResource = transform(scu, structureMap, response,"TestResults")

        Assert.assertEquals(4, targetResource.entry.size)

        val encounter = targetResource.entry[0].resource as Encounter
        val sampleEncounter = "test-results-questionnaire/sample/enc.json".parseSampleResource() as Encounter

        assertResourceContent(sampleEncounter, encounter)

        val tType = targetResource.entry[1].resource as Observation
        val sampleTType = "test-results-questionnaire/sample/obs_res_type.json".parseSampleResource() as Observation

        assertResourceContent(tType, sampleTType)

        val g6pd = targetResource.entry[2].resource as Observation
        val sampleG6pd = "test-results-questionnaire/sample/obs_g6pd.json".parseSampleResource() as Observation

        assertResourceContent(g6pd, sampleG6pd)

        val hb = targetResource.entry[3].resource as Observation
        val sampleHb = "test-results-questionnaire/sample/obs_hb.json".parseSampleResource() as Observation

        assertResourceContent(hb, sampleHb)
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

        Assert.assertEquals(expectedStr, actualStr)
    }

    @Test
    fun testG6pdPatientRegistrationExtraction() {
        val structureMap = "patient-registration-questionnaire/structure-map.txt".readFile()
        val response = "patient-registration-questionnaire/questionnaire-response.json".readFile()

        val scu = buildStructureMapUtils()
        val targetResource = transform(scu, structureMap, response,"PatientRegistration")

        Assert.assertEquals(2, targetResource.entry.size)

        val patient = targetResource.entry[0].resource as Patient
        val samplePatient = "patient-registration-questionnaire/sample/patient.json".parseSampleResource() as Patient

        assertResourceContent(patient, samplePatient)

        val condition = targetResource.entry[1].resource as Condition
        val sampleCondition = "patient-registration-questionnaire/sample/condition.json".parseSampleResource() as Condition
        // replace subject as registration forms generate uuid on the fly
        sampleCondition.subject = condition.subject

        assertResourceContent(condition, sampleCondition)
    }
}