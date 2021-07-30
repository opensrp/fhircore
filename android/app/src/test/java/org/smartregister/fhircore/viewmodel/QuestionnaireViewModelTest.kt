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

package org.smartregister.fhircore.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.google.android.fhir.logicalId
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Patient
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.robolectric.annotation.Config
import org.smartregister.fhircore.FhirApplication
import org.smartregister.fhircore.RobolectricTest
import org.smartregister.fhircore.activity.QuestionnaireActivity
import org.smartregister.fhircore.activity.QuestionnaireActivityTest
import org.smartregister.fhircore.shadow.FhirApplicationShadow

@Config(shadows = [FhirApplicationShadow::class])
class QuestionnaireViewModelTest : RobolectricTest() {

  private lateinit var viewModel: QuestionnaireViewModel

  @Before
  fun setUp() {
    val savedState = SavedStateHandle()
    savedState[QuestionnaireActivity.QUESTIONNAIRE_PATH_KEY] = "patient-registration.json"
    viewModel = QuestionnaireViewModel(FhirApplication.getContext(), savedState)
  }

  @Test
  fun testVerifyQuestionnaireSubjectType() {
    val jsonObject = JSONObject(viewModel.questionnaire)

    Assert.assertEquals("Patient", jsonObject.getJSONArray("subjectType").getString(0))
    Assert.assertNotNull(viewModel.questionnaireJson)
  }

  @Test
  fun testVerifySavedResource() {
    val sourcePatient = QuestionnaireActivityTest.TEST_PATIENT_1

    viewModel.saveResource(sourcePatient)
    val patient = runBlocking {
      FhirApplication.fhirEngine(FhirApplication.getContext())
        .load(Patient::class.java, QuestionnaireActivityTest.TEST_PATIENT_1_ID)
    }

    Assert.assertNotNull(patient)
    Assert.assertEquals(sourcePatient.logicalId, patient.logicalId)
  }
}
