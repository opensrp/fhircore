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

package org.smartregister.fhirecore.quest.ui.patient.register

import android.app.Activity
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.datacapture.mapping.ResourceMapper
import com.google.android.fhir.sync.Sync
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkObject
import java.time.OffsetDateTime
import kotlinx.coroutines.flow.flowOf
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.CodeType
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_ARG_FORM
import org.smartregister.fhircore.quest.QuestApplication
import org.smartregister.fhircore.quest.ui.patient.register.form.PatientQuestionnaireActivity
import org.smartregister.fhirecore.quest.robolectric.ActivityRobolectricTest
import org.smartregister.fhirecore.quest.shadow.FakeKeyStore
import org.smartregister.fhirecore.quest.shadow.QuestApplicationShadow

@Config(shadows = [QuestApplicationShadow::class])
internal class PatientQuestionnaireActivityTest : ActivityRobolectricTest() {
  private lateinit var application: QuestApplication
  private lateinit var questionnaireActivity: PatientQuestionnaireActivity

  @Before
  fun setUp() {
    application = ApplicationProvider.getApplicationContext()

    mockkObject(Sync)
    every { Sync.basicSyncJob(any()).stateFlow() } returns flowOf()
    every { Sync.basicSyncJob(any()).lastSyncTimestamp() } returns OffsetDateTime.now()

    val intent = Intent().apply { putExtra(QUESTIONNAIRE_ARG_FORM, "patient-registration") }

    questionnaireActivity =
      Robolectric.buildActivity(PatientQuestionnaireActivity::class.java, intent).create().get()
  }

  @After
  fun cleanup() {
    unmockkObject(Sync)
  }

  @Test
  fun testActivityShouldNotNull() {
    assertNotNull(questionnaireActivity)
  }

  @Test
  fun testHandleQuestionnaireResponseShouldCallSavePatient() {
    val repository = mockk<DefaultRepository>()
    coEvery { repository.save(any()) } just runs

    mockkObject(ResourceMapper)
    coEvery { ResourceMapper.extract(any(), any()) } returns
      Bundle().apply { this.addEntry().apply { this.resource = Patient() } }

    questionnaireActivity.repository = repository
    ReflectionHelpers.setField(
      questionnaireActivity,
      "questionnaire",
      Questionnaire().apply {
        id = "1923"
        subjectType.add(CodeType("Patient"))
      }
    )

    questionnaireActivity.handleQuestionnaireResponse(QuestionnaireResponse())

    coVerify(timeout = 2000) { repository.save(any()) }

    unmockkObject(ResourceMapper)
  }

  override fun getActivity(): Activity {
    return questionnaireActivity
  }

  companion object {
    @JvmStatic
    @BeforeClass
    fun beforeClass() {
      FakeKeyStore.setup
    }
  }
}
