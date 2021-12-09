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

import androidx.core.os.bundleOf
import io.mockk.mockk
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.smartregister.fhircore.anc.robolectric.RobolectricTest
import org.smartregister.fhircore.anc.ui.anccare.details.AncDetailsFragment
import org.smartregister.fhircore.anc.ui.details.careplan.CarePlanDetailsFragment
import org.smartregister.fhircore.anc.ui.details.vitalsigns.VitalSignsDetailsFragment

class ViewPagerAdapterTest : RobolectricTest() {

  private lateinit var viewPagerAdapter: ViewPagerAdapter

  @Before
  fun setUp() {
    viewPagerAdapter = ViewPagerAdapter(mockk(), mockk(), true, bundleOf())
  }

  @Test
  fun testGetItemCountShouldReturnValidCount() {
    Assert.assertEquals(2, viewPagerAdapter.itemCount)
  }

  @Test
  fun testCreateFragmentShouldReturnExpectedFragment() {

    Assert.assertEquals(
      AncDetailsFragment::class.simpleName,
      viewPagerAdapter.createFragment(0).javaClass.simpleName
    )
    Assert.assertEquals(
      VitalSignsDetailsFragment::class.simpleName,
      viewPagerAdapter.createFragment(1).javaClass.simpleName
    )
    Assert.assertEquals(
      AncDetailsFragment::class.simpleName,
      viewPagerAdapter.createFragment(-1).javaClass.simpleName
    )

    viewPagerAdapter = ViewPagerAdapter(mockk(), mockk(), false, bundleOf())

    Assert.assertEquals(
      CarePlanDetailsFragment::class.simpleName,
      viewPagerAdapter.createFragment(0).javaClass.simpleName
    )
    Assert.assertEquals(
      VitalSignsDetailsFragment::class.simpleName,
      viewPagerAdapter.createFragment(1).javaClass.simpleName
    )
    Assert.assertEquals(
      CarePlanDetailsFragment::class.simpleName,
      viewPagerAdapter.createFragment(-1).javaClass.simpleName
    )
  }
}
