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

package org.smartregister.fhircore.anc.data.report

import android.content.Context
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.android.fhir.FhirEngine
import org.smartregister.fhircore.anc.data.report.model.ReportItem
import org.smartregister.fhircore.engine.util.extension.decodeJson

class ReportRepository(
  val fhirEngine: FhirEngine,
  private val patientId: String,
  private val mContext: Context
) : PagingSource<Int, ReportItem>() {

  val SAMPLE_REPORT_MEASURES_FILE = "sample_data_report_measures.json"

  override fun getRefreshKey(state: PagingState<Int, ReportItem>): Int? {
    return state.anchorPosition
  }

  override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ReportItem> {
    return try {
      val data = createTestData(mContext)
      LoadResult.Page(data = data, prevKey = null, nextKey = null)
    } catch (e: Exception) {
      LoadResult.Error(e)
    }
  }

  /** Load report-measures from asset directory */
  suspend fun createTestData(context: Context): List<ReportItem> {
    val json =
      context.assets.open(SAMPLE_REPORT_MEASURES_FILE).bufferedReader().use { it.readText() }
    val dataList: List<ReportItem> = json.decodeJson()
    return dataList
  }
}
