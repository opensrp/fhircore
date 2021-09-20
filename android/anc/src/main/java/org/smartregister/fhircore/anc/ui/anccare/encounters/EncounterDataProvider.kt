package org.smartregister.fhircore.anc.ui.anccare.encounters

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import org.smartregister.fhircore.anc.data.model.EncounterItem

interface EncounterDataProvider {

  fun getEncounterList(): Flow<PagingData<EncounterItem>>
  fun getAppBackClickListener(): () -> Unit = {}
}