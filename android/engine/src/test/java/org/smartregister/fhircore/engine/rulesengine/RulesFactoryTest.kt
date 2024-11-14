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

package org.smartregister.fhircore.engine.rulesengine

import androidx.test.core.app.ApplicationProvider
import ca.uhn.fhir.context.FhirContext
import com.google.android.fhir.datacapture.extensions.logicalId
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.apache.commons.jexl3.JexlException
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Group
import org.hl7.fhir.r4.model.ListResource
import org.hl7.fhir.r4.model.Location
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.Task
import org.hl7.fhir.r4.model.Task.TaskStatus
import org.jeasy.rules.api.Facts
import org.jeasy.rules.api.Rules
import org.jeasy.rules.core.DefaultRulesEngine
import org.joda.time.LocalDate
import org.joda.time.Period
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.engine.app.fakes.Faker
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.domain.model.RelatedResourceCount
import org.smartregister.fhircore.engine.domain.model.RepositoryResourceData
import org.smartregister.fhircore.engine.domain.model.RuleConfig
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.rule.CoroutineTestRule
import org.smartregister.fhircore.engine.rulesengine.services.LocationService
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.extension.SDF_YYYY_MM_DD
import org.smartregister.fhircore.engine.util.extension.asReference
import org.smartregister.fhircore.engine.util.extension.plusYears
import org.smartregister.fhircore.engine.util.fhirpath.FhirPathDataExtractor

@HiltAndroidTest
class RulesFactoryTest : RobolectricTest() {
  @get:Rule(order = 0) val hiltAndroidRule = HiltAndroidRule(this)

  @kotlinx.coroutines.ExperimentalCoroutinesApi
  @get:Rule(order = 1)
  val coroutineRule = CoroutineTestRule()

  @Inject lateinit var fhirPathDataExtractor: FhirPathDataExtractor

  @Inject lateinit var dispatcherProvider: DispatcherProvider

  @Inject lateinit var locationService: LocationService
  private val rulesEngine = mockk<DefaultRulesEngine>()
  private val configurationRegistry: ConfigurationRegistry = Faker.buildTestConfigurationRegistry()
  private lateinit var rulesFactory: RulesFactory
  private lateinit var rulesEngineService: RulesFactory.RulesEngineService

  @Inject lateinit var fhirContext: FhirContext
  private lateinit var defaultRepository: DefaultRepository

  @Before
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun setUp() {
    hiltAndroidRule.inject()
    defaultRepository = mockk(relaxed = true)
    rulesFactory =
      spyk(
        RulesFactory(
          context = ApplicationProvider.getApplicationContext(),
          configurationRegistry = configurationRegistry,
          fhirPathDataExtractor = fhirPathDataExtractor,
          dispatcherProvider = dispatcherProvider,
          locationService = locationService,
          fhirContext = fhirContext,
          defaultRepository = defaultRepository,
        ),
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
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun fireRulesCallsRulesEngineFireWithCorrectRulesAndFacts() {
    runTest {
      val baseResource = Faker.buildPatient()
      val relatedResourcesMap: Map<String, List<Resource>> = emptyMap()
      val ruleConfig =
        RuleConfig(
          name = "patientName",
          description = "Retrieve patient name",
          actions = listOf("data.put('familyName', fhirPath.extractValue(Group, 'Group.name'))"),
        )
      val ruleConfigs = listOf(ruleConfig)

      ReflectionHelpers.setField(rulesFactory, "rulesEngine", rulesEngine)
      every { rulesEngine.fire(any(), any()) } just runs
      val rules = rulesFactory.generateRules(ruleConfigs)
      rulesFactory.fireRules(
        rules = rules,
        repositoryResourceData =
          RepositoryResourceData(
            resource = baseResource,
            secondaryRepositoryResourceData = null,
            relatedResourcesMap = relatedResourcesMap,
          ),
        params = emptyMap(),
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
  }

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun fireRulesCallsRulesEngineFireWithCorrectRulesAndFactsWhenMissingRelatedResourcesMap() {
    runTest {
      val baseResource = Faker.buildPatient()
      val ruleConfig =
        RuleConfig(
          name = "patientName",
          description = "Retrieve patient name",
          actions = listOf("data.put('familyName', fhirPath.extractValue(Group, 'Group.name'))"),
        )
      val ruleConfigs = listOf(ruleConfig)

      ReflectionHelpers.setField(rulesFactory, "rulesEngine", rulesEngine)
      every { rulesEngine.fire(any(), any()) } just runs
      val rules = rulesFactory.generateRules(ruleConfigs)
      rulesFactory.fireRules(
        rules = rules,
        repositoryResourceData =
          RepositoryResourceData(
            resource = baseResource,
            secondaryRepositoryResourceData =
              listOf(
                RepositoryResourceData(
                  resourceRulesEngineFactId = "commodities",
                  resource = Group().apply { id = "Commodity1" },
                  relatedResourcesMap =
                    mapOf(
                      "stockObservations" to
                        listOf(
                          Observation().apply { id = "Obsv1" },
                          Observation().apply { id = "Obsv2" },
                        ),
                      "latestObservations" to
                        listOf(
                          Observation().apply { id = "Obsv3" },
                          Observation().apply { id = "Obsv4" },
                        ),
                    ),
                  relatedResourcesCountMap =
                    mapOf("stockCount" to listOf(RelatedResourceCount(count = 20))),
                ),
                RepositoryResourceData(
                  resourceRulesEngineFactId = "commodities",
                  resource = Group().apply { id = "Commodity2" },
                  relatedResourcesMap =
                    mapOf(
                      "stockObservations" to
                        listOf(
                          Observation().apply { id = "Obsv6" },
                          Observation().apply { id = "Obsv7" },
                        ),
                    ),
                  relatedResourcesCountMap =
                    mapOf("stockCount" to listOf(RelatedResourceCount(count = 10))),
                ),
              ),
          ),
        params = emptyMap(),
      )

      val factsSlot = slot<Facts>()
      val rulesSlot = slot<Rules>()
      verify { rulesEngine.fire(capture(rulesSlot), capture(factsSlot)) }

      val facts = factsSlot.captured
      val capturedBaseResource = facts.get<Patient>(baseResource.resourceType.name)
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

      // Assertions for secondary resources
      val factsMap = facts.asMap()
      val commodities: List<Resource> = factsMap["commodities"] as List<Resource>
      Assert.assertNotNull(commodities)
      Assert.assertEquals(2, commodities.size)
      Assert.assertEquals("Commodity1", commodities.first().id)
      Assert.assertEquals("Commodity2", commodities.last().id)

      val latestObservations: List<Resource> = factsMap["latestObservations"] as List<Resource>
      Assert.assertNotNull(latestObservations)
      Assert.assertEquals(2, latestObservations.size)
      Assert.assertEquals("Obsv3", latestObservations.first().id)
      Assert.assertEquals("Obsv4", latestObservations.last().id)

      val stockObservations: List<Resource> = factsMap["stockObservations"] as List<Resource>
      Assert.assertNotNull(stockObservations)
      Assert.assertEquals(4, stockObservations.size)
      Assert.assertEquals("Obsv1", stockObservations.first().id)
      Assert.assertEquals("Obsv7", stockObservations.last().id)

      val stockCount: List<RelatedResourceCount> =
        factsMap["stockCount"] as List<RelatedResourceCount>
      Assert.assertNotNull(stockCount)
      Assert.assertEquals(2, stockCount.size)
      Assert.assertEquals(20, stockCount.first().count)
      Assert.assertEquals(10, stockCount.last().count)
    }
  }

  @Test
  fun retrieveRelatedResourcesReturnsCorrectResource() {
    populateFactsWithResources()
    val result =
      rulesEngineService.retrieveRelatedResources(
        resource = Faker.buildPatient(),
        relatedResourceKey = ResourceType.CarePlan.name,
        referenceFhirPathExpression = "CarePlan.subject.reference",
      )
    Assert.assertEquals(1, result.size)
    Assert.assertEquals("CarePlan", result[0].resourceType.name)
    Assert.assertEquals("careplan-1", result[0].logicalId)
  }

  @Test
  fun retrieveRelatedResourcesReturnsCorrectResourceWithForwardInclude() {
    val patient = Faker.buildPatient()
    val group =
      Group().apply {
        id = "grp1"
        addMember(
          Group.GroupMemberComponent().apply { entity = patient.asReference() },
        )
      }
    populateFactsWithResources(group)
    val result =
      rulesEngineService.retrieveRelatedResources(
        resource = group,
        relatedResourceKey = ResourceType.Patient.name,
        referenceFhirPathExpression = "Group.member.entity.reference",
        isRevInclude = false,
      )
    Assert.assertEquals(1, result.size)
    Assert.assertEquals("Patient", result[0].resourceType.name)
    Assert.assertEquals(patient.logicalId, result[0].logicalId)
  }

  @Test
  fun retrieveRelatedResourcesWithoutReferenceReturnsResources() {
    populateFactsWithResources()
    val result =
      rulesEngineService.retrieveRelatedResources(
        resource = Faker.buildPatient(),
        relatedResourceKey = ResourceType.CarePlan.name,
        referenceFhirPathExpression = "",
      )
    Assert.assertEquals(1, result.size)
  }

  @Test
  fun retrieveParentResourcesReturnsCorrectResource() {
    populateFactsWithResources()
    val result =
      rulesEngineService.retrieveParentResource(
        childResource = Faker.buildCarePlan(),
        parentResourceType = "Patient",
        fhirPathExpression = "CarePlan.subject.reference",
      )
    Assert.assertEquals("Patient", result!!.resourceType.name)
    Assert.assertEquals("sampleId", result.logicalId)
  }

  @Test
  fun extractGenderReturnsCorrectGender() {
    Assert.assertEquals(
      "Male",
      rulesEngineService.extractGender(Patient().setGender(Enumerations.AdministrativeGender.MALE)),
    )
    Assert.assertEquals(
      "Female",
      rulesEngineService.extractGender(
        Patient().setGender(Enumerations.AdministrativeGender.FEMALE),
      ),
    )
    Assert.assertEquals(
      "Other",
      rulesEngineService.extractGender(
        Patient().setGender(Enumerations.AdministrativeGender.OTHER),
      ),
    )
    Assert.assertEquals(
      "Unknown",
      rulesEngineService.extractGender(
        Patient().setGender(Enumerations.AdministrativeGender.UNKNOWN),
      ),
    )
    Assert.assertEquals("", rulesEngineService.extractGender(Patient()))
  }

  @Test
  fun extractDOBReturnsCorrectDate() {
    Assert.assertEquals(
      "03/10/2015",
      rulesEngineService.extractDOB(
        Patient().setBirthDate(LocalDate.parse("2015-10-03").toDate()),
        "dd/MM/YYYY",
      ),
    )
  }

  @Test
  fun shouldFormatDateWithExpectedFormat() {
    val inputDate = LocalDate.parse("2021-10-10").toDate()

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
      rulesEngineService.formatDate(inputDateString, inputDateFormat, expectedFormat),
    )

    val expectedFormat2 = "dd yyyy"
    Assert.assertEquals(
      "10 2021",
      rulesEngineService.formatDate(inputDateString, inputDateFormat, expectedFormat2),
    )

    Assert.assertEquals(
      "Sun, Oct 10 2021",
      rulesEngineService.formatDate(inputDateString, inputDateFormat),
    )
  }

  @Test
  fun shouldInputDateTimeStringWithExpectedFormat() {
    val inputDateString = "2023-09-01T00:00:00.00Z"
    val inputDateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
    val expectedFormat = "dd-MM-yyyy"
    Assert.assertEquals(
      "01-09-2023",
      rulesEngineService.formatDate(inputDateString, inputDateFormat, expectedFormat),
    )
  }

  @Test
  fun mapResourcesToLabeledCSVReturnsCorrectLabels() {
    val fhirPathExpression = "Patient.active and (Patient.birthDate >= today() - 5 'years')"
    val resources =
      listOf(
        Patient().setBirthDate(LocalDate.parse("2015-10-03").toDate()),
        Patient().setActive(true).setBirthDate(LocalDate.parse("2019-10-03").toDate()),
        Patient().setActive(true).setBirthDate(LocalDate.parse("2020-10-03").toDate()),
      )

    val result = rulesEngineService.mapResourcesToLabeledCSV(resources, fhirPathExpression, "CHILD")
    Assert.assertEquals("CHILD", result)
  }

  @Test
  fun mapResourceToLabeledCSVReturnsCorrectLabels() {
    val fhirPathExpression = "Patient.active and (Patient.birthDate >= today() - 5 'years')"
    val resource = Patient().setActive(true).setBirthDate(Date().plusYears(-5))

    val result = rulesEngineService.mapResourceToLabeledCSV(resource, fhirPathExpression, "CHILD")
    Assert.assertEquals("CHILD", result)
  }

  @Test
  fun `test mapResourcesToLabeledCSV with no resources`() {
    val resources = null
    val fhirPathExpression = "Patient.active and (Patient.birthDate <= today() - 60 'years')"
    val label = "ELDERLY"
    val matchAllExtraConditions = false
    val extraConditions = emptyArray<Any>()

    val result =
      rulesEngineService.mapResourcesToLabeledCSV(
        resources,
        fhirPathExpression,
        label,
        matchAllExtraConditions,
        *extraConditions,
      )

    Assert.assertEquals("", result)
  }

  @Test
  fun `test mapResourcesToLabeledCSV with no matching resources`() {
    val resources =
      listOf(
        Patient().setBirthDate(LocalDate.parse("2015-10-03").toDate()),
        Patient().setActive(true).setBirthDate(LocalDate.parse("1954-10-03").toDate()),
        Patient().setActive(true).setBirthDate(LocalDate.parse("1944-10-03").toDate()),
      )
    val fhirPathExpression = "Patient.active and (Patient.birthDate <= today() - 60 'years')"
    val label = "ELDERLY"
    val matchAllExtraConditions = false
    val extraConditions = emptyArray<Any>()

    val result =
      rulesEngineService.mapResourcesToLabeledCSV(
        resources,
        fhirPathExpression,
        label,
        matchAllExtraConditions,
        *extraConditions,
      )

    Assert.assertEquals("ELDERLY", result)
  }

  @Test
  fun filterResourceList() {
    val fhirPathExpression =
      "Task.status = 'ready' or Task.status = 'cancelled' or  Task.status = 'failed'"
    val resources =
      listOf(
        Task().apply { status = TaskStatus.COMPLETED },
        Task().apply { status = TaskStatus.READY },
        Task().apply { status = TaskStatus.CANCELLED },
      )

    Assert.assertTrue(
      rulesEngineService
        .filterResources(
          resources = resources,
          conditionalFhirPathExpression = fhirPathExpression,
        )
        .size == 2,
    )
  }

  @Test
  fun fetchDescriptionFromReadyTasks() {
    val fhirPathExpression = "Task.status = 'ready'"
    val resources =
      listOf(
        Task().apply {
          status = TaskStatus.COMPLETED
          description = "minus"
        },
        Task().apply {
          status = TaskStatus.READY
          description = "plus"
        },
        Task().apply {
          status = TaskStatus.CANCELLED
          description = "multiply"
        },
        Task().apply {
          status = TaskStatus.COMPLETED
          description = "minus five"
        },
      )

    val filteredTask = rulesEngineService.filterResources(resources, fhirPathExpression)
    val descriptionList =
      rulesEngineService.mapResourcesToExtractedValues(filteredTask, "Task.description")
    Assert.assertTrue(descriptionList.first() == "plus")
  }

  @Test
  fun filterResourceListWithWrongExpression() {
    val fhirPathExpression = "Task.status = 'not ready'"
    val resources =
      listOf(
        Task().apply { status = TaskStatus.COMPLETED },
        Task().apply { status = TaskStatus.REQUESTED },
        Task().apply { status = TaskStatus.CANCELLED },
      )

    val results = rulesEngineService.filterResources(resources, fhirPathExpression)
    Assert.assertTrue(results.isEmpty())
  }

  @Test
  fun pickNamesOfPatientFromCertainAge() {
    val fhirPathExpression =
      "(Patient.birthDate <= today() - 2 'years') and (Patient.birthDate >= today() - 4 'years')"
    val resources =
      listOf(
        Patient().apply {
          birthDate = LocalDate.parse("2015-10-03").toDate()
          addName().apply { family = "alpha" }
        },
        Patient().apply {
          birthDate = LocalDate.parse("2017-10-03").toDate()
          addName().apply { family = "beta" }
        },
        Patient().apply {
          birthDate = LocalDate.parse("2018-10-03").toDate()
          addName().apply { family = "gamma" }
        },
        Patient().apply {
          birthDate = LocalDate.parse("2019-10-03").toDate()
          addName().apply { family = "rays" }
        },
        Patient().apply {
          birthDate = LocalDate.parse("2021-10-03").toDate()
          addName().apply { family = "light" }
        },
      )

    val patientsList = rulesEngineService.filterResources(resources, fhirPathExpression)
    val names =
      rulesEngineService.mapResourcesToExtractedValues(patientsList, "Patient.name.family")
    Assert.assertTrue(names.isNotEmpty())
  }

  @Test
  fun testLimitTo() {
    val source = mutableListOf("apple", "banana", "cherry", "date")
    val expected = mutableListOf("apple", "banana")
    Assert.assertTrue(expected == rulesEngineService.limitTo(source.toMutableList(), 2))
  }

  @Test
  fun testLimitToWithResources() {
    val source =
      mutableListOf(
        Condition().apply {
          id = "001"
          clinicalStatus = CodeableConcept(Coding("", "0001", "Pregnant"))
        },
        Condition().apply {
          id = "002"
          clinicalStatus = CodeableConcept(Coding("", "0002", "Family Planning"))
        },
      )
    val expected =
      mutableListOf(
        Condition().apply {
          id = "001"
          clinicalStatus = CodeableConcept(Coding("", "0001", "Pregnant"))
        },
      )

    val result = rulesEngineService.limitTo(source.toMutableList(), 1)
    Assert.assertTrue(result?.size == expected.size)
    with(result?.first() as Condition) { Assert.assertEquals(expected[0].id, id) }
  }

  @Test
  fun testLimitToWithNull() {
    val result = rulesEngineService.limitTo(null, 1)
    Assert.assertTrue(result.isEmpty())
  }

  @Test
  fun testLimitToWithNullSourceAndNUllLimit() {
    val result = rulesEngineService.limitTo(null, null)
    Assert.assertTrue(result.isEmpty())
  }

  @Test
  fun testLimitToWithZeroLimit() {
    val source =
      mutableListOf(
        Condition().apply {
          id = "001"
          clinicalStatus = CodeableConcept(Coding("", "0001", "Pregnant"))
        },
        Condition().apply {
          id = "002"
          clinicalStatus = CodeableConcept(Coding("", "0002", "Family Planning"))
        },
      )

    val result = rulesEngineService.limitTo(source.toMutableList(), 0)
    Assert.assertTrue(result.isEmpty())
  }

  @Test
  fun testJoinToStringWithNulls() {
    val source = mutableListOf("apple", null, "banana", "cherry", null, "date")
    val expected = "apple, banana, cherry, date"
    Assert.assertTrue(expected == rulesEngineService.joinToString(source))
  }

  @Test
  fun testJoinToStringWithoutNulls() {
    val source = mutableListOf("apple", "banana", "cherry", "date")
    val expected = "apple, banana, cherry, date"
    Assert.assertTrue(expected == rulesEngineService.joinToString(source.toMutableList()))
  }

  @Test
  fun testJoinToStringWithWhitespace() {
    val source = mutableListOf("apple", "banana", " cherry", "   ", "date ")
    val expected = "apple, banana, cherry, date "
    Assert.assertTrue(expected == rulesEngineService.joinToString(source.toMutableList()))
  }

  @Test
  fun testJoinToStringWithEmptyList() {
    val source = mutableListOf<String?>()
    val expected = ""
    Assert.assertTrue(expected == rulesEngineService.joinToString(source))
  }

  @Test
  fun testJoinToStringSpecialCharacters() {
    val source = mutableListOf("apple", "banana", " cherry", "  ", "date ", "CM-NTD Leprosy")
    val expected = "apple,banana,cherry,date ,CM-NTD Leprosy"
    Assert.assertTrue(
      expected ==
        rulesEngineService.joinToString(
          source.toMutableList(),
          "(?<=^|,)[\\s,]*(\\w[\\w\\s&-]*)(?=[\\s,]*$|,)",
          ",",
        ),
    )
  }

  @Test
  fun testJoinToStringSpecialCharactersWithDefinedSeparator() {
    val source = mutableListOf("apple", "banana", " cherry", "  ", "date ", "CM&NTD Leprosy")
    val expected = "apple:banana:cherry:date :CM&NTD Leprosy"
    Assert.assertTrue(
      expected ==
        rulesEngineService.joinToString(
          source.toMutableList(),
          "(?<=^|,)[\\s,]*(\\w[\\w\\s&-]*)(?=[\\s,]*$|,)",
          ":",
        ),
    )
  }

  @Test
  fun testJoinToStringWithExtraCommasAndSpaces() {
    val source =
      mutableListOf(
        "apple",
        null,
        " cherry",
        "date ",
        ",   ",
        " ,",
        ", ",
        ",     ",
        "  ,   ",
        "   ,     ",
        "   ,",
        ", ",
        ",     ",
        "     ,   ",
        "      , ",
      )
    val expected = "apple, cherry, date "
    Assert.assertTrue(expected == rulesEngineService.joinToString(source))
  }

  @Test
  fun pickCodesFromCertainConditions() {
    val resources =
      listOf(
        Condition().apply {
          id = "001"
          clinicalStatus = CodeableConcept(Coding("", "0001", "Pregnant"))
        },
        Condition().apply {
          id = "002"
          clinicalStatus = CodeableConcept(Coding("", "0002", "Family Planning"))
        },
      )
    val conditions =
      rulesEngineService.filterResources(
        resources,
        "Condition.clinicalStatus.coding.display = 'Pregnant'",
      )
    val conditionIds = rulesEngineService.mapResourcesToExtractedValues(conditions, "Condition.id")
    Assert.assertTrue(conditionIds.first() == "001")
  }

  @Test
  fun filterResourcesIsEmptyWhenEmptyExpression() {
    val result = rulesEngineService.filterResources(listOf(), "")
    Assert.assertEquals(result.size, 0)
  }

  @Test
  fun filterResourcesIsEmptyWhenEmptyResources() {
    val result = rulesEngineService.filterResources(listOf(), "something")
    Assert.assertEquals(result.size, 0)
  }

  @Test
  fun mapResourcesToExtractedValuesIsEmptyWhenEmptyExpression() {
    val result = rulesEngineService.mapResourcesToExtractedValues(listOf(), "")
    Assert.assertEquals(result.size, 0)
  }

  @Test
  fun mapResourcesToExtractedValuesIsEmptyWhenEmptyResources() {
    val result = rulesEngineService.mapResourcesToExtractedValues(listOf(), "something")
    Assert.assertEquals(result.size, 0)
  }

  @Test
  fun evaluateToBooleanReturnsFalseWhenResourcesNull() {
    val fhirPathExpression = ""

    Assert.assertFalse(rulesEngineService.evaluateToBoolean(null, fhirPathExpression, true))
    Assert.assertFalse(rulesEngineService.evaluateToBoolean(null, fhirPathExpression, false))
  }

  @Test
  fun evaluateToBooleanReturnsCorrectValueWhenMatchAllIsTrue() {
    val fhirPathExpression = "Patient.active"
    val patients =
      mutableListOf(
        Patient().setActive(true),
        Patient().setActive(true),
        Patient().setActive(true),
      )

    Assert.assertTrue(rulesEngineService.evaluateToBoolean(patients, fhirPathExpression, true))

    patients.add(Patient().setActive(false))
    Assert.assertFalse(rulesEngineService.evaluateToBoolean(patients, fhirPathExpression, true))
  }

  @Test
  fun evaluateToBooleanDefaultMatchAllIsFalse() {
    val fhirPathExpression = "Patient.active"
    val patients =
      mutableListOf(Patient().setActive(true), Patient().setActive(true), Patient().setActive(true))

    Assert.assertTrue(rulesEngineService.evaluateToBoolean(patients, fhirPathExpression, false))
  }

  @Test
  fun evaluateToBooleanReturnsCorrectValueWhenMatchAllIsFalse() {
    val fhirPathExpression = "Patient.active"
    val patients =
      mutableListOf(
        Patient().setActive(true),
        Patient().setActive(true),
        Patient().setActive(true),
      )

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
        "jexl exception, consider checking for null before usage: e.g var != null",
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

  @Test
  fun testGenerateRandomNumberOfLengthSix() {
    val generatedNumber = rulesEngineService.generateRandomSixDigitInt()
    Assert.assertEquals(generatedNumber.toString().length, 6)
  }

  @Test
  fun testFilterListShouldReturnMatchingResource() {
    val resources =
      listOf(
        Condition().apply {
          id = "1"
          clinicalStatus = CodeableConcept(Coding("", "0001", "pregnant"))
        },
        Condition().apply {
          id = "2"
          clinicalStatus = CodeableConcept(Coding("", "0002", "family-planning"))
        },
      )

    val result = rulesEngineService.filterResources(resources, "Condition.id = 2")

    Assert.assertTrue(result.size == 1)
    with(result.first() as Condition) {
      Assert.assertEquals("2", id)
      Assert.assertEquals("0002", clinicalStatus.codingFirstRep.code)
      Assert.assertEquals("family-planning", clinicalStatus.codingFirstRep.display)
    }
  }

  @Test
  fun testFilterListShouldReturnEmptyListWhenFieldNotFound() {
    val listOfResources =
      listOf(
        Condition().apply {
          id = "1"
          clinicalStatus = CodeableConcept(Coding("", "0001", "pregnant"))
        },
      )

    val result = rulesEngineService.filterResources(listOfResources, "unknown_field")

    Assert.assertTrue(result.isEmpty())
  }

  private fun populateFactsWithResources(vararg resource: Resource = emptyArray()) {
    val carePlanRelatedResource = mutableListOf(Faker.buildCarePlan())
    val patient = Faker.buildPatient()
    val patientRelatedResource = mutableListOf(patient)

    val facts = ReflectionHelpers.getField<Facts>(rulesFactory, "facts")
    facts.apply {
      put(carePlanRelatedResource[0].resourceType.name, carePlanRelatedResource)
      put(patientRelatedResource[0].resourceType.name, patientRelatedResource)
      resource.forEach { put(it.resourceType.name, it) }
    }
    ReflectionHelpers.setField(rulesFactory, "facts", facts)
  }

  @Test
  fun testPrettifyDateReturnXDaysAgo() {
    val weeksAgo = 2
    val inputDateString = LocalDate.now().minusWeeks(weeksAgo).toString()
    val expected = rulesFactory.RulesEngineService().prettifyDate(inputDateString)
    Assert.assertEquals("$weeksAgo weeks ago", expected)
  }

  @Test
  fun testPrettifyDateWithDateAsInput() {
    val inputDate = Date()
    val expected = rulesFactory.RulesEngineService().prettifyDate(inputDate)
    Assert.assertEquals("", expected)
  }

  @Test
  fun testDaysPassed() {
    val daysAgo = 14
    val inputDateString = LocalDate.now().minusDays(daysAgo).toString()
    val daysPassedResult =
      rulesFactory.RulesEngineService().daysPassed(inputDateString, SDF_YYYY_MM_DD)
    Assert.assertEquals("14", daysPassedResult)
  }

  @Test
  fun extractAge() {
    val dateFormatter: DateTimeFormatter? = DateTimeFormat.forPattern("yyyy-MM-dd")
    val period =
      Period(
        LocalDate.parse("2005-01-01", dateFormatter),
        LocalDate.parse(LocalDate.now().toString(), dateFormatter),
      )
    Assert.assertEquals(
      period.years.toString() + "y",
      rulesEngineService.extractAge(
        Patient()
          .setBirthDate(
            LocalDate.parse("2005-01-01").toDate(),
          ),
      ),
    )
  }

  @Test
  fun testExtractSharedPrefValuesReturnsPractitionerId() {
    val sharedPreferenceKey = "PRACTITIONER_ID"
    val expectedValue = "1234"
    every {
      configurationRegistry.sharedPreferencesHelper.read(
        sharedPreferenceKey,
        "",
      )
    } returns expectedValue
    val result = rulesEngineService.extractPractitionerInfoFromSharedPrefs(sharedPreferenceKey)

    verify { configurationRegistry.sharedPreferencesHelper.read(sharedPreferenceKey, "") }
    Assert.assertEquals(expectedValue, result)
  }

  @Test
  fun testExtractSharedPrefValuesReturnsCareTeam() {
    val sharedPreferenceKey = "CARE_TEAM"
    val expectedValue = "1234"
    every {
      configurationRegistry.sharedPreferencesHelper.read(
        sharedPreferenceKey,
        "",
      )
    } returns expectedValue
    val result = rulesEngineService.extractPractitionerInfoFromSharedPrefs(sharedPreferenceKey)

    verify { configurationRegistry.sharedPreferencesHelper.read(sharedPreferenceKey, "") }
    Assert.assertEquals(expectedValue, result)
  }

  @Test
  fun testExtractSharedPrefValuesReturnsOrganization() {
    val sharedPreferenceKey = "ORGANIZATION"
    val expectedValue = "1234"
    every {
      configurationRegistry.sharedPreferencesHelper.read(
        sharedPreferenceKey,
        "",
      )
    } returns expectedValue
    val result = rulesEngineService.extractPractitionerInfoFromSharedPrefs(sharedPreferenceKey)

    verify { configurationRegistry.sharedPreferencesHelper.read(sharedPreferenceKey, "") }
    Assert.assertEquals(expectedValue, result)
  }

  @Test
  fun testExtractSharedPrefValuesReturnsPractitionerLocation() {
    val sharedPreferenceKey = "PRACTITIONER_LOCATION"
    val expectedValue = "Demo Facility"
    every {
      configurationRegistry.sharedPreferencesHelper.read(
        sharedPreferenceKey,
        "",
      )
    } returns expectedValue
    val result = rulesEngineService.extractPractitionerInfoFromSharedPrefs(sharedPreferenceKey)

    verify { configurationRegistry.sharedPreferencesHelper.read(sharedPreferenceKey, "") }
    Assert.assertEquals(expectedValue, result)
  }

  @Test
  fun testExtractSharedPrefValuesReturnsPractitionerLocationId() {
    val sharedPreferenceKey = "PRACTITIONER_LOCATION_ID"
    val expectedValue = "ABCD1234"
    every {
      configurationRegistry.sharedPreferencesHelper.read(
        sharedPreferenceKey,
        "",
      )
    } returns expectedValue
    val result = rulesEngineService.extractPractitionerInfoFromSharedPrefs(sharedPreferenceKey)

    verify { configurationRegistry.sharedPreferencesHelper.read(sharedPreferenceKey, "") }
    Assert.assertEquals(expectedValue, result)
  }

  @Test
  fun testExtractSharedPrefValuesThrowsAnExceptionWhenKeyIsInvalid() {
    val sharedPreferenceKey = "INVALID_KEY"
    Assert.assertThrows(
      "key is not a member of practitioner keys: ",
      IllegalArgumentException::class.java,
    ) {
      rulesEngineService.extractPractitionerInfoFromSharedPrefs(sharedPreferenceKey)
    }
  }

  @Test
  fun testUpdateResourceWithNullResource() {
    rulesEngineService.updateResource(null, "List.entry[0].item.reference", "new-ref")
    verify { defaultRepository wasNot Called }
  }

  @Test
  fun testUpdateResourceWithNullPath() {
    val resource = ListResource().apply { id = "list1" }
    rulesEngineService.updateResource(resource, null, "new-value")
    verify { defaultRepository wasNot Called }
  }

  @Test
  fun testUpdateResourceWithEmptyPath() {
    val resource = ListResource().apply { id = "list1" }
    rulesEngineService.updateResource(resource, "", "new-value")
    verify { defaultRepository wasNot Called }
  }

  @Test
  fun testUpdateResourceWithValidResourceAndPathAndPurgeAffectedResourcesIsTrue() {
    val resource =
      ListResource().apply {
        id = "list1"
        addEntry().apply { item.apply { reference = "old-ref" } }
      }

    runBlocking {
      coEvery { defaultRepository.purge(any<Resource>(), any()) } returns Unit

      rulesEngineService.updateResource(
        resource = resource,
        path = "List.entry[0].item.reference",
        value = "Group/new-ref",
        purgeAffectedResources = true,
        createLocalChangeEntitiesAfterPurge = true,
      )

      coVerify {
        defaultRepository.purge(
          withArg {
            Assert.assertEquals("Group/new-ref", (it as ListResource).entry[0].item.reference)
          },
          any(),
        )
      }

      coVerify {
        defaultRepository.addOrUpdate(
          any(),
          withArg {
            Assert.assertEquals("Group/new-ref", (it as ListResource).entry[0].item.reference)
          },
        )
      }
    }
  }

  @Test
  fun testUpdateResourceWithValidResourceAndPathAndPurgeAffectedResourcesIsTrueAndPathStartsWithDollarSign() {
    val resource =
      ListResource().apply {
        id = "list1"
        addEntry().apply { item.apply { reference = "old-ref" } }
      }

    runBlocking {
      coEvery { defaultRepository.purge(any<Resource>(), any()) } returns Unit

      rulesEngineService.updateResource(
        resource = resource,
        path = "$.entry[0].item.reference",
        value = "Group/new-ref",
        purgeAffectedResources = true,
        createLocalChangeEntitiesAfterPurge = true,
      )

      coVerify {
        defaultRepository.purge(
          withArg {
            Assert.assertEquals("Group/new-ref", (it as ListResource).entry[0].item.reference)
          },
          any(),
        )
      }

      coVerify {
        defaultRepository.addOrUpdate(
          any(),
          withArg {
            Assert.assertEquals("Group/new-ref", (it as ListResource).entry[0].item.reference)
          },
        )
      }
    }
  }

  @Test
  fun testUpdateResourceWithValidResourceAndPathAndPurgeAffectedResourcesAndCreateLocalChangeEntitiesAfterPurgeAreTrue() {
    val resource =
      ListResource().apply {
        id = "list1"
        addEntry().apply { item.apply { reference = "old-ref" } }
      }

    runBlocking {
      coEvery { defaultRepository.purge(any<Resource>(), any()) } returns Unit
      coEvery { defaultRepository.addOrUpdate(any(), any()) } returns Unit

      rulesEngineService.updateResource(
        resource = resource,
        path = "List.entry[0].item.reference",
        value = "Group/new-ref",
        purgeAffectedResources = true,
        createLocalChangeEntitiesAfterPurge = true,
      )

      coVerify {
        defaultRepository.purge(
          withArg {
            Assert.assertEquals("Group/new-ref", (it as ListResource).entry[0].item.reference)
          },
          any(),
        )
      }

      coVerify {
        defaultRepository.addOrUpdate(
          any(),
          withArg {
            Assert.assertEquals("Group/new-ref", (it as ListResource).entry[0].item.reference)
          },
        )
      }
    }
  }

  @Test
  fun testUpdateResourceWithValidResourceAndPathAndPurgeAffectedResourcesIsFalseAndCreateLocalChangeEntitiesAfterPurgeIsTrue() {
    val resource =
      ListResource().apply {
        id = "list1"
        addEntry().apply { item.apply { reference = "old-ref" } }
      }

    runBlocking {
      coEvery { defaultRepository.addOrUpdate(any(), any()) } returns Unit

      rulesEngineService.updateResource(
        resource = resource,
        path = "List.entry[0].item.reference",
        value = "Group/new-ref",
        purgeAffectedResources = true,
        createLocalChangeEntitiesAfterPurge = true,
      )

      coVerify {
        defaultRepository.addOrUpdate(
          any(),
          withArg {
            Assert.assertEquals("Group/new-ref", (it as ListResource).entry[0].item.reference)
          },
        )
      }
    }
  }

  @Test
  fun testFilterResourcesByJsonPathWithNullResources() {
    val results =
      rulesEngineService.filterResourcesByJsonPath(null, "$.resourceType", "STRING", "Group", 0)
    Assert.assertNull(results)
  }

  @Test
  fun testFilterResourcesByJsonPathWithBlankResources() {
    val results =
      rulesEngineService.filterResourcesByJsonPath(listOf(), "$.resourceType", "STRING", "Group", 0)
    Assert.assertNull(results)
  }

  @Test
  fun testFilterResourcesByJsonPathWithBlankJsonPathExpression() {
    val results =
      rulesEngineService.filterResourcesByJsonPath(getListOfResource(), "", "STRING", "Group", 0)
    Assert.assertNull(results)
  }

  @Test
  fun testFilterResourcesByJsonPathWithInvalidJsonPathExpression() {
    val results =
      rulesEngineService.filterResourcesByJsonPath(
        getListOfResource(),
        "$.date",
        "STRING",
        "Group",
        0,
      )
    Assert.assertNull(results)
  }

  @Test
  fun testFilterResourcesByJsonPathWithInvalidDataType() {
    val results =
      rulesEngineService.filterResourcesByJsonPath(
        getListOfResource(),
        "$.resourceType",
        "code",
        "Group",
        0,
      )
    Assert.assertEquals(0, results?.size)
  }

  @Test
  fun testFilterResourcesByJsonPathFieldWithValidResourcesAndJsonPathExpressionAndDataTypeAndCompareToResultAndNonExistentValue() {
    val results =
      rulesEngineService.filterResourcesByJsonPath(
        getListOfResource(),
        "$.resourceType",
        "STRING",
        "Patient",
        0,
      )

    Assert.assertEquals(0, results?.size)
  }

  @Test
  fun testFilterResourcesByJsonPathFieldWithValidResourcesAndJsonPathExpressionAndDataTypeAndValueAndCompareToResult() {
    val results =
      rulesEngineService.filterResourcesByJsonPath(
        getListOfResource(),
        "$.resourceType",
        "STRING",
        "Group",
        0,
      )

    Assert.assertEquals(2, results?.size)
    with(results?.first() as Resource) {
      Assert.assertEquals("group-id-1", id)
      Assert.assertEquals("Group", resourceType.name)
    }
  }

  @Test
  fun mapResourcesToExtractedValuesReturnsCorrectlyFormattedString() {
    val patientsList =
      listOf(
        Patient().apply {
          birthDate = LocalDate.parse("2015-10-03").toDate()
          addName().apply { family = "alpha" }
        },
        Patient().apply {
          birthDate = LocalDate.parse("2017-10-03").toDate()
          addName().apply { family = "beta" }
        },
        Patient().apply {
          birthDate = LocalDate.parse("2018-10-03").toDate()
          addName().apply { family = "gamma" }
        },
      )

    val names =
      rulesEngineService.mapResourcesToExtractedValues(patientsList, "Patient.name.family", " | ")
    Assert.assertEquals("alpha | beta | gamma", names)
  }

  @Test
  fun mapResourcesToExtractedValuesReturnsEmptyStringWhenFhirPathExpressionIsEmpty() {
    val patientsList =
      listOf(
        Patient().apply {
          birthDate = LocalDate.parse("2015-10-03").toDate()
          addName().apply { family = "alpha" }
        },
        Patient().apply {
          birthDate = LocalDate.parse("2017-10-03").toDate()
          addName().apply { family = "beta" }
        },
        Patient().apply {
          birthDate = LocalDate.parse("2018-10-03").toDate()
          addName().apply { family = "gamma" }
        },
      )

    val names = rulesEngineService.mapResourcesToExtractedValues(patientsList, "", " | ")
    Assert.assertEquals("", names)
  }

  private fun getListOfResource(): List<Resource> {
    return listOf(
      Group().apply { id = "group-id-1" },
      Location().apply { id = "location-id-1" },
      ListResource().apply {
        id = "list-id-1"
        addEntry().apply { item.apply { reference = "Group/group-id-1" } }
      },
      Group().apply { id = "group-id-2" },
    )
  }
}
