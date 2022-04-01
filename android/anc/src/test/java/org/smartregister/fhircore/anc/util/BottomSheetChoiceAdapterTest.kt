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

package org.smartregister.fhircore.anc.util

import android.view.ViewGroup
import android.widget.RadioButton
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.smartregister.fhircore.anc.robolectric.RobolectricTest
import org.smartregister.fhircore.anc.util.bottomsheet.BottomSheetChoiceAdapter
import org.smartregister.fhircore.anc.util.bottomsheet.BottomSheetDataModel
import org.smartregister.fhircore.anc.util.bottomsheet.OnClickListener

class BottomSheetChoiceAdapterTest : RobolectricTest() {

  private lateinit var adapter: BottomSheetChoiceAdapter
  private lateinit var onClickListener: OnClickListener

  @Before
  fun setUp() {
    onClickListener =
      object : OnClickListener {
        override fun onClick(rb: RadioButton, position: Int) {}
      }
    adapter = BottomSheetChoiceAdapter(onClickListener = onClickListener)
  }

  @Test
  fun testVerifyAdapterMethodsCall() {

    val viewGroup = mockk<ViewGroup>()
    every { viewGroup.context } returns ApplicationProvider.getApplicationContext()

    val list =
      mutableListOf(
        BottomSheetDataModel(id = "12", itemName = "1111", itemDetail = "first", selected = false)
      )
    adapter.submitList(list)

    val viewHolder = adapter.createViewHolder(viewGroup, 0)
    Assert.assertNotNull(viewHolder)

    adapter.bindViewHolder(viewHolder, 0)
  }

  @Test
  fun testAdapterDiffUtilEquatesDifferentObjectsWithSameId() {

    val diffCallback = BottomSheetChoiceAdapter.BottomSheetDataModelItemDiffCallback
    val item =
      BottomSheetDataModel(id = "12", itemName = "1111", itemDetail = "first", selected = false)

    // change title only
    val itemDifferentTitle =
      BottomSheetDataModel(id = "11", itemName = "1111", itemDetail = "first", selected = false)
    Assert.assertFalse(diffCallback.areItemsTheSame(item, itemDifferentTitle))
    Assert.assertFalse(diffCallback.areContentsTheSame(item, itemDifferentTitle))

    // same title with different content
    val itemDifferentSelection =
      BottomSheetDataModel(id = "12", itemName = "1111", itemDetail = "first", selected = true)
    Assert.assertTrue(diffCallback.areItemsTheSame(item, itemDifferentSelection))
    Assert.assertFalse(diffCallback.areContentsTheSame(item, itemDifferentSelection))

    // identical items
    val identical =
      BottomSheetDataModel(id = "12", itemName = "1111", itemDetail = "first", selected = false)
    Assert.assertTrue(diffCallback.areItemsTheSame(item, identical))
    Assert.assertTrue(diffCallback.areContentsTheSame(item, identical))
  }
}
