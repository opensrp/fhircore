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
import androidx.lifecycle.MutableLiveData
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import ca.uhn.fhir.parser.IParser
import com.google.android.fhir.FhirEngine
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkObject
import java.io.ByteArrayInputStream
import java.io.InputStream
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.hl7.fhir.instance.model.api.IBaseBundle
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.annotation.Config
import org.smartregister.fhircore.anc.activity.ActivityRobolectricTest
import org.smartregister.fhircore.anc.coroutine.CoroutineTestRule
import org.smartregister.fhircore.anc.shadow.AncApplicationShadow
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.util.FileUtil

@ExperimentalCoroutinesApi
@Config(shadows = [AncApplicationShadow::class])
class ReportHomeActivityTest : ActivityRobolectricTest() {

  private lateinit var reportHomeActivity: ReportHomeActivity
  private lateinit var reportHomeActivitySpy: ReportHomeActivity

  @MockK lateinit var fhirEngine: FhirEngine
  @get:Rule var coroutinesTestRule = CoroutineTestRule()
  @MockK lateinit var parser: IParser
  @MockK lateinit var fhirResourceDataSource: FhirResourceDataSource
  var libraryData = FileUtil.readJsonFile("test/resources/cql/library.json")
  var valueSetData = FileUtil.readJsonFile("test/resources/cql/valueSet.json")
  val valueSetDataStream: InputStream = ByteArrayInputStream(valueSetData.toByteArray())
  var patientData = FileUtil.readJsonFile("test/resources/cql/patient.json")
  val patientDataStream: InputStream = ByteArrayInputStream(patientData.toByteArray())
  var helperData = FileUtil.readJsonFile("test/resources/cql/helper.json")
  val parameters = "{\"parameters\":\"parameters\"}"
  @MockK lateinit var reportViewModel: ReportViewModel

  @Before
  fun setUp() {
    MockKAnnotations.init(this, relaxUnitFun = true)
    mockkObject(FileUtil)
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
  fun testHandleCQLMeasureLoadPatient() {
    val testData = "test"
    reportHomeActivitySpy.handleCQLMeasureLoadPatient(testData)
    Assert.assertEquals(reportHomeActivitySpy.patientDetailsData, testData)
  }

  @Test
  fun testHandleCQLLibraryData() {
    val auxLibraryData = "auxLibraryData"
    every { reportHomeActivitySpy.loadCQLHelperData() } returns Unit
    every { FileUtil.writeFileOnInternalStorage(any(), any(), any(), any()) } returns Unit
    reportHomeActivitySpy.handleCQLLibraryData(auxLibraryData)
    Assert.assertEquals(auxLibraryData, reportHomeActivitySpy.libraryData)
  }

  @Test
  fun testHandleCQLHelperData() {
    val auxHelperData = "auxHelperData"
    every { reportHomeActivitySpy.loadCQLValueSetData() } returns Unit
    every { reportHomeActivitySpy.loadCQLLibrarySources() } returns Unit
    every { FileUtil.writeFileOnInternalStorage(any(), any(), any(), any()) } returns Unit

    reportHomeActivitySpy.handleCQLHelperData("auxHelperData")
    Assert.assertEquals(auxHelperData, reportHomeActivitySpy.helperData)
  }

  @Test
  fun testPostValueSetData() {
    reportHomeActivitySpy.postValueSetData(valueSetData)
    Assert.assertNotNull(reportHomeActivitySpy.valueSetBundle)
  }

  @Test
  fun testLoadCQLLibrarySources() {
    reportHomeActivitySpy.loadCQLLibrarySources()
    Assert.assertNotNull(reportHomeActivitySpy.libraryResources)
  }

  @Test
  fun testHandleCQLValueSetData() {
    val auxValueSetData = "auxValueSetData"
    every { FileUtil.writeFileOnInternalStorage(any(), any(), any(), any()) } returns Unit
    every { reportHomeActivitySpy.postValueSetData(any()) } returns Unit
    reportHomeActivitySpy.handleCQLValueSetData(auxValueSetData)
    Assert.assertEquals(auxValueSetData, reportHomeActivitySpy.valueSetData)
  }

  @Test
  fun testHandleCQL() {
    val parameters = "{\"parameters\":\"parameters\"}"

    every {
      reportHomeActivitySpy.libraryEvaluator.runCql(
        reportHomeActivitySpy.libraryResources,
        reportHomeActivitySpy.valueSetBundle,
        reportHomeActivitySpy.patientDataIBase,
        any(),
        any(),
        any(),
        any()
      )
    } returns parameters
    reportHomeActivitySpy.handleCQL()
    Assert.assertNotNull(parameters)
  }

  @Test
  fun testHandleMeasureEvaluate() {
    reportHomeActivitySpy.patientDetailsData = "Every,Woman Pregnant"
    every {
      reportHomeActivitySpy.measureEvaluator.runMeasureEvaluate(
        any(),
        reportHomeActivitySpy.libraryMeasure,
        any(),
        any(),
        any(),
        any(),
        any(),
        any()
      )
    } returns parameters
    reportHomeActivitySpy.handleMeasureEvaluate()
    Assert.assertNotNull(parameters)
  }

  @Test
  fun testLoadCQLLibraryData() {

    every { reportHomeActivitySpy.dir.exists() } returns true
    every { reportHomeActivitySpy.loadCQLHelperData() } returns Unit
    every { FileUtil.readFileFromInternalStorage(any(), any(), any()) } returns ""

    reportHomeActivitySpy.loadCQLLibraryData()

    every { reportHomeActivitySpy.dir.exists() } returns false
    val auxCQLLibraryData = "auxCQLLibraryData"
    val libraryData = MutableLiveData<String>()
    libraryData.value = auxCQLLibraryData

    coEvery { reportViewModel.fetchCQLLibraryData(parser, fhirResourceDataSource, any()) } returns
      libraryData

    reportHomeActivitySpy.loadCQLLibraryData()
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
      reportViewModel.fetchCQLMeasureEvaluateLibraryAndValueSets(
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
  fun testLoadCQLHelperData() {

    every { reportHomeActivitySpy.dir.exists() } returns true
    every { reportHomeActivitySpy.loadCQLLibrarySources() } returns Unit
    every { reportHomeActivitySpy.loadCQLValueSetData() } returns Unit
    every { FileUtil.readFileFromInternalStorage(any(), any(), any()) } returns ""

    reportHomeActivitySpy.loadCQLHelperData()

    every { reportHomeActivitySpy.dir.exists() } returns false
    val auxCQLHelperData = "auxCQLHelperData"
    val helperData = MutableLiveData<String>()
    helperData.value = auxCQLHelperData

    coEvery {
      reportViewModel.fetchCQLFhirHelperData(parser, fhirResourceDataSource, any())
    } returns helperData

    reportHomeActivitySpy.loadCQLHelperData()
    Assert.assertNotNull(helperData.value)
    Assert.assertEquals(auxCQLHelperData, helperData.value)
  }

  @Test
  fun testLoadCQLValueSetData() {

    every { reportHomeActivitySpy.dir.exists() } returns true
    every { reportHomeActivitySpy.postValueSetData(any()) } returns Unit
    every { FileUtil.readFileFromInternalStorage(any(), any(), any()) } returns valueSetData
    reportHomeActivitySpy.loadCQLValueSetData()

    every { reportHomeActivitySpy.dir.exists() } returns false
    val auxCQLValueSetData = "auxCQLValueSetData"
    val valueSetData = MutableLiveData<String>()
    valueSetData.value = auxCQLValueSetData
    coEvery { reportViewModel.fetchCQLValueSetData(parser, fhirResourceDataSource, any()) } returns
      valueSetData
    reportHomeActivitySpy.loadCQLValueSetData()
    Assert.assertNotNull(valueSetData)
    Assert.assertEquals(auxCQLValueSetData, valueSetData.value)
  }

  @Test
  fun testLoadCQLMeasurePatientData() {
    val auxCQLPatientData = "auxCQLPatientData"
    val patientData = MutableLiveData<String>()
    patientData.value = auxCQLPatientData
    reportHomeActivitySpy.patientId = "1"
    coEvery { reportViewModel.fetchCQLPatientData(parser, fhirResourceDataSource, any()) } returns
      patientData

    reportHomeActivitySpy.loadCQLMeasurePatientData()
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
  fun tearDown() {
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
}
