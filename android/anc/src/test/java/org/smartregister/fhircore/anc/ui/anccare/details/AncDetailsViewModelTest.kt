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

package org.smartregister.fhircore.anc.ui.anccare.details

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import ca.uhn.fhir.parser.IParser
import com.google.android.fhir.FhirEngine
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.spyk
import java.text.SimpleDateFormat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.bouncycastle.asn1.x500.style.RFC4519Style.title
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Resource
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.anc.coroutine.CoroutineTestRule
import org.smartregister.fhircore.anc.data.anc.AncPatientRepository
import org.smartregister.fhircore.anc.data.anc.model.AncPatientDetailItem
import org.smartregister.fhircore.anc.data.anc.model.AncPatientItem
import org.smartregister.fhircore.anc.data.anc.model.CarePlanItem
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource

@ExperimentalCoroutinesApi
internal class AncDetailsViewModelTest {
  private lateinit var fhirEngine: FhirEngine

  private lateinit var ancDetailsViewModel: AncDetailsViewModel

  private lateinit var patientRepository: AncPatientRepository

  private val patientId = "samplePatientId"

  @get:Rule var coroutinesTestRule = CoroutineTestRule()

  @get:Rule var instantTaskExecutorRule = InstantTaskExecutorRule()

  @Before
  fun setUp() {
    MockKAnnotations.init(this, relaxUnitFun = true)

    fhirEngine = mockk(relaxed = true)
    patientRepository = mockk()

    val ancPatientDetailItem = spyk<AncPatientDetailItem>()

    every { ancPatientDetailItem.patientDetails } returns
      AncPatientItem(patientId, "Mandela Nelson", "M", "26")
    every { ancPatientDetailItem.patientDetailsHead } returns AncPatientItem()
    coEvery { patientRepository.fetchDemographics(patientId) } returns ancPatientDetailItem

    ancDetailsViewModel =
      spyk(
        AncDetailsViewModel(patientRepository, coroutinesTestRule.testDispatcherProvider, patientId)
      )
  }

  @Test
  fun fetchDemographics() {
    coroutinesTestRule.runBlockingTest {
      val patient = spyk<Patient>().apply { idElement.id = patientId }
      coEvery { fhirEngine.load(Patient::class.java, patientId) } returns patient
      val ancPatientDetailItem: AncPatientDetailItem =
        ancDetailsViewModel.fetchDemographics().value!!
      Assert.assertNotNull(ancPatientDetailItem)
      Assert.assertEquals(ancPatientDetailItem.patientDetails.patientIdentifier, patientId)
      val patientDetails =
        ancPatientDetailItem.patientDetails.name +
          ", " +
          ancPatientDetailItem.patientDetails.gender +
          ", " +
          ancPatientDetailItem.patientDetails.age
      val patientId =
        ancPatientDetailItem.patientDetailsHead.demographics +
          " ID: " +
          ancPatientDetailItem.patientDetails.patientIdentifier

      Assert.assertEquals(patientDetails, "Mandela Nelson, M, 26")
      Assert.assertEquals(patientId, " ID: samplePatientId")
    }
  }

  @MockK lateinit var parser: IParser
  @MockK lateinit var fhirResourceDataSource: FhirResourceDataSource
  @MockK lateinit var resource: Resource
  @MockK lateinit var entryList: List<Bundle.BundleEntryComponent>
  @MockK lateinit var bundle: Bundle

  @Test
  fun fetchCQLLibraryDataTest() {
    val auxCQLLibraryData = "Library JSON"
    coroutinesTestRule.runBlockingTest {
      coEvery { fhirResourceDataSource.loadData(any()) } returns bundle
      coEvery { bundle.entry } returns entryList
      coEvery { entryList[0].resource } returns resource
      coEvery { parser.encodeResourceToString(resource) } returns auxCQLLibraryData
    }
    val libraryDataLiveData: String =
      ancDetailsViewModel.fetchCQLLibraryData(parser, fhirResourceDataSource, "").value!!
    Assert.assertEquals(auxCQLLibraryData, libraryDataLiveData)
  }

  @Test
  fun fetchCQLFhirHelperDataTest() {
    val auxCQLHelperData = "Helper JSON"
    coroutinesTestRule.runBlockingTest {
      coEvery { fhirResourceDataSource.loadData(any()) } returns bundle
      coEvery { bundle.entry } returns entryList
      coEvery { entryList[0].resource } returns resource
      coEvery { parser.encodeResourceToString(resource) } returns auxCQLHelperData
    }
    val libraryDataLiveData: String =
      ancDetailsViewModel.fetchCQLFhirHelperData(parser, fhirResourceDataSource, "").value!!
    Assert.assertEquals(auxCQLHelperData, libraryDataLiveData)
  }

  @Test
  fun fetchCQLValueSetDataTest() {
    val auxCQLValueSetData = "ValueSet JSON"
    coroutinesTestRule.runBlockingTest {
      coEvery { fhirResourceDataSource.loadData(any()) } returns bundle
      coEvery { parser.encodeResourceToString(bundle) } returns auxCQLValueSetData
    }
    val libraryDataLiveData: String =
      ancDetailsViewModel.fetchCQLValueSetData(parser, fhirResourceDataSource, "").value!!
    Assert.assertEquals(auxCQLValueSetData, libraryDataLiveData)
  }

  @Test
  fun fetchCQLPatientDataTest() {
    val auxCQLValueSetData = "Patient Data JSON"
    coroutinesTestRule.runBlockingTest {
      coEvery { fhirResourceDataSource.loadData(any()) } returns bundle
      coEvery { parser.encodeResourceToString(bundle) } returns auxCQLValueSetData
    }
    val libraryDataLiveData: String =
      ancDetailsViewModel.fetchCQLPatientData(parser, fhirResourceDataSource, "1").value!!
    Assert.assertEquals(auxCQLValueSetData, libraryDataLiveData)
  }

  @Test
  fun testFetchCarePlanShouldReturnExpectedCarePlan() {

    val cpTitle = "First Care Plan"
    val cpPeriodStartDate = SimpleDateFormat("yyyy-MM-dd").parse("2021-01-01")

    coEvery { patientRepository.fetchCarePlan(any(), any()) } returns
      listOf(CarePlanItem(cpTitle, cpPeriodStartDate!!))

    val carePlanList = ancDetailsViewModel.fetchCarePlan("")

    Assert.assertEquals(1, carePlanList.value!!.size)
    with(carePlanList.value!!.first()) {
      Assert.assertEquals(cpTitle, title)
      Assert.assertEquals(cpPeriodStartDate.time, periodStartDate.time)
    }
  }
}
