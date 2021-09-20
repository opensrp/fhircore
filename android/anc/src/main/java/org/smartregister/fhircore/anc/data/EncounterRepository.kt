package org.smartregister.fhircore.anc.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.search.search
import org.hl7.fhir.r4.model.Encounter
import org.smartregister.fhircore.anc.data.model.EncounterItem
import org.smartregister.fhircore.engine.data.domain.util.PaginationUtil

class EncounterRepository(val fhirEngine: FhirEngine, private val patientId: String) :
  PagingSource<Int, EncounterItem>() {

  override fun getRefreshKey(state: PagingState<Int, EncounterItem>): Int? {
    return state.anchorPosition
  }

  override suspend fun load(params: LoadParams<Int>): LoadResult<Int, EncounterItem> {
    return try {

      val nextPage = params.key ?: 0

      val encounters = fhirEngine.search<Encounter> {
        filter(Encounter.SUBJECT) { value = "Patient/$patientId" }
        from = nextPage * PaginationUtil.DEFAULT_PAGE_SIZE
        count = PaginationUtil.DEFAULT_PAGE_SIZE
      }

      val data = encounters.map {
        EncounterItem(it.id, it.status, it.class_.display, it.period.start)
      }

      LoadResult.Page(
        data = data,
        prevKey = if (data.isEmpty()) null else nextPage.plus(1),
        nextKey = null
      )

    } catch (e: Exception) {
      LoadResult.Error(e)
    }
  }
}