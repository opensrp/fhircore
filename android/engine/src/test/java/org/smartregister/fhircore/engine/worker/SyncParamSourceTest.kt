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

package org.smartregister.fhircore.engine.worker

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

@HiltAndroidTest
internal class SyncParamSourceTest : RobolectricTest() {
  private val testContext: Context = ApplicationProvider.getApplicationContext()

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val instantTaskExecutorRule = InstantTaskExecutorRule()

  private lateinit var syncParamSource: SyncParamSource

  @Before
  fun setUp() {
    hiltRule.inject()
    syncParamSource = SyncParamSource(testContext)
  }

  @Test
  fun testCompositionConfigRequestQue() {
    val compositionConfigParamPairsReq = mutableListOf<Pair<ResourceType, Map<String, String>>>()
    syncParamSource.compositionConfigRequestQue.push(mapOf(*compositionConfigParamPairsReq.toTypedArray()))
    runTest {
      syncParamSource.compositionConfigRequestQue.map { requestQue ->
        assert(requestQue.isNullOrEmpty())
      }
    }
  }

  @Test
  fun testCompositionListRequestQue() {
    val compositionListParamPairsReq = mutableListOf<Pair<ResourceType, Map<String, String>>>()
    syncParamSource.compositionListRequestQue.push(mapOf(*compositionListParamPairsReq.toTypedArray()))
    runTest {
      syncParamSource.compositionListRequestQue.map { requestQue ->
        assert(requestQue.isNullOrEmpty())
      }
    }
  }

  @Test
  fun testCompositionListItemRequestQue() {
    val compositionListItemParamPairsReq = mutableListOf<Pair<ResourceType, Map<String, String>>>()
    compositionListItemParamPairsReq.add(
      Pair(
        ResourceType.List,
        mapOf(ConfigurationRegistry.ID to "test-resourceId"),
      ),
    )
    compositionListItemParamPairsReq.add(
      Pair(
        ResourceType.fromCode(ResourceType.List.name),
        mapOf("_count" to "200"),
      ),
    )
    syncParamSource.compositionListItemRequestQue.push(mapOf(*compositionListItemParamPairsReq.toTypedArray()))
    runTest {
      syncParamSource.compositionListItemRequestQue.map { requestQue ->
        assert(requestQue.isNotEmpty())
      }
    }
  }
}
