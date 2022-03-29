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

package org.smartregister.fhircore.anc.ui.family.removememberfamily

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.android.fhir.FhirEngine
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.hl7.fhir.r4.model.HumanName
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.StringType
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.anc.coroutine.CoroutineTestRule
import org.smartregister.fhircore.anc.data.family.FamilyDetailRepository
import org.smartregister.fhircore.anc.data.patient.DeletionReason
import org.smartregister.fhircore.anc.data.patient.PatientRepository
import org.smartregister.fhircore.anc.robolectric.RobolectricTest
import org.smartregister.fhircore.anc.ui.family.removefamilymember.RemoveFamilyMemberQuestionnaireViewModel
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.cql.LibraryEvaluator
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper

@ExperimentalCoroutinesApi
@HiltAndroidTest
class RemoveFamilyMemberQuestionnaireViewModelTest : RobolectricTest() {

  @BindValue val sharedPreferencesHelper: SharedPreferencesHelper = mockk(relaxed = true)

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) var instantTaskExecutorRule = InstantTaskExecutorRule()

  @get:Rule(order = 2) var coroutineRule = CoroutineTestRule()

  private val fhirEngine: FhirEngine = mockk()

  private lateinit var viewModel: RemoveFamilyMemberQuestionnaireViewModel

  private lateinit var defaultRepo: DefaultRepository
  private var patientRepository: PatientRepository = mockk(relaxed = true)
  private var familyDetailRepository: FamilyDetailRepository = mockk(relaxed = true)
  private val libraryEvaluator: LibraryEvaluator = mockk()

  @Before
  fun setUp() {
    hiltRule.inject()

    defaultRepo = spyk(DefaultRepository(fhirEngine, DefaultDispatcherProvider()))
    val configurationRegistry = mockk<ConfigurationRegistry>()
    every { configurationRegistry.appId } returns "appId"
    viewModel =
      spyk(
        RemoveFamilyMemberQuestionnaireViewModel(
          fhirEngine = fhirEngine,
          defaultRepository = defaultRepo,
          configurationRegistry = configurationRegistry,
          transformSupportServices = mockk(),
          patientRepository = patientRepository,
          familyDetailRepository = familyDetailRepository,
          dispatcherProvider = defaultRepo.dispatcherProvider,
          sharedPreferencesHelper = sharedPreferencesHelper,
          libraryEvaluator = libraryEvaluator
        )
      )

    ReflectionHelpers.setField(viewModel, "defaultRepository", defaultRepo)
    viewModel.reasonRemove = "Other"
    coEvery { familyDetailRepository.fetchDemographics("111") } returns getPatient()
  }

  private fun getPatient(): Patient {
    val patient =
      Patient().apply {
        id = "111"
        name =
          listOf(
            HumanName().apply {
              given = listOf(StringType("john"))
              family = "doe"
            }
          )
      }
    return patient
  }

  @Test
  fun testSaveQuestionnaireResponseShouldAddIdAndAuthoredWhenQuestionnaireResponseDoesNotHaveId() {

    val questionnaire = Questionnaire().apply { id = "qId" }
    val questionnaireResponse = QuestionnaireResponse().apply { subject = Reference("12345") }
    coEvery { defaultRepo.addOrUpdate(any()) } returns Unit

    Assert.assertNull(questionnaireResponse.id)
    Assert.assertNull(questionnaireResponse.authored)

    runBlocking { viewModel.saveQuestionnaireResponse(questionnaire, questionnaireResponse) }

    Assert.assertNotNull(questionnaireResponse.id)
    Assert.assertNotNull(questionnaireResponse.authored)
  }

  @Test
  fun testChangeFamilyHeadShouldCallRepositoryMethod() {
    coEvery { familyDetailRepository.familyRepository.changeFamilyHead(any(), any()) } answers {}
    viewModel.changeFamilyHead("111", "222")
    coVerify { familyDetailRepository.familyRepository.changeFamilyHead(any(), any()) }
  }

  fun testHandlePatientSubjectShouldReturnSetCorrectReference() {
    val questionnaire = Questionnaire().apply { addSubjectType("Patient") }
    val questionnaireResponse = QuestionnaireResponse()

    Assert.assertFalse(questionnaireResponse.hasSubject())

    viewModel.handleQuestionnaireResponseSubject("123", questionnaire, questionnaireResponse)

    Assert.assertEquals("Patient/123", questionnaireResponse.subject.reference)
  }

  @Test
  fun testDeletePatientWithOtherReasonShouldCallPatientRepository() = runBlockingTest {
    coEvery { viewModel.reasonRemove } returns "Other"
    coEvery { patientRepository.deletePatient(any(), any()) } answers {}
    viewModel.deleteFamilyMember("111", DeletionReason.OTHER)
    coVerify { patientRepository.deletePatient(any(), any()) }
  }

  @Test
  fun testDeletePatientWithMovedAwayReasonShouldCallPatientRepository() = runBlockingTest {
    coEvery { viewModel.reasonRemove } returns "Moved away"
    coEvery { patientRepository.deletePatient(any(), any()) } answers {}
    viewModel.deleteFamilyMember("111", DeletionReason.MOVED_AWAY)
    coVerify { patientRepository.deletePatient(any(), any()) }
  }

  @Test
  fun testDeletePatientWithDiedReasonShouldCallPatientRepository() = runBlockingTest {
    coEvery { viewModel.reasonRemove } returns "Died"
    coEvery { patientRepository.deletePatient(any(), any()) } answers {}
    viewModel.deleteFamilyMember("111", DeletionReason.DIED)
    coVerify { patientRepository.deletePatient(any(), any()) }
  }

  @Test
  fun testGetReasonRemove() {
    coEvery { viewModel.reasonRemove } returns "Moved away"
    val deletionReason = viewModel.getReasonRemove()
    Assert.assertEquals(deletionReason, DeletionReason.MOVED_AWAY)
    coEvery { viewModel.reasonRemove } returns "Other"
    val deletionReasonTwo = viewModel.getReasonRemove()
    Assert.assertEquals(deletionReasonTwo, DeletionReason.OTHER)
    coEvery { viewModel.reasonRemove } returns "Died"
    val deletionReasonThree = viewModel.getReasonRemove()
    Assert.assertEquals(deletionReasonThree, DeletionReason.DIED)
    coEvery { viewModel.reasonRemove } returns ""
    val deletionReasonEmpty = viewModel.getReasonRemove()
    Assert.assertEquals(deletionReasonEmpty, DeletionReason.OTHER)
  }
}
