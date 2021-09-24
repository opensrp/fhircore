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

package org.smartregister.fhircore.anc.data.family

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.search.search
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.anc.data.family.model.FamilyMemberItem
import org.smartregister.fhircore.anc.ui.family.register.FamilyItemMapper
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.DispatcherProvider

class FamilyDetailRepository(
  private val familyId: String,
  private val fhirEngine: FhirEngine,
  private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider
) {

  fun fetchDemographics(): LiveData<Patient> {
    val data = MutableLiveData<Patient>()
    CoroutineScope(dispatcherProvider.io()).launch {
      data.postValue(fhirEngine.load(Patient::class.java, familyId))
    }
    return data
  }

  fun fetchFamilyMembers(): LiveData<List<FamilyMemberItem>> {
    val data = MutableLiveData<List<FamilyMemberItem>>()
    CoroutineScope(dispatcherProvider.io()).launch {
      val members =
        fhirEngine
          .search<Patient> { filter(Patient.LINK) { this.value = "Patient/$familyId" } }
          .map { FamilyItemMapper.toFamilyMemberItem(it) }
      data.postValue(members)
    }
    return data
  }

  fun fetchEncounters(): LiveData<List<Encounter>> {
    val data = MutableLiveData<List<Encounter>>()
    CoroutineScope(dispatcherProvider.io()).launch {
      val encounters =
        fhirEngine.search<Encounter> {
          filter(Encounter.SUBJECT) { value = "Patient/$familyId" }
          from = 0
          count = 3
        }
      data.postValue(encounters)
    }
    return data
  }
}
