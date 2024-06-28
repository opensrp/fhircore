package org.smartregister.fhircore.engine.pdf

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.testing.launchFragmentInContainer
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.Binary
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class PdfLauncherFragmentTest {

  @get:Rule
  var hiltRule = HiltAndroidRule(this)

  private lateinit var pdfLauncherViewModel: PdfLauncherViewModel
  private lateinit var pdfGenerator: PdfGenerator
  private lateinit var questionnaireConfig: QuestionnaireConfig

  @Before
  fun setUp() {
    hiltRule.inject()

    pdfLauncherViewModel = mock(PdfLauncherViewModel::class.java)
    pdfGenerator = mock(PdfGenerator::class.java)
    questionnaireConfig = QuestionnaireConfig(
      id = "questionnaireId",
      resourceIdentifier = "subjectId",
      resourceType = ResourceType.Patient,
      htmlBinaryId = "htmlBinaryId",
      htmlTitle = "htmlTitle"
    )
  }

  @Test
  fun testFragmentLaunchesAndFetchesDataAndGeneratesPdf() = runTest {
    `when`(pdfLauncherViewModel.retrieveQuestionnaireResponse(anyString(), anyString(), any(ResourceType::class.java)))
      .thenReturn(QuestionnaireResponse())

    `when`(pdfLauncherViewModel.retrieveBinary(anyString()))
      .thenReturn(Binary().apply { content = "htmlContent".toByteArray() })

    val bundle = bundleOf("questionnaire_config" to questionnaireConfig)

    val scenario = launchFragmentInContainer<PdfLauncherFragment>(bundle)

    scenario.onFragment { fragment ->
      runTest {
        verify(pdfLauncherViewModel).retrieveQuestionnaireResponse(
          eq("questionnaireId"),
          eq("subjectId"),
          eq(ResourceType.Patient)
        )

        verify(pdfLauncherViewModel).retrieveBinary(eq("htmlBinaryId"))

        verify(pdfGenerator).generatePdfWithHtml(
          eq("htmlContent"),
          eq("htmlTitle"),
          any(),
        )
      }
    }
  }
}
