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

import com.google.android.fhir.datacapture.validation.NotValidated
import com.google.android.fhir.datacapture.views.QuestionnaireViewItem
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.hl7.fhir.r4.model.BooleanType
import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

@HiltAndroidTest
class EditTextQrCodeViewHolderFactoryTest : RobolectricTest() {

  @get:Rule(order = 0) var hiltAndroidRule = HiltAndroidRule(this)

  @Before
  fun setUp() {
    hiltAndroidRule.inject()
  }

  @Test
  fun matcherCorrectlyMatchesQuestionnaireItem() {
    val questionnaireItem =
      Questionnaire.QuestionnaireItemComponent().apply {
        addExtension(
          Extension("https://github.com/opensrp/android-fhir/StructureDefinition/qr-code-widget"),
        )
      }

    Assert.assertTrue(EditTextQrCodeViewHolderFactory.matcher(questionnaireItem))
  }

  @Test
  fun qrCodeWidgetQuestionnaireItemWithExtensionSetOnceReadOnlyIsCorrectlyMarkedAsSetOnceReadOnly() {
    val questionnaireItem =
      Questionnaire.QuestionnaireItemComponent().apply {
        addExtension(
          Extension("https://github.com/opensrp/android-fhir/StructureDefinition/qr-code-widget")
            .apply { addExtension("set-only-readonly", BooleanType(true)) },
        )
      }
    val questionnaireViewItem =
      QuestionnaireViewItem(
        questionnaireItem,
        QuestionnaireResponse.QuestionnaireResponseItemComponent(),
        validationResult = NotValidated,
        answersChangedCallback = { _, _, _, _ -> },
      )

    Assert.assertTrue(questionnaireViewItem.isSetOnceReadOnly)
  }

  @Test
  fun qrCodeWidgetQuestionnaireItemWithoutExtensionSetOnceReadOnlyIsCorrectlyMarkedAsNotSetOnceReadOnly() {
    val questionnaireItem =
      Questionnaire.QuestionnaireItemComponent().apply {
        addExtension(
          Extension("https://github.com/opensrp/android-fhir/StructureDefinition/qr-code-widget"),
        )
      }
    val questionnaireViewItem =
      QuestionnaireViewItem(
        questionnaireItem,
        QuestionnaireResponse.QuestionnaireResponseItemComponent(),
        validationResult = NotValidated,
        answersChangedCallback = { _, _, _, _ -> },
      )

    Assert.assertFalse(questionnaireViewItem.isSetOnceReadOnly)
  }
}
