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

package org.smartregister.fhircore.engine.ui.register

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import org.smartregister.fhircore.engine.configuration.view.RegisterViewConfiguration
import org.smartregister.fhircore.engine.ui.register.model.RegisterFilterType
import org.smartregister.fhircore.engine.util.ListenerIntent

abstract class BaseRegisterFragment<I : Any, O : Any> : Fragment() {

  open lateinit var registerDataViewModel: RegisterDataViewModel<I, O>

  open val registerViewModel by activityViewModels<RegisterViewModel>()

  open lateinit var registerViewConfiguration: RegisterViewConfiguration

  /**
   * Implement functionality to navigate to details view when an item with [uniqueIdentifier] is
   * clicked
   */
  abstract fun navigateToDetails(uniqueIdentifier: String)

  /**
   * Implement click listener for list row. [listenerIntent] describe the intention of the current
   * click e.g. OPEN_PROFILE, EXIT etc so you can differentiate different click actions performed on
   * the UI elements e.g clicking a button to record vaccine or patient name to open their profile .
   * [data] of type [O] is also passed when item is clicked.
   */
  abstract fun onItemClicked(listenerIntent: ListenerIntent, data: O)

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

    registerDataViewModel =
      initializeRegisterDataViewModel().apply {
        this.currentPage.observe(viewLifecycleOwner, { registerDataViewModel.loadPageData(it) })
      }

    registerViewModel.filterValue.observe(
      viewLifecycleOwner,
      {
        val (registerFilterType, value) = it
        if (value != null) {
          registerDataViewModel.run {
            showResultsCount(true)
            filterRegisterData(
              registerFilterType = registerFilterType,
              filterValue = value,
              registerFilter = this@BaseRegisterFragment::performFilter
            )
          }
        } else {
          registerDataViewModel.run {
            showResultsCount(false)
            reloadCurrentPageData()
          }
        }
      }
    )

    registerViewModel.run {
      refreshRegisterData.observe(
        viewLifecycleOwner,
        { refreshData ->
          if (refreshData) {
            setRefreshRegisterData(false)
            registerDataViewModel.reloadCurrentPageData(refreshTotalRecordsCount = true)
          }
        }
      )
    }

    registerViewModel.lastSyncTimestamp.observe(
      viewLifecycleOwner,
      { registerDataViewModel.setShowLoader(it.isNullOrEmpty()) }
    )
  }

  override fun onResume() {
    super.onResume()
    registerDataViewModel.reloadCurrentPageData(refreshTotalRecordsCount = true)
  }

  override fun onDestroy() {
    viewModelStore.clear()
    super.onDestroy()
  }

  /** Initialize the [RegisterDataViewModel] class */
  @Suppress("UNCHECKED_CAST")
  abstract fun initializeRegisterDataViewModel(): RegisterDataViewModel<I, O>

  /**
   * Generic function to perform any filtering of type [registerFilterType] on the [data]. Returns
   * true when the [value] is is matched on the data. See [RegisterFilterType] for applicable filter
   * types, you can always update the enum to support more custom filters. The filter will be
   * applied to the paginated data
   */
  abstract fun performFilter(registerFilterType: RegisterFilterType, data: O, value: Any): Boolean
}
