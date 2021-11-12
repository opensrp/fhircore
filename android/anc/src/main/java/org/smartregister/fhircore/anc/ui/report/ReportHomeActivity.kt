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

package org.smartregister.fhircore.anc.ui.report

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.util.Pair
import com.google.android.material.datepicker.MaterialDatePicker
import java.util.Calendar
import org.smartregister.fhircore.anc.AncApplication
import org.smartregister.fhircore.anc.data.report.ReportRepository
import org.smartregister.fhircore.anc.ui.report.ReportViewModel.ReportScreen
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.ui.theme.AppTheme

class ReportHomeActivity : BaseMultiLanguageActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val patientId =
      intent.extras?.getString(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY) ?: ""
    val repository = ReportRepository((application as AncApplication).fhirEngine, patientId)
    val viewModel = ReportViewModel.get(this, application as AncApplication, repository)
    viewModel.backPress.observe(
      this,
      {
        if (it) {
          finish()
        }
      }
    )
    viewModel.showDatePicker.observe(
      this,
      {
        if (it) {
          showDateRangePicker(viewModel)
        }
      }
    )

    setContent {
      val reportState = remember { viewModel.reportState }
      AppTheme { ReportView(viewModel) }
    }
  }

  @Composable
  fun ReportView(viewModel: ReportViewModel) {
    // Choose which screen to show based on the value in the ReportScreen from ReportState
    when (viewModel.reportState.currentScreen) {
      ReportScreen.HOME -> ReportHomeScreen(viewModel)
      ReportScreen.FILTER -> ReportFilterScreen(viewModel)
      ReportScreen.PICK_PATIENT -> ReportFilterScreen(viewModel)
      ReportScreen.RESULT -> ReportResultScreen(viewModel)
    }
  }

  private fun showDateRangePicker(viewModel: ReportViewModel) {
    val builder = MaterialDatePicker.Builder.dateRangePicker()
    val now = Calendar.getInstance()
    builder.setSelection(Pair(now.timeInMillis, now.timeInMillis))
    val dateRangePicker = builder.build()
    dateRangePicker.show(supportFragmentManager, dateRangePicker.toString())
    dateRangePicker.addOnPositiveButtonClickListener {
      viewModel.onDateSelected(dateRangePicker.selection)
    }
  }
}
