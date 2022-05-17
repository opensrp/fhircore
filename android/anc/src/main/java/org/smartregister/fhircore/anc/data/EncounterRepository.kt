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

package org.smartregister.fhircore.anc.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.search.getQuery
import com.google.android.fhir.search.search
import javax.inject.Inject
import org.hl7.fhir.r4.model.Encounter
import org.smartregister.fhircore.anc.data.model.EncounterItem
import org.smartregister.fhircore.engine.domain.util.PaginationConstant

class EncounterRepository @Inject constructor(val fhirEngine: FhirEngine) :
  PagingSource<Int, EncounterItem>() {

  lateinit var patientId: String

  override fun getRefreshKey(state: PagingState<Int, EncounterItem>): Int? {
    return state.anchorPosition
  }

  override suspend fun load(params: LoadParams<Int>): LoadResult<Int, EncounterItem> {
    return try {

      val nextPage = params.key ?: 0

      val encounters =
        fhirEngine.search<Encounter> {
          apply { filter(Encounter.SUBJECT, { value = "Patient/$patientId" }) }.getQuery()
          from = nextPage * PaginationConstant.DEFAULT_PAGE_SIZE
          count = PaginationConstant.DEFAULT_PAGE_SIZE
        }

      val data =
        encounters.map { EncounterItem(it.id, it.status, it.class_.display, it.period.start) }

      LoadResult.Page(
        data = data,
        prevKey = null,
        nextKey = if (data.isEmpty()) null else nextPage.plus(1)
      )
    } catch (e: Exception) {
      LoadResult.Error(e)
    }
  }
}
