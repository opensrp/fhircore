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

package org.smartregister.fhircore.anc.ui.family.register

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import androidx.paging.compose.collectAsLazyPagingItems
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.anc.data.family.FamilyPaginatedRepository
import org.smartregister.fhircore.anc.data.family.model.FamilyItem
import org.smartregister.fhircore.anc.ui.family.FamilyFormConfig
import org.smartregister.fhircore.anc.ui.family.details.FamilyDetailsActivity
import org.smartregister.fhircore.anc.ui.family.register.components.FamilyRow
import org.smartregister.fhircore.engine.ui.components.PaginatedList
import org.smartregister.fhircore.engine.ui.register.BaseRegisterDataViewModel
import org.smartregister.fhircore.engine.ui.register.ComposeRegisterFragment
import org.smartregister.fhircore.engine.ui.register.model.RegisterFilterType
import org.smartregister.fhircore.engine.util.ListenerIntent
import org.smartregister.fhircore.engine.util.extension.createFactory

class FamilyRegisterFragment : ComposeRegisterFragment<Patient, FamilyItem>() {

  override lateinit var paginatedDataSource: FamilyPaginatedRepository

  override lateinit var registerDataViewModel: BaseRegisterDataViewModel<Patient, FamilyItem>

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    registerDataViewModel =
      ViewModelProvider(
        requireActivity(),
        BaseRegisterDataViewModel(
            application = requireActivity().application,
            paginatedDataSource = paginatedDataSource,
          )
          .createFactory()
      ).get()
  }

  override fun navigateToDetails(uniqueIdentifier: String) {
    startActivity(
      Intent(requireActivity(), FamilyDetailsActivity::class.java).apply {
        putExtra(FamilyFormConfig.FAMILY_ARG_ITEM_ID, uniqueIdentifier)
      }
    )
  }

  @Composable
  override fun ConstructRegisterList() {
    val registerData = registerDataViewModel.registerData.observeAsState()
    PaginatedList(
      pagingItems = registerData.value!!.collectAsLazyPagingItems(),
      { familyItem ->
        FamilyRow(
          familyItem = familyItem,
          clickListener = { listenerIntent, data -> onItemClicked(listenerIntent, data) }
        )
      }
    )
  }

  override fun onItemClicked(listenerIntent: ListenerIntent, data: FamilyItem) {
    //todo we may want to provide defaults as well so that we do not have to implement it
    if (listenerIntent is OpenFamilyProfile) {
      navigateToDetails(data.id)
    }
  }

  //todo maybe we need to do a db call
  override fun performFilter(
      registerFilterType: RegisterFilterType,
      data: FamilyItem,
      value: Any
  ): Boolean {
    return when (registerFilterType) {
      RegisterFilterType.SEARCH_FILTER -> {
        if (value is String && value.isEmpty()) return true
        else
          data.name.contains(value.toString(), ignoreCase = true) ||
            data.id.contentEquals(value.toString())
      }
      RegisterFilterType.OVERDUE_FILTER -> {
        return false // todo
      }
    }
  }
}
