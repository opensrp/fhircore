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

package org.smartregister.fhircore.anc.ui.details.adapter

import android.view.ViewGroup
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.anc.data.model.CarePlanItem
import org.smartregister.fhircore.anc.databinding.ItemCareplanBinding
import org.smartregister.fhircore.anc.robolectric.RobolectricTest

class CarePlanAdapterTest : RobolectricTest() {

  private lateinit var adapter: CarePlanAdapter

  @Before
  fun setUp() {
    adapter = CarePlanAdapter()
  }

  @Test
  fun testVerifyAdapterMethodsCall() {

    val viewGroup = mockk<ViewGroup>()
    every { viewGroup.context } returns ApplicationProvider.getApplicationContext()

    val list = mutableListOf(CarePlanItem("1", "CP Title", due = false, overdue = true))
    adapter.submitList(list)

    val viewHolder = adapter.createViewHolder(viewGroup, 0)
    Assert.assertNotNull(viewHolder)

    adapter.bindViewHolder(viewHolder, 0)

    val containerView = ReflectionHelpers.getField<ItemCareplanBinding>(viewHolder, "containerView")
    with(containerView) {
      Assert.assertTrue(carPlanDatePassed!!)
      Assert.assertEquals("CP Title Overdue", carPlanTitle)
    }

    list.add(CarePlanItem("1", "New CP Title", due = false, overdue = false))

    adapter.bindViewHolder(viewHolder, 1)

    with(containerView) {
      Assert.assertFalse(carPlanDatePassed!!)
      Assert.assertEquals("New CP Title", carPlanTitle)
    }
  }

  @Test
  fun testAdapterDiffUtilEquatesDifferentObjectsWithSameId() {

    val diffCallback = CarePlanAdapter.CarePlanItemDiffCallback
    val item = CarePlanItem("1111", "first", due = false, overdue = true)

    // change title only
    val itemDifferentVaccine = CarePlanItem("1111", "second", due = false, overdue = true)
    Assert.assertFalse(diffCallback.areItemsTheSame(item, itemDifferentVaccine))
    Assert.assertFalse(diffCallback.areContentsTheSame(item, itemDifferentVaccine))

    // same title with different content
    val itemWithMatchingVaccine = CarePlanItem("1111", "first", due = true, overdue = false)
    Assert.assertTrue(diffCallback.areItemsTheSame(item, itemWithMatchingVaccine))
    Assert.assertFalse(diffCallback.areContentsTheSame(item, itemWithMatchingVaccine))

    // identical items
    val identical = CarePlanItem("1111", "first", due = false, overdue = true)
    Assert.assertTrue(diffCallback.areItemsTheSame(item, identical))
    Assert.assertTrue(diffCallback.areContentsTheSame(item, identical))
  }
}
