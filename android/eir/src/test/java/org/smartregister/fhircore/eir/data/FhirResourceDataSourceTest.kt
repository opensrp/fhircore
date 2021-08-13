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

package org.smartregister.fhircore.eir.data

import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.smartregister.fhircore.eir.RobolectricTest
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceService

class FhirResourceDataSourceTest : RobolectricTest() {

  private lateinit var dataSource: FhirResourceDataSource

  @MockK private lateinit var resourceService: FhirResourceService

  @Before
  fun setUp() {
    MockKAnnotations.init(this)
    dataSource = FhirResourceDataSource(resourceService)
  }

  @Test
  fun testLoadDataShouldVerifyLoadBundle() {
    coEvery { resourceService.getResource(any()) } returns mockk()

    runBlocking { dataSource.loadData("") }
    coVerify(exactly = 1) { resourceService.getResource(any()) }
  }

  @Test
  fun testInsertShouldVerifyInsertResource() {
    coEvery { resourceService.insertResource(any(), any(), any()) } returns mockk()

    runBlocking { dataSource.insert("", "", "") }
    coVerify(exactly = 1) { resourceService.insertResource(any(), any(), any()) }
  }

  @Test
  fun testUpdateShouldVerifyUpdateResource() {
    coEvery { resourceService.updateResource(any(), any(), any()) } returns mockk()

    runBlocking { dataSource.update("", "", "") }
    coVerify(exactly = 1) { resourceService.updateResource(any(), any(), any()) }
  }

  @Test
  fun testDeleteShouldVerifyDeleteResource() {
    coEvery { resourceService.deleteResource(any(), any()) } returns mockk()

    runBlocking { dataSource.delete("", "") }
    coVerify(exactly = 1) { resourceService.deleteResource(any(), any()) }
  }
}
