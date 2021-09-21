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
    every { patientDetailsFragment.processCQLPatientBundle(any()) } returns auxPatientData
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

  @Test
  fun processCQLPatientBundleTest() {
    val auxPatientData =
      "{\"resourceType\":\"Bundle\",\"id\":\"68e9aa5a-1afc-4655-ab18-8ec63752221d\",\"meta\":{\"lastUpdated\":\"2021-09-17T14:06:32.994+00:00\"},\"type\":\"searchset\",\"total\":10,\"link\":[{\"relation\":\"self\",\"url\":\"http://fhir.labs.smartregister.org/fhir/Patient/e8725b4c-6db0-4158-a24d-50a5ddf1c2ed/\$everything?_format=json\"}],\"entry\":[{\"fullUrl\":\"http://fhir.labs.smartregister.org/fhir/Patient/e8725b4c-6db0-4158-a24d-50a5ddf1c2ed\",\"resource\":{\"resourceType\":\"Patient\",\"id\":\"e8725b4c-6db0-4158-a24d-50a5ddf1c2ed\",\"meta\":{\"versionId\":\"2\",\"lastUpdated\":\"2021-09-17T12:58:08.297+00:00\",\"source\":\"#fffb4c67568a71c4\",\"profile\":[\"http://hl7.org/fhir/StructureDefinition/Patient\",\"http://fhir.org/guides/who/anc-cds/StructureDefinition/ancpatient\"],\"tag\":[{\"system\":\"https://www.snomed.org\",\"code\":\"35359004\",\"display\":\"Family\"},{\"system\":\"https://www.snomed.org\",\"code\":\"77386006\",\"display\":\"Pregnant\"}]},\"text\":{\"status\":\"generated\",\"div\":\"<div xmlns=\\\"http://www.w3.org/1999/xhtml\\\"><div class=\\\"hapiHeaderText\\\">Patient <b>MOM </b></div><table class=\\\"hapiPropertyTable\\\"><tbody><tr><td>Address</td><td><span>Nairobi </span></td></tr><tr><td>Date of birth</td><td><span>17 September 1976</span></td></tr></tbody></table></div>\"},\"extension\":[{\"url\":\"http://hl7.org/fhir/StructureDefinition/flag-detail\",\"valueString\":\"Pregnant\"},{\"url\":\"http://hl7.org/fhir/StructureDefinition/flag-detail\",\"valueString\":\"Family\"}],\"identifier\":[{\"use\":\"official\",\"value\":\"100011\"}],\"active\":true,\"name\":[{\"family\":\"Mom\",\"given\":[\"Patient\"]}],\"telecom\":[{\"system\":\"phone\",\"value\":\"121212121212\"}],\"gender\":\"female\",\"birthDate\":\"1976-09-17\",\"address\":[{\"city\":\"Nairobi\",\"district\":\"Rahim Yar Khan\"}]},\"search\":{\"mode\":\"match\"}},{\"fullUrl\":\"http://fhir.labs.smartregister.org/fhir/Observation/2018\",\"resource\":{\"resourceType\":\"Observation\",\"id\":\"2018\",\"meta\":{\"versionId\":\"1\",\"lastUpdated\":\"2021-09-17T13:09:51.003+00:00\",\"source\":\"#bab1ad33429d23c9\",\"profile\":[\"http://fhir.org/guides/who/anc-cds/StructureDefinition/hbobservation\"]},\"text\":{\"status\":\"generated\",\"div\":\"<div xmlns=\\\"http://www.w3.org/1999/xhtml\\\"><p><b>Generated Narrative with Details</b></p><p><b>id</b>: mom-hb</p><p><b>meta</b>: </p><p><b>status</b>: final</p><p><b>category</b>: exam <span style=\\\"background: LightGoldenRodYellow\\\">(Details : http://hl7.org/fhir/observation-category code 'exam' = 'exam)</span></p><p><b>code</b>: Haemoglobin measured from haemoglobinometer (g/dl) <span style=\\\"background: LightGoldenRodYellow\\\">(Details : {http://openmrs.org/concepts code '165395AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA' = 'Hb', given as 'Haemoglobin measured from haemoglobinometer (g/dl)'})</span></p><p><b>subject</b>: <a href=\\\"Examples-FirstContact-patient-mom.html\\\">Eve Everywoman. Generated Summary: id: mom; 1000 (OFFICIAL); active; Patient Mom (OFFICIAL); ph: 555-555-2003(MOBILE); gender: female; birthDate: 1993-05-31</a></p><p><b>encounter</b>: <a href=\\\"Examples-FirstContact-encounter-mom-first-contact.html\\\">Generated Summary: id: mom-first-contact; status: in-progress; <span title=\\\"{http://terminology.hl7.org/CodeSystem/v3-ActCode AMB}\\\">ambulatory</span>; <span title=\\\"Codes: {http://example.org/CodeSystem/encounter-type anc-contact}\\\">Antenatal care contact</span>; period: May 24, 2019, 12:00:00 AM --&gt; (ongoing)</a></p><p><b>effective</b>: May 24, 2019, 3:30:00 PM --&gt; May 24, 2019, 3:30:00 PM</p><p><b>issued</b>: May 24, 2019, 3:30:00 PM</p><p><b>performer</b>: <a href=\\\"Examples-FirstContact-practitioner-midwife.html\\\">Mabel Midwife. Generated Summary: id: midwife; active; Mabel Midwife ; gender: female</a></p><p><b>value</b>: 7.2 g/dl<span style=\\\"background: LightGoldenRodYellow\\\"> (Details: UCUM code g/dL = 'g/dL')</span></p><p><b>interpretation</b>: Low <span style=\\\"background: LightGoldenRodYellow\\\">(Details : {http://terminology.hl7.org/CodeSystem/v3-ObservationInterpretation code 'L' = 'Low', given as 'Low'})</span></p><h3>ReferenceRanges</h3><table class=\\\"grid\\\"><tr><td>-</td><td><b>Low</b></td><td><b>High</b></td></tr><tr><td>*</td><td>7.5 g/dl<span style=\\\"background: LightGoldenRodYellow\\\"> (Details: UCUM code g/dL = 'g/dL')</span></td><td>10 g/dl<span style=\\\"background: LightGoldenRodYellow\\\"> (Details: UCUM code g/dL = 'g/dL')</span></td></tr></table></div>\"},\"status\":\"final\",\"category\":[{\"coding\":[{\"system\":\"http://hl7.org/fhir/observation-category\",\"code\":\"exam\"}]}],\"code\":{\"coding\":[{\"system\":\"http://openmrs.org/concepts\",\"code\":\"165395AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\",\"display\":\"Haemoglobin measured from haemoglobinometer (g/dl)\"}]},\"subject\":{\"reference\":\"Patient/e8725b4c-6db0-4158-a24d-50a5ddf1c2ed\",\"display\":\"Patient Mom\"},\"encounter\":{\"reference\":\"Encounter/2015\"},\"effectivePeriod\":{\"start\":\"2020-10-11T15:30:00Z\",\"end\":\"2020-10-11T15:30:00Z\"},\"issued\":\"2020-10-11T15:30:00Z\",\"performer\":[{\"reference\":\"Practitioner/1747\",\"display\":\"Mabel Midwife\"}],\"valueQuantity\":{\"value\":7.2,\"unit\":\"g/dl\",\"system\":\"http://unitsofmeasure.org\",\"code\":\"g/dL\"},\"interpretation\":[{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/v3-ObservationInterpretation\",\"code\":\"L\",\"display\":\"Low\"}]}],\"referenceRange\":[{\"low\":{\"value\":7.5,\"unit\":\"g/dl\",\"system\":\"http://unitsofmeasure.org\",\"code\":\"g/dL\"},\"high\":{\"value\":10,\"unit\":\"g/dl\",\"system\":\"http://unitsofmeasure.org\",\"code\":\"g/dL\"}}]},\"search\":{\"mode\":\"match\"}},{\"fullUrl\":\"http://fhir.labs.smartregister.org/fhir/Observation/2017\",\"resource\":{\"resourceType\":\"Observation\",\"id\":\"2017\",\"meta\":{\"versionId\":\"1\",\"lastUpdated\":\"2021-09-17T13:08:17.539+00:00\",\"source\":\"#672d70cfde351aee\"},\"status\":\"final\",\"code\":{\"coding\":[{\"system\":\"http://loinc.org\",\"code\":\"11779-6\",\"display\":\"Delivery date Estimated from last menstrual period\"}]},\"subject\":{\"reference\":\"Patient/e8725b4c-6db0-4158-a24d-50a5ddf1c2ed\"},\"valueDateTime\":\"2021-04-18\",\"_valueDateTime\":{\"extension\":[{\"url\":\"http://hl7.org/fhir/StructureDefinition/cqf-expression\",\"valueExpression\":{\"language\":\"text/cql\",\"expression\":\"Today() + 27 weeks\"}}]}},\"search\":{\"mode\":\"match\"}},{\"fullUrl\":\"http://fhir.labs.smartregister.org/fhir/Observation/2016\",\"resource\":{\"resourceType\":\"Observation\",\"id\":\"2016\",\"meta\":{\"versionId\":\"1\",\"lastUpdated\":\"2021-09-17T13:04:50.213+00:00\",\"source\":\"#501146b66df96133\"},\"status\":\"final\",\"code\":{\"coding\":[{\"system\":\"http://loinc.org\",\"code\":\"82810-3\",\"display\":\"Pregnancy status\"}]},\"subject\":{\"reference\":\"Patient/e8725b4c-6db0-4158-a24d-50a5ddf1c2ed\"},\"valueCodeableConcept\":{\"coding\":[{\"system\":\"http://loinc.org\",\"code\":\"LA15173-0\",\"display\":\"Pregnant\"}]}},\"search\":{\"mode\":\"match\"}},{\"fullUrl\":\"http://fhir.labs.smartregister.org/fhir/Patient/a815aaee-d869-4427-ba93-5a61c74adb10\",\"resource\":{\"resourceType\":\"Patient\",\"id\":\"a815aaee-d869-4427-ba93-5a61c74adb10\",\"meta\":{\"versionId\":\"2\",\"lastUpdated\":\"2021-09-17T12:59:17.293+00:00\",\"source\":\"#690a618c054cd2ed\",\"profile\":[\"http://hl7.org/fhir/StructureDefinition/Patient\"]},\"text\":{\"status\":\"generated\",\"div\":\"<div xmlns=\\\"http://www.w3.org/1999/xhtml\\\"><div class=\\\"hapiHeaderText\\\">Patient three <b>MEMBER </b></div><table class=\\\"hapiPropertyTable\\\"><tbody><tr><td>Address</td><td><span>Nairobi </span></td></tr><tr><td>Date of birth</td><td><span>05 September 2021</span></td></tr></tbody></table></div>\"},\"identifier\":[{\"use\":\"official\",\"value\":\"100012\"}],\"active\":true,\"name\":[{\"family\":\"Member\",\"given\":[\"Patient three\"]}],\"telecom\":[{\"system\":\"phone\",\"value\":\"21212121212\"}],\"gender\":\"female\",\"birthDate\":\"2021-09-05\",\"address\":[{\"city\":\"Nairobi\",\"district\":\"Rahim Yar Khan\"}],\"link\":[{\"other\":{\"reference\":\"Patient/e8725b4c-6db0-4158-a24d-50a5ddf1c2ed\"},\"type\":\"refer\"}]},\"search\":{\"mode\":\"match\"}},{\"fullUrl\":\"http://fhir.labs.smartregister.org/fhir/Patient/cdf7aed4-821c-484a-b12a-2a0c3a907807\",\"resource\":{\"resourceType\":\"Patient\",\"id\":\"cdf7aed4-821c-484a-b12a-2a0c3a907807\",\"meta\":{\"versionId\":\"2\",\"lastUpdated\":\"2021-09-17T13:00:43.381+00:00\",\"source\":\"#39da07506e20a427\",\"profile\":[\"http://hl7.org/fhir/StructureDefinition/Patient\",\"http://fhir.org/guides/who/anc-cds/StructureDefinition/ancpatient\"],\"tag\":[{\"system\":\"https://www.snomed.org\",\"code\":\"77386006\",\"display\":\"Pregnant\"}]},\"text\":{\"status\":\"generated\",\"div\":\"<div xmlns=\\\"http://www.w3.org/1999/xhtml\\\"><div class=\\\"hapiHeaderText\\\">Patient two <b>MOM TWO </b></div><table class=\\\"hapiPropertyTable\\\"><tbody><tr><td>Address</td><td><span>Nairobi </span></td></tr><tr><td>Date of birth</td><td><span>17 September 1985</span></td></tr></tbody></table></div>\"},\"extension\":[{\"url\":\"http://hl7.org/fhir/StructureDefinition/flag-detail\",\"valueString\":\"Pregnant\"}],\"identifier\":[{\"use\":\"official\",\"value\":\"100013\"}],\"active\":true,\"name\":[{\"family\":\"Mom two\",\"given\":[\"Patient two\"]}],\"telecom\":[{\"system\":\"phone\",\"value\":\"21212121212\"}],\"gender\":\"female\",\"birthDate\":\"1985-09-17\",\"address\":[{\"city\":\"Nairobi\",\"district\":\"Rahim Yar Khan\"}],\"link\":[{\"other\":{\"reference\":\"Patient/e8725b4c-6db0-4158-a24d-50a5ddf1c2ed\"},\"type\":\"refer\"}]},\"search\":{\"mode\":\"match\"}},{\"fullUrl\":\"http://fhir.labs.smartregister.org/fhir/Goal/d1853316-17fd-4ebb-96cb-8120a38bdf9e\",\"resource\":{\"resourceType\":\"Goal\",\"id\":\"d1853316-17fd-4ebb-96cb-8120a38bdf9e\",\"meta\":{\"versionId\":\"1\",\"lastUpdated\":\"2021-09-17T12:51:04.693+00:00\",\"source\":\"#ba5f8082a49ee86b\"},\"lifecycleStatus\":\"active\",\"subject\":{\"reference\":\"Patient/e8725b4c-6db0-4158-a24d-50a5ddf1c2ed\"}},\"search\":{\"mode\":\"match\"}},{\"fullUrl\":\"http://fhir.labs.smartregister.org/fhir/Condition/23390fe8-a48d-42a6-98bd-eadbca0aa33f\",\"resource\":{\"resourceType\":\"Condition\",\"id\":\"23390fe8-a48d-42a6-98bd-eadbca0aa33f\",\"meta\":{\"versionId\":\"1\",\"lastUpdated\":\"2021-09-17T12:51:03.642+00:00\",\"source\":\"#e7d882277be61c18\"},\"clinicalStatus\":{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/condition-clinical\",\"code\":\"active\"}]},\"verificationStatus\":{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/condition-ver-status\",\"code\":\"confirmed\"}]},\"category\":[{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/condition-category\",\"code\":\"problem-list-item\",\"display\":\"Problem List Item\"}]}],\"code\":{\"coding\":[{\"system\":\"http://example.org/tbd\",\"code\":\"TBD\",\"display\":\"Pregnancy\"}],\"text\":\"Pregnancy\"},\"subject\":{\"reference\":\"Patient/e8725b4c-6db0-4158-a24d-50a5ddf1c2ed\"},\"onsetDateTime\":\"2021-07-06\"},\"search\":{\"mode\":\"match\"}},{\"fullUrl\":\"http://fhir.labs.smartregister.org/fhir/Practitioner/1747\",\"resource\":{\"resourceType\":\"Practitioner\",\"id\":\"1747\",\"meta\":{\"versionId\":\"1\",\"lastUpdated\":\"2021-09-06T05:44:24.513+00:00\",\"source\":\"#36856d49828faa6a\"}},\"search\":{\"mode\":\"match\"}},{\"fullUrl\":\"http://fhir.labs.smartregister.org/fhir/Encounter/2015\",\"resource\":{\"resourceType\":\"Encounter\",\"id\":\"2015\",\"meta\":{\"versionId\":\"1\",\"lastUpdated\":\"2021-09-17T13:03:38.816+00:00\",\"source\":\"#d844fa2fe47c0963\"}},\"search\":{\"mode\":\"match\"}}]}"
    var cqlPatientBundle = patientDetailsFragment.processCQLPatientBundle(auxPatientData)
    Assert.assertNotNull(cqlPatientBundle)
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
        patientDetailsViewModel.fetchCQLLibraryData(parser, fhirResourceDataSource)
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
        patientDetailsViewModel.fetchCQLFhirHelperData(parser, fhirResourceDataSource)
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
        patientDetailsViewModel.fetchCQLValueSetData(parser, fhirResourceDataSource)
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
