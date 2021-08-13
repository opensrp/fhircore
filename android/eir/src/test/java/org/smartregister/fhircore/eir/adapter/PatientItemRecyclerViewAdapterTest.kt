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

package org.smartregister.fhircore.eir.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.smartregister.fhircore.eir.EirApplication
import org.smartregister.fhircore.eir.R
import org.smartregister.fhircore.eir.RobolectricTest
import org.smartregister.fhircore.eir.model.PatientItem
import org.smartregister.fhircore.eir.ui.patient.register.PatientItemRecyclerViewAdapter

class PatientItemRecyclerViewAdapterTest : RobolectricTest() {

  private lateinit var adapter: PatientItemRecyclerViewAdapter

  @Before
  fun setUp() {
    adapter = PatientItemRecyclerViewAdapter(mockk())
  }

  @Test
  fun testVerifyAdapterMethodsCall() {
    mockkStatic(LayoutInflater::from)

    val itemView =
      LayoutInflater.from(EirApplication.getContext())
        .inflate(R.layout.patient_list_item, null, false)
    val layoutInflater = mockk<LayoutInflater>()
    val viewGroup = mockk<ViewGroup>()

    every { viewGroup.context } returns EirApplication.getContext()
    every { LayoutInflater.from(any()) } returns layoutInflater
    every { layoutInflater.inflate(any<Int>(), any(), any()) } returns itemView

    val list = listOf(mockk<PatientItem>())
    adapter.submitList(list)

    val viewHolder = spyk(adapter.createViewHolder(viewGroup, 0))
    Assert.assertNotNull(viewHolder)

    every { viewHolder.bindTo(any(), any()) } answers {}
    adapter.bindViewHolder(viewHolder, 0)
    verify(exactly = 1) { viewHolder.bindTo(any(), any()) }
  }

  @Test
  fun testAdapterDiffUtilEquatesDifferentObjectsWithSameId() {
    val diffCallback = PatientItemRecyclerViewAdapter.PatientItemDiffCallback()

    val item =
      PatientItem(
        id = "1",
        name = "name",
        gender = "Male",
        dob = "2021-01-01",
        html = "asd",
        phone = "011",
        logicalId = "1234",
        risk = "high risk",
        lastSeen = "07-20-2021"
      )

    // change id only
    val itemDifferentId =
      PatientItem(
        id = "2",
        name = "name",
        gender = "Male",
        dob = "2021-01-01",
        html = "asd",
        phone = "011",
        logicalId = "1234",
        risk = "high risk",
        lastSeen = "07-20-2021"
      )
    Assert.assertFalse(diffCallback.areContentsTheSame(item, itemDifferentId))
    Assert.assertFalse(diffCallback.areItemsTheSame(item, itemDifferentId))

    // same id different content
    val itemWithMatchingId =
      PatientItem(
        id = "1",
        name = "name1",
        gender = "Male",
        dob = "2021-01-01",
        html = "asd",
        phone = "011",
        logicalId = "1234",
        risk = "high risk",
        lastSeen = "07-20-2021"
      )
    Assert.assertFalse(diffCallback.areContentsTheSame(item, itemWithMatchingId))
    Assert.assertTrue(diffCallback.areItemsTheSame(item, itemWithMatchingId))

    // identical items
    val identical =
      PatientItem(
        id = "1",
        name = "name",
        gender = "Male",
        dob = "2021-01-01",
        html = "asd",
        phone = "011",
        logicalId = "1234",
        risk = "high risk",
        lastSeen = "07-20-2021"
      )
    Assert.assertTrue(diffCallback.areContentsTheSame(item, identical))
    Assert.assertTrue(diffCallback.areItemsTheSame(item, identical))
  }
}
