package org.smartregister.fhircore.engine.ui.register

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

/**
 * Subclass of [FragmentStateAdapter] that provides the adapter for the ViewPager.
 * [supportedFragments] are the fragments we want to display on the view pager
 */
class RegisterPagerAdapter(
  activity: FragmentActivity,
  private val supportedFragments: List<Fragment>
) : FragmentStateAdapter(activity) {

  override fun getItemCount() = supportedFragments.size
  override fun createFragment(position: Int): Fragment {
    if (supportedFragments.isEmpty())
      throw IllegalAccessException(
        "Provide at least one fragment extending the BaseRegisterFragment"
      )
    return supportedFragments[position]
  }
}
