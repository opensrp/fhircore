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
import java.util.Date
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.DateType
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.Resource
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.anc.coroutine.CoroutineTestRule
import org.smartregister.fhircore.anc.data.anc.AncPatientRepository
import org.smartregister.fhircore.anc.data.anc.model.AncPatientDetailItem
import org.smartregister.fhircore.anc.data.anc.model.AncPatientItem
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.util.DateUtils.getDate
import org.smartregister.fhircore.engine.util.extension.plusWeeksAsString

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

    coEvery { patientRepository.fetchCarePlan(any()) } returns
      listOf(buildCarePlanWithActive("1111"))

    val carePlanList = ancDetailsViewModel.fetchCarePlan().value

    if (carePlanList != null && carePlanList.isNotEmpty()) {
      Assert.assertEquals(1, carePlanList!!.size)
      with(carePlanList!!.first()) { Assert.assertEquals(cpTitle, title) }
    }
  }

  private fun buildCarePlanWithActive(subject: String): CarePlan {
    val date = DateType(Date())
    val end = date.plusWeeksAsString(4).getDate("yyyy-MM-dd")
    return CarePlan().apply {
      this.id = "11190"
      this.status = CarePlan.CarePlanStatus.ACTIVE
      this.period.start = date.value
      this.period.end = end
      this.subject = Reference().apply { reference = "Patient/$subject" }
      this.addActivity().detail.apply {
        this.description = "First Care Plan"
        this.scheduledPeriod.start = Date()
        this.status = CarePlan.CarePlanActivityStatus.SCHEDULED
      }
    }
  }

  @Test
  fun fetchCQLMeasureEvaluateLibraryAndValueSetsTest() {
    val auxCQLLibraryAndValueSetData = "{\"parameters\":\"parameters\"}"
    coroutinesTestRule.runBlockingTest {
      coEvery { fhirResourceDataSource.loadData(any()) } returns bundle
      coEvery { bundle.entry } returns entryList
      coEvery { entryList[0].resource } returns resource
      coEvery { parser.encodeResourceToString(resource) } returns auxCQLLibraryAndValueSetData
    }
    val libraryDataLiveData: String =
      ancDetailsViewModel.fetchCQLMeasureEvaluateLibraryAndValueSets(
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
