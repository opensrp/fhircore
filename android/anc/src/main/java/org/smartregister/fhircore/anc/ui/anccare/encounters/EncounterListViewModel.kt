package org.smartregister.fhircore.anc.ui.anccare.encounters

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import org.smartregister.fhircore.anc.AncApplication
import org.smartregister.fhircore.anc.data.EncounterRepository
import org.smartregister.fhircore.anc.data.model.EncounterItem
import org.smartregister.fhircore.engine.data.domain.util.PaginationUtil
import org.smartregister.fhircore.engine.util.extension.createFactory

class EncounterListViewModel(
  application: Application,
  private val repository: EncounterRepository
) : AndroidViewModel(application), EncounterDataProvider {

  override fun getEncounterList(): Flow<PagingData<EncounterItem>> {
    return Pager(PagingConfig(pageSize = PaginationUtil.DEFAULT_PAGE_SIZE)) {
      repository
    }.flow
  }

  companion object {
    fun get(
      owner: ViewModelStoreOwner,
      application: AncApplication,
      repository: EncounterRepository
    ): EncounterListViewModel {
      return ViewModelProvider(
        owner,
        EncounterListViewModel(application, repository).createFactory()
      )[EncounterListViewModel::class.java]
    }
  }
}