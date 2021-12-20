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

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.anc.data.family.FamilyDetailRepository
import org.smartregister.fhircore.anc.data.family.model.FamilyMemberItem
import timber.log.Timber

@HiltViewModel
class FamilyDetailViewModel
@Inject
constructor(
  val repository: FamilyDetailRepository,
) : ViewModel() {

  var isRemoveFamily = MutableLiveData(false)

  var isRemoveFamilyMenuItemClicked = MutableLiveData(false)

  val addNewMember = MutableLiveData(false)

  val backClicked = MutableLiveData(false)

  val memberItemClicked = MutableLiveData<FamilyMemberItem>(null)

  val encounterItemClicked = MutableLiveData<Encounter>(null)

  val demographics = MutableLiveData<Patient>()

  val familyMembers = MutableLiveData<List<FamilyMemberItem>>()

  val encounters = MutableLiveData<List<Encounter>>()

  val familyCarePlans = MutableLiveData<List<CarePlan>>()

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
  fun removeFamily(familyId: String) {

    viewModelScope.launch {
      try {
        val family: Patient =
          repository.loadResource(familyId)
            ?: throw ResourceNotFoundException("Family resource for that ID NOT Found")

        repository.delete(family)
        isRemoveFamily.postValue(true)
      } catch (e: Exception) {
        Timber.e(e)
      }
    }
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

  fun onRemoveFamilyMenuItemClicked() {
    isRemoveFamilyMenuItemClicked.value = true
  }

  fun onSeeAllEncountersListener() {}

  fun onSeeUpcomingServicesListener() {}

  fun onEncounterItemClicked(encounter: Encounter) {
    // TODO handle click listener for encounter
  }
}
