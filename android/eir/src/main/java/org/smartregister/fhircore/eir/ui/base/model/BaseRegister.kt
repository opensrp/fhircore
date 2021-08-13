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

package org.smartregister.fhircore.eir.ui.base.model

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
