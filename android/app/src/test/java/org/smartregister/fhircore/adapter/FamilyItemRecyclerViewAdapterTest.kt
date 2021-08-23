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

package org.smartregister.fhircore.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.smartregister.fhircore.FhirApplication
import org.smartregister.fhircore.R
import org.smartregister.fhircore.RobolectricTest
import org.smartregister.fhircore.model.FamilyItem
import org.smartregister.fhircore.model.FamilyMemberItem

class FamilyItemRecyclerViewAdapterTest : RobolectricTest() {

  private lateinit var adapter: FamilyItemRecyclerViewAdapter

  @Before
  fun setUp() {
    adapter = FamilyItemRecyclerViewAdapter(mockk())
  }

  @Test
  fun testVerifyAdapterMethodsCall() {
    mockkStatic(LayoutInflater::from)

    val itemView =
      LayoutInflater.from(FhirApplication.getContext())
        .inflate(R.layout.family_list_item, null, false)
    val layoutInflater = mockk<LayoutInflater>()
    val viewGroup = mockk<ViewGroup>()

    every { viewGroup.context } returns FhirApplication.getContext()
    every { LayoutInflater.from(any()) } returns layoutInflater
    every { layoutInflater.inflate(any<Int>(), any(), any()) } returns itemView

    val list = listOf(mockk<FamilyItem>())
    adapter.submitList(list)

    val viewHolder = spyk(adapter.createViewHolder(viewGroup, 0))
    Assert.assertNotNull(viewHolder)

    every { viewHolder.bindTo(any(), any()) } answers {}
    adapter.bindViewHolder(viewHolder, 0)
    verify(exactly = 1) { viewHolder.bindTo(any(), any()) }

    unmockkStatic(LayoutInflater::from)
  }

  @Test
  fun testAdapterDiffUtilEquatesDifferentObjectsWithSameId() {
    val diffCallback = FamilyItemRecyclerViewAdapter.FamilyItemDiffCallback()

    val item =
      FamilyItem(
        id = "1",
        name = "name",
        gender = "male",
        dob = "2021-01-01",
        area = "nairobi",
        members = listOf(FamilyMemberItem("122", "Pregnant"))
      )

    // change id only
    val itemDifferentId =
      FamilyItem(
        id = "2",
        name = "name",
        gender = "male",
        dob = "2021-01-01",
        area = "nairobi",
        members = listOf(FamilyMemberItem("122", "Pregnant"))
      )
    Assert.assertFalse(diffCallback.areContentsTheSame(item, itemDifferentId))
    Assert.assertFalse(diffCallback.areItemsTheSame(item, itemDifferentId))

    // same id different content
    val itemWithMatchingId =
      FamilyItem(
        id = "1",
        name = "name1",
        gender = "male",
        dob = "2021-01-02",
        area = "nairobi",
        members = listOf(FamilyMemberItem("122", "Pregnant"))
      )
    Assert.assertFalse(diffCallback.areContentsTheSame(item, itemWithMatchingId))
    Assert.assertTrue(diffCallback.areItemsTheSame(item, itemWithMatchingId))

    // identical items
    val identical =
      FamilyItem(
        id = "1",
        name = "name",
        gender = "male",
        dob = "2021-01-01",
        area = "nairobi",
        members = listOf(FamilyMemberItem("122", "Pregnant"))
      )
    Assert.assertTrue(diffCallback.areContentsTheSame(item, identical))
    Assert.assertTrue(diffCallback.areItemsTheSame(item, identical))
  }
}
