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

package org.smartregister.fhircore.quest.ui.sdc.qrCode

import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import androidx.test.core.view.MotionEventBuilder
import com.google.android.fhir.datacapture.validation.NotValidated
import com.google.android.fhir.datacapture.views.QuestionnaireViewItem
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.StringType
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.hiltActivityForTestScenario
import org.smartregister.fhircore.quest.robolectric.RobolectricTest
import org.smartregister.fhircore.quest.util.QrCodeScanUtils

@HiltAndroidTest
class EditTextQrCodeItemViewHolderFactoryTest : RobolectricTest() {

  @get:Rule(order = 0) var hiltAndroidRule = HiltAndroidRule(this)

  @Before
  fun setUp() {
    hiltAndroidRule.inject()
  }

  @Test
  fun getQuestionnaireItemViewHolderDelegateShouldUpdateTextCorrectlyWhenScanQrCodeReceived() {
    mockkConstructor(QrCodeCameraDialogFragment::class)
    val sampleQrCode = "d84fbd12-4f22-423a-8645-5525504e1bcb"
    /**
     * Using style 'com.google.android.material.R.style.Theme_Material3_DayNight' to prevent
     * Robolectric [UnsupportedOperationException] error for 'attr/colorSurfaceVariant'
     * https://github.com/robolectric/robolectric/issues/4961#issuecomment-488517645
     */
    hiltActivityForTestScenario(com.google.android.material.R.style.Theme_Material3_DayNight).use {
      scenario ->
      scenario.onActivity { activity ->
        val parentView = FrameLayout(activity)
        val viewHolder = EditTextQrCodeItemViewHolderFactory { _, _ -> }.create(parentView)
        val textInputLayout =
          viewHolder.itemView.findViewById<TextInputLayout>(R.id.text_input_layout)
        Assert.assertNotNull(textInputLayout)
        val textInputEditText =
          textInputLayout.findViewById<TextInputEditText>(R.id.text_input_edit_text)
        Assert.assertNotNull(textInputEditText)
        every {
          anyConstructed<QrCodeCameraDialogFragment>()
            .show(any<FragmentManager>(), QrCodeScanUtils.QR_CODE_SCAN_UTILS_TAG)
        } answers
          {
            activity.supportFragmentManager.setFragmentResult(
              QrCodeCameraDialogFragment.RESULT_REQUEST_KEY,
              bundleOf(QrCodeCameraDialogFragment.RESULT_REQUEST_KEY to sampleQrCode),
            )
          }

        textInputEditText.dispatchTouchEvent(
          MotionEventBuilder.newBuilder().setAction(MotionEvent.ACTION_UP).build(),
        )
        Assert.assertEquals(sampleQrCode, textInputEditText.text.toString())
      }
    }
    unmockkConstructor(QrCodeCameraDialogFragment::class)
  }

  @Test
  fun getQuestionnaireItemViewHolderDelegateShouldSetCorrectAnswerText() {
    val sampleQrCode = "d84fbd12-4f22-423a-8645-5525504e1bcb"
    /**
     * Using style 'com.google.android.material.R.style.Theme_Material3_DayNight' to prevent
     * Robolectric [UnsupportedOperationException] error for 'attr/colorSurfaceVariant'
     * https://github.com/robolectric/robolectric/issues/4961#issuecomment-488517645
     */
    hiltActivityForTestScenario(com.google.android.material.R.style.Theme_Material3_DayNight).use {
      scenario ->
      scenario.onActivity { activity ->
        val parentView = FrameLayout(activity)
        val viewHolder = EditTextQrCodeItemViewHolderFactory { _, _ -> }.create(parentView)
        viewHolder.bind(
          QuestionnaireViewItem(
            questionnaireItem =
              Questionnaire.QuestionnaireItemComponent().apply {
                linkId = "sample-text"
                type = Questionnaire.QuestionnaireItemType.STRING
                addExtension(
                  Extension().apply {
                    url =
                      "https://github.com/opensrp/android-fhir/StructureDefinition/questionnaire-itemControl"
                    setValue(
                      CodeableConcept()
                        .addCoding(
                          Coding().apply {
                            system =
                              "https://github.com/opensrp/android-fhir/questionnaire-item-control"
                            code = "qr_code-widget"
                          },
                        ),
                    )
                  },
                )
              },
            questionnaireResponseItem =
              QuestionnaireResponse.QuestionnaireResponseItemComponent().apply {
                linkId = "sample-text"
                addAnswer(
                  QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
                    value = StringType(sampleQrCode)
                  },
                )
              },
            validationResult = NotValidated,
            answersChangedCallback = { _, _, _, _ -> },
          ),
        )
        val textInputEditText =
          viewHolder.itemView.findViewById<TextInputEditText>(R.id.text_input_edit_text)
        Assert.assertEquals(sampleQrCode, textInputEditText.text.toString())
      }
    }
  }

  @Test
  fun matcherCorrectlyMatchesQuestionnaireItem() {
    val questionnaireItem =
      Questionnaire.QuestionnaireItemComponent().apply {
        addExtension(
          Extension().apply {
            url =
              "https://github.com/opensrp/android-fhir/StructureDefinition/questionnaire-itemControl"
            setValue(
              CodeableConcept()
                .addCoding(
                  Coding().apply {
                    system = "https://github.com/opensrp/android-fhir/questionnaire-item-control"
                    code = "qr_code-widget"
                  },
                ),
            )
          },
        )
      }
    TODO("Fix and move to the correct class")

    //    Assert.assertTrue(EditTextQrCodeItemViewHolderFactory.matcher(questionnaireItem))
  }
}
