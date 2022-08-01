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

package org.smartregister.fhircore.quest.ui.patient.remove

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.engine.data.local.register.PatientRegisterRepository
import timber.log.Timber

@HiltViewModel
internal class HivPatientViewModel @Inject constructor(val repository: PatientRegisterRepository) :
  ViewModel() {

  val profile = MutableLiveData<Patient>()
  var isRemoved = MutableLiveData(false)
  var isDiscarded = MutableLiveData(false)

  fun fetch(profileId: String) {
    viewModelScope.launch { profile.postValue(repository.loadResource(profileId)) }
  }

  fun remove(profileId: String) {
    viewModelScope.launch {
      try {
        repository.registerDaoFactory.hivRegisterDao.removePatient(profileId)
        isRemoved.postValue(true)
      } catch (e: Exception) {
        Timber.e(e)
        isDiscarded.postValue(true)
      }
    }
  }

  fun discard() {
    isDiscarded.postValue(true)
  }
}
