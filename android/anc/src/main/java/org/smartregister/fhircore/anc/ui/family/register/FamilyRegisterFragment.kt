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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.paging.compose.LazyPagingItems
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.smartregister.fhircore.anc.data.family.FamilyRepository
import org.smartregister.fhircore.anc.data.family.model.FamilyItem
import org.smartregister.fhircore.anc.ui.family.details.FamilyDetailsActivity
import org.smartregister.fhircore.anc.ui.family.register.components.FamilyList
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_ARG_PATIENT_KEY
import org.smartregister.fhircore.engine.ui.register.ComposeRegisterFragment
import org.smartregister.fhircore.engine.ui.register.RegisterDataViewModel
import org.smartregister.fhircore.engine.ui.register.model.RegisterFilterType
import org.smartregister.fhircore.engine.util.ListenerIntent
import org.smartregister.fhircore.engine.util.extension.createFactory

@AndroidEntryPoint
class FamilyRegisterFragment : ComposeRegisterFragment<Family, FamilyItem>() {

  @Inject lateinit var familyRepository: FamilyRepository

  override fun navigateToDetails(uniqueIdentifier: String) {
    startActivity(
      Intent(requireActivity(), FamilyDetailsActivity::class.java).apply {
        putExtra(QUESTIONNAIRE_ARG_PATIENT_KEY, uniqueIdentifier)
      }
    )
  }

  @Composable
  override fun ConstructRegisterList(pagingItems: LazyPagingItems<FamilyItem>, modifier: Modifier) {
    FamilyList(
      pagingItems = pagingItems,
      modifier = Modifier,
      clickListener = { listenerIntent, data -> onItemClicked(listenerIntent, data) }
    )
  }

  override fun onItemClicked(listenerIntent: ListenerIntent, data: FamilyItem) {
    if (listenerIntent is OpenFamilyProfile) {
      navigateToDetails(data.id)
    }
  }

  /**
   * Filters the given data if there is a matching condition.
   *
   * SEARCH_FILTER will filters data based on name OR id OR identifier.
   *
   * OVERDUE_FILTER will filters data based on services overdue.
   *
   * @param registerFilterType the filter type
   * @param data the data that will be filtered
   * @param value the query
   *
   * @return true if the data should be filtered
   */
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
            data.id.contentEquals(value.toString()) ||
            data.identifier.contentEquals(value.toString())
      }
      RegisterFilterType.OVERDUE_FILTER -> {
        data.servicesOverdue != null && data.servicesOverdue > 0
      }
      else -> false
    }
  }

  override fun initializeRegisterDataViewModel(): RegisterDataViewModel<Family, FamilyItem> {
    val registerDataViewModel =
      RegisterDataViewModel(
        application = requireActivity().application,
        registerRepository = familyRepository
      )
    return ViewModelProvider(viewModelStore, registerDataViewModel.createFactory())[
      registerDataViewModel::class.java]
  }

  companion object {
    const val TAG = "FamilyRegisterFragment"
  }
}
