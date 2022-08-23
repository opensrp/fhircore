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

import com.google.android.fhir.logicalId
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import java.util.Date
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
import org.junit.Test
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.engine.app.fakes.Faker
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.domain.model.RuleConfig
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.fhirpath.FhirPathDataExtractor

class RulesFactoryTest : RobolectricTest() {

  private lateinit var rulesEngine: DefaultRulesEngine
  private lateinit var fhirPathDataExtractor: FhirPathDataExtractor
  private lateinit var configurationRegistry: ConfigurationRegistry
  private lateinit var rulesFactory: RulesFactory
  private lateinit var rulesEngineService: RulesFactory.RulesEngineService

  @Before
  fun setUp() {
    configurationRegistry = Faker.buildTestConfigurationRegistry(mockk())
    fhirPathDataExtractor = mockk(relaxed = true)
    rulesEngine = mockk()
    rulesFactory = spyk(RulesFactory(configurationRegistry = configurationRegistry))
    rulesEngineService = rulesFactory.RulesEngineService()
  }

  @Test
  fun initPopulatesFactsWithDataAndFhirPathValues() {
    val facts = ReflectionHelpers.getField<Facts>(rulesFactory, "facts")
    Assert.assertEquals(3, facts.asMap().size)
    Assert.assertNotNull(facts.get("data"))
    Assert.assertNotNull(facts.get("fhirPath"))
    Assert.assertNotNull(facts.get("service"))
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
