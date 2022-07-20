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

package org.smartregister.fhircore.quest.ui.report.measure

import android.content.Context
import androidx.navigation.NavController
import org.hl7.fhir.r4.model.MeasureReport
import org.smartregister.fhircore.engine.configuration.view.MeasureReportRowData
import org.smartregister.fhircore.quest.ui.shared.models.MeasureReportPatientViewData

sealed class MeasureReportEvent {
  data class OnSelectMeasure(
    val measureReportRowData: MeasureReportRowData,
    val navController: NavController
  ) : MeasureReportEvent()
  data class OnDateRangeSelected(val newDateRange: androidx.core.util.Pair<Long, Long>) :
    MeasureReportEvent()
  data class GenerateReport(val navController: NavController, val context: Context) :
    MeasureReportEvent()
  data class OnReportTypeChanged(
    val measureReportType: MeasureReport.MeasureReportType,
    val navController: NavController
  ) : MeasureReportEvent()
  data class OnPatientSelected(val patientViewData: MeasureReportPatientViewData) :
    MeasureReportEvent()
  data class OnSearchTextChanged(val searchText: String) : MeasureReportEvent()
}
