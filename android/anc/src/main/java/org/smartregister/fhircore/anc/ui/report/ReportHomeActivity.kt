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
import ca.uhn.fhir.context.FhirVersionEnum
import ca.uhn.fhir.parser.IParser
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.common.collect.Lists
import dagger.hilt.android.AndroidEntryPoint
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.hl7.fhir.instance.model.api.IBaseBundle
import org.hl7.fhir.instance.model.api.IBaseResource
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.data.model.PatientItem
import org.smartregister.fhircore.anc.data.model.VisitStatus
import org.smartregister.fhircore.anc.data.patient.PatientRepository
import org.smartregister.fhircore.anc.data.report.model.ResultItem
import org.smartregister.fhircore.anc.ui.anccare.shared.Anc
import org.smartregister.fhircore.anc.ui.report.ReportViewModel.ReportScreen
import org.smartregister.fhircore.engine.cql.CqlLibraryHelper
import org.smartregister.fhircore.engine.cql.LibraryEvaluator
import org.smartregister.fhircore.engine.cql.MeasureEvaluator
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.ui.base.AlertDialogue
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.ui.register.RegisterDataViewModel
import org.smartregister.fhircore.engine.ui.register.model.RegisterFilterType
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import org.smartregister.fhircore.engine.util.FileUtil
import org.smartregister.fhircore.engine.util.extension.createFactory

@AndroidEntryPoint
class ReportHomeActivity : BaseMultiLanguageActivity() {

  @Inject lateinit var fhirResourceDataSource: FhirResourceDataSource
  @Inject lateinit var patientRepository: PatientRepository
  @Inject lateinit var cqlLibraryHelper: CqlLibraryHelper
  @Inject lateinit var libraryEvaluator: LibraryEvaluator

  lateinit var parser: IParser
  lateinit var fhirContext: FhirContext
  lateinit var measureEvaluator: MeasureEvaluator
  lateinit var libraryResources: List<IBaseResource>
  lateinit var dir: File
  lateinit var libraryMeasure: IBaseBundle
  lateinit var valueSetBundle: IBaseBundle
  lateinit var patientId: String

  var libraryData: String = ""
  var helperData: String = ""
  var valueSetData: String = ""
  var cqlMeasureReportReportType = ""

  private val evaluatorId = "ANCRecommendationA2"
  private val contextCql = "patient"
  private val contextLabel = "mom-with-anemia"
  private var cqlBaseUrl = ""
  private var libraryUrl = ""
  private var measureEvaluateLibraryUrl = ""
  private var measureTypeUrl = ""
  private var cqlMeasureReportUrl = ""
  private var cqlMeasureReportStartDate = ""
  private var cqlMeasureReportEndDate = ""
  private var cqlMeasureReportLibInitialString = ""
  private var cqlMeasureReportSubject = ""
  private var cqlHelperUrl = ""
  private var valueSetUrl = ""
  private var patientUrl = ""
  private val cqlConfigFileName = "configs/cql_configs.properties"
  private val dirCqlDirRoot = "cql_libraries"
  private val fileNameMainLibraryCql = "main_library_cql"
  private val fileNameHelperLibraryCql = "helper_library_cql"
  private val fileNameValueSetLibraryCql = "value_set_library_cql"
  private val fileNameMeasureLibraryCql = "measure_library_cql"
  private var patientResourcesIBase = ArrayList<IBaseResource>()
  lateinit var patientDataIBase: IBaseBundle

  val reportViewModel by viewModels<ReportViewModel>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    measureEvaluator = MeasureEvaluator()
    fhirContext = FhirContext.forCached(FhirVersionEnum.R4)
    parser = fhirContext.newJsonParser()

    val patientId =
      intent.extras?.getString(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY) ?: ""
    reportViewModel.apply {
      this.patientId = patientId
      registerDataViewModel =
        initializeRegisterDataViewModel(this@ReportHomeActivity.patientRepository)
    }

    reportViewModel.registerDataViewModel.currentPage.observe(this) {
      reportViewModel.registerDataViewModel.loadPageData(it)
    }

    reportViewModel.backPress.observe(this) {
      if (it) {
        finish()
      }
    }

    reportViewModel.showDatePicker.observe(this) {
      if (it) {
        showDatePicker()
      }
    }

    reportViewModel.processGenerateReport.observe(this) {
      if (it) {
        // Todo: for Davison, update params for All patient selection
        generateMeasureReport(
          startDate = reportViewModel.startDate.value ?: "",
          endDate = reportViewModel.endDate.value ?: "",
          reportType = reportViewModel.selectedMeasureReportItem.value?.reportType ?: "",
          patientId = reportViewModel.selectedPatientItem.value?.patientIdentifier ?: "",
          subject = reportViewModel.selectedPatientItem.value?.familyName ?: ""
        )
      }
    }

    reportViewModel.alertSelectPatient.observe(this) {
      if (it) {
        AlertDialogue.showErrorAlert(
          context = this,
          message = getString(R.string.select_patient),
          title = getString(R.string.invalid_selection)
        )
      }
    }

    cqlBaseUrl =
      this.let { FileUtil.getProperty("smart_register_base_url", it, cqlConfigFileName) }!!

    libraryUrl =
      cqlBaseUrl + this.let { FileUtil.getProperty("cql_library_url", it, cqlConfigFileName) }

    cqlHelperUrl =
      cqlBaseUrl +
        this.let { FileUtil.getProperty("cql_helper_library_url", it, cqlConfigFileName) }

    valueSetUrl =
      cqlBaseUrl + this.let { FileUtil.getProperty("cql_value_set_url", it, cqlConfigFileName) }

    patientUrl =
      cqlBaseUrl + this.let { FileUtil.getProperty("cql_patient_url", it, cqlConfigFileName) }

    measureEvaluateLibraryUrl =
      this.let {
        FileUtil.getProperty("cql_measure_report_library_value_sets_url", it, cqlConfigFileName)
      }!!

    measureTypeUrl =
      this.let { FileUtil.getProperty("cql_measure_report_resource_url", it, cqlConfigFileName) }!!

    cqlMeasureReportUrl =
      this.let { FileUtil.getProperty("cql_measure_report_url", it, cqlConfigFileName) }!!

    cqlMeasureReportLibInitialString =
      this.let {
        FileUtil.getProperty("cql_measure_report_lib_initial_string", it, cqlConfigFileName)
      }!!

    reportViewModel.patientSelectionType.observe(this) {
      if (it.equals("Individual", true)) {
        reportViewModel.filterValue.postValue(kotlin.Pair(RegisterFilterType.SEARCH_FILTER, ""))
        reportViewModel.reportState.currentScreen = ReportScreen.PICK_PATIENT
      }
    }

    reportViewModel.isReadyToGenerateReport.observe(this) {
      reportViewModel.reportState.currentScreen = ReportScreen.FILTER
    }

    reportViewModel.filterValue.observe(this) {
      lifecycleScope.launch(Dispatchers.Main) {
        val (registerFilterType, value) = it
        if ((value as String).isNotEmpty()) {
          reportViewModel.registerDataViewModel.run {
            showResultsCount(true)
            filterRegisterData(
              registerFilterType = registerFilterType,
              filterValue = value,
              registerFilter = this@ReportHomeActivity::performFilter
            )
            reportViewModel.reportState.currentScreen = ReportScreen.PICK_PATIENT
          }
        } else {
          reportViewModel.registerDataViewModel.run {
            showResultsCount(false)
            reloadCurrentPageData()
          }
          reportViewModel.reportState.currentScreen = ReportScreen.PICK_PATIENT
        }
      }
    }

    setContent {
      AppTheme {
        Surface(color = colorResource(id = R.color.white)) {
          Column {
            ReportView(reportViewModel)
            loadMeasureEvaluateLibrary()
          }
        }
      }
    }
  }

  fun loadCqlLibraryData() {
    dir = File(this.filesDir, "$dirCqlDirRoot/$fileNameMainLibraryCql")
    if (dir.exists()) {
      libraryData =
        this.let { FileUtil.readFileFromInternalStorage(it, fileNameMainLibraryCql, dirCqlDirRoot) }
          .toString()
      loadCqlHelperData()
    } else {
      reportViewModel
        .fetchCqlLibraryData(parser, fhirResourceDataSource, libraryUrl)
        .observe(this, this::handleCqlLibraryData)
    }
    reportViewModel.reportState.currentScreen = ReportScreen.PREHOMElOADING
  }

  fun loadCqlHelperData() {
    dir = File(this.filesDir, "$dirCqlDirRoot/$fileNameHelperLibraryCql")
    if (dir.exists()) {
      helperData =
        this.let {
            FileUtil.readFileFromInternalStorage(it, fileNameHelperLibraryCql, dirCqlDirRoot)
          }
          .toString()
      loadCqlLibrarySources()
      loadCqlValueSetData()
    } else {
      reportViewModel
        .fetchCqlFhirHelperData(parser, fhirResourceDataSource, cqlHelperUrl)
        .observe(this, this::handleCqlHelperData)
    }
  }

  fun loadCqlValueSetData() {
    dir = File(this.filesDir, "$dirCqlDirRoot/$fileNameValueSetLibraryCql")
    if (dir.exists()) {
      valueSetData =
        this.let {
            FileUtil.readFileFromInternalStorage(it, fileNameValueSetLibraryCql, dirCqlDirRoot)
          }
          .toString()
      postValueSetData(valueSetData)
    } else {
      reportViewModel
        .fetchCqlValueSetData(parser, fhirResourceDataSource, valueSetUrl)
        .observe(this, this::handleCqlValueSetData)
    }
  }

  fun postValueSetData(valueSetData: String) {
    val valueSetStream: InputStream = ByteArrayInputStream(valueSetData.toByteArray())
    valueSetBundle = parser.parseResource(valueSetStream) as IBaseBundle
  }

  fun loadMeasureEvaluateLibrary() {
    reportViewModel.reportState.currentScreen = ReportScreen.HOME
    dir = File(this.filesDir, "$dirCqlDirRoot/$fileNameMeasureLibraryCql")
    if (dir.exists()) {
      lifecycleScope.launch(Dispatchers.IO) {
        libraryMeasure =
          cqlLibraryHelper.loadMeasureEvaluateLibrary(fileNameMeasureLibraryCql, dirCqlDirRoot)
      }
    } else {
      reportViewModel
        .fetchCqlMeasureEvaluateLibraryAndValueSets(
          parser,
          fhirResourceDataSource,
          measureEvaluateLibraryUrl,
          measureTypeUrl,
          cqlMeasureReportLibInitialString
        )
        .observe(this@ReportHomeActivity, this@ReportHomeActivity::handleMeasureEvaluateLibrary)
    }
  }

  fun handleCqlLibraryData(auxLibraryData: String) {
    libraryData = auxLibraryData
    this.let {
      FileUtil.writeFileOnInternalStorage(it, fileNameMainLibraryCql, libraryData, dirCqlDirRoot)
    }
    loadCqlHelperData()
  }

  fun loadCqlLibrarySources() {
    val libraryStream: InputStream = ByteArrayInputStream(libraryData.toByteArray())
    val fhirHelpersStream: InputStream = ByteArrayInputStream(helperData.toByteArray())
    val library = parser.parseResource(libraryStream)
    val fhirHelpersLibrary = parser.parseResource(fhirHelpersStream)
    libraryResources = Lists.newArrayList(library, fhirHelpersLibrary)
  }

  fun handleCqlHelperData(auxHelperData: String) {
    helperData = auxHelperData
    this.let {
      FileUtil.writeFileOnInternalStorage(it, fileNameHelperLibraryCql, helperData, dirCqlDirRoot)
    }
    loadCqlLibrarySources()
    loadCqlValueSetData()
  }

  fun handleCqlValueSetData(auxValueSetData: String) {
    valueSetData = auxValueSetData
    this.let {
      FileUtil.writeFileOnInternalStorage(
        it,
        fileNameValueSetLibraryCql,
        valueSetData,
        dirCqlDirRoot
      )
    }
    postValueSetData(valueSetData)
  }

  fun handleCql(): String {
    return libraryEvaluator.runCql(
      libraryResources,
      valueSetBundle,
      patientDataIBase,
      fhirContext,
      evaluatorId,
      contextCql,
      contextLabel
    )
  }

  fun handleMeasureEvaluate() {
    lifecycleScope.launch(Dispatchers.IO) {
      val parameters =
        measureEvaluator.runMeasureEvaluate(
          patientResourcesIBase,
          libraryMeasure,
          fhirContext,
          cqlMeasureReportUrl,
          cqlMeasureReportStartDate,
          cqlMeasureReportEndDate,
          cqlMeasureReportReportType,
          cqlMeasureReportSubject
        )
      launch(Dispatchers.Main) {
        var resultItem = ResultItem("True", true, "", "100", "100")
        reportViewModel.resultForIndividual.value = resultItem
        reportViewModel.reportState.currentScreen = ReportScreen.RESULT
      }
    }
  }

  fun handleMeasureEvaluateLibrary(measureEvaluateLibraryData: String) {
    lifecycleScope.launch(Dispatchers.IO) {
      cqlLibraryHelper.writeMeasureEvaluateLibraryData(
        measureEvaluateLibraryData,
        fileNameMeasureLibraryCql,
        dirCqlDirRoot
      )
      libraryMeasure =
        cqlLibraryHelper.loadMeasureEvaluateLibrary(fileNameMeasureLibraryCql, dirCqlDirRoot)
    }
  }

  fun loadCqlMeasurePatientData() {
    reportViewModel
      .fetchCqlPatientData(parser, fhirResourceDataSource, "$patientUrl$patientId/\$everything")
      .observe(this, this::handleCqlMeasureLoadPatient)
  }

  fun handleCqlMeasureLoadPatient(auxPatientData: String) {
    if (auxPatientData.isNotEmpty()) {
      val testData = libraryEvaluator.processCqlPatientBundle(auxPatientData)
      val patientDataStream: InputStream = ByteArrayInputStream(testData.toByteArray())
      patientDataIBase = parser.parseResource(patientDataStream) as IBaseBundle
      patientResourcesIBase.add(patientDataIBase)
      handleMeasureEvaluate()
    } else {
      // Todo: for Davison update result item when empty response for loadPatient Api
      reportViewModel.resultForIndividual.value =
        ResultItem(isMatchedIndicator = false, status = "Failed")
      reportViewModel.reportState.currentScreen = ReportScreen.RESULT
    }
  }

  fun generateMeasureReport(
    startDate: String,
    endDate: String,
    reportType: String,
    patientId: String,
    subject: String,
  ) {
    val pattern = "yyyy-MM-dd"
    val simpleDateFormat = SimpleDateFormat(pattern)

    cqlMeasureReportStartDate = simpleDateFormat.format(Date(startDate))
    cqlMeasureReportEndDate = simpleDateFormat.format(Date(endDate))
    this.patientId = patientId
    cqlMeasureReportSubject = subject
    cqlMeasureReportReportType = reportType

    reportViewModel.reportState.currentScreen = ReportScreen.PREHOMElOADING
    loadCqlMeasurePatientData()
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

  fun showDatePicker() {
    MaterialDatePicker.Builder.datePicker().apply {
      setSelection(reportViewModel.getSelectionDate())
      val startDateMillis = reportViewModel.startDateTimeMillis.value ?: Date().time
      val endDateMillis = reportViewModel.endDateTimeMillis.value ?: Date().time
      val forStartOnly = if (reportViewModel.isChangingStartDate.value != false) 1L else 0L
      setCalendarConstraints(limitRange(forStartOnly, startDateMillis, endDateMillis).build())
      with(this.build()) {
        show(supportFragmentManager, this.toString())
        addOnPositiveButtonClickListener(reportViewModel::onDatePicked)
      }
    }
  }

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
