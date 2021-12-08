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
import java.util.Date
import org.hl7.fhir.r4.model.Encounter
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.anc.data.model.EncounterItem
import org.smartregister.fhircore.anc.databinding.ItemEncountersBinding
import org.smartregister.fhircore.anc.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.extension.makeItReadable

class EncounterAdapterTest : RobolectricTest() {

  private lateinit var adapter: EncounterAdapter

  @Before
  fun setUp() {
    adapter = EncounterAdapter()
  }

  @Test
  fun testVerifyAdapterMethodsCall() {

    val viewGroup = mockk<ViewGroup>()
    every { viewGroup.context } returns ApplicationProvider.getApplicationContext()

    val date = Date()
    adapter.submitList(listOf(EncounterItem("id", Encounter.EncounterStatus.FINISHED, "", date)))

    val viewHolder = adapter.createViewHolder(viewGroup, 0)
    Assert.assertNotNull(viewHolder)

    adapter.bindViewHolder(viewHolder, 0)

    val containerView =
      ReflectionHelpers.getField<ItemEncountersBinding>(viewHolder, "containerView")
    Assert.assertEquals("${date.makeItReadable()} Encounter", containerView.date)
  }

  @Test
  fun testAdapterDiffUtilEquatesDifferentObjectsWithSameId() {

    val diffCallback = EncounterAdapter.EncounterItemDiffCallback
    val item = EncounterItem("1111", Encounter.EncounterStatus.ARRIVED, "first", Date())

    // change title only
    val itemDifferentVaccine =
      EncounterItem("1111", Encounter.EncounterStatus.ARRIVED, "second", Date())
    Assert.assertFalse(diffCallback.areItemsTheSame(item, itemDifferentVaccine))
    Assert.assertFalse(diffCallback.areContentsTheSame(item, itemDifferentVaccine))

    // same title with different content
    val itemWithMatchingVaccine =
      EncounterItem("1111", Encounter.EncounterStatus.FINISHED, "first", Date())
    Assert.assertTrue(diffCallback.areItemsTheSame(item, itemWithMatchingVaccine))
    Assert.assertFalse(diffCallback.areContentsTheSame(item, itemWithMatchingVaccine))
  }
}
