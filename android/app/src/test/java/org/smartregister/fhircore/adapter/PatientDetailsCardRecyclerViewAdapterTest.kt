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
import io.mockk.verify
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.smartregister.fhircore.FhirApplication
import org.smartregister.fhircore.R
import org.smartregister.fhircore.RobolectricTest
import org.smartregister.fhircore.fragment.PatientDetailsCard

class PatientDetailsCardRecyclerViewAdapterTest : RobolectricTest() {

  lateinit var adapter: PatientDetailsCardRecyclerViewAdapter

  @Before
  fun setUp() {
    adapter = PatientDetailsCardRecyclerViewAdapter()
  }

  @Test
  fun testVerifyAdapterMethodsCall() {
    mockkStatic(LayoutInflater::from)

    val itemView =
      LayoutInflater.from(FhirApplication.getContext())
        .inflate(R.layout.patient_details_card_item, null, false)
    val layoutInflater = mockk<LayoutInflater>()
    val viewGroup = mockk<ViewGroup>()

    every { viewGroup.context } returns FhirApplication.getContext()
    every { LayoutInflater.from(any()) } returns layoutInflater
    every { layoutInflater.inflate(any<Int>(), any(), any()) } returns itemView

    val list = listOf(mockk<PatientDetailsCard>())
    adapter.submitList(list)

    val viewHolder = spyk(adapter.createViewHolder(viewGroup, 0))
    Assert.assertNotNull(viewHolder)

    every { viewHolder.bindTo(any()) } answers {}
    adapter.bindViewHolder(viewHolder, 0)
    verify(exactly = 1) { viewHolder.bindTo(any()) }
  }

  @Test
  fun testAdapterDiffUtilEquatesDifferentObjectsWithSameId() {
    val diffCallback = PatientDetailsCardRecyclerViewAdapter.PatientDetailsCardDiffCallback()

    val item = PatientDetailsCard(0, 0, "1", "Patient", "RegistrationDate", "Details")

    // change id only
    val itemDifferentId = PatientDetailsCard(0, 0, "2", "Patient", "RegistrationDate", "Details")
    Assert.assertFalse(diffCallback.areContentsTheSame(item, itemDifferentId))
    Assert.assertFalse(diffCallback.areItemsTheSame(item, itemDifferentId))

    // same id different content
    val itemWithMatchingId =
      PatientDetailsCard(0, 0, "1", "Patient", "RegistrationDates", "Details")
    Assert.assertFalse(diffCallback.areContentsTheSame(item, itemWithMatchingId))
    Assert.assertTrue(diffCallback.areItemsTheSame(item, itemWithMatchingId))

    // identical items
    val identical = PatientDetailsCard(0, 0, "1", "Patient", "RegistrationDate", "Details")
    Assert.assertTrue(diffCallback.areContentsTheSame(item, identical))
    Assert.assertTrue(diffCallback.areItemsTheSame(item, identical))
  }
}
