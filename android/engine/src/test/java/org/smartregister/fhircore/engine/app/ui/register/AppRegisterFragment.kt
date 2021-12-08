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

package org.smartregister.fhircore.engine.app.ui.register

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.paging.compose.LazyPagingItems
import io.mockk.mockk
import org.smartregister.fhircore.engine.ui.register.ComposeRegisterFragment
import org.smartregister.fhircore.engine.ui.register.RegisterDataViewModel
import org.smartregister.fhircore.engine.ui.register.model.RegisterFilterType
import org.smartregister.fhircore.engine.util.ListenerIntent

class AppRegisterFragment : ComposeRegisterFragment<String, String>() {

  override fun navigateToDetails(uniqueIdentifier: String) {
    // Overridden Do nothing
  }

  override fun onItemClicked(listenerIntent: ListenerIntent, data: String) {
    // Overridden Do nothing
  }

  override fun initializeRegisterDataViewModel(): RegisterDataViewModel<String, String> = mockk()

  override fun performFilter(
    registerFilterType: RegisterFilterType,
    data: String,
    value: Any
  ): Boolean = data.isNotEmpty()

  @Composable
  override fun ConstructRegisterList(pagingItems: LazyPagingItems<String>) {
    LazyColumn() {}
  }

  companion object {
    const val TAG = "AppRegisterFragment"
  }
}
