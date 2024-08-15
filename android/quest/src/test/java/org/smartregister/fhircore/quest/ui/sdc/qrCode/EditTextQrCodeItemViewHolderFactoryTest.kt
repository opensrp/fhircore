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
import androidx.appcompat.app.AppCompatActivity
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
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.BooleanType
import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.StringType
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.hiltActivityForTestScenario
import org.smartregister.fhircore.quest.robolectric.RobolectricTest
import org.smartregister.fhircore.quest.ui.sdc.qrCode.scan.QRCodeScannerDialogFragment
import org.smartregister.fhircore.quest.util.QrCodeScanUtils

@HiltAndroidTest
class EditTextQrCodeItemViewHolderFactoryTest : RobolectricTest() {

  @get:Rule(order = 0) var hiltAndroidRule = HiltAndroidRule(this)

  private val parentView =
    FrameLayout(
      Robolectric.buildActivity(AppCompatActivity::class.java).create().get().apply {
        /**
         * Using style 'com.google.android.material.R.style.Theme_Material3_DayNight' to prevent
         * Robolectric [UnsupportedOperationException] error for 'attr/colorSurfaceVariant'
         * https://github.com/robolectric/robolectric/issues/4961#issuecomment-488517645
         */
        setTheme(com.google.android.material.R.style.Theme_Material3_DayNight)
      },
    )

  @Before
  fun setUp() {
    hiltAndroidRule.inject()
  }

  @Test
  fun shouldUpdateTextCorrectlyWhenScanQrCodeReceived() {
    mockkConstructor(QRCodeScannerDialogFragment::class)
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
          anyConstructed<QRCodeScannerDialogFragment>()
            .show(any<FragmentManager>(), QrCodeScanUtils.QR_CODE_SCAN_UTILS_TAG)
        } answers
          {
            activity.supportFragmentManager.setFragmentResult(
              QRCodeScannerDialogFragment.RESULT_REQUEST_KEY,
              bundleOf(QRCodeScannerDialogFragment.RESULT_REQUEST_KEY to sampleQrCode),
            )
          }

        textInputEditText.dispatchTouchEvent(
          MotionEventBuilder.newBuilder().setAction(MotionEvent.ACTION_UP).build(),
        )
        Assert.assertEquals(sampleQrCode, textInputEditText.text.toString())
      }
    }
    unmockkConstructor(QRCodeScannerDialogFragment::class)
  }

  @Test
  fun shouldSetCorrectAnswerText() {
    val sampleQrCode = "d84fbd12-4f22-423a-8645-5525504e1bcb"
    val viewHolder = EditTextQrCodeItemViewHolderFactory { _, _ -> }.create(parentView)
    viewHolder.bind(
      QuestionnaireViewItem(
        questionnaireItem =
          Questionnaire.QuestionnaireItemComponent().apply {
            linkId = "sample-text"
            type = Questionnaire.QuestionnaireItemType.STRING
            addExtension(
              Extension(
                "https://github.com/opensrp/android-fhir/StructureDefinition/qr-code-widget",
              ),
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

  @Test
  fun shouldSetInputDisabledWhenQuestionViewItemHasAnswerAndQuestionnaireItemIsReadOnly() {
    val viewHolder = EditTextQrCodeItemViewHolderFactory { _, _ -> }.create(parentView)
    viewHolder.bind(
      QuestionnaireViewItem(
        questionnaireItem =
          Questionnaire.QuestionnaireItemComponent().apply {
            linkId = "linkid-a"
            readOnly = true
            type = Questionnaire.QuestionnaireItemType.STRING
            addExtension(
              Extension(
                "https://github.com/opensrp/android-fhir/StructureDefinition/qr-code-widget",
              ),
            )
          },
        questionnaireResponseItem =
          QuestionnaireResponse.QuestionnaireResponseItemComponent().apply { linkId = "linkid-a" },
        validationResult = NotValidated,
        answersChangedCallback = { _, _, _, _ -> },
      ),
    )

    viewHolder.itemView.findViewById<TextInputEditText>(R.id.text_input_edit_text).apply {
      Assert.assertTrue(this.isEnabled)
      Assert.assertTrue(this.text.isNullOrBlank())
    }

    viewHolder.bind(
      QuestionnaireViewItem(
        questionnaireItem =
          Questionnaire.QuestionnaireItemComponent().apply {
            linkId = "linkid-a"
            readOnly = true
            type = Questionnaire.QuestionnaireItemType.STRING
            addExtension(
              Extension(
                "https://github.com/opensrp/android-fhir/StructureDefinition/qr-code-widget",
              ),
            )
          },
        questionnaireResponseItem =
          QuestionnaireResponse.QuestionnaireResponseItemComponent().apply {
            linkId = "linkid-a"
            addAnswer(
              QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
                value = StringType("d84fbd12-4f22-423a-8645-5525504e1bcb")
              },
            )
          },
        validationResult = NotValidated,
        answersChangedCallback = { _, _, _, _ -> },
      ),
    )

    viewHolder.itemView.findViewById<TextInputEditText>(R.id.text_input_edit_text).apply {
      Assert.assertFalse(this.isEnabled)
      Assert.assertEquals("d84fbd12-4f22-423a-8645-5525504e1bcb", this.text.toString())
    }
  }

  @Test
  fun shouldSetInputDisabledWhenQuestionViewItemHasAnswerAndIsSetOnceReadOnly() {
    val viewHolder = EditTextQrCodeItemViewHolderFactory { _, _ -> }.create(parentView)
    viewHolder.bind(
      QuestionnaireViewItem(
        questionnaireItem =
          Questionnaire.QuestionnaireItemComponent().apply {
            linkId = "linkid-a"
            type = Questionnaire.QuestionnaireItemType.STRING
            addExtension(
              Extension(
                  "https://github.com/opensrp/android-fhir/StructureDefinition/qr-code-widget",
                )
                .apply { addExtension("set-only-readonly", BooleanType(true)) },
            )
          },
        questionnaireResponseItem =
          QuestionnaireResponse.QuestionnaireResponseItemComponent().apply { linkId = "linkid-a" },
        validationResult = NotValidated,
        answersChangedCallback = { _, _, _, _ -> },
      ),
    )

    viewHolder.itemView.findViewById<TextInputEditText>(R.id.text_input_edit_text).apply {
      Assert.assertTrue(this.isEnabled)
      Assert.assertTrue(this.text.isNullOrBlank())
    }

    viewHolder.bind(
      QuestionnaireViewItem(
        questionnaireItem =
          Questionnaire.QuestionnaireItemComponent().apply {
            linkId = "linkid-a"
            type = Questionnaire.QuestionnaireItemType.STRING
            addExtension(
              Extension(
                  "https://github.com/opensrp/android-fhir/StructureDefinition/qr-code-widget",
                )
                .apply { addExtension("set-only-readonly", BooleanType(true)) },
            )
          },
        questionnaireResponseItem =
          QuestionnaireResponse.QuestionnaireResponseItemComponent().apply {
            linkId = "linkid-a"
            addAnswer(
              QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
                value = StringType("d84fbd12-4f22-423a-8645-5525504e1bcb")
              },
            )
          },
        validationResult = NotValidated,
        answersChangedCallback = { _, _, _, _ -> },
      ),
    )

    viewHolder.itemView.findViewById<TextInputEditText>(R.id.text_input_edit_text).apply {
      Assert.assertFalse(this.isEnabled)
      Assert.assertEquals("d84fbd12-4f22-423a-8645-5525504e1bcb", this.text.toString())
    }
  }

  @Test
  fun shouldCallOnQrCodeChangedWhenNewTextIsSet() = runTest {
    var qrCode: String? = null
    val viewHolder =
      EditTextQrCodeItemViewHolderFactory { _, qrAnswer ->
          qrCode = (qrAnswer?.value as? StringType)?.value
        }
        .create(parentView)
    viewHolder.bind(
      QuestionnaireViewItem(
        questionnaireItem =
          Questionnaire.QuestionnaireItemComponent().apply {
            linkId = "linkid-a"
            type = Questionnaire.QuestionnaireItemType.STRING
            addExtension(
              Extension(
                "https://github.com/opensrp/android-fhir/StructureDefinition/qr-code-widget",
              ),
            )
          },
        questionnaireResponseItem = QuestionnaireResponse.QuestionnaireResponseItemComponent(),
        validationResult = NotValidated,
        answersChangedCallback = { _, _, _, _ -> },
      ),
    )
    val sampleCode = "d84fbd12-4f22-423a-8645-5525504e1bcb"
    viewHolder.itemView
      .findViewById<TextInputEditText>(R.id.text_input_edit_text)
      .setText(sampleCode)

    Assert.assertNotNull(qrCode)
    Assert.assertEquals(sampleCode, qrCode)
  }
}
