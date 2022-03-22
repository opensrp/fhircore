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

package org.smartregister.fhircore.anc.ui.family.removefamily

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.anc.data.family.FamilyDetailRepository
import timber.log.Timber

@HiltViewModel
class RemoveFamilyViewModel
@Inject
constructor(
  val repository: FamilyDetailRepository,
) : ViewModel() {

  var isRemoveFamily = MutableLiveData(false)
  var discardRemoving = MutableLiveData(false)
  val demographics = MutableLiveData<Patient>()

  fun fetchFamilyName(familyId: String) {
    viewModelScope.launch { demographics.postValue(repository.fetchDemographics(familyId)) }
  }

  fun removeFamily(familyId: String) {
    viewModelScope.launch {
      try {
        val family: Patient = repository.fetchDemographics(familyId)
        repository.delete(family)
        isRemoveFamily.postValue(true)
      } catch (e: Exception) {
        Timber.e(e)
        discardRemoving.postValue(true)
      }
    }
  }

  fun discardRemovingFamily() {
    discardRemoving.postValue(true)
  }
}
