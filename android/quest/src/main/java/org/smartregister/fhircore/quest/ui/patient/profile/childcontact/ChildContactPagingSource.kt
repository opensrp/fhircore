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

package org.smartregister.fhircore.quest.ui.patient.profile.childcontact

import androidx.paging.PagingSource
import androidx.paging.PagingState
import org.hl7.fhir.r4.model.Resource
import org.smartregister.fhircore.engine.data.local.register.PatientRegisterRepository
import org.smartregister.fhircore.quest.data.patient.model.PatientPagingSourceState
import org.smartregister.fhircore.quest.ui.shared.models.RegisterViewData
import org.smartregister.fhircore.quest.util.mappers.RegisterViewDataMapper

/**
 * @property _patientPagingSourceState as state containing the properties used in the
 * [PatientRegisterRepository] function for loading data to the paging source.
 */
class ChildContactPagingSource(
  private val otherChildResource: List<Resource>,
  private val patientRegisterRepository: PatientRegisterRepository,
  private val registerViewDataMapper: RegisterViewDataMapper
) : PagingSource<Int, RegisterViewData>() {

  private var _patientPagingSourceState = PatientPagingSourceState()

  /**
   * To load data for the current page, nextKey and prevKey for [params] are both set to null to
   * prevent automatic loading of by the [PagingSource]. This is done in order to explicitly allow
   * loading of data by manually clicking navigation previous or next buttons.
   *
   * To load all data (by automatically paginating the results in the background), set loadAll to
   * true (Checking if data is not empty prevents querying for more results when there is none):
   *
   * prevKey = if (pageNumber == 0) null else pageNumber - 1
   *
   * nextKey = if (data.isNotEmpty()) pageNumber + 1 else null
   */
  override suspend fun load(params: LoadParams<Int>): LoadResult<Int, RegisterViewData> {
    return try {
      val data =
        patientRegisterRepository.loadChildrenRegisterData(
            healthModule = _patientPagingSourceState.healthModule,
            otherPatientResource = otherChildResource
          )
          .map { registerViewDataMapper.transformInputToOutputModel(it) }

      LoadResult.Page(data = data, prevKey = null, nextKey = null)
    } catch (exception: Exception) {
      LoadResult.Error(exception)
    }
  }

  fun setPatientPagingSourceState(patientPagingSourceState: PatientPagingSourceState) {
    this._patientPagingSourceState = patientPagingSourceState
  }

  override fun getRefreshKey(state: PagingState<Int, RegisterViewData>): Int? {
    return state.anchorPosition
  }
}
