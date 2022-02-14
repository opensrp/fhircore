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

package org.smartregister.fhircore.quest

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.logicalId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.spyk
import kotlinx.coroutines.runBlocking
import org.cqframework.cql.cql2elm.CqlTranslator
import org.cqframework.cql.cql2elm.FhirLibrarySourceProvider
import org.cqframework.cql.cql2elm.LibraryManager
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Library
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert
import org.junit.Test
import org.smartregister.fhircore.engine.cql.LibraryEvaluator
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

class CqlContentTest : RobolectricTest() {
  val fhirContext = FhirContext.forCached(FhirVersionEnum.R4)
  val parser = fhirContext.newJsonParser()!!
  val evaluator = LibraryEvaluator().apply { initialize() }

  @Test
  fun runCqlLibraryTestForPqMedication() {
    val resourceDir = "cql/pq-medication"
    val cql = "$resourceDir/cql.txt".readFile()

    val cqlElm = toJsonElm(cql).readStringToBase64Encoded()
    val cqlLibrary =
      parser.parseResource(
        "$resourceDir/library.json".readFile().replace("#library-elm.json", cqlElm)
      ) as
        Library

    println(cqlLibrary.convertToString(false))

    val fhirHelpersLibrary = "cql-common/helper.json".parseSampleResource() as Library
    val fhirModelLibrary = "cql-common/fhir-model.json".parseSampleResource() as Library

    val dataBundle = "$resourceDir/data.json".parseSampleResource() as Bundle

    val patient =
      dataBundle.entry.first { it.resource.resourceType == ResourceType.Patient }.resource as
        Patient

    val fhirEngine = mockk<FhirEngine>()
    val defaultRepository = spyk(DefaultRepository(fhirEngine, DefaultDispatcherProvider()))

    coEvery { fhirEngine.load(Library::class.java, cqlLibrary.logicalId) } returns cqlLibrary
    coEvery { fhirEngine.load(Library::class.java, fhirHelpersLibrary.logicalId) } returns
      fhirHelpersLibrary
    coEvery { fhirEngine.load(Library::class.java, fhirModelLibrary.logicalId) } returns
      fhirModelLibrary
    coEvery { defaultRepository.save(any()) } just runs
    coEvery { defaultRepository.search(any()) } returns listOf()

    val result = runBlocking {
      evaluator.runCqlLibrary(
        cqlLibrary.logicalId,
        patient,
        dataBundle.apply {
          this.entry.removeIf { it.resource.resourceType == ResourceType.Patient }
        },
        defaultRepository,
        true
      )
    }

    assertOutput(
      "$resourceDir/output_medication_request.json",
      result,
      ResourceType.MedicationRequest
    )

    coVerify { defaultRepository.save(any()) }
  }

  @Test
  fun runCqlLibraryTestForTestResults() {
    val resourceDir = "cql/test-results"
    val cql = "$resourceDir/cql.txt".readFile()

    val cqlElm = toJsonElm(cql).readStringToBase64Encoded()
    val cqlLibrary =
      parser.parseResource(
        "$resourceDir/library.json".readFile().replace("#library-elm.json", cqlElm)
      ) as
        Library

    println(cqlLibrary.convertToString(false))

    val fhirHelpersLibrary = "cql-common/helper.json".parseSampleResource() as Library
    val fhirModelLibrary = "cql-common/fhir-model.json".parseSampleResource() as Library

    val dataBundle = "$resourceDir/data.json".parseSampleResource() as Bundle

    val patient =
      dataBundle.entry.first { it.resource.resourceType == ResourceType.Patient }.resource as
        Patient

    val fhirEngine = mockk<FhirEngine>()
    val defaultRepository = spyk(DefaultRepository(fhirEngine, DefaultDispatcherProvider()))

    coEvery { fhirEngine.load(Library::class.java, cqlLibrary.logicalId) } returns cqlLibrary
    coEvery { fhirEngine.load(Library::class.java, fhirHelpersLibrary.logicalId) } returns
      fhirHelpersLibrary
    coEvery { fhirEngine.load(Library::class.java, fhirModelLibrary.logicalId) } returns
      fhirModelLibrary
    coEvery { defaultRepository.save(any()) } just runs
    coEvery { defaultRepository.search(any()) } returns listOf()

    val result = runBlocking {
      evaluator.runCqlLibrary(
        cqlLibrary.logicalId,
        patient,
        dataBundle.apply {
          this.entry.removeIf { it.resource.resourceType == ResourceType.Patient }
        },
        defaultRepository,
        true
      )
    }

    assertOutput("$resourceDir/output_condition.json", result, ResourceType.Condition)
    assertOutput("$resourceDir/output_service_request.json", result, ResourceType.ServiceRequest)
    assertOutput(
      "$resourceDir/output_diagnostic_report.json",
      result,
      ResourceType.DiagnosticReport
    )

    coVerify(exactly = 3) { defaultRepository.save(any()) }
  }

  @Test
  fun runCqlLibraryTestForControlTest() {
    val resourceDir = "cql/control-test"
    val cql = "$resourceDir/cql.txt".readFile()

    val cqlElm = toJsonElm(cql).readStringToBase64Encoded()
    val cqlLibrary =
      parser.parseResource(
        "$resourceDir/library.json".readFile().replace("#library-elm.json", cqlElm)
      ) as
        Library

    println(cqlLibrary.convertToString(false))

    val fhirHelpersLibrary = "cql-common/helper.json".parseSampleResource() as Library
    val fhirModelLibrary = "cql-common/fhir-model.json".parseSampleResource() as Library

    val dataBundle = "$resourceDir/data.json".parseSampleResource() as Bundle

    val fhirEngine = mockk<FhirEngine>()
    val defaultRepository = spyk(DefaultRepository(fhirEngine, DefaultDispatcherProvider()))

    coEvery { fhirEngine.load(Library::class.java, cqlLibrary.logicalId) } returns cqlLibrary
    coEvery { fhirEngine.load(Library::class.java, fhirHelpersLibrary.logicalId) } returns
      fhirHelpersLibrary
    coEvery { fhirEngine.load(Library::class.java, fhirModelLibrary.logicalId) } returns
      fhirModelLibrary
    coEvery { defaultRepository.save(any()) } just runs

    val result = runBlocking {
      evaluator.runCqlLibrary(cqlLibrary.logicalId, null, dataBundle, defaultRepository)
    }

    println(result)

    Assert.assertTrue(result.contains("OUTPUT -> Correct Result"))
    Assert.assertTrue(
      result.contains(
        "OUTPUT -> \nDetails:\n" +
          "Value (3.0) is in Normal G6PD Range 0-3\n" +
          "Value (12.0) is in Normal Haemoglobin Range 8-12"
      )
    )

    val observationSlot = slot<Observation>()
    coVerify { defaultRepository.save(capture(observationSlot)) }

    Assert.assertEquals(
      "QuestionnaireResponse/1212121",
      observationSlot.captured.focusFirstRep.reference
    )
    Assert.assertEquals(
      "Correct Result",
      observationSlot.captured.valueCodeableConcept.codingFirstRep.display
    )
    Assert.assertEquals("Device Operation", observationSlot.captured.code.codingFirstRep.display)
    Assert.assertEquals("Patient/P1", observationSlot.captured.subject.reference)
  }

  private fun toJsonElm(cql: String): String {
    val libraryManager = LibraryManager(evaluator.modelManager)
    libraryManager.librarySourceLoader.registerProvider(FhirLibrarySourceProvider())

    val translator: CqlTranslator =
      CqlTranslator.fromText(cql, evaluator.modelManager, libraryManager)

    return translator.toJxson().also { println(it.replace("\n", "").replace("   ", "")) }
  }

  private fun assertOutput(resource: String, cqlResult: List<String>, type: ResourceType) {
    println(cqlResult)

    val expectedResource = resource.parseSampleResource().convertToString(true)
    val cqlResultStr =
      cqlResult.find { it.startsWith("OUTPUT") && it.contains("\"resourceType\":\"$type\"") }!!
        .replaceTimePart()

    println(cqlResultStr)
    println(expectedResource)

    Assert.assertTrue(cqlResultStr.contains("OUTPUT -> $expectedResource"))
  }
}
