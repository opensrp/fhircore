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

package org.smartregister.fhircore.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import org.smartregister.fhircore.R
import org.smartregister.fhircore.activity.core.BaseRegisterActivity
import org.smartregister.fhircore.adapter.AncItemRecyclerViewAdapter
import org.smartregister.fhircore.model.BaseRegister
import org.smartregister.fhircore.model.PatientItem
import org.smartregister.fhircore.viewholder.AncItemViewHolder
import org.smartregister.fhircore.viewmodel.AncListViewModel

class AncListFragment : BaseListFragment<PatientItem, AncItemViewHolder>() {
  private val viewModel by activityViewModels<AncListViewModel>()

  override fun getFragmentListLayout(): Int {
    return R.layout.anc_fragment_list
  }

  private fun getRegister(): BaseRegister {
    return (requireActivity() as BaseRegisterActivity).register
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    setupListFragment(
      R.id.anc_list,
      viewModel.paginatedDataList,
      AncItemRecyclerViewAdapter(this::onListItemClicked),
      view
    )
    setupSearch(getRegister().searchBox()!!)
    setupEmptyListView(R.id.empty_list_message_container, view)
    setupProgress(R.id.loader_overlay, viewModel.loader)

    super.onViewCreated(view, savedInstanceState)
  }

  fun onListItemClicked(patientItem: PatientItem) {}

  override fun loadData(currentSearch: String?, page: Int, pageSize: Int) {
    viewModel.searchResults(currentSearch, page, pageSize)
  }
}
