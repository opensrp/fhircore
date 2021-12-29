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

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.view.View
import android.widget.TextView
import androidx.lifecycle.MutableLiveData
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import ca.uhn.fhir.parser.IParser
import com.google.android.fhir.FhirEngine
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkObject
import java.io.ByteArrayInputStream
import java.io.InputStream
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.hl7.fhir.instance.model.api.IBaseBundle
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowAlertDialog
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.coroutine.CoroutineTestRule
import org.smartregister.fhircore.anc.data.patient.PatientRepository
import org.smartregister.fhircore.anc.data.report.ReportRepository
import org.smartregister.fhircore.anc.robolectric.ActivityRobolectricTest
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.ui.base.AlertDialogue
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.FileUtil

@ExperimentalCoroutinesApi
@HiltAndroidTest
class ReportHomeActivityTest : ActivityRobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
  @get:Rule(order = 1) val coroutinesTestRule = CoroutineTestRule()
  @Inject lateinit var reportRepository: ReportRepository
  @Inject lateinit var dispatcherProvider: DispatcherProvider
  @Inject lateinit var patientRepository: PatientRepository
  @MockK lateinit var fhirEngine: FhirEngine
  @MockK lateinit var parser: IParser
  @MockK lateinit var fhirResourceDataSource: FhirResourceDataSource
  @BindValue lateinit var reportViewModel: ReportViewModel
  private lateinit var reportHomeActivity: ReportHomeActivity
  private lateinit var reportHomeActivitySpy: ReportHomeActivity
  var libraryData = FileUtil.readJsonFile("test/resources/cql/library.json")
  var valueSetData = FileUtil.readJsonFile("test/resources/cql/valueSet.json")
  val valueSetDataStream: InputStream = ByteArrayInputStream(valueSetData.toByteArray())
  var patientData = FileUtil.readJsonFile("test/resources/cql/patient.json")
  val patientDataStream: InputStream = ByteArrayInputStream(patientData.toByteArray())
  var helperData = FileUtil.readJsonFile("test/resources/cql/helper.json")
  val parameters = "{\"parameters\":\"parameters\"}"

  @Before
  fun setUp() {
    hiltRule.inject()
    MockKAnnotations.init(this, relaxUnitFun = true)
    mockkObject(FileUtil)
    reportViewModel =
      spyk(
        ReportViewModel(
          repository = reportRepository,
          dispatcher = dispatcherProvider,
          patientRepository = patientRepository
        )
      )
    reportHomeActivity = Robolectric.buildActivity(ReportHomeActivity::class.java).create().get()
    reportHomeActivitySpy = spyk(objToCopy = reportHomeActivity)
    reportHomeActivitySpy.libraryResources = ArrayList()
    reportHomeActivitySpy.fhirContext = FhirContext.forCached(FhirVersionEnum.R4)
    reportHomeActivitySpy.parser = reportHomeActivitySpy.fhirContext.newJsonParser()
    reportHomeActivitySpy.valueSetBundle =
      reportHomeActivitySpy.parser.parseResource(valueSetDataStream) as IBaseBundle
    reportHomeActivitySpy.patientDataIBase =
      reportHomeActivitySpy.parser.parseResource(patientDataStream) as IBaseBundle
    reportHomeActivitySpy.libraryData = libraryData
    reportHomeActivitySpy.helperData = helperData
    reportHomeActivitySpy.libraryMeasure = reportHomeActivitySpy.valueSetBundle
  }

  @Test
  fun testActivityNotNull() {
    Assert.assertNotNull(reportHomeActivity)
  }

  override fun getActivity(): Activity {
    return reportHomeActivity
  }

  @Test
  fun testHandleCqlMeasureLoadPatient() {
    val testData = patientData
    every { reportHomeActivitySpy.handleMeasureEvaluate() } returns Unit
    reportHomeActivitySpy.handleCqlMeasureLoadPatient(testData)
    Assert.assertNotNull(reportHomeActivitySpy.patientDataIBase)
  }

  @Test
  fun testHandleCqlLibraryData() {
    val auxLibraryData = "auxLibraryData"
    every { reportHomeActivitySpy.loadCqlHelperData() } returns Unit
    every { FileUtil.writeFileOnInternalStorage(any(), any(), any(), any()) } returns Unit
    reportHomeActivitySpy.handleCqlLibraryData(auxLibraryData)
    Assert.assertEquals(auxLibraryData, reportHomeActivitySpy.libraryData)
  }

  @Test
  fun testHandleCqlHelperData() {
    val auxHelperData = "auxHelperData"
    every { reportHomeActivitySpy.loadCqlValueSetData() } returns Unit
    every { reportHomeActivitySpy.loadCqlLibrarySources() } returns Unit
    every { FileUtil.writeFileOnInternalStorage(any(), any(), any(), any()) } returns Unit

    reportHomeActivitySpy.handleCqlHelperData("auxHelperData")
    Assert.assertEquals(auxHelperData, reportHomeActivitySpy.helperData)
  }

  @Test
  fun testPostValueSetData() {
    reportHomeActivitySpy.postValueSetData(valueSetData)
    Assert.assertNotNull(reportHomeActivitySpy.valueSetBundle)
  }

  @Test
  fun testLoadCqlLibrarySources() {
    reportHomeActivitySpy.loadCqlLibrarySources()
    Assert.assertNotNull(reportHomeActivitySpy.libraryResources)
  }

  @Test
  fun testHandleCqlValueSetData() {
    val auxValueSetData = "auxValueSetData"
    every { FileUtil.writeFileOnInternalStorage(any(), any(), any(), any()) } returns Unit
    every { reportHomeActivitySpy.postValueSetData(any()) } returns Unit
    reportHomeActivitySpy.handleCqlValueSetData(auxValueSetData)
    Assert.assertEquals(auxValueSetData, reportHomeActivitySpy.valueSetData)
  }

  @Test
  fun testHandleCql() {
    val parameters = "{\"parameters\":\"parameters\"}"

    every {
      reportHomeActivitySpy.libraryEvaluator.runCql(
        resources = reportHomeActivitySpy.libraryResources,
        valueSetData = reportHomeActivitySpy.valueSetBundle,
        testData = reportHomeActivitySpy.patientDataIBase,
        fhirContext = any(),
        evaluatorId = any(),
        context = any(),
        contextLabel = any()
      )
    } returns parameters
    reportHomeActivitySpy.handleCql()
    Assert.assertNotNull(parameters)
  }

  @Test
  fun testHandleMeasureEvaluate() {
    every {
      reportHomeActivitySpy.measureEvaluator.runMeasureEvaluate(
        patientResources = any(),
        library = reportHomeActivitySpy.libraryMeasure,
        fhirContext = any(),
        url = any(),
        periodStartDate = any(),
        periodEndDate = any(),
        reportType = any(),
        subject = any()
      )
    } returns parameters
    reportHomeActivitySpy.handleMeasureEvaluate()
    Assert.assertNotNull(parameters)
  }

  @Test
  fun testLoadCqlLibraryData() {

    every { reportHomeActivitySpy.dir.exists() } returns true
    every { reportHomeActivitySpy.loadCqlHelperData() } returns Unit
    every { FileUtil.readFileFromInternalStorage(any(), any(), any()) } returns ""

    reportHomeActivitySpy.loadCqlLibraryData()

    every { reportHomeActivitySpy.dir.exists() } returns false
    val auxCQLLibraryData = "auxCQLLibraryData"
    val libraryData = MutableLiveData<String>()
    libraryData.value = auxCQLLibraryData

    coEvery { reportViewModel.fetchCqlLibraryData(parser, fhirResourceDataSource, any()) } returns
      libraryData

    reportHomeActivitySpy.loadCqlLibraryData()
    Assert.assertNotNull(libraryData.value)
    Assert.assertEquals(auxCQLLibraryData, libraryData.value)
  }

  @Test
  fun testLoadMeasureEvaluateLibrary() {
    every { reportHomeActivitySpy.dir.exists() } returns true
    every { FileUtil.readFileFromInternalStorage(any(), any(), any()) } returns valueSetData
    reportHomeActivitySpy.loadMeasureEvaluateLibrary()
    Assert.assertNotNull(reportHomeActivitySpy.libraryMeasure)

    every { reportHomeActivitySpy.dir.exists() } returns false
    val auxCQLMeasureEvaluateData = "loadMeasureEvaluateLibraryData"
    val libraMeasureEvaluateData = MutableLiveData<String>()
    libraMeasureEvaluateData.value = auxCQLMeasureEvaluateData
    coEvery {
      reportViewModel.fetchCqlMeasureEvaluateLibraryAndValueSets(
        parser,
        fhirResourceDataSource,
        any(),
        any(),
        any()
      )
    } returns libraMeasureEvaluateData

    reportHomeActivitySpy.loadMeasureEvaluateLibrary()
    Assert.assertNotNull(libraMeasureEvaluateData.value)
    Assert.assertEquals(auxCQLMeasureEvaluateData, libraMeasureEvaluateData.value)
  }

  @Test
  fun testLoadCqlHelperData() {

    every { reportHomeActivitySpy.dir.exists() } returns true
    every { reportHomeActivitySpy.loadCqlLibrarySources() } returns Unit
    every { reportHomeActivitySpy.loadCqlValueSetData() } returns Unit
    every { FileUtil.readFileFromInternalStorage(any(), any(), any()) } returns ""

    reportHomeActivitySpy.loadCqlHelperData()

    every { reportHomeActivitySpy.dir.exists() } returns false
    val auxCQLHelperData = "auxCQLHelperData"
    val helperData = MutableLiveData<String>()
    helperData.value = auxCQLHelperData

    coEvery {
      reportViewModel.fetchCqlFhirHelperData(parser, fhirResourceDataSource, any())
    } returns helperData

    reportHomeActivitySpy.loadCqlHelperData()
    Assert.assertNotNull(helperData.value)
    Assert.assertEquals(auxCQLHelperData, helperData.value)
  }

  @Test
  fun testLoadCqlValueSetData() {

    every { reportHomeActivitySpy.dir.exists() } returns true
    every { reportHomeActivitySpy.postValueSetData(any()) } returns Unit
    every { FileUtil.readFileFromInternalStorage(any(), any(), any()) } returns valueSetData
    reportHomeActivitySpy.loadCqlValueSetData()

    every { reportHomeActivitySpy.dir.exists() } returns false
    val auxCQLValueSetData = "auxCQLValueSetData"
    val valueSetData = MutableLiveData<String>()
    valueSetData.value = auxCQLValueSetData
    coEvery { reportViewModel.fetchCqlValueSetData(parser, fhirResourceDataSource, any()) } returns
      valueSetData
    reportHomeActivitySpy.loadCqlValueSetData()
    Assert.assertNotNull(valueSetData)
    Assert.assertEquals(auxCQLValueSetData, valueSetData.value)
  }

  @Test
  fun testLoadCqlMeasurePatientData() {
    val auxCQLPatientData = "auxCQLPatientData"
    val patientData = MutableLiveData<String>()
    patientData.value = auxCQLPatientData
    reportHomeActivitySpy.patientId = "1"
    coEvery { reportViewModel.fetchCqlPatientData(parser, fhirResourceDataSource, any()) } returns
      patientData

    reportHomeActivitySpy.loadCqlMeasurePatientData()
    Assert.assertNotNull(patientData.value)
    Assert.assertEquals(auxCQLPatientData, patientData.value)
  }

  @Test
  fun testHandleMeasureEvaluateLibrary() {
    every { reportHomeActivitySpy.dir.exists() } returns true
    every { FileUtil.writeFileOnInternalStorage(any(), any(), any(), any()) } returns Unit
    reportHomeActivitySpy.handleMeasureEvaluateLibrary(valueSetData)
    Assert.assertNotNull(reportHomeActivitySpy.libraryMeasure)
  }

  @After
  override fun tearDown() {
    super.tearDown()
    unmockkObject(FileUtil)
  }

  @Test
  fun testShowDatePicker() {
    coEvery { reportViewModel.showDatePicker.value } returns true
    reportHomeActivitySpy.showDatePicker()
    Assert.assertEquals(true, reportViewModel.showDatePicker.value)
  }

  @Test
  fun testLimitRange() {
    Assert.assertNotNull(reportHomeActivitySpy.limitRange(1L, 2L, 3L))
  }

  @Test
  fun generateMeasureReportTest() {
    val reportMeasureItem = "ANC"
    val selectedPatientId = "123456789"
    val selectedPatientName = "Patient Mom"
    val startDate = "01/12/2020"
    val endDate = "01/12/2021"

    every { reportHomeActivitySpy.loadCqlMeasurePatientData() } returns Unit

    reportHomeActivitySpy.generateMeasureReport(
      startDate,
      endDate,
      reportMeasureItem,
      selectedPatientId,
      selectedPatientName
    )
    Assert.assertEquals(reportHomeActivitySpy.patientId, selectedPatientId)
  }

  @Test
  fun testProcessGenerateReport() {
    coEvery { reportViewModel.patientSelectionType.value } returns "All"
    reportViewModel.auxGenerateReport()
    Assert.assertEquals(true, reportViewModel.processGenerateReport.value)
    Assert.assertEquals(
      ReportViewModel.ReportScreen.PREHOMElOADING,
      reportViewModel.reportState.currentScreen
    )

    val reportMeasureItem = "ANC"
    val selectedPatientId = "123456789"
    val selectedPatientName = "Patient Mom"
    val startDate = "01/12/2020"
    val endDate = "01/12/2021"
    every { reportHomeActivitySpy.loadCqlMeasurePatientData() } returns Unit
    reportHomeActivitySpy.generateMeasureReport(
      startDate,
      endDate,
      reportMeasureItem,
      selectedPatientId,
      selectedPatientName
    )
    Assert.assertEquals(reportHomeActivitySpy.patientId, selectedPatientId)
  }

  @Test
  fun testHandleCQLMeasureLoadPatientForEmptyData() {
    val testData = ""
    // every { reportHomeActivitySpy.handleMeasureEvaluate() } returns Unit
    reportHomeActivitySpy.handleCqlMeasureLoadPatient(testData)
    Assert.assertEquals(
      ReportViewModel.ReportScreen.RESULT,
      reportViewModel.reportState.currentScreen
    )
    Assert.assertEquals("Failed", reportViewModel.resultForIndividual.value?.status)
  }

  @Test
  fun testGenerateMeasureReport() {
    coEvery { reportViewModel.processGenerateReport.value } returns true
    val reportMeasureItem = "ANC"
    val selectedPatientId = "123456789"
    val selectedPatientName = "Patient Mom"
    val startDate = "01/12/2020"
    val endDate = "01/12/2021"
    reportHomeActivitySpy.generateMeasureReport(
      startDate,
      endDate,
      reportMeasureItem,
      selectedPatientId,
      selectedPatientName
    )
    Assert.assertEquals(
      ReportViewModel.ReportScreen.PREHOMElOADING,
      reportViewModel.reportState.currentScreen
    )
    Assert.assertEquals(reportHomeActivitySpy.cqlMeasureReportReportType, reportMeasureItem)
  }

  @Test
  fun testProcessGenerateReportForInvalidData() {
    coEvery { reportViewModel.patientSelectionType.value } returns "not-all"
    coEvery { reportViewModel.selectedPatientItem.value } returns null
    reportViewModel.auxGenerateReport()
    Assert.assertEquals(true, reportViewModel.alertSelectPatient.value)

    AlertDialogue.showErrorAlert(
      context = reportHomeActivity,
      message = getString(R.string.select_patient),
      title = getString(R.string.invalid_selection)
    )

    val dialog = Shadows.shadowOf(ShadowAlertDialog.getLatestAlertDialog())

    assertSimpleMessageDialog(
      dialog,
      getString(R.string.select_patient),
      getString(R.string.invalid_selection),
      getString(R.string.questionnaire_alert_ack_button_title)
    )
  }

  private fun assertSimpleMessageDialog(
    dialog: ShadowAlertDialog,
    message: String,
    title: String,
    confirmButtonTitle: String
  ) {
    val alertDialog = ReflectionHelpers.getField<AlertDialog>(dialog, "realAlertDialog")

    Assert.assertNotNull(dialog)
    Assert.assertTrue(alertDialog.isShowing)

    Assert.assertEquals(
      message,
      dialog.view.findViewById<TextView>(org.smartregister.fhircore.engine.R.id.tv_alert_message)!!
        .text
    )
    Assert.assertEquals(title, dialog.title)

    Assert.assertEquals(
      View.GONE,
      dialog.view.findViewById<View>(org.smartregister.fhircore.engine.R.id.pr_circular)!!
        .visibility
    )

    val confirmButton = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE)
    Assert.assertEquals(View.VISIBLE, confirmButton.visibility)
    Assert.assertEquals(confirmButtonTitle, confirmButton.text)
  }
}
