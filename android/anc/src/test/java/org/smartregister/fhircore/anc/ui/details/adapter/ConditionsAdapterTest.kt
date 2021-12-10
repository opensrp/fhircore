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
import org.smartregister.fhircore.anc.data.model.ConditionItem
import org.smartregister.fhircore.anc.databinding.ItemPlanTextBinding
import org.smartregister.fhircore.anc.robolectric.RobolectricTest

class ConditionsAdapterTest : RobolectricTest() {

  private lateinit var adapter: ConditionsAdapter

  @Before
  fun setUp() {
    adapter = ConditionsAdapter()
  }

  @Test
  fun testVerifyAdapterMethodsCall() {

    val viewGroup = mockk<ViewGroup>()
    every { viewGroup.context } returns ApplicationProvider.getApplicationContext()

    val list = listOf(ConditionItem("1", "C-Title"))
    adapter.submitList(list)

    val viewHolder = adapter.createViewHolder(viewGroup, 0)
    Assert.assertNotNull(viewHolder)

    adapter.bindViewHolder(viewHolder, 0)

    val containerView = ReflectionHelpers.getField<ItemPlanTextBinding>(viewHolder, "containerView")
    Assert.assertEquals("C-Title", containerView.title)
  }

  @Test
  fun testAdapterDiffUtilEquatesDifferentObjectsWithSameId() {

    val diffCallback = ConditionsAdapter.ConditionItemDiffCallback
    val item = ConditionItem("1111", "first")

    // change title only
    val itemDifferentVaccine = ConditionItem("1111", "second")
    Assert.assertFalse(diffCallback.areItemsTheSame(item, itemDifferentVaccine))
    Assert.assertFalse(diffCallback.areContentsTheSame(item, itemDifferentVaccine))

    // same title with different content
    val itemWithMatchingVaccine = ConditionItem("1112", "first")
    Assert.assertTrue(diffCallback.areItemsTheSame(item, itemWithMatchingVaccine))
    Assert.assertFalse(diffCallback.areContentsTheSame(item, itemWithMatchingVaccine))

    // identical items
    val identical = ConditionItem("1111", "first")
    Assert.assertTrue(diffCallback.areItemsTheSame(item, identical))
    Assert.assertTrue(diffCallback.areContentsTheSame(item, identical))
  }
}
