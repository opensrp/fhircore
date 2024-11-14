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

package org.smartregister.fhircore.engine.data.local

import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.Resource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.rule.CoroutineTestRule

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
class ContentCacheTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val coroutineTestRule = CoroutineTestRule()

  @Inject lateinit var contentCache: ContentCache

  private val resourceId = "123"
  private val mockResource: Resource = Questionnaire().apply { id = resourceId }

  @Before
  fun setUp() {
    hiltRule.inject()
  }

  @Test
  fun `saveResource should store resource in cache`() = runTest {
    contentCache.saveResource(mockResource)

    val cachedResource = contentCache.getResource(mockResource.resourceType, mockResource.idPart)
    assertNotNull(cachedResource)
    assertEquals(mockResource.idPart, cachedResource?.idPart)
  }

  @Test
  fun `getResource should return the correct resource from cache`() = runTest {
    contentCache.saveResource(mockResource)

    val result = contentCache.getResource(mockResource.resourceType, mockResource.idPart)
    assertEquals(mockResource.idPart, result?.idPart)
  }

  @Test
  fun `getResource should return null if resource does not exist`() = runTest {
    val result = contentCache.getResource(mockResource.resourceType, "non_existing_id")
    assertNull(result)
  }

  @Test
  fun `invalidate should clear all resources from cache`() = runTest {
    contentCache.saveResource(mockResource)
    contentCache.invalidate()

    val result = contentCache.getResource(mockResource.resourceType, mockResource.idPart)
    assertNull(result)
  }
}
