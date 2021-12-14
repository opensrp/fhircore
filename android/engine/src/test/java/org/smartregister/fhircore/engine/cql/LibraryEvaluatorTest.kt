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

package org.smartregister.fhircore.engine.cql

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import com.google.common.collect.Lists
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import org.hl7.fhir.instance.model.api.IBaseBundle
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.Library
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.smartregister.fhircore.engine.util.FileUtil
import timber.log.Timber

class LibraryEvaluatorTest {
  var evaluator: LibraryEvaluator? = null
  var libraryData = ""
  var helperData = ""
  var valueSetData = ""
  var testData = ""
  var result = ""
  var evaluatorId = "ANCRecommendationA2"
  var context = "Patient"
  var contextLabel = "mom-with-anemia"

  @Before
  fun setUp() {
    try {
      libraryData = FileUtil.readJsonFile("test/resources/cql/libraryevaluator/library.json")
      helperData = FileUtil.readJsonFile("test/resources/cql/libraryevaluator/helper.json")
      valueSetData = FileUtil.readJsonFile("test/resources/cql/libraryevaluator/valueSet.json")
      testData = FileUtil.readJsonFile("test/resources/cql/libraryevaluator/patient.json")
      result = FileUtil.readJsonFile("test/resources/cql/libraryevaluator/result.json")
      evaluator = LibraryEvaluator()
    } catch (e: IOException) {
      Timber.e(e, e.message)
    }
  }

  @Test
  fun runCqlTest() {
    val fhirContext = FhirContext.forCached(FhirVersionEnum.R4)
    val parser = fhirContext.newJsonParser()!!
    val libraryStream: InputStream = ByteArrayInputStream(libraryData.toByteArray())
    val fhirHelpersStream: InputStream = ByteArrayInputStream(helperData.toByteArray())
    val library = parser.parseResource(libraryStream)
    val fhirHelpersLibrary = parser.parseResource(fhirHelpersStream)
    val patientResources: List<IBaseResource> = Lists.newArrayList(library, fhirHelpersLibrary)

    val valueSetStream: InputStream = ByteArrayInputStream(valueSetData.toByteArray())
    val valueSetBundle = parser.parseResource(valueSetStream) as IBaseBundle

    val dataStream: InputStream = ByteArrayInputStream(testData.toByteArray())
    val dataBundle = parser.parseResource(dataStream) as IBaseBundle

    val auxResult =
      evaluator!!.runCql(
        patientResources,
        valueSetBundle,
        dataBundle,
        fhirContext,
        evaluatorId,
        context,
        contextLabel
      )
    Assert.assertEquals(result, auxResult)
  }

  @Test
  fun createBundleTestForG6pd() {
    val result = evaluator!!.createBundle(listOf(Patient(), Observation(), Condition()))

    Assert.assertEquals(ResourceType.Patient, result.entry[0].resource.resourceType)
    Assert.assertEquals(ResourceType.Observation, result.entry[1].resource.resourceType)
    Assert.assertEquals(ResourceType.Condition, result.entry[2].resource.resourceType)
  }

  @Test
  fun runCqlLibraryTestForG6pd() {
    val fhirContext = FhirContext.forCached(FhirVersionEnum.R4)
    val parser = fhirContext.newJsonParser()!!
    val cqlLibrary =
      parser.parseResource(
        FileUtil.readJsonFile("test/resources/cql/g6pdlibraryevaluator/library.json")
      ) as
        Library
    val fhirHelpersLibrary =
      parser.parseResource(
        FileUtil.readJsonFile("test/resources/cql/g6pdlibraryevaluator/helper.json")
      ) as
        Library

    val dataBundle =
      parser.parseResource(
        FileUtil.readJsonFile("test/resources/cql/g6pdlibraryevaluator/patient.json")
      ) as
        Bundle

    val result = evaluator!!.runCqlLibrary(cqlLibrary, fhirHelpersLibrary, Bundle(), dataBundle)

    System.out.println(result)

    Assert.assertTrue(result.contains("AgeRange -> BooleanType[true]"))
    Assert.assertTrue(result.contains("Female -> BooleanType[true]"))
    Assert.assertTrue(result.contains("is Pregnant -> BooleanType[true]"))
    Assert.assertTrue(result.contains("What is the Haemoglobin value ? -> DecimalType[13.0]"))
    Assert.assertTrue(result.contains("What is the G6PD reading value ? -> DecimalType[4.0]"))
  }

  @Test
  fun processCQLPatientBundleTest() {
    val results = evaluator!!.processCQLPatientBundle(testData)
    Assert.assertNotNull(results)
  }
}
