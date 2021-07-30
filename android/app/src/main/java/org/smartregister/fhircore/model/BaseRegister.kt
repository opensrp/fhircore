package org.smartregister.fhircore.model

import android.app.Activity
import android.view.View
import android.widget.EditText
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2

data class BaseRegister(
  val context: Activity,
  @LayoutRes val contentLayoutId: Int,
  val listFragment: Fragment,
  @IdRes val viewPagerId: Int,
  @IdRes var newRegistrationViewId: Int? = null,
  var newRegistrationQuestionnaireIdentifier: String? = null,
  var newRegistrationQuestionnaireTitle: String? = null,
  @IdRes var searchBoxId: Int? = null,
  @IdRes var barcodeScannerViewId: Int? = null,
  ) {
  fun viewPager(): ViewPager2 {
    return context.findViewById(viewPagerId)
  }

  fun newRegistrationView(): View? {
    newRegistrationViewId ?: return null
    return context.findViewById(newRegistrationViewId!!)
  }

  fun searchBox(): EditText? {
    searchBoxId ?: return null

    return context.findViewById(searchBoxId!!)
  }
}
