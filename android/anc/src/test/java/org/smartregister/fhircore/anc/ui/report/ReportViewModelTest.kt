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

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import ca.uhn.fhir.parser.IParser
import com.google.android.fhir.FhirEngine
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Resource
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.anc.coroutine.CoroutineTestRule
import org.smartregister.fhircore.anc.data.report.ReportRepository
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource

@ExperimentalCoroutinesApi
internal class ReportViewModelTest {

  private lateinit var fhirEngine: FhirEngine
  private lateinit var reportRepository: ReportRepository
  private lateinit var reportViewModel: ReportViewModel
  @MockK lateinit var parser: IParser
  @MockK lateinit var fhirResourceDataSource: FhirResourceDataSource
  @MockK lateinit var resource: Resource
  @MockK lateinit var entryList: List<Bundle.BundleEntryComponent>
  @MockK lateinit var bundle: Bundle

  @get:Rule var coroutinesTestRule = CoroutineTestRule()
  @get:Rule var instantTaskExecutorRule = InstantTaskExecutorRule()

  @Before
  fun setUp() {
    MockKAnnotations.init(this, relaxUnitFun = true)

    fhirEngine = mockk(relaxed = true)
    reportRepository = mockk()

    reportViewModel =
      spyk(ReportViewModel(reportRepository, coroutinesTestRule.testDispatcherProvider))
  }

  @Test
  fun testShouldVerifyBackClickListener() {
    reportViewModel.onBackPress()
    Assert.assertEquals(true, reportViewModel.backPress.value)
  }

  @Test
  fun testFetchCQLLibraryData() {
    val auxCQLLibraryData = "Library JSON"
    coroutinesTestRule.runBlockingTest {
      coEvery { fhirResourceDataSource.loadData(any()) } returns bundle
      coEvery { bundle.entry } returns entryList
      coEvery { entryList[0].resource } returns resource
      coEvery { parser.encodeResourceToString(resource) } returns auxCQLLibraryData
    }
    val libraryDataLiveData: String =
      reportViewModel.fetchCQLLibraryData(parser, fhirResourceDataSource, "").value!!
    Assert.assertEquals(auxCQLLibraryData, libraryDataLiveData)
  }

  @Test
  fun testFetchCQLFhirHelperData() {
    val auxCQLHelperData = "Helper JSON"
    coroutinesTestRule.runBlockingTest {
      coEvery { fhirResourceDataSource.loadData(any()) } returns bundle
      coEvery { bundle.entry } returns entryList
      coEvery { entryList[0].resource } returns resource
      coEvery { parser.encodeResourceToString(resource) } returns auxCQLHelperData
    }
    val libraryDataLiveData: String =
      reportViewModel.fetchCQLFhirHelperData(parser, fhirResourceDataSource, "").value!!
    Assert.assertEquals(auxCQLHelperData, libraryDataLiveData)
  }

  @Test
  fun testFetchCQLValueSetData() {
    val auxCQLValueSetData = "ValueSet JSON"
    coroutinesTestRule.runBlockingTest {
      coEvery { fhirResourceDataSource.loadData(any()) } returns bundle
      coEvery { parser.encodeResourceToString(bundle) } returns auxCQLValueSetData
    }
    val libraryDataLiveData: String =
      reportViewModel.fetchCQLValueSetData(parser, fhirResourceDataSource, "").value!!
    Assert.assertEquals(auxCQLValueSetData, libraryDataLiveData)
  }

  @Test
  fun testFetchCQLPatientData() {
    val auxCQLValueSetData = "Patient Data JSON"
    coroutinesTestRule.runBlockingTest {
      coEvery { fhirResourceDataSource.loadData(any()) } returns bundle
      coEvery { parser.encodeResourceToString(bundle) } returns auxCQLValueSetData
    }
    val libraryDataLiveData: String =
      reportViewModel.fetchCQLPatientData(parser, fhirResourceDataSource, "1").value!!
    Assert.assertEquals(auxCQLValueSetData, libraryDataLiveData)
  }

  @Test
  fun testFetchCQLMeasureEvaluateLibraryAndValueSets() {
    val auxCQLLibraryAndValueSetData = "{\"parameters\":\"parameters\"}"
    coroutinesTestRule.runBlockingTest {
      coEvery { fhirResourceDataSource.loadData(any()) } returns bundle
      coEvery { bundle.entry } returns entryList
      coEvery { entryList[0].resource } returns resource
      coEvery { parser.encodeResourceToString(resource) } returns auxCQLLibraryAndValueSetData
    }
    val libraryDataLiveData: String =
      reportViewModel.fetchCQLMeasureEvaluateLibraryAndValueSets(
          parser,
          fhirResourceDataSource,
          "https://hapi.fhir.org/baseR4/Library?_id=ANCDataElements,WHOCommon,ANCConcepts,ANCContactDataElements,FHIRHelpers,ANCStratifiers,ANCIND01,ANCCommon,ANCBaseDataElements,FHIRCommon,ANCBaseConcepts",
          "https://hapi.fhir.org/baseR4/Measure?_id=ANCIND01",
          ""
        )
        .value!!
    Assert.assertNotNull(libraryDataLiveData)
  }
}
