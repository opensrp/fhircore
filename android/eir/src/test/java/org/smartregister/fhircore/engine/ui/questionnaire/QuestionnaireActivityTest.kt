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

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.widget.Button
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.FhirEngine
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.test.runBlockingTest
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.RelatedPerson
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.RealObject
import org.robolectric.shadows.ShadowBuild
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.eir.EirApplication
import org.smartregister.fhircore.eir.R
import org.smartregister.fhircore.eir.activity.ActivityRobolectricTest
import org.smartregister.fhircore.eir.coroutine.CoroutineTestRule
import org.smartregister.fhircore.eir.shadow.EirApplicationShadow
import org.smartregister.fhircore.eir.shadow.TestUtils
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider

@Config(shadows = [EirApplicationShadow::class, QuestionnaireActivityTest.CustomBuilder::class])
class QuestionnaireActivityTest : ActivityRobolectricTest() {
  private lateinit var context: EirApplication
  private lateinit var questionnaireActivity: QuestionnaireActivity
  private lateinit var questionnaireViewModel: QuestionnaireViewModel
  private lateinit var intent: Intent

  @get:Rule var instantTaskExecutorRule = InstantTaskExecutorRule()
  @get:Rule var coroutinesTestRule = CoroutineTestRule()

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    questionnaireViewModel = spyk(QuestionnaireViewModel(context))
    val samplePatientRegisterQuestionnaire =
      TestUtils.loadQuestionnaire(context, REGISTER_QUESTIONNAIRE_ID)

    val fhirEngine: FhirEngine = mockk()

    coEvery { fhirEngine.load(Questionnaire::class.java, "1903") } returns
      samplePatientRegisterQuestionnaire
    coEvery { fhirEngine.load(Patient::class.java, TEST_PATIENT_1_ID) } returns TEST_PATIENT_1
    coEvery { fhirEngine.save(any()) } answers {}
    coEvery {
      hint(Patient::class)
      fhirEngine.search<Patient>(any())
    } returns listOf(Patient())
    coEvery {
      hint(RelatedPerson::class)
      fhirEngine.search<RelatedPerson>(any())
    } returns listOf(RelatedPerson())

    ReflectionHelpers.setField(context, "fhirEngine\$delegate", lazy { fhirEngine })

    mockkObject(DefaultDispatcherProvider)
    every { DefaultDispatcherProvider.io() } returns
      coroutinesTestRule.testDispatcherProvider.main()

    intent =
      Intent().apply {
        putExtra(QuestionnaireActivity.QUESTIONNAIRE_TITLE_KEY, "Patient registration")
        putExtra(QuestionnaireActivity.QUESTIONNAIRE_ARG_FORM, "patient-registration")
        putExtra(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY, TEST_PATIENT_1_ID)
      }

    val controller = Robolectric.buildActivity(QuestionnaireActivity::class.java, intent)
    questionnaireActivity = controller.create().resume().get()
    questionnaireActivity.questionnaireViewModel = questionnaireViewModel
    questionnaireActivity.supportFragmentManager.executePendingTransactions()
  }

  @After
  fun cleanup() {
    unmockkObject(DefaultDispatcherProvider)
  }

  @Test
  fun testActivityShouldNotNull() {
    Assert.assertNotNull(questionnaireActivity)
  }

  @Test
  fun testRequiredIntentShouldInsertValues() {
    val result = QuestionnaireActivity.requiredIntentArgs("1234", "my-form", "quest-id")
    Assert.assertEquals("my-form", result.getString(QuestionnaireActivity.QUESTIONNAIRE_ARG_FORM))
    Assert.assertEquals(
      "1234",
      result.getString(QuestionnaireActivity.QUESTIONNAIRE_ARG_PATIENT_KEY)
    )
    Assert.assertEquals("quest-id", result.getString(QuestionnaireActivity.QUESTIONNAIRE_ARG_ID))
  }

  @Test
  fun testGetQuestionnaireConfigShouldLoadRightConfig() = runBlockingTest {
    val result = questionnaireActivity.getQuestionnaireConfig("patient-registration")
    Assert.assertEquals("patient-registration", result.form)
    Assert.assertEquals("Add Patient", result.title)
    Assert.assertEquals("1903", result.identifier)
  }

  @Test
  fun `save-button click should call savedExtractedResources()`() {

    val viewModel = mockk<QuestionnaireViewModel>()
    every { viewModel.extractionProgress } returns MutableLiveData(false)
    every { viewModel.extractAndSaveResources(any(), any(), any(), any()) } answers {}
    questionnaireActivity.questionnaireViewModel = viewModel

    questionnaireActivity.findViewById<Button>(R.id.btn_save_client_info).performClick()

    verify(exactly = 1) { viewModel.extractAndSaveResources(any(), any(), any(), any()) }
  }

  override fun getActivity(): Activity {
    return questionnaireActivity
  }

  @Implements(AlertDialog.Builder::class)
  class CustomBuilder : ShadowBuild() {

    @RealObject private var builder: AlertDialog.Builder? = null

    @Implementation
    fun setView(restId: Int): AlertDialog.Builder? {
      return builder
    }
  }

  companion object {
    const val REGISTER_QUESTIONNAIRE_ID = "sample_patient_registration.json"
    const val TEST_PATIENT_1_ID = "test_patient_1_id"
    val TEST_PATIENT_1 = TestUtils.TEST_PATIENT_1
  }
}
