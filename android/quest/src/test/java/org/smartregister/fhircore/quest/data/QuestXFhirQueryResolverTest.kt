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

package org.smartregister.fhircore.quest.data

import com.google.android.fhir.FhirEngine
import com.google.android.fhir.search.Search
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.Task
import org.junit.Assert
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QuestXFhirQueryResolverTest {

  private val fhirEngine = mockk<FhirEngine>()

  @Test
  fun resolve() = runTest {
    val patient = Patient()
    val task = Task()
    val resources = listOf<Resource>(patient, task)
    coEvery { fhirEngine.search<Resource>(ofType<Search>()) } answers
      {
        val type = firstArg<Search>().type
        resources.filter { it.resourceType == type }
      }
    val xFhirResolver = QuestXFhirQueryResolver(fhirEngine)
    val result = xFhirResolver.resolve("Patient?active=true")
    Assert.assertTrue(result.isNotEmpty())
    Assert.assertTrue(
      result.containsAll(resources.filter { it.resourceType == ResourceType.Patient })
    )
  }
}
