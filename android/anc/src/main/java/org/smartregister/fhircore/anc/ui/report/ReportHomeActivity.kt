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
import android.os.Parcel
import android.os.Parcelable
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Surface
import androidx.compose.ui.res.colorResource
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import ca.uhn.fhir.context.FhirContext
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.workflow.FhirOperator
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.MeasureReport
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.data.model.PatientItem
import org.smartregister.fhircore.anc.data.model.VisitStatus
import org.smartregister.fhircore.anc.data.patient.PatientRepository
import org.smartregister.fhircore.anc.ui.anccare.shared.Anc
import org.smartregister.fhircore.anc.ui.report.ReportViewModel.ReportScreen
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.ui.base.AlertDialogue
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.ui.register.RegisterDataViewModel
import org.smartregister.fhircore.engine.ui.register.model.RegisterFilterType
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.extension.createFactory
import org.smartregister.fhircore.engine.util.extension.loadCqlLibraryBundle
import timber.log.Timber

@AndroidEntryPoint
class ReportHomeActivity : BaseMultiLanguageActivity() {

  @Inject lateinit var fhirResourceDataSource: FhirResourceDataSource

  @Inject lateinit var patientRepository: PatientRepository

  @Inject lateinit var fhirOperator: FhirOperator

  @Inject lateinit var fhirEngine: FhirEngine

  @Inject lateinit var fhirContext: FhirContext

  @Inject lateinit var dispatcherProvider: DispatcherProvider

  lateinit var registerDataViewModel: RegisterDataViewModel<Anc, PatientItem>

  lateinit var patientId: String

  val reportViewModel by viewModels<ReportViewModel>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val patientId =
      intent.extras?.getString(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY) ?: ""
    reportViewModel.apply {
      this.patientId = patientId
      registerDataViewModel =
        initializeRegisterDataViewModel(this@ReportHomeActivity.patientRepository)
    }

    registerDataViewModel.currentPage.observe(this, { registerDataViewModel.loadPageData(it) })

    reportViewModel.backPress.observe(
      this,
      {
        if (it) {
          finish()
        }
      }
    )

    reportViewModel.showDatePicker.observe(
      this,
      {
        if (it) {
          MaterialDatePicker.Builder.dateRangePicker().apply {
            setTitleText("Select dates")
            setSelection(
                androidx.core.util.Pair(
                  MaterialDatePicker.thisMonthInUtcMilliseconds(),
                  MaterialDatePicker.todayInUtcMilliseconds()
                )
              )
              .build()
              .show(supportFragmentManager, "DatePickerDialog")
          }
        }
      }
    )

    reportViewModel.processGenerateReport.observe(
      this,
      {
        if (it) {
          lifecycleScope.launch {
            withContext(dispatcherProvider.io()) {

              // TODO Load all patient's data and bundle

              fhirEngine.loadCqlLibraryBundle(
                context = this@ReportHomeActivity,
                fhirOperator = fhirOperator,
                sharedPreferencesHelper = sharedPreferencesHelper,
                resourcesBundlePath = "measure/ANCIND01-bundle.json"
              )

              val measureReport: MeasureReport =
                fhirOperator.evaluateMeasure(
                  url = "http://fhir.org/guides/who/anc-cds/Measure/ANCIND01",
                  start = "2020-01-01",
                  end = "2020-01-31",
                  reportType = "subject",
                  subject = patientId
                )
              Timber.i(FhirContext.forR4().newJsonParser().encodeResourceToString(measureReport))
            }
          }
        }
      }
    )

    reportViewModel.alertSelectPatient.observe(
      this,
      {
        if (it) {
          AlertDialogue.showErrorAlert(
            context = this,
            message = getString(R.string.select_patient),
            title = getString(R.string.invalid_selection)
          )
        }
      }
    )

    reportViewModel.patientSelectionType.observe(
      this,
      {
        if (it.equals("Individual", true)) {
          reportViewModel.filterValue.postValue(Pair(RegisterFilterType.SEARCH_FILTER, ""))
          reportViewModel.currentScreen = ReportScreen.PICK_PATIENT
        }
      }
    )

    reportViewModel.filterValue.observe(
      this,
      {
        lifecycleScope.launch(Dispatchers.Main) {
          val (registerFilterType, value) = it
          if ((value as String).isNotEmpty()) {
            registerDataViewModel.run {
              showResultsCount(true)
              filterRegisterData(
                registerFilterType = registerFilterType,
                filterValue = value,
                registerFilter = this@ReportHomeActivity::performFilter
              )
              reportViewModel.currentScreen = ReportScreen.PICK_PATIENT
            }
          } else {
            registerDataViewModel.run {
              showResultsCount(false)
              reloadCurrentPageData()
            }
            reportViewModel.currentScreen = ReportScreen.PICK_PATIENT
          }
        }
      }
    )

    setContent {
      AppTheme {
        Surface(color = colorResource(id = R.color.white)) {
          Column {
            ReportView(
              reportViewModel = reportViewModel,
              registerDataViewModel = registerDataViewModel
            )
          }
        }
      }
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

  fun showDatePicker() {}

  /*  Limit selectable range to start and end Date provided */
  fun limitRange(
    forStartDateOnly: Long,
    startDateMillis: Long,
    endDateMillis: Long
  ): CalendarConstraints.Builder {
    val constraintsBuilderRange = CalendarConstraints.Builder()
    if (forStartDateOnly == 1L) constraintsBuilderRange.setEnd(endDateMillis)
    else constraintsBuilderRange.setStart(startDateMillis)
    constraintsBuilderRange.setValidator(
      RangeValidator(forStartDateOnly, startDateMillis, endDateMillis)
    )
    return constraintsBuilderRange
  }

  class RangeValidator(
    private val forStartDateOnly: Long,
    private val minDate: Long,
    private val maxDate: Long
  ) : CalendarConstraints.DateValidator {
    constructor(parcel: Parcel) : this(parcel.readLong(), parcel.readLong(), parcel.readLong())

    override fun writeToParcel(dest: Parcel?, flags: Int) {}

    override fun describeContents(): Int {
      TODO("nothing to implement")
    }

    override fun isValid(date: Long): Boolean {
      return if (forStartDateOnly == 1L) maxDate >= date else minDate <= date
    }

    companion object CREATOR : Parcelable.Creator<RangeValidator> {
      override fun createFromParcel(parcel: Parcel): RangeValidator {
        return RangeValidator(parcel)
      }

      override fun newArray(size: Int): Array<RangeValidator?> {
        return arrayOfNulls(size)
      }
    }
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
