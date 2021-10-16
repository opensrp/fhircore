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
import org.smartregister.fhircore.anc.data.sharedmodel.EncounterItem
import org.smartregister.fhircore.engine.data.domain.util.PaginationUtil
import org.smartregister.fhircore.engine.util.extension.createFactory

class EncounterListViewModel(
  application: Application,
  private val repository: EncounterRepository
) : AndroidViewModel(application), EncounterDataProvider {

  private var mBackClickListener: () -> Unit = {}

  override fun getEncounterList(): Flow<PagingData<EncounterItem>> {
    return Pager(PagingConfig(pageSize = PaginationUtil.DEFAULT_PAGE_SIZE)) { repository }.flow
  }

  override fun getAppBackClickListener(): () -> Unit {
    return mBackClickListener
  }

  fun setAppBackClickListener(listener: () -> Unit) {
    this.mBackClickListener = listener
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
