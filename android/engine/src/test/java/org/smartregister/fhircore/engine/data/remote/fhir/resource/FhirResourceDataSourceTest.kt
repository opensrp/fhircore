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

package org.smartregister.fhircore.engine.data.remote.fhir.resource

import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.OperationOutcome
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FhirResourceDataSourceTest {
  private val resourceService: FhirResourceService = mockk()

  private lateinit var fhirResourceDataSource: FhirResourceDataSource

  @Before
  fun setUp() {
    fhirResourceDataSource = spyk(FhirResourceDataSource(resourceService))
  }

  @Test
  fun testLoadDataShouldRetrieveResource() {
    runTest {
      val bundle = Bundle()
      coEvery { resourceService.getResource(any()) } returns bundle
      Assert.assertEquals(bundle, fhirResourceDataSource.getResource("http://fake.url"))
    }
  }

  @Test
  fun testInsertShouldAddResource() {
    runTest {
      val resource = Patient()
      coEvery { resourceService.insertResource(any(), any(), any()) } returns resource
      Assert.assertEquals(
        resource,
        fhirResourceDataSource.insert(ResourceType.Patient.name, "id", "{}"),
      )
    }
  }

  @Test
  fun testUpdateShouldUpdateResource() {
    runTest {
      val operationOutcome = OperationOutcome()
      coEvery { resourceService.updateResource(any(), any(), any()) } returns operationOutcome
      Assert.assertEquals(
        operationOutcome,
        fhirResourceDataSource.update(ResourceType.Patient.name, "id", "{}"),
      )
    }
  }

  @Test
  fun testDeleteShouldRemoveResource() {
    runTest {
      val operationOutcome = OperationOutcome()
      coEvery { resourceService.deleteResource(any(), any()) } returns operationOutcome
      Assert.assertEquals(
        operationOutcome,
        fhirResourceDataSource.delete(ResourceType.Patient.name, "id"),
      )
    }
  }

  @Test
  fun testSearchResourceShouldReturnBundle() {
    runTest {
      val bundle = Bundle()
      coEvery { resourceService.searchResource(any(), any()) } returns bundle
      Assert.assertEquals(
        bundle,
        fhirResourceDataSource.search(
          ResourceType.Practitioner.name,
          mapOf("identifier" to "19292929"),
        ),
      )
    }
  }
}
