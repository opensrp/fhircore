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

package org.smartregister.fhircore.anc.ui.details.child

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.fhir.FhirEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.smartregister.fhircore.anc.ui.details.child.model.ChildProfileViewData

@HiltViewModel
class ChildDetailsViewModel @Inject constructor(val fhirEngine: FhirEngine) : ViewModel() {

  val childProfileViewData: MutableState<ChildProfileViewData> =
    mutableStateOf(ChildProfileViewData())
  fun retrieveChildProfileViewData(patientId: String) {
    // TODO retrieve patient details and tasks. Can write a custom mapper to handle this
    viewModelScope.launch {}
  }
}
