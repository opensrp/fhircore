package org.smartregister.fhircore.anc.ui.family.details

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.anc.data.family.model.FamilyMemberItem

interface FamilyDetailDataProvider {

  fun getDemographics(): LiveData<Patient>
  fun getFamilyMembers(): LiveData<List<FamilyMemberItem>>
  fun getEncounters() : LiveData<List<Encounter>>

  fun getAppBackClickListener(): () -> Unit = {}
  fun getMemberItemClickListener(): (item: FamilyMemberItem) -> Unit = {}
  fun getAddMemberItemClickListener(): () -> Unit = {}
  fun getSeeAllEncounterClickListener(): () -> Unit = {}
  fun getEncounterItemClickListener(): (item: Encounter) -> Unit = {}
}