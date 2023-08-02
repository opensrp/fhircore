/*
 * Copyright 2021-2023 Ona Systems, Inc
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
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.logicalId
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import kotlinx.coroutines.runBlocking
import org.cqframework.cql.cql2elm.CqlTranslator
import org.cqframework.cql.cql2elm.CqlTranslatorOptions
import org.cqframework.cql.cql2elm.LibraryManager
import org.cqframework.cql.cql2elm.quick.FhirLibrarySourceProvider
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Library
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.cql.LibraryEvaluator
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.rulesengine.ConfigRulesExecutor
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

@HiltAndroidTest
class CqlContentTest : RobolectricTest() {

  @get:Rule(order = 0) var hiltRule = HiltAndroidRule(this)
  private val fhirContext: FhirContext = FhirContext.forCached(FhirVersionEnum.R4)
  private val parser = fhirContext.newJsonParser()!!
  private val configurationRegistry = Faker.buildTestConfigurationRegistry()
  private val configService: ConfigService = mockk()
  private val configRulesExecutor: ConfigRulesExecutor = mockk()
  private lateinit var evaluator: LibraryEvaluator
  private lateinit var defaultRepository: DefaultRepository
  private val fhirEngine = mockk<FhirEngine>()

  @Before
  fun setUp() {
    hiltRule.inject()
    defaultRepository =
      spyk(
        DefaultRepository(
          fhirEngine = fhirEngine,
          dispatcherProvider = coroutineTestRule.testDispatcherProvider,
          sharedPreferencesHelper = mockk(),
          configurationRegistry = configurationRegistry,
          configService = configService,
          configRulesExecutor = configRulesExecutor,
        ),
      )
    evaluator = LibraryEvaluator(defaultRepository).apply { initialize() }
  }

  @Test
  fun runCqlLibraryTestForPqMedication() {
    val resourceDir = "cql/pq-medication"
    val cql = "$resourceDir/cql.txt".readFile()

    val cqlElm = toJsonElm(cql).readStringToBase64Encoded()
    val cqlLibrary =
      parser.parseResource(
        "$resourceDir/library.json".readFile().replace("#library-elm.json", cqlElm),
      ) as Library

    println(cqlLibrary.convertToString(false) as String)

    val fhirHelpersLibrary = "cql-common/helper.json".parseSampleResourceFromFile() as Library

    val patient =
      "patient-registration-questionnaire/sample/patient.json".parseSampleResourceFromFile()
        as Patient
    val dataBundle =
      Bundle().apply {
        // output of test results extraction is input of this cql
        "test-results-questionnaire/sample"
          .readDir()
          .map { it.parseSampleResource() as Resource }
          .forEach { addEntry().apply { resource = it } }

        // output of test results cql is also added to input of this cql
        "cql/test-results/sample"
          .readDir()
          .map { it.parseSampleResource() as Resource }
          .forEach { addEntry().apply { resource = it } }
      }

    coEvery { fhirEngine.get(ResourceType.Library, cqlLibrary.logicalId) } returns cqlLibrary
    coEvery { fhirEngine.get(ResourceType.Library, fhirHelpersLibrary.logicalId) } returns
      fhirHelpersLibrary
    coEvery { defaultRepository.create(any(), any()) } returns emptyList()
    coEvery { defaultRepository.search(any()) } returns listOf()

    val result = runBlocking {
      evaluator.runCqlLibrary(
        libraryId = cqlLibrary.logicalId,
        patient = patient,
        data =
          dataBundle.apply {
            this.entry.removeIf { it.resource.resourceType == ResourceType.Patient }
          },
        outputLog = true,
      )
    }

    assertOutput(
      "$resourceDir/output_medication_request.json",
      result,
      ResourceType.MedicationRequest,
    )

    coVerify { defaultRepository.create(any(), any()) }
  }

  @Test
  fun runCqlLibraryTestForTestResults() {
    val resourceDir = "cql/test-results"
    val cql = "$resourceDir/cql.txt".readFile()

    val cqlElm = toJsonElm(cql).readStringToBase64Encoded()
    val cqlLibrary =
      parser.parseResource(
        "$resourceDir/library.json".readFile().replace("#library-elm.json", cqlElm),
      ) as Library

    println(cqlLibrary.convertToString(false) as String)

    val fhirHelpersLibrary = "cql-common/helper.json".parseSampleResourceFromFile() as Library

    val patient =
      "patient-registration-questionnaire/sample/patient.json".parseSampleResourceFromFile()
        as Patient
    val dataBundle =
      Bundle().apply {
        // output of test results extraction is input of this cql
        "test-results-questionnaire/sample"
          .readDir()
          .map { it.parseSampleResource() as Resource }
          .forEach { addEntry().apply { resource = it } }
      }

    coEvery { fhirEngine.get(ResourceType.Library, cqlLibrary.logicalId) } returns cqlLibrary
    coEvery { fhirEngine.get(ResourceType.Library, fhirHelpersLibrary.logicalId) } returns
      fhirHelpersLibrary
    coEvery { defaultRepository.create(any(), any()) } returns emptyList()
    coEvery { defaultRepository.search(any()) } returns listOf()

    val result = runBlocking {
      evaluator.runCqlLibrary(
        libraryId = cqlLibrary.logicalId,
        patient = patient,
        data =
          dataBundle.apply {
            this.entry.removeIf { it.resource.resourceType == ResourceType.Patient }
          },
        outputLog = true,
      )
    }

    assertOutput("$resourceDir/sample/output_condition.json", result, ResourceType.Condition)
    assertOutput(
      "$resourceDir/sample/output_service_request.json",
      result,
      ResourceType.ServiceRequest,
    )
    assertOutput(
      "$resourceDir/sample/output_diagnostic_report.json",
      result,
      ResourceType.DiagnosticReport,
    )

    coVerify(exactly = 3) { defaultRepository.create(any(), any()) }
  }

  @Ignore
  @Test
  fun runCqlLibraryTestForControlTest() {
    val resourceDir = "cql/control-test"
    val cql = "$resourceDir/cql.txt".readFile()

    val cqlElm = toJsonElm(cql).readStringToBase64Encoded()
    val cqlLibrary =
      parser.parseResource(
        "$resourceDir/library.json".readFile().replace("#library-elm.json", cqlElm),
      ) as Library

    println(cqlLibrary.convertToString(false) as String)

    val fhirHelpersLibrary = "cql-common/helper.json".parseSampleResourceFromFile() as Library

    val dataBundle =
      Bundle().apply {
        addEntry().apply {
          // questionnaire-response of test results is input of this cql
          resource =
            "test-results-questionnaire/questionnaire-response.json".parseSampleResourceFromFile()
              as Resource
        }
      }

    coEvery { fhirEngine.get(ResourceType.Library, cqlLibrary.logicalId) } returns cqlLibrary
    coEvery { fhirEngine.get(ResourceType.Library, fhirHelpersLibrary.logicalId) } returns
      fhirHelpersLibrary
    coEvery { defaultRepository.create(any(), any()) } returns emptyList()
    coEvery { configService.provideResourceTags(any()) } returns listOf()

    val result = runBlocking {
      evaluator.runCqlLibrary(libraryId = cqlLibrary.logicalId, patient = null, data = dataBundle)
    }

    println(result)

    Assert.assertTrue(result.contains("OUTPUT -> Correct Result"))
    Assert.assertTrue(
      result.contains(
        "OUTPUT -> \nDetails:\n" +
          "Value (3.0) is in Normal G6PD Range 0-3\n" +
          "Value (11.0) is in Normal Haemoglobin Range 8-12",
      ),
    )

    val observationSlot = slot<Observation>()
    val booleanSlot = slot<Boolean>()
    coVerify { defaultRepository.create(capture(booleanSlot), capture(observationSlot)) }

    Assert.assertEquals(
      "QuestionnaireResponse/TEST_QUESTIONNAIRE_RESPONSE",
      observationSlot.captured.focusFirstRep.reference,
    )
    Assert.assertEquals(
      "Correct Result",
      observationSlot.captured.valueCodeableConcept.codingFirstRep.display,
    )
    Assert.assertEquals("Device Operation", observationSlot.captured.code.codingFirstRep.display)
  }

  private fun toJsonElm(cql: String): String {
    val libraryManager = LibraryManager(evaluator.modelManager)
    libraryManager.librarySourceLoader.registerProvider(FhirLibrarySourceProvider())

    val translator: CqlTranslator =
      CqlTranslator.fromText(
        cql,
        evaluator.modelManager,
        libraryManager,
        *CqlTranslatorOptions.defaultOptions().options.toTypedArray(),
      )

    return translator.toJson().also { println(it.replace("\n", "").replace("   ", "")) }
  }

  private fun assertOutput(resource: String, cqlResult: List<String>, type: ResourceType) {
    println(cqlResult)

    val expectedResource = resource.parseSampleResourceFromFile().convertToString(true)
    val cqlResultStr =
      cqlResult
        .find { it.startsWith("OUTPUT") && it.contains("\"resourceType\":\"$type\"") }!!
        .replaceTimePart()

    println(cqlResultStr)
    println(expectedResource as String)

    Assert.assertTrue(cqlResultStr.contains("OUTPUT -> $expectedResource"))
  }
}
