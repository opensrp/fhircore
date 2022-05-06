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

package org.smartregister.fhircore.quest.data.report.measure

import android.content.Context
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.android.fhir.FhirEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.withContext
import org.smartregister.fhircore.engine.appfeature.AppFeature
import org.smartregister.fhircore.engine.data.local.register.dao.AncPatientRegisterDao
import org.smartregister.fhircore.engine.domain.model.RegisterData
import org.smartregister.fhircore.engine.util.AssetUtil
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.quest.data.report.measure.models.MeasureReportRowData

class MeasureReportRepository
@Inject
constructor(
  val fhirEngine: FhirEngine,
  @ApplicationContext val context: Context,
  val dispatcherProvider: DefaultDispatcherProvider,
  val ancPatientRegisterDao: AncPatientRegisterDao
) : PagingSource<Int, MeasureReportRowData>() {

  override fun getRefreshKey(state: PagingState<Int, MeasureReportRowData>): Int? {
    return state.anchorPosition
  }

  override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MeasureReportRowData> {
    return try {
      val data =
        withContext(dispatcherProvider.io()) {
          AssetUtil.decodeAsset<List<MeasureReportRowData>>(SAMPLE_REPORT_MEASURES_FILE, context)
        }
      LoadResult.Page(data = data, prevKey = null, nextKey = null)
    } catch (e: Exception) {
      LoadResult.Error(e)
    }
  }

  suspend fun retrievePatients(currentPage: Int): List<RegisterData> {
    return ancPatientRegisterDao.loadRegisterData(
      currentPage = currentPage,
      loadAll = true,
      AppFeature.PatientManagement.name
    )
  }

  companion object {
    const val SAMPLE_REPORT_MEASURES_FILE = "sample_data_report_measures.json"
  }
}
