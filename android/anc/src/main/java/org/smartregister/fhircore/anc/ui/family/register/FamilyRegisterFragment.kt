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
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.emptyFlow
import org.smartregister.fhircore.anc.AncApplication
import org.smartregister.fhircore.anc.data.family.FamilyRepository
import org.smartregister.fhircore.anc.data.family.model.FamilyItem
import org.smartregister.fhircore.anc.ui.family.FamilyFormConfig
import org.smartregister.fhircore.anc.ui.family.details.FamilyDetailsActivity
import org.smartregister.fhircore.anc.ui.family.register.components.FamilyList
import org.smartregister.fhircore.engine.ui.register.ComposeRegisterFragment
import org.smartregister.fhircore.engine.ui.register.RegisterDataViewModel
import org.smartregister.fhircore.engine.ui.register.model.RegisterFilterType
import org.smartregister.fhircore.engine.util.ListenerIntent
import org.smartregister.fhircore.engine.util.extension.createFactory

class FamilyRegisterFragment : ComposeRegisterFragment<Family, FamilyItem>() {

  lateinit var familyRepository: FamilyRepository

  override lateinit var registerDataViewModel: RegisterDataViewModel<Family, FamilyItem>

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    familyRepository =
      FamilyRepository(
        (requireActivity().application as AncApplication).fhirEngine,
        FamilyItemMapper
      )

    registerDataViewModel =
      ViewModelProvider(
        requireActivity(),
        RegisterDataViewModel(
            application = requireActivity().application,
            registerRepository = familyRepository
          )
          .createFactory()
      )[RegisterDataViewModel::class.java] as
        RegisterDataViewModel<Family, FamilyItem>
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
    val registerData = registerDataViewModel.registerData.collectAsState(emptyFlow())
    FamilyList(
      pagingItems = registerData.value!!.collectAsLazyPagingItems(),
      modifier = Modifier,
      clickListener = { listenerIntent, data -> onItemClicked(listenerIntent, data) }
    )
  }

  override fun onItemClicked(listenerIntent: ListenerIntent, data: FamilyItem) {
    if (listenerIntent is OpenFamilyProfile) {
      navigateToDetails(data.id)
    }
  }

  // todo maybe we need to do a db call
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
