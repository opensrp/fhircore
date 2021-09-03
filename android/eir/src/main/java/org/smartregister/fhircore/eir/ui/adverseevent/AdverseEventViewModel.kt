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

package org.smartregister.fhircore.eir.ui.adverseevent

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.PositiveIntType
import org.smartregister.fhircore.engine.data.local.repository.patient.PatientRepository
import org.smartregister.fhircore.engine.data.local.repository.patient.model.PatientVaccineSummary
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.DispatcherProvider

class AdverseEventViewModel(
  application: Application,
  private val patientRepository: PatientRepository,
  private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider
) : AndroidViewModel(application) {

  fun getPatientImmunization(logicalId: String): LiveData<Immunization> {
    val mutableLiveData: MutableLiveData<Immunization> = MutableLiveData()
    viewModelScope.launch(dispatcherProvider.io()) {
      val immunizations = patientRepository.getPatientImmunizations(logicalId = logicalId)
      if (!immunizations.isNullOrEmpty()) {
        mutableLiveData.postValue(immunizations.first())
      }
    }
    return mutableLiveData
  }
}
