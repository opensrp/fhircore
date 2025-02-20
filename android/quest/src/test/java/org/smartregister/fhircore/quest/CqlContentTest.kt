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

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.knowledge.KnowledgeManager
import com.google.android.fhir.workflow.FhirOperator
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.io.File
import java.io.InputStream
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Library
import org.hl7.fhir.r4.model.MetadataResource
import org.hl7.fhir.r4.model.Parameters
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.extension.encodeResourceToString
import org.smartregister.fhircore.engine.util.extension.valueToString
import org.smartregister.fhircore.quest.robolectric.RobolectricTest
import org.smartregister.fhircore.quest.sdk.CqlBuilder

@HiltAndroidTest
@OptIn(ExperimentalCoroutinesApi::class)
class CqlContentTest : RobolectricTest() {

  @get:Rule var hiltRule = HiltAndroidRule(this)

  @Inject lateinit var knowledgeManager: KnowledgeManager

  @Inject lateinit var fhirOperator: FhirOperator

  @Inject lateinit var fhirEngine: FhirEngine

  @Inject lateinit var dispatcherProvider: DispatcherProvider

  private val context: Context = ApplicationProvider.getApplicationContext()

  @Before
  fun setUp() {
    hiltRule.inject()
  }

  @Test
  fun runCqlLibraryTestForPqMedication() =
    runTest(context = UnconfinedTestDispatcher()) {
      val resourceDir = "cql/pq-medication"
      val cql = "$resourceDir/cql.txt".readFile()

      val cqlLibrary = buildCqlLibrary(cql)

      val dataBundle =
        loadTestResultsSampleData().apply {
          // output of test results cql is also added to input of this cql
          "cql/test-results/sample"
            .readDir()
            .map { it.parseSampleResource() as Resource }
            .forEach { addEntry().apply { resource = it } }
        }

      createTestData(dataBundle, cqlLibrary)

      val result =
        fhirOperator.evaluateLibrary(
          cqlLibrary.url,
          dataBundle.entry.find { it.resource.resourceType == ResourceType.Patient }!!.resource.id,
          null,
        ) as Parameters

      printResult(result)

      assertOutput(
        "$resourceDir/output_medication_request.json",
        result,
        ResourceType.MedicationRequest,
      )
    }

  @Test
  fun runCqlLibraryTestForTestResults() =
    runTest(context = UnconfinedTestDispatcher()) {
      val resourceDir = "cql/test-results"
      val cql = "$resourceDir/cql.txt".readFile()

      val cqlLibrary = buildCqlLibrary(cql)

      val dataBundle = loadTestResultsSampleData()

      createTestData(dataBundle, cqlLibrary)

      val result =
        fhirOperator.evaluateLibrary(
          cqlLibrary.url,
          dataBundle.entry.find { it.resource.resourceType == ResourceType.Patient }!!.resource.id,
          null,
          null,
          null,
        ) as Parameters

      printResult(result)

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
    }

  @Test
  fun runCqlLibraryTestForControlTest() =
    runTest(context = UnconfinedTestDispatcher()) {
      val resourceDir = "cql/control-test"
      val cql = "$resourceDir/cql.txt".readFile()

      val cqlLibrary = buildCqlLibrary(cql)

      val dataBundle =
        loadTestResultsSampleData().apply {
          addEntry().apply {
            // questionnaire-response of test results is input of this cql
            resource =
              "test-results-questionnaire/questionnaire-response.json".parseSampleResourceFromFile()
                as Resource
          }
        }

      createTestData(dataBundle, cqlLibrary)

      val result =
        fhirOperator.evaluateLibrary(
          cqlLibrary.url,
          dataBundle.entry.find { it.resource.resourceType == ResourceType.Patient }!!.resource.id,
          null,
        ) as Parameters

      printResult(result)

      Assert.assertTrue(
        result.getParameterValues("OUTPUT").first().valueToString() == "Correct Result",
      )
      Assert.assertEquals(
        result.getParameterValues("OUTPUT").elementAt(1).valueToString(),
        "\nDetails:\n" +
          "Value (3.0) is in Normal G6PD Range 0-3\n" +
          "Value (11.0) is in Normal Haemoglobin Range 8-12",
      )
    }

  private fun compileAndBuild(cqlInputStream: InputStream): Library {
    val cqlText = CqlBuilder.load(cqlInputStream)
    return CqlBuilder.compile(cqlText).let {
      CqlBuilder.assembleFhirLib(
        cqlText,
        it.toJson(),
        it.toXml(),
        it.toELM().identifier.id,
        it.toELM().identifier.version,
      )
    }
  }

  private fun buildCqlLibrary(cql: String): Library = compileAndBuild(cql.byteInputStream())

  private fun loadTestResultsSampleData(): Bundle {
    return Bundle().apply {
      // output of test results extraction is input of this cql
      "test-results-questionnaire/sample"
        .readDir()
        .map { it.parseSampleResource() as Resource }
        .forEach { addEntry().apply { resource = it } }
    }
  }

  private suspend fun createTestData(dataBundle: Bundle, cqlLibrary: Library) {
    dataBundle.entry.forEach { fhirEngine.create(it.resource) }
    knowledgeManager.index(
      writeToFile(cqlLibrary.apply { url = url.substring(0, url.indexOf('|')) }),
    )
  }

  private fun writeToFile(resource: Resource): File {
    val fileName =
      if (resource is MetadataResource && resource.name != null) {
        resource.name
      } else {
        resource.idElement.idPart
      }
    return File(context.filesDir, fileName).apply { writeText(resource.encodeResourceToString()) }
  }

  private fun printResult(result: Parameters) {
    result.parameter.forEach {
      println(
        it.name +
          " -> " +
          if (it.hasResource()) {
            it.resource.encodeResourceToString()
          } else if (it.hasValue()) it.value.valueToString() else " FOUND NULL",
      )
    }
  }

  private fun assertOutput(
    resource: String,
    cqlResult: Parameters,
    type: ResourceType,
  ) {
    val outputs =
      cqlResult.parameter.map {
        it.name to
          if (it.hasResource()) {
            it.resource.encodeResourceToString()
          } else {
            it.valueToString()
          }
      }

    val expectedResource = resource.parseSampleResourceFromFile().convertToString(true)
    val cqlResultStr =
      outputs
        .find {
          it.first.startsWith("OUTPUT") && it.second.contains("\"resourceType\":\"${type.name}")
        }!!
        .second
        .replaceTimePart()

    println(cqlResultStr)
    println(expectedResource)

    Assert.assertEquals(expectedResource, cqlResultStr)
  }
}
