/*
 * Copyright 2021-2024 Ona Systems, Inc
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

package org.smartregister.fhircore.quest.ui.pdf

import android.os.Bundle
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Binary
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.util.extension.encodeJson
import org.smartregister.fhircore.quest.app.fakes.HiltTestActivity
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

@HiltAndroidTest
class PdfLauncherFragmentTest : RobolectricTest() {

  @get:Rule val hiltRule = HiltAndroidRule(this)

  @BindValue val pdfLauncherViewModel: PdfLauncherViewModel = mockk(relaxed = true)

  private val pdfGenerator: PdfGenerator = mockk(relaxed = true)

  @Before
  fun setUp() {
    hiltRule.inject()
  }

  @Test
  fun testPdfGeneration() = runBlocking {
    val questionnaireResponse = QuestionnaireResponse()
    val htmlBinary = Binary().apply { content = "mock content".toByteArray() }

    coEvery { pdfLauncherViewModel.retrieveQuestionnaireResponse(any(), any(), any()) } returns
      questionnaireResponse
    coEvery { pdfLauncherViewModel.retrieveBinary(any()) } returns htmlBinary

    val questionnaireConfig =
      QuestionnaireConfig(
          id = "1",
          resourceIdentifier = "123",
          resourceType = ResourceType.Patient,
          htmlBinaryId = "1234",
          htmlTitle = "Title",
        )
        .encodeJson()

    val fragmentArgs =
      Bundle().apply {
        putString(PdfLauncherFragment.EXTRA_QUESTIONNAIRE_CONFIG_KEY, questionnaireConfig)
      }

    val activity = Robolectric.buildActivity(HiltTestActivity::class.java).create().resume().get()

    val fragment =
      PdfLauncherFragment().apply {
        arguments = fragmentArgs
        pdfGenerator = this@PdfLauncherFragmentTest.pdfGenerator
      }

    activity.supportFragmentManager.beginTransaction().add(fragment, null).commitNow()

    coVerify { pdfLauncherViewModel.retrieveQuestionnaireResponse(any(), any(), any()) }
    coVerify { pdfLauncherViewModel.retrieveBinary(any()) }
    verify { pdfGenerator.generatePdfWithHtml(any(), any(), any()) }
  }

  @Test
  fun testPdfGenerationWhenQuestionnaireResponseIsNull() = runBlocking {
    val questionnaireResponse: QuestionnaireResponse? = null
    val htmlBinary = Binary().apply { content = "mock content".toByteArray() }

    coEvery { pdfLauncherViewModel.retrieveQuestionnaireResponse(any(), any(), any()) } returns
      questionnaireResponse
    coEvery { pdfLauncherViewModel.retrieveBinary(any()) } returns htmlBinary

    val questionnaireConfig =
      QuestionnaireConfig(
          id = "1",
          resourceIdentifier = "123",
          resourceType = ResourceType.Patient,
          htmlBinaryId = "1234",
          htmlTitle = "Title",
        )
        .encodeJson()

    val fragmentArgs =
      Bundle().apply {
        putString(PdfLauncherFragment.EXTRA_QUESTIONNAIRE_CONFIG_KEY, questionnaireConfig)
      }

    val activity = Robolectric.buildActivity(HiltTestActivity::class.java).create().resume().get()

    val fragment =
      PdfLauncherFragment().apply {
        arguments = fragmentArgs
        pdfGenerator = this@PdfLauncherFragmentTest.pdfGenerator
      }

    activity.supportFragmentManager.beginTransaction().add(fragment, null).commitNow()

    coVerify { pdfLauncherViewModel.retrieveQuestionnaireResponse(any(), any(), any()) }
    coVerify { pdfLauncherViewModel.retrieveBinary(any()) }
    verify(inverse = true) { pdfGenerator.generatePdfWithHtml(any(), any(), any()) }
  }

  @Test
  fun testPdfGenerationWhenHtmlBinaryIsNull() = runBlocking {
    val questionnaireResponse = QuestionnaireResponse()
    val htmlBinary: Binary? = null

    coEvery { pdfLauncherViewModel.retrieveQuestionnaireResponse(any(), any(), any()) } returns
      questionnaireResponse
    coEvery { pdfLauncherViewModel.retrieveBinary(any()) } returns htmlBinary

    val questionnaireConfig =
      QuestionnaireConfig(
          id = "1",
          resourceIdentifier = "123",
          resourceType = ResourceType.Patient,
          htmlBinaryId = "1234",
          htmlTitle = "Title",
        )
        .encodeJson()

    val fragmentArgs =
      Bundle().apply {
        putString(PdfLauncherFragment.EXTRA_QUESTIONNAIRE_CONFIG_KEY, questionnaireConfig)
      }

    val activity = Robolectric.buildActivity(HiltTestActivity::class.java).create().resume().get()

    val fragment =
      PdfLauncherFragment().apply {
        arguments = fragmentArgs
        pdfGenerator = this@PdfLauncherFragmentTest.pdfGenerator
      }

    activity.supportFragmentManager.beginTransaction().add(fragment, null).commitNow()

    coVerify { pdfLauncherViewModel.retrieveQuestionnaireResponse(any(), any(), any()) }
    coVerify { pdfLauncherViewModel.retrieveBinary(any()) }
    verify(inverse = true) { pdfGenerator.generatePdfWithHtml(any(), any(), any()) }
  }

  @Test
  fun testPdfGenerationWhenQuestionnaireResponseAndHtmlBinaryIsNull() = runBlocking {
    val questionnaireResponse: QuestionnaireResponse? = null
    val htmlBinary: Binary? = null

    coEvery { pdfLauncherViewModel.retrieveQuestionnaireResponse(any(), any(), any()) } returns
      questionnaireResponse
    coEvery { pdfLauncherViewModel.retrieveBinary(any()) } returns htmlBinary

    val questionnaireConfig =
      QuestionnaireConfig(
          id = "1",
          resourceIdentifier = "123",
          resourceType = ResourceType.Patient,
          htmlBinaryId = "1234",
          htmlTitle = "Title",
        )
        .encodeJson()

    val fragmentArgs =
      Bundle().apply {
        putString(PdfLauncherFragment.EXTRA_QUESTIONNAIRE_CONFIG_KEY, questionnaireConfig)
      }

    val activity = Robolectric.buildActivity(HiltTestActivity::class.java).create().resume().get()

    val fragment =
      PdfLauncherFragment().apply {
        arguments = fragmentArgs
        pdfGenerator = this@PdfLauncherFragmentTest.pdfGenerator
      }

    activity.supportFragmentManager.beginTransaction().add(fragment, null).commitNow()

    coVerify { pdfLauncherViewModel.retrieveQuestionnaireResponse(any(), any(), any()) }
    coVerify { pdfLauncherViewModel.retrieveBinary(any()) }
    verify(inverse = true) { pdfGenerator.generatePdfWithHtml(any(), any(), any()) }
  }
}
