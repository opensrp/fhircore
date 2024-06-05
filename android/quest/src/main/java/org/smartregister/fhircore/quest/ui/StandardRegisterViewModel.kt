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

package org.smartregister.fhircore.quest.ui

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.smartregister.fhircore.quest.ui.shared.models.RegisterViewData

interface StandardRegisterViewModel {
  fun onEvent(event: StandardRegisterEvent)

  fun refresh()

  fun progressMessage(): String

  val isRefreshing: StateFlow<Boolean>
  val currentPage: StateFlow<Int>
  val pageRegisterListItemData: StateFlow<Flow<PagingData<RegisterViewData.ListItemView>>>
  val pageNavigationItemViewData:
    StateFlow<Flow<PagingData<RegisterViewData.PageNavigationItemView>>>
  val searchText: StateFlow<String>
}
