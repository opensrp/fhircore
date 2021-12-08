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

package org.smartregister.fhircore.anc.ui.family.details

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.anc.data.family.FamilyDetailRepository
import org.smartregister.fhircore.anc.data.family.model.FamilyMemberItem

@HiltViewModel
class FamilyDetailViewModel
@Inject
constructor(
  val repository: FamilyDetailRepository,
) : ViewModel() {

  val addNewMember = MutableLiveData(false)

  val changeHead = MutableLiveData(false)

  val backClicked = MutableLiveData(false)

  val memberItemClicked = MutableLiveData<FamilyMemberItem>(null)

  val encounterItemClicked = MutableLiveData<Encounter>(null)

  val demographics = MutableLiveData<Patient>()

  val familyMembers = MutableLiveData<List<FamilyMemberItem>>()

  val encounters = MutableLiveData<List<Encounter>>()

  val familyCarePlans = MutableLiveData<List<CarePlan>>()

  fun MutableLiveData<List<FamilyMemberItem>>.othersEligibleForHead() =
    this.value?.filter { it.deathDate == null && !it.houseHoldHead }

  fun fetchDemographics(familyId: String) {
    viewModelScope.launch { demographics.postValue(repository.fetchDemographics(familyId)) }
  }

  fun fetchFamilyMembers(familyId: String) {
    viewModelScope.launch { familyMembers.postValue(repository.fetchFamilyMembers(familyId)) }
  }

  fun fetchCarePlans(familyId: String) {
    viewModelScope.launch { familyCarePlans.postValue(repository.fetchFamilyCarePlans(familyId)) }
  }

  fun fetchEncounters(familyId: String) {
    viewModelScope.launch { encounters.postValue(repository.fetchEncounters(familyId)) }
  }

  fun changeFamilyHead(currentHead: String, newHead: String): LiveData<Boolean> {
    val changed = MutableLiveData(false)
    viewModelScope.launch {
      repository.familyRepository.changeFamilyHead(currentHead, newHead)
      changed.postValue(true)
    }
    return changed
  }

  fun onMemberItemClick(familyMemberItem: FamilyMemberItem) {
    memberItemClicked.value = familyMemberItem
  }

  fun onAppBackClick() {
    backClicked.value = true
  }

  fun onAddMemberItemClicked() {
    addNewMember.value = true
  }

  fun onChangeHeadClicked() {
    changeHead.value = true
  }

  fun onSeeAllEncountersListener() {}

  fun onSeeUpcomingServicesListener() {}

  fun onEncounterItemClicked(encounter: Encounter) {
    // TODO handle click listener for encounter
  }
}
