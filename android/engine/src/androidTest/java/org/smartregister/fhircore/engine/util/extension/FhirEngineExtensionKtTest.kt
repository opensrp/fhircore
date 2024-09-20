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

package org.smartregister.fhircore.engine.util.extension

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.FhirEngineConfiguration
import com.google.android.fhir.FhirEngineProvider
import com.google.android.fhir.search.search
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

@MediumTest
class FhirEngineExtensionKtTest {

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private lateinit var fhirEngine: FhirEngine

  @Before
  fun setUp() {
    FhirEngineProvider.init(FhirEngineConfiguration(testMode = true))
    fhirEngine = FhirEngineProvider.getInstance(context)

    val patients = (0..1000).map { Patient().apply { id = "test-patient-$it" } }
    val questionnaires = (0..3).map { Questionnaire().apply { id = "test-questionnaire-$it" } }
    runBlocking { fhirEngine.create(*patients.toTypedArray(), *questionnaires.toTypedArray()) }
  }

  @After
  fun tearDown() {
    runBlocking { fhirEngine.clearDatabase() }
    FhirEngineProvider.cleanup()
  }

  @Test
  fun test_search_time_searches_sequentially_and_short_running_query_waits() {
    val fetchedResources = mutableListOf<Resource>()
    runBlocking {
      launch {
        val patients = fhirEngine.search<Patient> {}.map { it.resource }
        fetchedResources += patients
      }

      launch {
        val questionnaires = fhirEngine.search<Questionnaire> {}.map { it.resource }
        fetchedResources += questionnaires
      }
    }
    val indexOfResultOfShortQuery =
      fetchedResources.indexOfFirst { it.resourceType == ResourceType.Questionnaire }
    val indexOfResultOfLongQuery =
      fetchedResources.indexOfFirst { it.resourceType == ResourceType.Patient }
    Assert.assertTrue(indexOfResultOfShortQuery > indexOfResultOfLongQuery)
  }

  @Test
  fun test_batchedSearch_returns_short_running_query_and_long_running_does_not_block() {
    val fetchedResources = mutableListOf<Resource>()
    runBlocking {
      launch {
        val patients = fhirEngine.batchedSearch<Patient> {}.map { it.resource }
        fetchedResources += patients
      }

      launch {
        val questionnaires = fhirEngine.search<Questionnaire> {}
        fetchedResources + questionnaires
      }
    }

    val indexOfResultOfShortQuery =
      fetchedResources.indexOfFirst { it.resourceType == ResourceType.Questionnaire }
    val indexOfResultOfLongQuery =
      fetchedResources.indexOfFirst { it.resourceType == ResourceType.Patient }
    Assert.assertTrue(indexOfResultOfShortQuery < indexOfResultOfLongQuery)
  }
}
