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

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.logicalId
import com.google.common.collect.Lists
import com.google.gson.Gson
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.mockk
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.util.Base64
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.instance.model.api.IBaseBundle
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.DecimalType
import org.hl7.fhir.r4.model.Library
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.app.fakes.Faker
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.FileUtil
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import timber.log.Timber

@HiltAndroidTest
class LibraryEvaluatorTest : RobolectricTest() {

  @get:Rule val hiltRule = HiltAndroidRule(this)

  private val application = ApplicationProvider.getApplicationContext<Application>()
  @Inject lateinit var gson: Gson
  private val configurationRegistry = Faker.buildTestConfigurationRegistry()
  @Inject lateinit var configService: ConfigService

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
    hiltRule.inject()
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
  fun testGetStringValueWithResourceShouldReturnCorrectStringRepresentation() {
    val resource = Patient().apply { id = "123" }
    val resourceStr =
      FhirContext.forCached(FhirVersionEnum.R4).newJsonParser().encodeResourceToString(resource)

    val result = evaluator!!.getStringRepresentation(resource)

    Assert.assertEquals(resourceStr, result)
  }

  @Test
  fun testGetStringValueWithTypeDataShouldReturnCorrectStringRepresentation() {
    val type = DecimalType(123)
    val typeStr = type.toString()

    val result = evaluator!!.getStringRepresentation(type)

    Assert.assertEquals(typeStr, result)
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
    val cqlElm = FileUtil.readJsonFile("test/resources/cql/g6pdlibraryevaluator/library-elm.json")

    val cqlLibrary =
      parser.parseResource(
        FileUtil.readJsonFile("test/resources/cql/g6pdlibraryevaluator/library.json")
          .replace("#library-elm.json", Base64.getEncoder().encodeToString(cqlElm.toByteArray()))
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

    val patient =
      dataBundle.entry.first { it.resource.resourceType == ResourceType.Patient }.resource as
        Patient

    val fhirEngine = mockk<FhirEngine>()
    val sharedPreferencesHelper = SharedPreferencesHelper(application, gson)
    val defaultRepository =
      DefaultRepository(
        fhirEngine,
        DefaultDispatcherProvider(),
        sharedPreferencesHelper,
        configurationRegistry,
        configService
      )

    coEvery { fhirEngine.get(ResourceType.Library, cqlLibrary.logicalId) } returns cqlLibrary
    coEvery { fhirEngine.get(ResourceType.Library, fhirHelpersLibrary.logicalId) } returns
      fhirHelpersLibrary
    coEvery { fhirEngine.create(any()) } answers { listOf() }

    val result = runBlocking {
      evaluator!!.runCqlLibrary(
        cqlLibrary.logicalId,
        patient,
        dataBundle.apply { entry.removeIf { it.resource.resourceType == ResourceType.Patient } },
        defaultRepository,
        true
      )
    }

    System.out.println(result)

    Assert.assertTrue(result.contains("AgeRange -> BooleanType[true]"))
    Assert.assertTrue(result.contains("Female -> BooleanType[true]"))
    Assert.assertTrue(result.contains("is Pregnant -> BooleanType[true]"))
    Assert.assertTrue(result.contains("Abnormal Haemoglobin -> BooleanType[false]"))
  }

  @Test
  fun processCqlPatientBundleTest() {
    val results = evaluator!!.processCqlPatientBundle(testData)
    Assert.assertNotNull(results)
  }
}
