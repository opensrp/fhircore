package org.smartregister.fhircore.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.search.search
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.Patient
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.CoroutineTestRule

@ExperimentalCoroutinesApi
class PatientDetailsViewModelTest {

  private lateinit var fhirEngine: FhirEngine

  private lateinit var patientDetailsViewModel: PatientDetailsViewModel

  private val patientId = "samplePatientId"

  @get:Rule var coroutinesTestRule = CoroutineTestRule()

  @get:Rule var instantTaskExecutorRule = InstantTaskExecutorRule()

  @Before
  fun setUp() {
    fhirEngine = mockk(relaxed = true)
    patientDetailsViewModel =
      spyk(
        PatientDetailsViewModel(
          dispatcher = coroutinesTestRule.testDispatcherProvider,
          fhirEngine = fhirEngine,
          patientId = patientId
        )
      )
  }

  @Test
  fun fetchDemographics() {
    coroutinesTestRule.runBlockingTest {
      val patient = spyk<Patient>().apply { idElement.id = patientId }
      coEvery { fhirEngine.load(Patient::class.java, patientId) } returns patient
      patientDetailsViewModel.fetchDemographics()
      Assert.assertNotNull(patientDetailsViewModel.patientDemographics.value)
      Assert.assertNotNull(patientDetailsViewModel.patientDemographics.value!!.idElement)
      Assert.assertEquals(
        patientDetailsViewModel.patientDemographics.value?.idElement!!.id,
        patientId
      )
    }
  }

  @Test
  fun fetchImmunizations() {
    coroutinesTestRule.runBlockingTest {
      val immunizations = listOf(mockk<Immunization>())

      coEvery<List<Immunization>> {
        fhirEngine.search { filter(Immunization.PATIENT) { value = "Patient/$patientId" } }
      } returns immunizations

      patientDetailsViewModel.fetchImmunizations()
      Assert.assertNotNull(patientDetailsViewModel.patientImmunizations.value)
      Assert.assertEquals(patientDetailsViewModel.patientImmunizations.value?.size, 1)
    }
  }
}
