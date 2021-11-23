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
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import ca.uhn.fhir.parser.IParser
import com.google.android.fhir.FhirEngine
import com.google.common.collect.Lists
import dagger.hilt.android.AndroidEntryPoint
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.util.concurrent.Executors
import org.hl7.fhir.instance.model.api.IBaseBundle
import org.hl7.fhir.instance.model.api.IBaseResource
import org.smartregister.fhircore.anc.AncApplication
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.data.report.ReportRepository
import org.smartregister.fhircore.engine.cql.LibraryEvaluator
import org.smartregister.fhircore.engine.cql.MeasureEvaluator
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.ui.base.BaseMultiLanguageActivity
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.FileUtil
import javax.inject.Inject

@AndroidEntryPoint
class ReportHomeActivity : BaseMultiLanguageActivity() {

  @Inject lateinit var fhirResourceDataSource: FhirResourceDataSource
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
  val reportViewModel by viewModels<ReportViewModel>()

  val executor = Executors.newSingleThreadExecutor()
  val handler = Handler(Looper.getMainLooper())

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    libraryEvaluator = LibraryEvaluator()
    measureEvaluator = MeasureEvaluator()
    fhirContext = FhirContext.forCached(FhirVersionEnum.R4)
    parser = fhirContext.newJsonParser()

    val patientId =
      intent.extras?.getString(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY) ?: ""
    reportViewModel.patientId = patientId
    reportViewModel.backPress.observe(
      this,
      {
        if (it) {
          finish()
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

    setContent {
      AppTheme {
        Surface(color = colorResource(id = R.color.white)) {
          Column {
            TopAppBar(
              title = {
                Text(text = stringResource(id = R.string.reports), Modifier.testTag(TOOLBAR_TITLE))
              },
              navigationIcon = {
                IconButton(
                  onClick = reportViewModel::onBackPress,
                  Modifier.testTag(TOOLBAR_BACK_ARROW)
                ) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back arrow") }
              }
            )
            loadCQLLibraryData()
            loadMeasureEvaluateLibrary()
            ReportHomeScreen(reportViewModel)
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
  }

  fun loadCQLMeasurePatientData() {
    reportViewModel
      .fetchCQLPatientData(parser, fhirResourceDataSource, "$patientURL$patientId/\$everything")
      .observe(this, this::handleCQLMeasureLoadPatient)
  }

  fun handleCQLMeasureLoadPatient(auxPatientData: String) {
    patientDetailsData = auxPatientData
  }
}
