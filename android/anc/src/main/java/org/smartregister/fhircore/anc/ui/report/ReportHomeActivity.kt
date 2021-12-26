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
import org.smartregister.fhircore.engine.cql.LibraryEvaluator
import org.smartregister.fhircore.engine.cql.MeasureEvaluator
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
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

  lateinit var parser: IParser
  lateinit var fhirContext: FhirContext
  lateinit var libraryEvaluator: LibraryEvaluator
  lateinit var measureEvaluator: MeasureEvaluator
  lateinit var libraryResources: List<IBaseResource>
  var libraryData: String = ""
  var helperData: String = ""
  var valueSetData: String = ""
  val evaluatorId = "ANCRecommendationA2"
  val contextCQL = "patient"
  val contextLabel = "mom-with-anemia"
  var cqlBaseURL = ""
  var libraryURL = ""
  var measureEvaluateLibraryURL = ""
  var measureTypeURL = ""
  var cqlMeasureReportURL = ""
  var cqlMeasureReportStartDate = ""
  var cqlMeasureReportEndDate = ""
  var cqlMeasureReportReportType = ""
  var cqlMeasureReportLibInitialString = ""
  var cqlMeasureReportSubject = ""
  var cqlHelperURL = ""
  var valueSetURL = ""
  var patientURL = ""
  val cqlConfigFileName = "configs/cql_configs.properties"
  lateinit var dir: File
  lateinit var libraryMeasure: IBaseBundle
  var measureEvaluateLibraryData: String = ""
  lateinit var valueSetBundle: IBaseBundle
  val dirCQLDirRoot = "cql_libraries"
  val fileNameMainLibraryCql = "main_library_cql"
  val fileNameHelperLibraryCql = "helper_library_cql"
  val fileNameValueSetLibraryCql = "value_set_library_cql"
  val fileNameMeasureLibraryCql = "measure_library_cql"
  var patientResourcesIBase = ArrayList<IBaseResource>()
  lateinit var patientDataIBase: IBaseBundle
  lateinit var patientId: String
  val reportViewModel by viewModels<ReportViewModel>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    libraryEvaluator = LibraryEvaluator()
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

    reportViewModel.registerDataViewModel.currentPage.observe(
      this,
      { reportViewModel.registerDataViewModel.loadPageData(it) }
    )

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
          showDatePicker()
        }
      }
    )

    cqlBaseURL =
      this.let { FileUtil.getProperty("smart_register_base_url", it, cqlConfigFileName) }!!

    libraryURL =
      cqlBaseURL + this.let { FileUtil.getProperty("cql_library_url", it, cqlConfigFileName) }

    cqlHelperURL =
      cqlBaseURL +
        this.let { FileUtil.getProperty("cql_helper_library_url", it, cqlConfigFileName) }

    valueSetURL =
      cqlBaseURL + this.let { FileUtil.getProperty("cql_value_set_url", it, cqlConfigFileName) }

    patientURL =
      cqlBaseURL + this.let { FileUtil.getProperty("cql_patient_url", it, cqlConfigFileName) }

    measureEvaluateLibraryURL =
      this.let {
        FileUtil.getProperty("cql_measure_report_library_value_sets_url", it, cqlConfigFileName)
      }!!

    measureTypeURL =
      this.let { FileUtil.getProperty("cql_measure_report_resource_url", it, cqlConfigFileName) }!!

    cqlMeasureReportURL =
      this.let { FileUtil.getProperty("cql_measure_report_url", it, cqlConfigFileName) }!!

    cqlMeasureReportLibInitialString =
      this.let {
        FileUtil.getProperty("cql_measure_report_lib_initial_string", it, cqlConfigFileName)
      }!!

    reportViewModel.patientSelectionType.observe(
      this,
      {
        if (it.equals("Individual", true)) {
          reportViewModel.filterValue.postValue(kotlin.Pair(RegisterFilterType.SEARCH_FILTER, ""))
          reportViewModel.reportState.currentScreen = ReportScreen.PICK_PATIENT
        }
      }
    )

    reportViewModel.isReadyToGenerateReport.observe(
      this,
      { reportViewModel.reportState.currentScreen = ReportScreen.FILTER }
    )

    reportViewModel.filterValue.observe(
      this,
      {
        lifecycleScope.launch(Dispatchers.Main) {
          val (registerFilterType, value) = it
          if (value != null) {
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
    )

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

  fun loadCQLLibraryData() {
    dir = File(this.filesDir, "$dirCQLDirRoot/$fileNameMainLibraryCql")
    if (dir.exists()) {
      libraryData =
        this.let { FileUtil.readFileFromInternalStorage(it, fileNameMainLibraryCql, dirCQLDirRoot) }
          .toString()
      loadCQLHelperData()
    } else {
      reportViewModel
        .fetchCQLLibraryData(parser, fhirResourceDataSource, libraryURL)
        .observe(this, this::handleCQLLibraryData)
    }
    reportViewModel.reportState.currentScreen = ReportScreen.PREHOMElOADING
  }

  fun loadCQLHelperData() {
    dir = File(this.filesDir, "$dirCQLDirRoot/$fileNameHelperLibraryCql")
    if (dir.exists()) {
      helperData =
        this.let {
            FileUtil.readFileFromInternalStorage(it, fileNameHelperLibraryCql, dirCQLDirRoot)
          }
          .toString()
      loadCQLLibrarySources()
      loadCQLValueSetData()
    } else {
      reportViewModel
        .fetchCQLFhirHelperData(parser, fhirResourceDataSource, cqlHelperURL)
        .observe(this, this::handleCQLHelperData)
    }
  }

  fun loadCQLValueSetData() {
    dir = File(this.filesDir, "$dirCQLDirRoot/$fileNameValueSetLibraryCql")
    if (dir.exists()) {
      valueSetData =
        this.let {
            FileUtil.readFileFromInternalStorage(it, fileNameValueSetLibraryCql, dirCQLDirRoot)
          }
          .toString()
      postValueSetData(valueSetData)
    } else {
      reportViewModel
        .fetchCQLValueSetData(parser, fhirResourceDataSource, valueSetURL)
        .observe(this, this::handleCQLValueSetData)
    }
  }

  fun postValueSetData(valueSetData: String) {
    val valueSetStream: InputStream = ByteArrayInputStream(valueSetData.toByteArray())
    valueSetBundle = parser.parseResource(valueSetStream) as IBaseBundle
  }

  fun loadMeasureEvaluateLibrary() {
    reportViewModel.reportState.currentScreen = ReportScreen.PREHOMElOADING
    dir = File(this.filesDir, "$dirCQLDirRoot/$fileNameMeasureLibraryCql")
    if (dir.exists()) {
      measureEvaluateLibraryData =
        this.let {
            FileUtil.readFileFromInternalStorage(it, fileNameMeasureLibraryCql, dirCQLDirRoot)
          }
          .toString()
      val libraryStreamMeasure: InputStream =
        ByteArrayInputStream(measureEvaluateLibraryData.toByteArray())
      libraryMeasure = parser.parseResource(libraryStreamMeasure) as IBaseBundle
      reportViewModel.reportState.currentScreen = ReportScreen.HOME
    } else {
      reportViewModel
        .fetchCQLMeasureEvaluateLibraryAndValueSets(
          parser,
          fhirResourceDataSource,
          measureEvaluateLibraryURL,
          measureTypeURL,
          cqlMeasureReportLibInitialString
        )
        .observe(this, this::handleMeasureEvaluateLibrary)
    }
  }

  fun handleCQLLibraryData(auxLibraryData: String) {
    libraryData = auxLibraryData
    this.let {
      FileUtil.writeFileOnInternalStorage(it, fileNameMainLibraryCql, libraryData, dirCQLDirRoot)
    }
    loadCQLHelperData()
  }

  fun loadCQLLibrarySources() {
    val libraryStream: InputStream = ByteArrayInputStream(libraryData.toByteArray())
    val fhirHelpersStream: InputStream = ByteArrayInputStream(helperData.toByteArray())
    val library = parser.parseResource(libraryStream)
    val fhirHelpersLibrary = parser.parseResource(fhirHelpersStream)
    libraryResources = Lists.newArrayList(library, fhirHelpersLibrary)
  }

  fun handleCQLHelperData(auxHelperData: String) {
    helperData = auxHelperData
    this.let {
      FileUtil.writeFileOnInternalStorage(it, fileNameHelperLibraryCql, helperData, dirCQLDirRoot)
    }
    loadCQLLibrarySources()
    loadCQLValueSetData()
  }

  fun handleCQLValueSetData(auxValueSetData: String) {
    valueSetData = auxValueSetData
    this.let {
      FileUtil.writeFileOnInternalStorage(
        it,
        fileNameValueSetLibraryCql,
        valueSetData,
        dirCQLDirRoot
      )
    }
    postValueSetData(valueSetData)
  }

  fun handleCQL(): String {
    return libraryEvaluator.runCql(
      libraryResources,
      valueSetBundle,
      patientDataIBase,
      fhirContext,
      evaluatorId,
      contextCQL,
      contextLabel
    )
  }

  fun handleMeasureEvaluate() {
    val parameters =
      measureEvaluator.runMeasureEvaluate(
        patientResourcesIBase,
        libraryMeasure,
        fhirContext,
        cqlMeasureReportURL,
        cqlMeasureReportStartDate,
        cqlMeasureReportEndDate,
        cqlMeasureReportReportType,
        cqlMeasureReportSubject
      )
    var resultItem = ResultItem("True", true, "", "100", "100")
    reportViewModel.resultForIndividual.value = resultItem
    reportViewModel.reportState.currentScreen = ReportScreen.RESULT
  }

  fun handleMeasureEvaluateLibrary(auxMeasureEvaluateLibData: String) {
    measureEvaluateLibraryData = auxMeasureEvaluateLibData
    this.let {
      FileUtil.writeFileOnInternalStorage(
        it,
        fileNameMeasureLibraryCql,
        measureEvaluateLibraryData,
        dirCQLDirRoot
      )
    }
    val libraryStreamMeasure: InputStream =
      ByteArrayInputStream(measureEvaluateLibraryData.toByteArray())
    libraryMeasure = parser.parseResource(libraryStreamMeasure) as IBaseBundle
    reportViewModel.reportState.currentScreen = ReportScreen.HOME
  }

  fun loadCQLMeasurePatientData() {
    reportViewModel
      .fetchCQLPatientData(parser, fhirResourceDataSource, "$patientURL$patientId/\$everything")
      .observe(this, this::handleCQLMeasureLoadPatient)
  }

  fun handleCQLMeasureLoadPatient(auxPatientData: String) {
    if (auxPatientData.isNotEmpty()) {
      val testData = libraryEvaluator.processCQLPatientBundle(auxPatientData)
      val patientDataStream: InputStream = ByteArrayInputStream(testData!!.toByteArray())
      patientDataIBase = parser.parseResource(patientDataStream) as IBaseBundle
      patientResourcesIBase.add(patientDataIBase)
      handleMeasureEvaluate()
    } else {
      var resultItem = ResultItem("Failed", false, "", "Failed", "0")
      reportViewModel.resultForIndividual.value = resultItem
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
    loadCQLMeasurePatientData()
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
