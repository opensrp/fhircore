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
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.search.Order
import com.google.android.fhir.search.search
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.util.Utils
import timber.log.Timber

class BaseViewModel(application: Application, private val fhirEngine: FhirEngine) :
  AndroidViewModel(application) {
  var covaxClientsCount = MutableLiveData(0)

  fun loadClientCount() {
    Timber.d("Loading client counts")

    viewModelScope.launch {
      val p: List<Patient> =
        fhirEngine.search {
          Utils.addBasePatientFilter(this)

          apply {}
          sort(Patient.GIVEN, Order.ASCENDING)
        }

      covaxClientsCount.value = p.size // TODO use a proper count query after Google devs respond

      Timber.d("Loaded %s clients from db", p.size)
    }
  }

  class BaseViewModelFactory(
    private val mApplication: Application,
    private val fhirEngine: FhirEngine
  ) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
      return BaseViewModel(mApplication, fhirEngine) as T
    }
  }
}
