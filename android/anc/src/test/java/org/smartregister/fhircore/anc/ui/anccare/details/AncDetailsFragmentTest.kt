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

import android.view.View
import android.widget.TextView
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import ca.uhn.fhir.parser.IParser
import com.google.android.fhir.FhirEngine
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.android.synthetic.main.fragment_anc_details.button_CQLEvaluate
import kotlinx.android.synthetic.main.fragment_anc_details.textView_CQLResults
import kotlinx.android.synthetic.main.fragment_anc_details.textView_EvaluateCQLHeader
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.annotation.Config
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.coroutine.CoroutineTestRule
import org.smartregister.fhircore.anc.data.anc.AncPatientRepository
import org.smartregister.fhircore.anc.data.anc.model.AncPatientDetailItem
import org.smartregister.fhircore.anc.data.anc.model.AncPatientItem
import org.smartregister.fhircore.anc.robolectric.FragmentRobolectricTest
import org.smartregister.fhircore.anc.shadow.AncApplicationShadow
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.util.FileUtil

@ExperimentalCoroutinesApi
@Config(shadows = [AncApplicationShadow::class])
internal class AncDetailsFragmentTest : FragmentRobolectricTest() {

  private lateinit var fhirEngine: FhirEngine

  private lateinit var patientDetailsViewModel: AncDetailsViewModel

  private lateinit var patientDetailsActivity: AncDetailsActivity

  private lateinit var patientRepository: AncPatientRepository

  private lateinit var fragmentScenario: FragmentScenario<AncDetailsFragment>

  private lateinit var patientDetailsFragment: AncDetailsFragment

  @get:Rule var coroutinesTestRule = CoroutineTestRule()

  @get:Rule var instantTaskExecutorRule = InstantTaskExecutorRule()

  private val patientId = "samplePatientId"
  var ancPatientDetailItem = spyk<AncPatientDetailItem>()

  val fileUtil = FileUtil()

  @Before
  fun setUp() {

    MockKAnnotations.init(this, relaxUnitFun = true)

    fhirEngine = mockk(relaxed = true)

    patientRepository = mockk()

    every { ancPatientDetailItem.patientDetails } returns
      AncPatientItem(patientId, "Mandela Nelson", "M", "26")
    every { ancPatientDetailItem.patientDetailsHead } returns AncPatientItem()
    coEvery { patientRepository.fetchDemographics(patientId) } returns ancPatientDetailItem

    patientDetailsViewModel =
      spyk(
        AncDetailsViewModel(patientRepository, coroutinesTestRule.testDispatcherProvider, patientId)
      )

    patientDetailsActivity =
      Robolectric.buildActivity(AncDetailsActivity::class.java).create().get()
    fragmentScenario =
      launchFragmentInContainer(
        factory =
          object : FragmentFactory() {
            override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
              val fragment = spyk(AncDetailsFragment.newInstance())
              every { fragment.activity } returns patientDetailsActivity
              fragment.ancDetailsViewModel = patientDetailsViewModel
              return fragment
            }
          }
      )

    fragmentScenario.onFragment { patientDetailsFragment = it }
  }

  @Test
  fun testThatViewsAreSetupCorrectly() {
    fragmentScenario.moveToState(Lifecycle.State.RESUMED)
    Assert.assertNotNull(patientDetailsFragment.view)

    // No CarePlan available text displayed
    val noVaccinesTextView =
      patientDetailsFragment.view?.findViewById<TextView>(R.id.txtView_noCarePlan)
    Assert.assertEquals(View.VISIBLE, noVaccinesTextView?.visibility)
    Assert.assertEquals("No care plan", noVaccinesTextView?.text.toString())

    // CarePlan list is not displayed
    val immunizationsListView =
      patientDetailsFragment.view?.findViewById<RecyclerView>(R.id.carePlanListView)
    Assert.assertEquals(View.GONE, immunizationsListView?.visibility)
  }

  override fun getFragmentScenario(): FragmentScenario<out Fragment> {
    return fragmentScenario
  }

  override fun getFragment(): Fragment {
    return patientDetailsFragment
  }

  @Test
  fun testThatDemographicViewsAreUpdated() {
    coroutinesTestRule.runBlockingTest {
      fragmentScenario.moveToState(Lifecycle.State.RESUMED)

      patientDetailsFragment.ancDetailsViewModel.patientDemographics.value = ancPatientDetailItem

      val ancPatientDetailItem = patientDetailsViewModel.fetchDemographics().value
      val patientDetails =
        ancPatientDetailItem?.patientDetails?.name +
          ", " +
          ancPatientDetailItem?.patientDetails?.gender +
          ", " +
          ancPatientDetailItem?.patientDetails?.age
      val patientId =
        ancPatientDetailItem?.patientDetailsHead?.demographics +
          " ID: " +
          ancPatientDetailItem?.patientDetails?.patientIdentifier

      val txtViewPatientDetails =
        patientDetailsFragment.view?.findViewById<TextView>(R.id.txtView_patientDetails)
      Assert.assertEquals(patientDetails, txtViewPatientDetails?.text.toString())

      val txtViewPatientId =
        patientDetailsFragment.view?.findViewById<TextView>(R.id.txtView_patientId)
      Assert.assertEquals(patientId, txtViewPatientId?.text.toString())
    }
  }

  @Test
  fun handleCQLLibraryDataTest() {
    val auxLibraryData = "auxLibraryData"
    every { patientDetailsFragment.loadCQLHelperData() } returns Unit
    patientDetailsFragment.handleCQLLibraryData(auxLibraryData)
    Assert.assertEquals(auxLibraryData, patientDetailsFragment.libraryData)
  }

  @Test
  fun handleCQLHelperDataTest() {
    val auxHelperData = "auxHelperData"
    every { patientDetailsFragment.loadCQLValueSetData() } returns Unit
    patientDetailsFragment.handleCQLHelperData("auxHelperData")
    Assert.assertEquals(auxHelperData, patientDetailsFragment.helperData)
  }

  @Test
  fun handleCQLValueSetDataTest() {
    val auxValueSetData = "auxValueSetData"
    every { patientDetailsFragment.loadCQLPatientData() } returns Unit
    patientDetailsFragment.handleCQLValueSetData(auxValueSetData)
    Assert.assertEquals(auxValueSetData, patientDetailsFragment.valueSetData)
  }

  @Test
  fun handleCQLPatientDataTest() {
    val auxPatientData = "auxPatientData"
    val parameters = "{\"parameters\":\"parameters\"}"
    every { patientDetailsFragment.libraryEvaluator.processCQLPatientBundle(any()) } returns
      auxPatientData
    every {
      patientDetailsFragment.libraryEvaluator.runCql(
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any()
      )
    } returns parameters
    patientDetailsFragment.handleCQLPatientData(auxPatientData)
    Assert.assertEquals(auxPatientData, patientDetailsFragment.testData)
    Assert.assertNotNull(patientDetailsFragment.textView_CQLResults)
  }

  @MockK lateinit var parser: IParser
  @MockK lateinit var fhirResourceDataSource: FhirResourceDataSource
  @Test
  fun loadCQLLibraryDataTest() {
    var auxCQLLibraryData = "auxCQLLibraryData"
    var libraryData = MutableLiveData<String>()
    libraryData.postValue(auxCQLLibraryData)

    coroutinesTestRule.runBlockingTest {
      coEvery {
        patientDetailsViewModel.fetchCQLLibraryData(parser, fhirResourceDataSource, any())
      } returns libraryData
    }
    patientDetailsFragment.loadCQLLibraryData()
    Assert.assertNotNull(libraryData.value)
    Assert.assertEquals(auxCQLLibraryData, libraryData.value)
  }

  @Test
  fun loadCQLHelperDataTest() {
    var auxCQLHelperData = "auxCQLHelperData"
    var helperData = MutableLiveData<String>()
    helperData.postValue(auxCQLHelperData)

    coroutinesTestRule.runBlockingTest {
      coEvery {
        patientDetailsViewModel.fetchCQLFhirHelperData(parser, fhirResourceDataSource, any())
      } returns helperData
    }
    patientDetailsFragment.loadCQLHelperData()
    Assert.assertNotNull(helperData.value)
    Assert.assertEquals(auxCQLHelperData, helperData.value)
  }

  @Test
  fun loadCQLValueSetDataTest() {
    var auxCQLValueSetData = "auxCQLValueSetData"
    var valueSetData = MutableLiveData<String>()
    valueSetData.postValue(auxCQLValueSetData)

    coroutinesTestRule.runBlockingTest {
      coEvery {
        patientDetailsViewModel.fetchCQLValueSetData(parser, fhirResourceDataSource, any())
      } returns valueSetData
    }
    patientDetailsFragment.loadCQLValueSetData()
    Assert.assertNotNull(valueSetData.value)
    Assert.assertEquals(auxCQLValueSetData, valueSetData.value)
  }

  @Test
  fun loadCQLPatientDataTest() {
    var auxCQLPatientData = "auxCQLPatientData"
    var valueSetData = MutableLiveData<String>()
    valueSetData.postValue(auxCQLPatientData)

    coroutinesTestRule.runBlockingTest {
      coEvery {
        patientDetailsViewModel.fetchCQLPatientData(parser, fhirResourceDataSource, any())
      } returns valueSetData
    }
    patientDetailsFragment.loadCQLPatientData()
    Assert.assertNotNull(valueSetData.value)
    Assert.assertEquals(auxCQLPatientData, valueSetData.value)
  }

  @Test
  fun showCQLCardTest() {
    val ANC_TEST_PATIENT_ID = "e8725b4c-6db0-4158-a24d-50a5ddf1c2ed"
    patientDetailsFragment.patientId = ANC_TEST_PATIENT_ID
    every { patientDetailsFragment.buttonCQLSetOnClickListener() } returns Unit
    patientDetailsFragment.showCQLCard()
    Assert.assertEquals(patientDetailsFragment.textView_EvaluateCQLHeader.visibility, View.VISIBLE)
    Assert.assertEquals(ANC_TEST_PATIENT_ID, patientDetailsFragment.patientId)
  }

  @Test
  fun buttonCQLSetOnClickListener() {
    every { patientDetailsFragment.loadCQLLibraryData() } returns Unit
    patientDetailsFragment.buttonCQLSetOnClickListener()
    Assert.assertEquals(true, patientDetailsFragment.button_CQLEvaluate.hasOnClickListeners())
  }
}
