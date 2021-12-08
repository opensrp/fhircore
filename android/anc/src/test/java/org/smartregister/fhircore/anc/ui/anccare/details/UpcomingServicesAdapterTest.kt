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

package org.smartregister.fhircore.anc.ui.anccare.details

import android.view.ViewGroup
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.anc.data.model.UpcomingServiceItem
import org.smartregister.fhircore.anc.databinding.ItemServicesBinding
import org.smartregister.fhircore.anc.robolectric.RobolectricTest

class UpcomingServicesAdapterTest : RobolectricTest() {

  private lateinit var adapter: UpcomingServicesAdapter

  @Before
  fun setUp() {
    adapter = UpcomingServicesAdapter()
  }

  @Test
  fun testVerifyAdapterMethodsCall() {

    val viewGroup = mockk<ViewGroup>()
    every { viewGroup.context } returns ApplicationProvider.getApplicationContext()

    adapter.submitList(listOf(UpcomingServiceItem("1", "New Title", "2021-01-01")))

    val viewHolder = adapter.createViewHolder(viewGroup, 0)
    Assert.assertNotNull(viewHolder)

    adapter.bindViewHolder(viewHolder, 0)

    val containerView = ReflectionHelpers.getField<ItemServicesBinding>(viewHolder, "containerView")
    with(containerView) {
      Assert.assertEquals("New Title", title)
      Assert.assertEquals("2021-01-01", date)
    }
  }

  @Test
  fun testAdapterDiffUtilEquatesDifferentObjectsWithSameId() {

    val diffCallback = UpcomingServicesAdapter.UpcomingServiceItemDiffCallback
    val item = UpcomingServiceItem("1111", "first", date = "2021-02-01")

    // change title only
    val itemDifferentVaccine = UpcomingServiceItem("1111", "second", date = "2021-02-01")
    Assert.assertFalse(diffCallback.areItemsTheSame(item, itemDifferentVaccine))
    Assert.assertFalse(diffCallback.areContentsTheSame(item, itemDifferentVaccine))

    // same title with different content
    val itemWithMatchingVaccine = UpcomingServiceItem("1111", "first", date = "2021-02-02")
    Assert.assertTrue(diffCallback.areItemsTheSame(item, itemWithMatchingVaccine))
    Assert.assertFalse(diffCallback.areContentsTheSame(item, itemWithMatchingVaccine))

    // identical items
    val identical = UpcomingServiceItem("1111", "first", date = "2021-02-01")
    Assert.assertTrue(diffCallback.areItemsTheSame(item, identical))
    Assert.assertTrue(diffCallback.areContentsTheSame(item, identical))
  }
}
