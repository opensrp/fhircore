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

package org.smartregister.fhircore.anc.ui.family.details.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.search.search
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.anc.data.family.model.FamilyMemberItem
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.DispatcherProvider

class FamilyDetailsViewModel(
  var dispatcher: DispatcherProvider = DefaultDispatcherProvider,
  val fhirEngine: FhirEngine,
  val familyId: String
) : ViewModel() {

  val familyDemographics = MutableLiveData<Patient>()
  val familyMembers = MutableLiveData<List<FamilyMemberItem>>()
  val familyEncounters = MutableLiveData<List<Encounter>>()

  fun fetchDemographics() {
    if (familyId.isNotEmpty())
      viewModelScope.launch(dispatcher.io()) {
        val patient = fhirEngine.load(Patient::class.java, familyId)
        familyDemographics.postValue(patient)
      }
  }

  fun fetchFamilyMembers() {

    viewModelScope.launch(dispatcher.io()) {
      val members =
        fhirEngine.search<Patient> { filter(Patient.LINK) { this.value = familyId } }.map {
          it.toFamilyMemberItem()
        }
      familyMembers.postValue(members)
    }
  }

  fun fetchEncounters() {
    viewModelScope.launch(dispatcher.io()) {
      val encounters =
        fhirEngine.search<Encounter> { filter(Encounter.SUBJECT) { value = "Patient/$familyId" } }
      familyEncounters.postValue(encounters)
    }
  }

  private fun Patient.toFamilyMemberItem(): FamilyMemberItem {
    val name = this.name?.first()?.nameAsSingleString ?: ""
    val gender = if (this.hasGenderElement()) this.genderElement.valueAsString else ""
    val dob = if (this.hasBirthDateElement()) this.birthDateElement.valueAsString else ""
    val ext = this.extension.firstOrNull { it.value.toString().contains("pregnant", true) }
    val pregnant = ext?.value?.toString() ?: ""

    return FamilyMemberItem(this.id, pregnant, name, gender, dob)
  }
}