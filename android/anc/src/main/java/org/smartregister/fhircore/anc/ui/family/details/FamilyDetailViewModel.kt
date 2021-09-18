package org.smartregister.fhircore.anc.ui.family.details

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.anc.AncApplication
import org.smartregister.fhircore.anc.data.family.FamilyDetailRepository
import org.smartregister.fhircore.anc.data.family.model.FamilyMemberItem
import org.smartregister.fhircore.engine.util.extension.createFactory

class FamilyDetailViewModel(
  application: AncApplication,
  private val repository: FamilyDetailRepository,
) : AndroidViewModel(application), FamilyDetailDataProvider {

  private val mDemographics : LiveData<Patient> by lazy {
    repository.fetchDemographics()
  }

  private val mFamilyMembers: LiveData<List<FamilyMemberItem>> by lazy {
    repository.fetchFamilyMembers()
  }

  private val mEncounters: LiveData<List<Encounter>> by lazy {
    repository.fetchEncounters()
  }

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
    return mEncounterItemClickListener;
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

  companion object {
    fun get(owner: ViewModelStoreOwner, application: AncApplication, repository: FamilyDetailRepository): FamilyDetailViewModel {
      return ViewModelProvider(owner, FamilyDetailViewModel(application, repository).createFactory())[FamilyDetailViewModel::class.java]
    }
  }
}