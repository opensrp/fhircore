/*
 * Copyright 2021-2024 Ona Systems, Inc
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

package org.smartregister.fhircore.quest

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import ca.uhn.fhir.parser.IParser
import com.google.android.fhir.datacapture.mapping.ResourceMapper
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.exceptions.FHIRException
import org.hl7.fhir.r4.context.SimpleWorkerContext
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Parameters
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.RelatedPerson
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.utilities.npm.FilesystemPackageCacheManager
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.smartregister.fhircore.engine.util.helper.TransformSupportServices
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

/**
 * Provides a playground for quickly testing and authoring questionnaire.json and the respective
 * StructureMap
 *
 * This should be removed at a later point once we have a more clear way of doing this
 */
class StructureMapUtilitiesTest : RobolectricTest() {
  private lateinit var packageCacheManager: FilesystemPackageCacheManager
  private lateinit var contextR4: SimpleWorkerContext
  private lateinit var transformSupportServices: TransformSupportServices
  private lateinit var structureMapUtilities: org.hl7.fhir.r4.utils.StructureMapUtilities
  private lateinit var iParser: IParser

  @Before
  fun setUp() {
    packageCacheManager = FilesystemPackageCacheManager(true)
    contextR4 =
      SimpleWorkerContext.fromPackage(packageCacheManager.loadPackage("hl7.fhir.r4.core", "4.0.1"))
        .apply {
          setExpansionProfile(Parameters())
          isCanRunWithoutTerminology = true
        }
    transformSupportServices = TransformSupportServices(contextR4)
    structureMapUtilities =
      org.hl7.fhir.r4.utils.StructureMapUtilities(contextR4, transformSupportServices)
    iParser = FhirContext.forCached(FhirVersionEnum.R4).newJsonParser()
  }

  @After
  fun packageTearDown() {
    // Clean up resources or reset states here
    packageCacheManager.clear()
  }

  @Test
  fun `perform family extraction`() {
    val registrationQuestionnaireResponseString: String =
      "content/general/family/questionnaire-response-standard.json".readFile()
    val registrationStructureMap = "content/general/family/family-registration.map".readFile()
    val structureMap =
      structureMapUtilities.parse(registrationStructureMap, "eCBIS Family Registration")
    val targetResource = Bundle()
    val baseElement =
      iParser.parseResource(
        QuestionnaireResponse::class.java,
        registrationQuestionnaireResponseString,
      )

    structureMapUtilities.transform(contextR4, baseElement, structureMap, targetResource)

    Assert.assertEquals(2, targetResource.entry.size)
    Assert.assertEquals("Group", targetResource.entry[0].resource.resourceType.toString())
    Assert.assertEquals("Encounter", targetResource.entry[1].resource.resourceType.toString())
  }

  @Test
  fun `perform disease extraction`() {
    val immunizationQuestionnaireResponseString: String =
      "content/general/disease-registration-resources/questionnaire_response.json".readFile()
    val immunizationStructureMap =
      "content/general/disease-registration-resources/structure-map.txt".readFile()
    val structureMap =
      structureMapUtilities.parse(immunizationStructureMap, "eCBIS Disease Registration")
    val targetResource = Bundle()
    val baseElement =
      iParser.parseResource(
        QuestionnaireResponse::class.java,
        immunizationQuestionnaireResponseString,
      )

    structureMapUtilities.transform(contextR4, baseElement, structureMap, targetResource)

    Assert.assertEquals(7, targetResource.entry.size)
    Assert.assertEquals("Condition", targetResource.entry[0].resource.resourceType.toString())
    Assert.assertEquals("Condition", targetResource.entry[1].resource.resourceType.toString())
  }

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun `populate immunization Questionnaire`() {
    val patientJson = "content/eir/immunization/patient.json".readFile()
    val immunizationJson = "content/eir/immunization/immunization-1.json".readFile()
    val immunizationStructureMap = "content/eir/immunization/structure-map.txt".readFile()
    val questionnaireJson = "content/eir/immunization/questionnaire.json".readFile()
    val structureMap =
      structureMapUtilities.parse(immunizationStructureMap, "ImmunizationRegistration")
    val targetResource = Bundle()
    val patient = iParser.parseResource(Patient::class.java, patientJson)
    val immunization = iParser.parseResource(Immunization::class.java, immunizationJson)
    val questionnaire = iParser.parseResource(Questionnaire::class.java, questionnaireJson)
    val questionnaireResponse: QuestionnaireResponse

    runBlocking {
      questionnaireResponse =
        ResourceMapper.populate(
          questionnaire,
          mapOf(
            ResourceType.Patient.name.lowercase() to patient,
            ResourceType.Immunization.name.lowercase() to immunization,
          ),
        )
    }

    structureMapUtilities.transform(contextR4, questionnaireResponse, structureMap, targetResource)

    Assert.assertEquals(2, targetResource.entry.size)
    Assert.assertEquals("Encounter", targetResource.entry[0].resource.resourceType.toString())
    Assert.assertEquals("Immunization", targetResource.entry[1].resource.resourceType.toString())
  }

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun `populate patient registration Questionnaire and extract Resources`() {
    val patientRegistrationQuestionnaire =
      "patient-registration-questionnaire/questionnaire.json".readFile()
    val patientRegistrationStructureMap =
      "patient-registration-questionnaire/structure-map.txt".readFile()
    val relatedPersonJson = "patient-registration-questionnaire/related-person.json".readFile()
    val patientJson = "patient-registration-questionnaire/sample/patient.json".readFile()
    val iParser: IParser = FhirContext.forCached(FhirVersionEnum.R4).newJsonParser()
    val questionnaire =
      iParser.parseResource(Questionnaire::class.java, patientRegistrationQuestionnaire)
    val patient = iParser.parseResource(Patient::class.java, patientJson)
    val relatedPerson = iParser.parseResource(RelatedPerson::class.java, relatedPersonJson)
    var questionnaireResponse: QuestionnaireResponse

    runBlocking {
      questionnaireResponse =
        ResourceMapper.populate(
          questionnaire,
          mapOf(
            ResourceType.Patient.name.lowercase() to patient,
            ResourceType.RelatedPerson.name.lowercase() to relatedPerson,
          ),
        )
    }

    val structureMap =
      structureMapUtilities.parse(patientRegistrationStructureMap, "PatientRegistration")
    val targetResource = Bundle()

    structureMapUtilities.transform(contextR4, questionnaireResponse, structureMap, targetResource)

    Assert.assertEquals(1, targetResource.entry.size)
    Assert.assertEquals("Patient", targetResource.entry[0].resource.resourceType.toString())
  }

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun `populate adverse event Questionnaire and extract Resources`() {
    val adverseEventQuestionnaire = "content/eir/adverse-event/questionnaire.json".readFile()
    val adverseEventStructureMap = "content/eir/adverse-event/structure-map.txt".readFile()
    val immunizationJson = "content/eir/adverse-event/immunization.json".readFile()
    val iParser: IParser = FhirContext.forCached(FhirVersionEnum.R4).newJsonParser()
    val questionnaire = iParser.parseResource(Questionnaire::class.java, adverseEventQuestionnaire)
    val immunization = iParser.parseResource(Immunization::class.java, immunizationJson)
    var questionnaireResponse: QuestionnaireResponse

    runBlocking {
      questionnaireResponse =
        ResourceMapper.populate(
          questionnaire,
          mapOf(
            ResourceType.Immunization.name.lowercase() to immunization,
            ResourceType.Patient.name.lowercase() to Patient(),
          ),
        )
    }

    val structureMap = structureMapUtilities.parse(adverseEventStructureMap, "AdverseEvent")
    val targetResource = Bundle()

    structureMapUtilities.transform(contextR4, questionnaireResponse, structureMap, targetResource)

    Assert.assertEquals(2, targetResource.entry.size)
    Assert.assertEquals("Immunization", targetResource.entry[0].resource.resourceType.toString())
    Assert.assertEquals("Observation", targetResource.entry[1].resource.resourceType.toString())
  }

  @Test
  fun `convert StructureMap to JSON`() {
    val patientRegistrationStructureMap =
      "patient-registration-questionnaire/structure-map.txt".readFile()
    val structureMap =
      structureMapUtilities.parse(patientRegistrationStructureMap, "PatientRegistration")
    val mapString = iParser.encodeResourceToString(structureMap)

    Assert.assertNotNull(mapString)
  }

  @Test
  fun `perform extraction from patient registration Questionnaire`() {
    val patientRegistrationQuestionnaireResponse =
      "patient-registration-questionnaire/questionnaire-response.json".readFile()
    val patientRegistrationStructureMap =
      "patient-registration-questionnaire/structure-map.txt".readFile()
    val structureMap =
      structureMapUtilities.parse(patientRegistrationStructureMap, "PatientRegistration")
    val targetResource = Bundle()
    val baseElement =
      iParser.parseResource(
        QuestionnaireResponse::class.java,
        patientRegistrationQuestionnaireResponse,
      )
    structureMapUtilities.transform(contextR4, baseElement, structureMap, targetResource)

    Assert.assertEquals(2, targetResource.entry.size)
    Assert.assertEquals("Patient", targetResource.entry[0].resource.resourceType.toString())
    Assert.assertEquals("Condition", targetResource.entry[1].resource.resourceType.toString())
  }

  @Test
  fun `perform extraction from adverse event Questionnaire`() {
    val adverseEventQuestionnaireResponse =
      "content/eir/adverse-event/questionnaire-response.json".readFile()
    val adverseEventStructureMap = "content/eir/adverse-event/structure-map.txt".readFile()
    val structureMap = structureMapUtilities.parse(adverseEventStructureMap, "AdverseEvent")
    val targetResource = Bundle()

    val baseElement =
      iParser.parseResource(QuestionnaireResponse::class.java, adverseEventQuestionnaireResponse)

    structureMapUtilities.transform(contextR4, baseElement, structureMap, targetResource)

    Assert.assertEquals(2, targetResource.entry.size)
    Assert.assertEquals("Immunization", targetResource.entry[0].resource.resourceType.toString())
    Assert.assertEquals("Observation", targetResource.entry[1].resource.resourceType.toString())
  }

  @Test
  fun `perform extraction from  vital signs metric Questionnaire`() {
    val vitalSignQuestionnaireResponse =
      "content/anc/vital-signs/metric/questionnaire-response-pulse-rate.json".readFile()
    val vitalSignStructureMap = "content/anc/vital-signs/metric/structure-map.txt".readFile()
    val structureMap = structureMapUtilities.parse(vitalSignStructureMap, "VitalSigns")
    val targetResource = Bundle()
    val baseElement =
      iParser.parseResource(QuestionnaireResponse::class.java, vitalSignQuestionnaireResponse)

    structureMapUtilities.transform(contextR4, baseElement, structureMap, targetResource)

    Assert.assertEquals(2, targetResource.entry.size)
    Assert.assertEquals("Encounter", targetResource.entry[0].resource.resourceType.toString())
    Assert.assertEquals("Observation", targetResource.entry[1].resource.resourceType.toString())
  }

  @Test
  fun `perform extraction from  vital signs standard Questionnaire`() {
    val vitalSignQuestionnaireResponse =
      "content/anc/vital-signs/standard/questionnaire-response-pulse-rate.json".readFile()
    val vitalSignStructureMap = "content/anc/vital-signs/standard/structure-map.txt".readFile()

    val structureMap = structureMapUtilities.parse(vitalSignStructureMap, "VitalSigns")
    val targetResource = Bundle()
    val baseElement =
      iParser.parseResource(QuestionnaireResponse::class.java, vitalSignQuestionnaireResponse)

    structureMapUtilities.transform(contextR4, baseElement, structureMap, targetResource)

    Assert.assertEquals(2, targetResource.entry.size)
    Assert.assertEquals("Encounter", targetResource.entry[0].resource.resourceType.toString())
    Assert.assertEquals("Observation", targetResource.entry[1].resource.resourceType.toString())
    packageCacheManager.clear()
  }

  @Test
  fun `perform location extraction`() {
    val locationQuestionnaireResponseString: String =
      "content/general/location/location-response-sample.json".readFile()
    val locationStructureMap = "content/general/location/location-structure-map.txt".readFile()
    val structureMap = structureMapUtilities.parse(locationStructureMap, "LocationRegistration")
    val targetResource = Bundle()
    val baseElement =
      iParser.parseResource(QuestionnaireResponse::class.java, locationQuestionnaireResponseString)

    structureMapUtilities.transform(contextR4, baseElement, structureMap, targetResource)

    Assert.assertEquals(1, targetResource.entry.size)
    Assert.assertEquals("Location", targetResource.entry[0].resource.resourceType.toString())
  }

  @Test
  fun `perform supply chain snapshot observation`() {
    val physicalInventoryCountQuestionnaireResponseString: String =
      "content/general/supply-chain/questionnaire-response-standard.json".readFile()
    val physicalInventoryCountStructureMap =
      "content/general/supply-chain/physical_inventory_count_and_stock.map".readFile()
    val structureMap =
      structureMapUtilities.parse(
        physicalInventoryCountStructureMap,
        "Physical Inventory Count and Stock Supply",
      )
    val targetResource = Bundle()
    val baseElement =
      iParser.parseResource(
        QuestionnaireResponse::class.java,
        physicalInventoryCountQuestionnaireResponseString,
      )

    structureMapUtilities.transform(contextR4, baseElement, structureMap, targetResource)

    // for some weird reason, the `entry` has 8 resources instead of 7. The 1st resource is blank.
    Assert.assertTrue(targetResource.entry.size == 9)
    Assert.assertTrue(targetResource.entry[2].resource is Observation)

    val observation = targetResource.entry[7].resource as Observation
    Assert.assertTrue(observation.code.text == "under-reporting")
  }

  @Test
  fun `perform extraction from  pregnancy outcome Questionnaire`() {
    val vitalSignQuestionnaireResponse =
      "content/anc/preg-outcome/questionnaire-response.json".readFile()
    val vitalSignStructureMap = "content/anc/preg-outcome/structure-map.txt".readFile()
    val structureMap =
      structureMapUtilities.parse(vitalSignStructureMap, "PregnancyOutcomeRegistration")
    val targetResource = Bundle()
    val baseElement =
      iParser.parseResource(QuestionnaireResponse::class.java, vitalSignQuestionnaireResponse)

    structureMapUtilities.transform(contextR4, baseElement, structureMap, targetResource)

    Assert.assertTrue(targetResource.entry.size > 10)
    val taskList =
      targetResource.entry.filter {
        it.resource.resourceType != null && it.resource.resourceType == ResourceType.Task
      }
    Assert.assertTrue(taskList.size == 10)
  }

  @Test(expected = FHIRException::class)
  fun `perform extraction for patient registration`() {
    val locationQuestionnaireResponseString: String =
      "content/general/who-eir/patient_registration_questionnaire_response.json".readFile()
    val locationStructureMap =
      "content/general/who-eir/patient_registration_structure_map.txt".readFile()
    val structureMap = structureMapUtilities.parse(locationStructureMap, "IMMZ-C-QRToPatient")
    val targetResource = Bundle()
    val baseElement =
      iParser.parseResource(QuestionnaireResponse::class.java, locationQuestionnaireResponseString)

    structureMapUtilities.transform(contextR4, baseElement, structureMap, targetResource)

    Assert.assertEquals(2, targetResource.entry.size)
    Assert.assertEquals("Patient", targetResource.entry[0].resource.resourceType.toString())
    Assert.assertEquals("Condition", targetResource.entry[0].resource.resourceType.toString())
  }
}
