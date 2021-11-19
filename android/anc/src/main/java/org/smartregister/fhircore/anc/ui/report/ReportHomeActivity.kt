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
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Surface
import androidx.compose.ui.res.colorResource
import androidx.core.util.Pair
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import ca.uhn.fhir.parser.IParser
import com.google.android.fhir.FhirEngine
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.common.collect.Lists
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.util.Calendar
import kotlin.collections.ArrayList
import kotlin.collections.List
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.hl7.fhir.instance.model.api.IBaseBundle
import org.hl7.fhir.instance.model.api.IBaseResource
import org.smartregister.fhircore.anc.AncApplication
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.data.model.PatientItem
import org.smartregister.fhircore.anc.data.model.VisitStatus
import org.smartregister.fhircore.anc.data.patient.PatientRepository
import org.smartregister.fhircore.anc.data.report.ReportRepository
import org.smartregister.fhircore.anc.ui.anccare.register.Anc
import org.smartregister.fhircore.anc.ui.anccare.register.AncItemMapper
import org.smartregister.fhircore.anc.ui.report.ReportViewModel.ReportScreen
import org.smartregister.fhircore.engine.cql.LibraryEvaluator
import org.smartregister.fhircore.engine.cql.MeasureEvaluator
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.ui.register.RegisterDataViewModel
import org.smartregister.fhircore.engine.ui.register.model.RegisterFilterType
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.FileUtil
import org.smartregister.fhircore.engine.util.extension.createFactory

class ReportHomeActivity : BaseMultiLanguageActivity() {

  lateinit var fhirResourceDataSource: FhirResourceDataSource
  private lateinit var fhirEngine: FhirEngine
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
  lateinit var patientDetailsData: String
  lateinit var patientId: String
  lateinit var reportViewModel: ReportViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    libraryEvaluator = LibraryEvaluator()
    measureEvaluator = MeasureEvaluator()
    fhirContext = FhirContext.forCached(FhirVersionEnum.R4)
    parser = fhirContext.newJsonParser()
    fhirResourceDataSource = FhirResourceDataSource.getInstance(AncApplication.getContext())
    fhirEngine = AncApplication.getContext().fhirEngine

    val patientId =
      intent.extras?.getString(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY) ?: ""
    val repository = ReportRepository((application as AncApplication).fhirEngine, patientId, this)
    val ancPatientRepository =
      PatientRepository((application as AncApplication).fhirEngine, AncItemMapper)
    val dispatcher: DispatcherProvider = DefaultDispatcherProvider
    reportViewModel = ReportViewModel(repository, ancPatientRepository, dispatcher)

    reportViewModel.registerDataViewModel = initializeRegisterDataViewModel(ancPatientRepository)
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
          showDateRangePicker(reportViewModel::onDateSelected)
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
            }
          } else {
            reportViewModel.registerDataViewModel.run {
              showResultsCount(false)
              reloadCurrentPageData()
            }
          }
        }
      }
    )

    setContent {
      AppTheme {
        Surface(color = colorResource(id = R.color.white)) {
          Column {
            ReportView(reportViewModel)
            loadCQLLibraryData()
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

  fun handleMeasureEvaluate(): String {
    return measureEvaluator.runMeasureEvaluate(
      patientResourcesIBase,
      libraryMeasure,
      fhirContext,
      cqlMeasureReportURL,
      cqlMeasureReportStartDate,
      cqlMeasureReportEndDate,
      cqlMeasureReportReportType,
      patientDetailsData.substring(0, patientDetailsData.indexOf(","))
    )
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
    patientDetailsData = auxPatientData
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
