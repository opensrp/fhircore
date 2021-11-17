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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.datepicker.MaterialDatePicker
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.smartregister.fhircore.anc.AncApplication
import org.smartregister.fhircore.anc.data.model.PatientItem
import org.smartregister.fhircore.anc.data.model.VisitStatus
import org.smartregister.fhircore.anc.data.patient.PatientRepository
import org.smartregister.fhircore.anc.data.report.ReportRepository
import org.smartregister.fhircore.anc.ui.anccare.register.Anc
import org.smartregister.fhircore.anc.ui.anccare.register.AncItemMapper
import org.smartregister.fhircore.anc.ui.report.ReportViewModel.ReportScreen
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.ui.register.RegisterDataViewModel
import org.smartregister.fhircore.engine.ui.register.model.RegisterFilterType
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import org.smartregister.fhircore.engine.util.extension.createFactory

class ReportHomeActivity : BaseMultiLanguageActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val patientId =
      intent.extras?.getString(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY) ?: ""
    val repository = ReportRepository((application as AncApplication).fhirEngine, patientId, this)
    val ancPatientRepository =
      PatientRepository((application as AncApplication).fhirEngine, AncItemMapper)
    val viewModel =
      ReportViewModel.get(this, application as AncApplication, repository, ancPatientRepository)

    viewModel.registerDataViewModel = initializeRegisterDataViewModel(ancPatientRepository)
    viewModel.registerDataViewModel.currentPage.observe(
      this,
      { viewModel.registerDataViewModel.loadPageData(it) }
    )

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
          showDateRangePicker(viewModel::onDateSelected)
        }
      }
    )

    viewModel.patientSelectionType.observe(
      this,
      {
        if (it.equals("Individual", true)) {
          viewModel.reportState.currentScreen = ReportScreen.PICK_PATIENT
        }
      }
    )

    viewModel.filterValue.observe(
      this,
      {
        lifecycleScope.launch(Dispatchers.Main) {
          val (registerFilterType, value) = it
          if (value != null) {
            viewModel.registerDataViewModel.run {
              showResultsCount(true)
              filterRegisterData(
                registerFilterType = registerFilterType,
                filterValue = value,
                registerFilter = this@ReportHomeActivity::performFilter
              )
            }
          } else {
            viewModel.registerDataViewModel.run {
              showResultsCount(false)
              reloadCurrentPageData()
            }
          }
        }
      }
    )

    setContent {
      val reportState = remember { viewModel.reportState }
      AppTheme { ReportView(viewModel) }
    }
  }

  private fun performFilter(
    registerFilterType: RegisterFilterType,
    data: PatientItem,
    value: Any
  ): Boolean {
    return when (registerFilterType) {
      RegisterFilterType.SEARCH_FILTER -> {
        if (value is String && value.isEmpty()) return true
        else
          data.name.contains(value.toString(), ignoreCase = true) ||
            data.patientIdentifier.contentEquals(value.toString())
      }
      RegisterFilterType.OVERDUE_FILTER -> {
        return data.visitStatus == VisitStatus.OVERDUE
      }
    }
  }

  @Composable
  fun ReportView(viewModel: ReportViewModel) {
    // Choose which screen to show based on the value in the ReportScreen from ReportState
    when (viewModel.reportState.currentScreen) {
      ReportScreen.HOME -> ReportHomeScreen(viewModel)
      ReportScreen.FILTER -> ReportFilterScreen(viewModel)
      ReportScreen.PICK_PATIENT -> ReportSelectPatientScreen(viewModel)
      ReportScreen.RESULT -> ReportResultScreen(viewModel)
    }
  }

  private fun showDateRangePicker(onDateSelected: (Pair<Long, Long>?) -> Unit) {
    val builder = MaterialDatePicker.Builder.dateRangePicker()
    val now = Calendar.getInstance()
    builder.setSelection(Pair(now.timeInMillis, now.timeInMillis))
    val dateRangePicker = builder.build()
    dateRangePicker.show(supportFragmentManager, dateRangePicker.toString())
    dateRangePicker.addOnPositiveButtonClickListener { onDateSelected(dateRangePicker.selection) }
  }

  @Suppress("UNCHECKED_CAST")
  fun initializeRegisterDataViewModel(
    ancPatientRepository: PatientRepository
  ): RegisterDataViewModel<Anc, PatientItem> {
    return ViewModelProvider(
      viewModelStore,
      RegisterDataViewModel(application = application, registerRepository = ancPatientRepository)
        .createFactory()
    )[RegisterDataViewModel::class.java] as
      RegisterDataViewModel<Anc, PatientItem>
  }
}
