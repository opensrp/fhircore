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

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import org.smartregister.fhircore.anc.ui.anccare.details.AncDetailsFragment
import org.smartregister.fhircore.anc.ui.details.careplan.CarePlanDetailsFragment
import org.smartregister.fhircore.anc.ui.details.vitalsigns.VitalSignsDetailsFragment

private const val NUM_TABS = 2

class ViewPagerAdapter(
  fragmentManager: FragmentManager,
  lifecycle: Lifecycle,
  val isPregnant: Boolean,
  val bundleOf: Bundle
) : FragmentStateAdapter(fragmentManager, lifecycle) {

  override fun getItemCount(): Int {
    return NUM_TABS
  }

  override fun createFragment(position: Int): Fragment {
    if (isPregnant) {
      when (position) {
        0 -> return AncDetailsFragment.newInstance(bundleOf)
        1 -> return VitalSignsDetailsFragment.newInstance(bundleOf)
      }
    } else {
      when (position) {
        0 -> return CarePlanDetailsFragment.newInstance(bundleOf)
        1 -> return VitalSignsDetailsFragment.newInstance(bundleOf)
      }
    }
    return if (isPregnant) AncDetailsFragment.newInstance(bundleOf)
    else CarePlanDetailsFragment.newInstance(bundleOf)
  }
}
