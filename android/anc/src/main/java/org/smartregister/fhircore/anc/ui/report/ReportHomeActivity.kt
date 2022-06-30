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
import androidx.activity.viewModels
import androidx.compose.material.Surface
import androidx.compose.ui.res.colorResource
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.data.model.PatientItem
import org.smartregister.fhircore.anc.data.model.VisitStatus
import org.smartregister.fhircore.anc.data.patient.PatientRepository
import org.smartregister.fhircore.anc.ui.anccare.shared.Anc
import org.smartregister.fhircore.anc.util.AncConfigClassification
import org.smartregister.fhircore.anc.util.AncJsonSpecificationProvider
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.view.ConfigurableComposableView
import org.smartregister.fhircore.engine.configuration.view.RegisterViewConfiguration
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.ui.register.RegisterDataViewModel
import org.smartregister.fhircore.engine.ui.register.model.RegisterFilterType
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import org.smartregister.fhircore.engine.util.extension.createFactory
import timber.log.Timber

@AndroidEntryPoint
class ReportHomeActivity :
  BaseMultiLanguageActivity(), ConfigurableComposableView<RegisterViewConfiguration> {

  @Inject lateinit var patientRepository: PatientRepository

  @Inject lateinit var configurationRegistry: ConfigurationRegistry

  @Inject lateinit var jsonSpecificationProvider: AncJsonSpecificationProvider

  lateinit var registerDataViewModel: RegisterDataViewModel<Anc, PatientItem>

  val reportViewModel by viewModels<ReportViewModel>()

  lateinit var registerViewConfiguration: RegisterViewConfiguration

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    registerViewConfiguration =
      configurationRegistry.retrieveConfiguration<RegisterViewConfiguration>(
        configClassification = AncConfigClassification.PATIENT_REPORT_REGISTER,
        jsonSpecificationProvider.getJson()
      )

    val currentActivity = this@ReportHomeActivity

    registerDataViewModel =
      initializeRegisterDataViewModel().also { dataViewModel ->
        dataViewModel.currentPage.observe(currentActivity) { dataViewModel.loadPageData(it) }
      }

    configureViews(registerViewConfiguration)

    reportViewModel.apply {
      setStartEndDate(
        startDate = getString(R.string.start_date),
        endDate = getString(R.string.end_date)
      )
      backPress.observe(currentActivity) { backPressed ->
        if (backPressed) {
          finish()
        }
      }
      showDatePicker.observe(currentActivity) {
        if (it) {
          showDateRangePicker()
        }
      }
      filterValue.observe(currentActivity) {
        val (registerFilterType, value) = it
        filterRegisterData(value, registerFilterType, currentActivity)
      }

      onGenerateReportClicked.observe(currentActivity) { generateReport ->
        if (generateReport) {
          if (reportViewModel.currentReportType.value!!.equals(
              other = getString(R.string.individual),
              ignoreCase = true
            )
          ) {
            val reportName = selectedMeasureReportItem.value?.name!!
            Timber.d(message = "This is the measure name => ".plus(reportName))
            reportViewModel.evaluateMeasure(
              context = currentActivity,
              measureUrl =
                buildString {
                  append("http://fhir.org/guides/who/anc-cds/Measure/")
                  append(reportName)
                },
              measureResourceBundleUrl =
                buildString {
                  append("measure/")
                  append(reportName)
                  append("-bundle.json")
                },
              individualEvaluation = true
            )
          } else if (reportViewModel.currentReportType.value!!.equals(
              other = getString(R.string.all),
              ignoreCase = true
            )
          ) {
            val reportName = selectedMeasureReportItem.value?.name!!
            Timber.d(message = "This is the measure name => ".plus(reportName))
            reportViewModel.evaluateMeasure(
              context = currentActivity,
              measureUrl =
                buildString {
                  append("http://fhir.org/guides/who/anc-cds/Measure/")
                  append(reportName)
                },
              measureResourceBundleUrl =
                buildString {
                  append("measure/")
                  append(reportName)
                  append("-bundle.json")
                },
              individualEvaluation = false
            )
          }
        }
      }
    }

    setContent {
      AppTheme {
        Surface(color = colorResource(id = R.color.white)) {
          ReportView(
            reportViewModel = reportViewModel,
            registerDataViewModel = registerDataViewModel
          )
        }
      }
    }
  }

  override fun configureViews(viewConfiguration: RegisterViewConfiguration) {
    registerDataViewModel.updateViewConfigurations(viewConfiguration)
  }

  private fun filterRegisterData(
    value: Any?,
    registerFilterType: RegisterFilterType,
    reportHomeActivity: ReportHomeActivity
  ) {
    if (value != null) {
      registerDataViewModel.run {
        showResultsCount(true)
        filterRegisterData(
          registerFilterType = registerFilterType,
          filterValue = value,
          registerFilter = reportHomeActivity::performFilter
        )
      }
    } else {
      registerDataViewModel.run {
        showResultsCount(false)
        reloadCurrentPageData()
      }
    }
  }

  fun showDateRangePicker() {
    val constraintsBuilder =
      CalendarConstraints.Builder().setValidator(DateValidatorPointBackward.now()).build()
    MaterialDatePicker.Builder.dateRangePicker()
      .apply {
        setCalendarConstraints(constraintsBuilder)
        setTitleText("Select dates")
        setSelection(reportViewModel.dateRange.value!!)
      }
      .build()
      .run {
        addOnPositiveButtonClickListener { selectedDateRange ->
          reportViewModel.setDateRange(selectedDateRange)
        }
        show(supportFragmentManager, DATE_PICKER_DIALOG_TAG)
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

  @Suppress("UNCHECKED_CAST")
  fun initializeRegisterDataViewModel(): RegisterDataViewModel<Anc, PatientItem> {
    return ViewModelProvider(
      viewModelStore,
      RegisterDataViewModel(application = application, registerRepository = patientRepository)
        .createFactory()
    )[RegisterDataViewModel::class.java] as
      RegisterDataViewModel<Anc, PatientItem>
  }

  companion object {
    const val DATE_PICKER_DIALOG_TAG = "DatePickerDialogTag"
  }
}
