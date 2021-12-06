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

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Task
import org.smartregister.fhircore.anc.AncApplication
import org.smartregister.fhircore.anc.data.family.FamilyDetailRepository
import org.smartregister.fhircore.anc.data.family.model.FamilyMemberItem
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.extension.createFactory

class FamilyDetailViewModel(
  application: AncApplication,
  private val repository: FamilyDetailRepository,
) : AndroidViewModel(application), FamilyDetailDataProvider {

  private val demographics by lazy { wrapped { repository.fetchDemographics() } }

  private val familyMembers by lazy { wrapped { repository.fetchFamilyMembers() } }

  private val encounters by lazy { wrapped { repository.fetchEncounters() } }

  private val familyCarePlans by lazy { wrapped { repository.fetchFamilyCarePlans() } }

  private fun <T> wrapped(block: suspend () -> T): MutableLiveData<T> {
    val result = MutableLiveData<T>()
    viewModelScope.launch {
      result.postValue(block())
    }
    return result
  }

  fun reloadData() {
    viewModelScope.launch{
      demographics.postValue(repository.fetchDemographics())
      familyMembers.postValue(repository.fetchFamilyMembers())
      encounters.postValue(repository.fetchEncounters())
      familyCarePlans.postValue(repository.fetchFamilyCarePlans())
    }
  }

  private var mAppBackClickListener: () -> Unit = {}
  private var mAddMemberItemClickListener: () -> Unit = {}
  private var mMemberItemClickListener: (item: FamilyMemberItem) -> Unit = {}
  private var mSeeAllEncounterClickListener: () -> Unit = {}
  private var mEncounterItemClickListener: (item: Encounter) -> Unit = {}
  private var mSeeAllUpcomingServiceClickListener: () -> Unit = {}
  private var mUpcomingServiceItemClickListener: (item: Task) -> Unit = {}

  override fun getDemographics(): LiveData<Patient> {
    return demographics
  }

  override fun getFamilyMembers(): LiveData<List<FamilyMemberItem>> {
    return familyMembers
  }

  override fun getEncounters(): LiveData<List<Encounter>> {
    return encounters
  }

  override fun getFamilyCarePlans(): LiveData<List<CarePlan>> {
    return familyCarePlans
  }

  override fun getAppBackClickListener(): () -> Unit {
    return mAppBackClickListener
  }

  override fun getMemberItemClickListener(): (item: FamilyMemberItem) -> Unit {
    return mMemberItemClickListener
  }

  override fun getAddMemberItemClickListener(): () -> Unit {
    return mAddMemberItemClickListener
  }

  override fun getSeeAllEncounterClickListener(): () -> Unit {
    return mSeeAllEncounterClickListener
  }

  override fun getEncounterItemClickListener(): (item: Encounter) -> Unit {
    return mEncounterItemClickListener
  }

  override fun getSeeAllUpcomingServiceClickListener(): () -> Unit {
    return mSeeAllUpcomingServiceClickListener
  }

  override fun getUpcomingServiceItemClickListener(): (item: Task) -> Unit {
    return mUpcomingServiceItemClickListener
  }

  fun setAppBackClickListener(listener: () -> Unit) {
    mAppBackClickListener = listener
  }

  fun setMemberItemClickListener(listener: (item: FamilyMemberItem) -> Unit) {
    mMemberItemClickListener = listener
  }

  fun setAddMemberItemClickListener(listener: () -> Unit) {
    mAddMemberItemClickListener = listener
  }

  fun setSeeAllEncounterClickListener(listener: () -> Unit) {
    mSeeAllEncounterClickListener = listener
  }

  fun setEncounterItemClickListener(listener: (item: Encounter) -> Unit) {
    mEncounterItemClickListener = listener
  }

  fun setSeeAllUpcomingServiceClickListener(listener: () -> Unit) {
    mSeeAllUpcomingServiceClickListener = listener
  }

  fun setUpcomingServiceItemClickListener(listener: (item: Task) -> Unit) {
    mUpcomingServiceItemClickListener = listener
  }

  companion object {
    fun get(
      owner: ViewModelStoreOwner,
      application: AncApplication,
      repository: FamilyDetailRepository
    ): FamilyDetailViewModel {
      return ViewModelProvider(
        owner,
        FamilyDetailViewModel(application, repository).createFactory()
      )[FamilyDetailViewModel::class.java]
    }
  }
}
