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

package org.smartregister.fhircore.eir.ui.adverseevent

import android.view.ViewGroup
import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.sync.Sync
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.robolectric.annotation.Config
import org.smartregister.fhircore.eir.robolectric.RobolectricTest
import org.smartregister.fhircore.eir.shadow.EirApplicationShadow
import org.smartregister.fhircore.eir.ui.patient.details.AdverseEventItem
import org.smartregister.fhircore.eir.ui.patient.details.ImmunizationAdverseEventItem

@Config(shadows = [EirApplicationShadow::class])
class MainAdverseEventAdapterTest : RobolectricTest() {

  private lateinit var adapter: MainAdverseEventAdapter

  @Before
  fun setUp() {
    mockkObject(Sync)
    adapter = MainAdverseEventAdapter()
  }

  @After
  fun cleanup() {
    unmockkObject(Sync)
  }

  @Test
  fun testVerifyAdapterMethodsCall() {

    val viewGroup = mockk<ViewGroup>()
    every { viewGroup.context } returns ApplicationProvider.getApplicationContext()

    val list = listOf(mockk<ImmunizationAdverseEventItem>())
    adapter.submitList(list)

    val viewHolder = spyk(adapter.createViewHolder(viewGroup, 0))
    Assert.assertNotNull(viewHolder)

    every { viewHolder.bindTo(any(), 0) } answers {}
    adapter.bindViewHolder(viewHolder, 0)
    verify(exactly = 1) { viewHolder.bindTo(any(), 0) }
  }

  @Test
  fun testAdapterDiffUtilEquatesDifferentObjectsWithSameId() {

    val diffCallback = MainAdverseEventAdapter.AdverseEventItemDiffCallback
    val item =
      ImmunizationAdverseEventItem(
        arrayListOf("1"),
        "Moderna",
        arrayListOf(Pair("1", arrayListOf(AdverseEventItem("22-Jan-2020", "Blood"))))
      )

    // change title only
    val itemDifferentVaccine =
      ImmunizationAdverseEventItem(
        arrayListOf("1"),
        "Pfizer",
        arrayListOf(Pair("1", arrayListOf(AdverseEventItem("22-Jan-2020", "Blood"))))
      )
    Assert.assertFalse(diffCallback.areItemsTheSame(item, itemDifferentVaccine))
    Assert.assertFalse(diffCallback.areContentsTheSame(item, itemDifferentVaccine))

    // same title with different content
    val itemWithMatchingVaccine =
      ImmunizationAdverseEventItem(
        arrayListOf("2"),
        "Moderna",
        arrayListOf(Pair("1", arrayListOf(AdverseEventItem("22-Jan-2020", "Blood"))))
      )
    Assert.assertTrue(diffCallback.areItemsTheSame(item, itemWithMatchingVaccine))
    Assert.assertFalse(diffCallback.areContentsTheSame(item, itemWithMatchingVaccine))

    // identical items
    val identical =
      ImmunizationAdverseEventItem(
        arrayListOf("1"),
        "Moderna",
        arrayListOf(Pair("1", arrayListOf(AdverseEventItem("22-Jan-2020", "Blood"))))
      )
    Assert.assertTrue(diffCallback.areItemsTheSame(item, identical))
    Assert.assertTrue(diffCallback.areContentsTheSame(item, identical))
  }
}
