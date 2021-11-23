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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import dagger.hilt.android.lifecycle.HiltViewModel
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.anc.AncApplication
import org.smartregister.fhircore.anc.data.family.FamilyDetailRepository
import org.smartregister.fhircore.anc.data.family.model.FamilyMemberItem
import org.smartregister.fhircore.engine.util.extension.createFactory
import javax.inject.Inject

@HiltViewModel
class FamilyDetailViewModel @Inject constructor(
  val repository: FamilyDetailRepository,
) : ViewModel(), FamilyDetailDataProvider {

  private val mDemographics: LiveData<Patient> by lazy { repository.fetchDemographics() }

  private val mFamilyMembers: LiveData<List<FamilyMemberItem>> by lazy {
    repository.fetchFamilyMembers()
  }

  private val mEncounters: LiveData<List<Encounter>> by lazy { repository.fetchEncounters() }

  private var mAppBackClickListener: () -> Unit = {}
  private var mAddMemberItemClickListener: () -> Unit = {}
  private var mMemberItemClickListener: (item: FamilyMemberItem) -> Unit = {}
  private var mSeeAllEncounterClickListener: () -> Unit = {}
  private var mEncounterItemClickListener: (item: Encounter) -> Unit = {}

  override fun getDemographics(): LiveData<Patient> {
    return mDemographics
  }

  override fun getFamilyMembers(): LiveData<List<FamilyMemberItem>> {
    return mFamilyMembers
  }

  override fun getEncounters(): LiveData<List<Encounter>> {
    return mEncounters
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
}
