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

package org.smartregister.fhircore.data

import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.smartregister.fhircore.RobolectricTest
import org.smartregister.fhircore.api.HapiFhirService

class HapiFhirResourceDataSourceTest : RobolectricTest() {

  private lateinit var dataSource: HapiFhirResourceDataSource

  @MockK private lateinit var service: HapiFhirService

  @Before
  fun setUp() {
    MockKAnnotations.init(this)
    dataSource = HapiFhirResourceDataSource(service)
  }

  @Test
  fun testLoadDataShouldVerifyLoadBundle() {
    coEvery { service.getResource(any()) } returns mockk()

    runBlocking { dataSource.loadData("") }
    coVerify(exactly = 1) { service.getResource(any()) }
  }

  @Test
  fun testInsertShouldVerifyInsertResource() {
    coEvery { service.insertResource(any(), any(), any()) } returns mockk()

    runBlocking { dataSource.insert("", "", "") }
    coVerify(exactly = 1) { service.insertResource(any(), any(), any()) }
  }

  @Test
  fun testUpdateShouldVerifyUpdateResource() {
    coEvery { service.updateResource(any(), any(), any()) } returns mockk()

    runBlocking { dataSource.update("", "", "") }
    coVerify(exactly = 1) { service.updateResource(any(), any(), any()) }
  }

  @Test
  fun testDeleteShouldVerifyDeleteResource() {
    coEvery { service.deleteResource(any(), any()) } returns mockk()

    runBlocking { dataSource.delete("", "") }
    coVerify(exactly = 1) { service.deleteResource(any(), any()) }
  }
}
