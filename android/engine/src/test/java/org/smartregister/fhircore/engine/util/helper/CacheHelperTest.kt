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

package org.smartregister.fhircore.engine.util.helper

import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
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
class CacheHelperTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val coroutineTestRule = CoroutineTestRule()

  private val resourceId = "123"
  private val mockResource: Resource = Questionnaire().apply { id = resourceId }

  @Before
  fun setUp() {
    hiltRule.inject()
  }

  @Test
  fun `saveResource should store resource in cache`() = runTest {
    CacheHelper.saveResource(mockResource.idPart, mockResource)

    val cachedResource =
      CacheHelper.getResource(mockResource::class.simpleName ?: "", mockResource.idPart)
    assertNotNull(cachedResource)
    assertEquals(mockResource.idPart, cachedResource?.idPart)
  }

  @Test
  fun `getResource should return null if resource type does not match`() = runTest {
    CacheHelper.saveResource(mockResource.idPart, mockResource)

    val result = CacheHelper.getResource("DifferentType", mockResource.idPart)
    assertNull(result)
  }

  @Test
  fun `getResource should return null if resource does not exist`() = runTest {
    val result = CacheHelper.getResource(mockResource::class.simpleName ?: "", "non_existing_id")
    assertNull(result)
  }

  @Test
  fun `invalidate should clear all resources from cache`() = runTest {
    CacheHelper.saveResource(mockResource.idPart, mockResource)
    CacheHelper.invalidate()

    val result = CacheHelper.getResource(mockResource::class.simpleName ?: "", mockResource.idPart)
    assertNull(result)
  }
}