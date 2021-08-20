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

package org.smartregister.fhircore.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.fhir.FhirEngine

class FhirListViewModelFactory(
  private val application: Application,
  private val fhirEngine: FhirEngine
) : ViewModelProvider.Factory {
  override fun <T : ViewModel?> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(BaseViewModel::class.java)) {
      return BaseViewModel(application, fhirEngine) as T
    }
    if (modelClass.isAssignableFrom(CovaxListViewModel::class.java)) {
      return CovaxListViewModel(application, fhirEngine) as T
    }
    if (modelClass.isAssignableFrom(FamilyListViewModel::class.java)) {
      return FamilyListViewModel(application, fhirEngine) as T
    }
    if (modelClass.isAssignableFrom(AncListViewModel::class.java)) {
      return AncListViewModel(application, fhirEngine) as T
    }
    throw IllegalArgumentException("Unknown ViewModel class")
  }
}
