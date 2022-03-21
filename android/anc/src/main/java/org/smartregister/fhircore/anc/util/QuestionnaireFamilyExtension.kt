package org.smartregister.fhircore.anc.util

import androidx.lifecycle.MutableLiveData
import org.smartregister.fhircore.anc.data.family.model.FamilyMemberItem


fun MutableLiveData<List<FamilyMemberItem>>.othersEligibleForHead() =
    this.value?.filter { it.deathDate == null && !it.houseHoldHead }