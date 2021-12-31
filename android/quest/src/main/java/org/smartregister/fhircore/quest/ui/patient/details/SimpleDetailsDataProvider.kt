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

package org.smartregister.fhircore.quest.ui.patient.details

import androidx.annotation.StringRes
import androidx.lifecycle.MutableLiveData
import org.smartregister.fhircore.quest.data.patient.model.DetailsViewItem

interface SimpleDetailsDataProvider {
  val detailsViewItem: MutableLiveData<DetailsViewItem>

  val onBackPressClicked: MutableLiveData<Boolean>
    get() = MutableLiveData(false)

  val onMenuItemClicked: MutableLiveData<Int>
    get() = MutableLiveData(-1)

  fun onMenuItemClickListener(@StringRes id: Int) {
    onMenuItemClicked.value = id
  }

  fun onBackPressed(backPressed: Boolean) {
    onBackPressClicked.value = backPressed
  }
}
