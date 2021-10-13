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

package org.smartregister.fhircore.engine.ui.questionnaire

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.logicalId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.rule.CoroutineTestRule

class QuestionnaireViewModelTest : RobolectricTest() {

  private lateinit var fhirEngine: FhirEngine
  private lateinit var questionnaireViewModel: QuestionnaireViewModel
  private lateinit var context: Application
  private lateinit var defaultRepo: DefaultRepository

  @get:Rule var instantTaskExecutorRule = InstantTaskExecutorRule()
  @get:Rule var coroutineRule = CoroutineTestRule()

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()

    fhirEngine = mockk()
    coEvery { fhirEngine.save(any()) } answers {}
    coEvery { fhirEngine.update(any()) } answers {}

    defaultRepo = spyk(DefaultRepository(fhirEngine))
    coEvery { defaultRepo.save(any()) } returns Unit
    coEvery { defaultRepo.addOrUpdate(any()) } returns Unit

    questionnaireViewModel = spyk(QuestionnaireViewModel(context))
    ReflectionHelpers.setField(questionnaireViewModel, "defaultRepository", defaultRepo)
  }

  @Test
  fun testLoadQuestionnaireShouldCallFhirEngine() {
    coEvery { fhirEngine.load(Questionnaire::class.java, "12345") } returns
      Questionnaire().apply { id = "12345" }
    val result = runBlocking { questionnaireViewModel.loadQuestionnaire("12345") }
    Assert.assertEquals("12345", result!!.logicalId)
  }

  @Test
  fun testExtractAndSaveResourcesWithResourceIdShouldSaveQuestionnaireResponse() {
    val questionnaireResponseSlot = slot<QuestionnaireResponse>()
    val questionnaire =
      Questionnaire().apply {
        addUseContext().apply {
          code = Coding().apply { code = "focus" }
          value = CodeableConcept().apply { addCoding().apply { code = "1234567" } }
        }
      }

    questionnaireViewModel.extractAndSaveResources(
      "12345",
      context,
      questionnaire,
      QuestionnaireResponse()
    )

    coVerify { defaultRepo.save(capture(questionnaireResponseSlot)) }

    Assert.assertEquals(
      "12345",
      questionnaireResponseSlot.captured.subject.reference.replace("Patient/", "")
    )
    Assert.assertEquals("1234567", questionnaireResponseSlot.captured.meta.tagFirstRep.code)
  }
}
