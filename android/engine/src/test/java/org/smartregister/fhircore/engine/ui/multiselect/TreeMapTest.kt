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

package org.smartregister.fhircore.engine.ui.multiselect

import android.app.Application
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.test.core.app.ApplicationProvider
import ca.uhn.fhir.context.FhirContext
import com.google.android.fhir.logicalId
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Location
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.extension.extractId

@HiltAndroidTest
class TreeMapTest : RobolectricTest() {

  @get:Rule val hiltAndroidRule = HiltAndroidRule(this)

  @Inject lateinit var fhirContext: FhirContext

  private lateinit var locationTreeNodes: List<TreeNode<Location>>

  @Before
  fun setUp() {
    hiltAndroidRule.inject()
    val locationsJson: String =
      ApplicationProvider.getApplicationContext<Application>()
        .assets
        .open("locations.json")
        .bufferedReader()
        .use { it.readText() }
    locationTreeNodes =
      fhirContext
        .newJsonParser()
        .parseResource(Bundle::class.java, locationsJson)
        .entry
        .map { it.resource as Location }
        .map { TreeNode(it.logicalId, it.partOf.extractId(), it) }
  }

  @Test
  fun testPopulateLookupMapShouldReturnMapOfTreeNodes() {
    val lookup: MutableMap<String, TreeNode<Location>> =
      TreeMap.populateLookupMap(locationTreeNodes, SnapshotStateMap())
    Assert.assertFalse(lookup.isEmpty())

    val rootLocation = lookup["eff94f33-c356-4634-8795-d52340706ba9"]
    Assert.assertNull(rootLocation?.parentId)
    Assert.assertTrue(rootLocation?.data is Location)
    Assert.assertEquals("eff94f33-c356-4634-8795-d52340706ba9", rootLocation?.data?.logicalId)

    val locationWithChildren = lookup["25c56dd5-4dca-449d-bf6e-665f90d0ff77"]
    Assert.assertNotNull(locationWithChildren)
    Assert.assertEquals(10, locationWithChildren?.children?.size)

    // Assert that each child location references the parent; all have same parent id
    locationWithChildren?.children?.forEach {
      Assert.assertEquals(locationWithChildren.id, it.parentId)
    }
  }
}
