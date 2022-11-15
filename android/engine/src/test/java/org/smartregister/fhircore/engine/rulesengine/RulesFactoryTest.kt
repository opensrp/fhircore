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

package org.smartregister.fhircore.engine.rulesengine

import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.logicalId
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import java.util.Date
import javax.inject.Inject
import org.apache.commons.jexl3.JexlException
import org.hl7.fhir.r4.model.Address
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.ContactPoint
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.HumanName
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Meta
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.StringType
import org.jeasy.rules.api.Facts
import org.jeasy.rules.api.Rules
import org.jeasy.rules.core.DefaultRulesEngine
import org.joda.time.LocalDate
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.engine.app.fakes.Faker
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.domain.model.RuleConfig
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.fhirpath.FhirPathDataExtractor

@HiltAndroidTest
class RulesFactoryTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltAndroidRule = HiltAndroidRule(this)

  @Inject lateinit var fhirPathDataExtractor: FhirPathDataExtractor

  private val rulesEngine = mockk<DefaultRulesEngine>()

  private val configurationRegistry: ConfigurationRegistry = Faker.buildTestConfigurationRegistry()

  private lateinit var rulesFactory: RulesFactory

  private lateinit var rulesEngineService: RulesFactory.RulesEngineService

  @Before
  fun setUp() {
    hiltAndroidRule.inject()
    rulesFactory =
      spyk(
        RulesFactory(
          ApplicationProvider.getApplicationContext(),
          configurationRegistry,
          fhirPathDataExtractor
        )
      )
    rulesEngineService = rulesFactory.RulesEngineService()
  }

  @Test
  fun initClearsExistingFacts() {
    val facts = ReflectionHelpers.getField<Facts>(rulesFactory, "facts")
    Assert.assertEquals(0, facts.asMap().size)
  }

  @Test
  fun beforeEvaluateReturnsTrue() {
    Assert.assertTrue(rulesFactory.beforeEvaluate(mockk(), mockk()))
  }

  @Test
  fun fireRuleCallsRulesEngineFireWithCorrectRulesAndFacts() {

    val baseResource = populateTestPatient()
    val relatedResourcesMap: Map<String, List<Resource>> = emptyMap()
    val ruleConfig =
      RuleConfig(
        name = "patientName",
        description = "Retrieve patient name",
        actions = listOf("data.put('familyName', fhirPath.extractValue(Group, 'Group.name'))")
      )
    val ruleConfigs = listOf(ruleConfig)

    ReflectionHelpers.setField(rulesFactory, "rulesEngine", rulesEngine)
    every { rulesEngine.fire(any(), any()) } just runs
    rulesFactory.fireRule(
      ruleConfigs = ruleConfigs,
      baseResource = baseResource,
      relatedResourcesMap = relatedResourcesMap
    )

    val factsSlot = slot<Facts>()
    val rulesSlot = slot<Rules>()
    verify { rulesEngine.fire(capture(rulesSlot), capture(factsSlot)) }

    val capturedBaseResource = factsSlot.captured.get<Patient>(baseResource.resourceType.name)
    Assert.assertEquals(baseResource.logicalId, capturedBaseResource.logicalId)
    Assert.assertTrue(capturedBaseResource.active)
    Assert.assertEquals(baseResource.birthDate, capturedBaseResource.birthDate)
    Assert.assertEquals(baseResource.name[0].given, capturedBaseResource.name[0].given)
    Assert.assertEquals(
      baseResource.address[0].city,
      capturedBaseResource.address[0].city,
    )

    val capturedRule = rulesSlot.captured.first()
    Assert.assertEquals(ruleConfig.name, capturedRule.name)
    Assert.assertEquals(ruleConfig.description, capturedRule.description)
  }

  @Test
  fun retrieveRelatedResourcesReturnsCorrectResource() {
    populateFactsWithResources()
    val result =
      rulesEngineService.retrieveRelatedResources(
        resource = populateTestPatient(),
        relatedResourceType = "CarePlan",
        fhirPathExpression = "CarePlan.subject.reference"
      )
    Assert.assertEquals(1, result.size)
    Assert.assertEquals("CarePlan", result[0].resourceType.name)
    Assert.assertEquals("careplan-1", result[0].logicalId)
  }

  @Test
  fun retrieveParentResourcesReturnsCorrectResource() {
    populateFactsWithResources()
    val result =
      rulesEngineService.retrieveParentResource(
        childResource = populateCarePlan(),
        parentResourceType = "Patient",
        fhirPathExpression = "CarePlan.subject.reference"
      )
    Assert.assertEquals("Patient", result!!.resourceType.name)
    Assert.assertEquals("patient-1", result.logicalId)
  }

  @Test
  fun extractGenderReturnsCorrectGender() {
    Assert.assertEquals(
      "Male",
      rulesEngineService.extractGender(Patient().setGender(Enumerations.AdministrativeGender.MALE))
    )
    Assert.assertEquals(
      "Female",
      rulesEngineService.extractGender(
        Patient().setGender(Enumerations.AdministrativeGender.FEMALE)
      )
    )
    Assert.assertEquals(
      "Other",
      rulesEngineService.extractGender(Patient().setGender(Enumerations.AdministrativeGender.OTHER))
    )
    Assert.assertEquals(
      "Unknown",
      rulesEngineService.extractGender(
        Patient().setGender(Enumerations.AdministrativeGender.UNKNOWN)
      )
    )
    Assert.assertEquals("", rulesEngineService.extractGender(Patient()))
  }

  @Test
  fun extractDOBReturnsCorrectDate() {
    Assert.assertEquals(
      "03/10/2015",
      rulesEngineService.extractDOB(
        Patient().setBirthDate(LocalDate.parse("2015-10-03").toDate()),
        "dd/MM/YYYY"
      )
    )
  }

  @Test
  fun shouldFormatDateWithExpectedFormat() {
    val inputDate = Date("2021/10/10")

    val expectedFormat = "dd-MM-yyyy"
    Assert.assertEquals("10-10-2021", rulesEngineService.formatDate(inputDate, expectedFormat))

    val expectedFormat2 = "dd yyyy"
    Assert.assertEquals("10 2021", rulesEngineService.formatDate(inputDate, expectedFormat2))

    Assert.assertEquals("Sun, Oct 10 2021", rulesEngineService.formatDate(inputDate))
  }

  @Test
  fun shouldInputDateStringWithExpectedFormat() {
    val inputDateString = "2021-10-10"
    val inputDateFormat = "yyyy-MM-dd"

    val expectedFormat = "dd-MM-yyyy"
    Assert.assertEquals(
      "10-10-2021",
      rulesEngineService.formatDate(inputDateString, inputDateFormat, expectedFormat)
    )

    val expectedFormat2 = "dd yyyy"
    Assert.assertEquals(
      "10 2021",
      rulesEngineService.formatDate(inputDateString, inputDateFormat, expectedFormat2)
    )

    Assert.assertEquals(
      "Sun, Oct 10 2021",
      rulesEngineService.formatDate(inputDateString, inputDateFormat)
    )
  }

  @Test
  fun mapResourcesToLabeledCSVReturnsCorrectLabels() {
    val fhirPathExpression = "Patient.active and (Patient.birthDate >= today() - 5 'years')"
    val resources =
      listOf(
        Patient().setBirthDate(LocalDate.parse("2015-10-03").toDate()),
        Patient().setActive(true).setBirthDate(LocalDate.parse("2019-10-03").toDate()),
        Patient().setActive(true).setBirthDate(LocalDate.parse("2020-10-03").toDate())
      )

    val result = rulesEngineService.mapResourcesToLabeledCSV(resources, fhirPathExpression, "CHILD")
    Assert.assertEquals("CHILD,CHILD", result)
  }

  @Test
  fun mapResourceToLabeledCSVReturnsCorrectLabels() {
    val fhirPathExpression = "Patient.active and (Patient.birthDate >= today() - 5 'years')"
    val resource = Patient().setActive(true).setBirthDate(LocalDate.parse("2019-10-03").toDate())

    val result = rulesEngineService.mapResourceToLabeledCSV(resource, fhirPathExpression, "CHILD")
    Assert.assertEquals("CHILD", result)
  }

  @Test
  fun evaluateToBooleanReturnsCorrectValueWhenMatchAllIsTrue() {
    val fhirPathExpression = "Patient.active"
    val patients =
      mutableListOf(Patient().setActive(true), Patient().setActive(true), Patient().setActive(true))

    Assert.assertTrue(rulesEngineService.evaluateToBoolean(patients, fhirPathExpression, true))

    patients.add(Patient().setActive(false))
    Assert.assertFalse(rulesEngineService.evaluateToBoolean(patients, fhirPathExpression, true))
  }

  @Test
  fun evaluateToBooleanReturnsCorrectValueWhenMatchAllIsFalse() {
    val fhirPathExpression = "Patient.active"
    val patients =
      mutableListOf(Patient().setActive(true), Patient().setActive(true), Patient().setActive(true))

    Assert.assertTrue(rulesEngineService.evaluateToBoolean(patients, fhirPathExpression, false))

    patients.add(Patient().setActive(false))
    Assert.assertTrue(rulesEngineService.evaluateToBoolean(patients, fhirPathExpression, false))
  }

  @Test
  fun onFailureLogsWarningForJexlException_Variable() {
    val exception = mockk<JexlException.Variable>()
    every { exception.localizedMessage } returns "jexl exception"
    every { exception.variable } returns "var"
    rulesFactory.onFailure(mockk(relaxed = true), mockk(relaxed = true), exception)
    verify {
      rulesFactory.log(
        exception,
        "jexl exception, consider checking for null before usage: e.g var != null"
      )
    }
  }

  @Test
  fun onFailureLogsErrorForException() {
    val exception = mockk<Exception>()
    every { exception.localizedMessage } returns "jexl exception"
    rulesFactory.onFailure(mockk(relaxed = true), mockk(relaxed = true), exception)
    verify { rulesFactory.log(exception) }
  }

  @Test
  fun onEvaluationErrorLogsError() {
    val exception = mockk<Exception>()
    every { exception.localizedMessage } returns "jexl exception"
    rulesFactory.onEvaluationError(mockk(relaxed = true), mockk(relaxed = true), exception)
    verify { rulesFactory.log(exception, "Evaluation error") }
  }

  private fun populateFactsWithResources() {
    val carePlanRelatedResource = mutableListOf(populateCarePlan())
    val patientRelatedResource = mutableListOf(populateTestPatient())
    val facts = ReflectionHelpers.getField<Facts>(rulesFactory, "facts")
    facts.apply {
      put(carePlanRelatedResource[0].resourceType.name, carePlanRelatedResource)
      put(patientRelatedResource[0].resourceType.name, patientRelatedResource)
    }
    ReflectionHelpers.setField(rulesFactory, "facts", facts)
  }

  private fun populateTestPatient(): Patient {
    val patientId = "patient-1"
    val patient: Patient =
      Patient().apply {
        id = patientId
        active = true
        birthDate = LocalDate.parse("1999-10-03").toDate()
        gender = Enumerations.AdministrativeGender.MALE
        address =
          listOf(
            Address().apply {
              city = "Nairobi"
              country = "Kenya"
            }
          )
        name =
          listOf(
            HumanName().apply {
              given = mutableListOf(StringType("Kiptoo"))
              family = "Maina"
            }
          )
        telecom = listOf(ContactPoint().apply { value = "12345" })
        meta = Meta().apply { lastUpdated = Date() }
      }
    return patient
  }

  private fun populateCarePlan(): CarePlan {
    val carePlan: CarePlan =
      CarePlan().apply {
        id = "careplan-1"
        identifier =
          mutableListOf(
            Identifier().apply {
              use = Identifier.IdentifierUse.OFFICIAL
              value = "value-1"
            }
          )
        subject = Reference().apply { reference = "Patient/patient-1" }
      }
    return carePlan
  }
}
