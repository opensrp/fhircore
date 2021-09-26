package org.smartregister.fhircore.anc.ui.madx.details.adapter

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import org.smartregister.fhircore.anc.ui.madx.details.nonanccareplan.CarePlanDetailsFragment
import org.smartregister.fhircore.anc.ui.madx.details.vitalsigns.VitalSignsDetailsFragment

private const val NUM_TABS = 2

class ViewPagerAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
    val bundleOf: Bundle
) :
    FragmentStateAdapter(fragmentManager, lifecycle) {

    override fun getItemCount(): Int {
        return NUM_TABS
    }

    override fun createFragment(position: Int): Fragment {
        when (position) {
            0 -> return CarePlanDetailsFragment.newInstance(bundleOf)
            1 -> return VitalSignsDetailsFragment.newInstance(bundleOf)
        }
        return CarePlanDetailsFragment.newInstance(bundleOf)
    }


}