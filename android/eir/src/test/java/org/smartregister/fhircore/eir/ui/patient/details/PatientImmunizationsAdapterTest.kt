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

package org.smartregister.fhircore.eir.ui.patient.details

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.robolectric.annotation.Config
import org.smartregister.fhircore.eir.R
import org.smartregister.fhircore.eir.robolectric.RobolectricTest
import org.smartregister.fhircore.eir.shadow.EirApplicationShadow

@Config(shadows = [EirApplicationShadow::class])
class PatientImmunizationsAdapterTest : RobolectricTest() {

  private lateinit var adapter: PatientImmunizationsAdapter

  @Before
  fun setUp() {
    adapter = PatientImmunizationsAdapter()
  }

  @Test
  fun testVerifyAdapterMethodsCall() {
    mockkStatic(LayoutInflater::from)

    val itemView =
      LayoutInflater.from(ApplicationProvider.getApplicationContext())
        .inflate(R.layout.immunization_list_item, null, false)
    val layoutInflater = mockk<LayoutInflater>()
    val viewGroup = mockk<ViewGroup>()

    every { viewGroup.context } returns ApplicationProvider.getApplicationContext()
    every { LayoutInflater.from(any()) } returns layoutInflater
    every { layoutInflater.inflate(any<Int>(), any(), any()) } returns itemView

    val list = listOf(mockk<ImmunizationItem>())
    adapter.submitList(list)

    val viewHolder = spyk(adapter.createViewHolder(viewGroup, 0))
    Assert.assertNotNull(viewHolder)

    every { viewHolder.bindTo(any()) } answers {}
    adapter.bindViewHolder(viewHolder, 0)
    verify(exactly = 1) { viewHolder.bindTo(any()) }
  }

  @Test
  fun testAdapterDiffUtilEquatesDifferentObjectsWithSameId() {

    val diffCallback = PatientImmunizationsAdapter.ImmunizationItemDiffCallback
    val item = ImmunizationItem("1", getDosesList())

    // change vaccine only
    val itemDifferentVaccine = ImmunizationItem("2", getDosesList())
    Assert.assertFalse(diffCallback.areItemsTheSame(item, itemDifferentVaccine))
    Assert.assertFalse(diffCallback.areContentsTheSame(item, itemDifferentVaccine))

    // same vaccine with different content
    val itemWithMatchingVaccine = ImmunizationItem("1", getDosesList("second"))
    Assert.assertTrue(diffCallback.areItemsTheSame(item, itemWithMatchingVaccine))
    Assert.assertFalse(diffCallback.areContentsTheSame(item, itemWithMatchingVaccine))

    // identical items
    val identical = ImmunizationItem("1", getDosesList())
    Assert.assertTrue(diffCallback.areItemsTheSame(item, identical))
    Assert.assertTrue(diffCallback.areContentsTheSame(item, identical))
  }

  private fun getDosesList(text: String = "first", num: Int = 1): List<Pair<String, Int>> {
    return listOf(Pair(text, num))
  }
}
