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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.Resource
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.smartregister.fhircore.engine.datastore.ContentCache

@OptIn(ExperimentalCoroutinesApi::class)
class ContentCacheTest {

  private val testDispatcher = StandardTestDispatcher()
  private val resourceId = "123"
  private val mockResource: Resource = Questionnaire()

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `saveResource should store resource in cache`() = runTest {
    ContentCache.saveResource(resourceId, mockResource)
    advanceUntilIdle() // Ensure coroutine has finished

    val cachedResource = ContentCache.getResource("${mockResource::class.simpleName}/$resourceId")
    assertNotNull(cachedResource)
    assertEquals(mockResource, cachedResource)
  }

  @Test
  fun `getResource should return the correct resource from cache`() = runTest {
    ContentCache.saveResource(resourceId, mockResource)
    advanceUntilIdle() // Ensure coroutine has finished

    val result = ContentCache.getResource("${mockResource::class.simpleName}/$resourceId")
    assertEquals(mockResource, result)
  }

  @Test
  fun `getResource should return null if resource does not exist`() = runTest {
    val result = ContentCache.getResource("non_existing_id")
    assertNull(result)
  }

  @Test
  fun `invalidate should clear all resources from cache`() = runTest {
    ContentCache.saveResource(resourceId, mockResource)
    advanceUntilIdle() // Ensure coroutine has finished

    ContentCache.invalidate()
    advanceUntilIdle() // Ensure coroutine has finished

    val result = ContentCache.getResource("${mockResource::class.simpleName}/$resourceId")
    assertNull(result)
  }
}
