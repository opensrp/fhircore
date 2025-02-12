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

import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.fhir.datacapture.validation.NotValidated
import com.google.android.fhir.datacapture.views.QuestionnaireViewItem
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coVerify
import io.mockk.spyk
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
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

@HiltAndroidTest
class EditTextQrCodeViewHolderFactoryTest : RobolectricTest() {

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

  private val sampleQrCode1 = "de49a176-c85b-4ced-8651-e27909d6b3f4"
  private val sampleQrCode2 = "0f2cfbea-e2c9-47b1-941d-39184306eb74"

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

  @Test
  fun shouldHideAddQrCodeButtonWhenQuestionnaireItemIsReadOnly() {
    val viewHolder = EditTextQrCodeViewHolderFactory.create(parentView)
    val questionnaireViewItem =
      QuestionnaireViewItem(
        questionnaireItem =
          Questionnaire.QuestionnaireItemComponent().apply {
            linkId = "linkId-a"
            readOnly = true
            addExtension(
              Extension(
                "https://github.com/opensrp/android-fhir/StructureDefinition/qr-code-widget",
              ),
            )
          },
        questionnaireResponseItem = QuestionnaireResponse.QuestionnaireResponseItemComponent(),
        validationResult = NotValidated,
        answersChangedCallback = { _, _, _, _ -> },
      )
    viewHolder.bind(questionnaireViewItem)
    Assert.assertEquals(
      View.GONE,
      viewHolder.itemView.findViewById<View?>(R.id.add_qr_code).visibility,
    )
  }

  @Test
  fun shouldHideAddQrCodeButtonWhenQuestionnaireItemRepeatsFalse() {
    val viewHolder = EditTextQrCodeViewHolderFactory.create(parentView)
    val questionnaireViewItem =
      QuestionnaireViewItem(
        questionnaireItem =
          Questionnaire.QuestionnaireItemComponent().apply {
            linkId = "linkId-a"
            repeats = false
            addExtension(
              Extension(
                "https://github.com/opensrp/android-fhir/StructureDefinition/qr-code-widget",
              ),
            )
          },
        questionnaireResponseItem = QuestionnaireResponse.QuestionnaireResponseItemComponent(),
        validationResult = NotValidated,
        answersChangedCallback = { _, _, _, _ -> },
      )
    viewHolder.bind(questionnaireViewItem)
    Assert.assertEquals(
      View.GONE,
      viewHolder.itemView.findViewById<View?>(R.id.add_qr_code).visibility,
    )
  }

  @Test
  fun shouldShowAddQrCodeButtonWhenQuestionnaireItemRepeatsTrue() {
    val viewHolder = EditTextQrCodeViewHolderFactory.create(parentView)
    val questionnaireViewItem =
      QuestionnaireViewItem(
        questionnaireItem =
          Questionnaire.QuestionnaireItemComponent().apply {
            linkId = "linkId-a"
            repeats = true
            addExtension(
              Extension(
                "https://github.com/opensrp/android-fhir/StructureDefinition/qr-code-widget",
              ),
            )
          },
        questionnaireResponseItem = QuestionnaireResponse.QuestionnaireResponseItemComponent(),
        validationResult = NotValidated,
        answersChangedCallback = { _, _, _, _ -> },
      )
    viewHolder.bind(questionnaireViewItem)
    Assert.assertEquals(
      View.VISIBLE,
      viewHolder.itemView.findViewById<View?>(R.id.add_qr_code).visibility,
    )
  }

  @Test
  fun shouldHideAddQrCodeButtonWhenQuestionnaireItemRepeatsAndIsReadOnly() {
    val viewHolder = EditTextQrCodeViewHolderFactory.create(parentView)
    val questionnaireViewItem =
      QuestionnaireViewItem(
        questionnaireItem =
          Questionnaire.QuestionnaireItemComponent().apply {
            linkId = "linkId-a"
            repeats = true
            readOnly = true
            addExtension(
              Extension(
                "https://github.com/opensrp/android-fhir/StructureDefinition/qr-code-widget",
              ),
            )
          },
        questionnaireResponseItem = QuestionnaireResponse.QuestionnaireResponseItemComponent(),
        validationResult = NotValidated,
        answersChangedCallback = { _, _, _, _ -> },
      )
    viewHolder.bind(questionnaireViewItem)
    Assert.assertEquals(
      View.GONE,
      viewHolder.itemView.findViewById<View?>(R.id.add_qr_code).visibility,
    )
  }

  @Test
  fun shouldHaveASingleAnswerItemWhenDoesNotRepeat() {
    val viewHolder = EditTextQrCodeViewHolderFactory.create(parentView)
    val questionnaireViewItem =
      QuestionnaireViewItem(
        questionnaireItem =
          Questionnaire.QuestionnaireItemComponent().apply {
            linkId = "linkId-a"
            repeats = false
            addExtension(
              Extension(
                "https://github.com/opensrp/android-fhir/StructureDefinition/qr-code-widget",
              ),
            )
          },
        questionnaireResponseItem =
          QuestionnaireResponse.QuestionnaireResponseItemComponent().apply {
            linkId = "linkId-a"
            addAnswer(
              QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
                value = StringType(sampleQrCode1)
              },
            )
          },
        validationResult = NotValidated,
        answersChangedCallback = { _, _, _, _ -> },
      )
    viewHolder.bind(questionnaireViewItem)
    val qrCodeAdapter =
      viewHolder.itemView.findViewById<RecyclerView>(R.id.recycler_view_qr_codes).adapter
        as? QrCodeViewItemAdapter
    Assert.assertNotNull(qrCodeAdapter)
    qrCodeAdapter!!
    Assert.assertEquals(1, qrCodeAdapter.currentList.size)
    Assert.assertEquals(
      sampleQrCode1,
      qrCodeAdapter.currentList.single().answers.single().valueStringType.value,
    )
  }

  @Test
  fun shouldAddAnEmptyItemWhenDoesNotRepeatAndNoAnswer() {
    val viewHolder = EditTextQrCodeViewHolderFactory.create(parentView)
    val questionnaireViewItem =
      QuestionnaireViewItem(
        questionnaireItem =
          Questionnaire.QuestionnaireItemComponent().apply {
            linkId = "linkId-a"
            repeats = false
            addExtension(
              Extension(
                "https://github.com/opensrp/android-fhir/StructureDefinition/qr-code-widget",
              ),
            )
          },
        questionnaireResponseItem = QuestionnaireResponse.QuestionnaireResponseItemComponent(),
        validationResult = NotValidated,
        answersChangedCallback = { _, _, _, _ -> },
      )
    viewHolder.bind(questionnaireViewItem)
    val qrCodeAdapter =
      viewHolder.itemView.findViewById<RecyclerView>(R.id.recycler_view_qr_codes).adapter
        as? QrCodeViewItemAdapter
    Assert.assertNotNull(qrCodeAdapter)
    qrCodeAdapter!!
    Assert.assertEquals(1, qrCodeAdapter.currentList.size)
  }

  @Test
  fun shouldAddMultipleAnswerItemsWhenRepeatsTrue() {
    val viewHolder = EditTextQrCodeViewHolderFactory.create(parentView)
    val questionnaireViewItem =
      QuestionnaireViewItem(
        questionnaireItem =
          Questionnaire.QuestionnaireItemComponent().apply {
            linkId = "linkId-a"
            repeats = true
            addExtension(
              Extension(
                "https://github.com/opensrp/android-fhir/StructureDefinition/qr-code-widget",
              ),
            )
          },
        questionnaireResponseItem =
          QuestionnaireResponse.QuestionnaireResponseItemComponent().apply {
            linkId = "linkId-a"
            addAnswer(
              QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
                value = StringType(sampleQrCode1)
              },
            )
            addAnswer(
              QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
                value = StringType(sampleQrCode2)
              },
            )
          },
        validationResult = NotValidated,
        answersChangedCallback = { _, _, _, _ -> },
      )
    viewHolder.bind(questionnaireViewItem)
    val qrCodeAdapter =
      viewHolder.itemView.findViewById<RecyclerView>(R.id.recycler_view_qr_codes).adapter
        as QrCodeViewItemAdapter
    qrCodeAdapter.currentList.apply {
      Assert.assertEquals(2, size)
      Assert.assertEquals(sampleQrCode1, first().answers.single().valueStringType.value)
      Assert.assertEquals(sampleQrCode2, last().answers.single().valueStringType.value)
    }
  }

  @Test
  fun addQrCodeShouldBeClickableWhenRepeatsTrue() {
    val viewHolder = EditTextQrCodeViewHolderFactory.create(parentView)
    val questionnaireViewItem =
      QuestionnaireViewItem(
        questionnaireItem =
          Questionnaire.QuestionnaireItemComponent().apply {
            linkId = "linkId-a"
            repeats = true
            addExtension(
              Extension(
                "https://github.com/opensrp/android-fhir/StructureDefinition/qr-code-widget",
              ),
            )
          },
        questionnaireResponseItem =
          QuestionnaireResponse.QuestionnaireResponseItemComponent().apply {
            linkId = "linkId-a"
            addAnswer(
              QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
                value = StringType(sampleQrCode1)
              },
            )
            addAnswer(
              QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
                value = StringType(sampleQrCode2)
              },
            )
          },
        validationResult = NotValidated,
        answersChangedCallback = { _, _, _, _ -> },
      )
    viewHolder.bind(questionnaireViewItem)
    Assert.assertTrue(viewHolder.itemView.findViewById<Button>(R.id.add_qr_code).callOnClick())
  }

  @Test
  fun `areItemsTheSame() should return true if the questionnaire item and the questionnaire response item are the same`() {
    val questionnaireItem =
      Questionnaire.QuestionnaireItemComponent().apply {
        linkId = "linkId-a"
        repeats = true
        addExtension(
          Extension(
            "https://github.com/opensrp/android-fhir/StructureDefinition/qr-code-widget",
          ),
        )
      }
    val questionnaireViewItem1 =
      QuestionnaireViewItem(
        questionnaireItem = questionnaireItem,
        questionnaireResponseItem =
          QuestionnaireResponse.QuestionnaireResponseItemComponent().apply {
            addAnswer(
              QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
                value = StringType(sampleQrCode1)
              },
            )
          },
        validationResult = NotValidated,
        answersChangedCallback = { _, _, _, _ -> },
      )

    val questionnaireViewItem2 =
      QuestionnaireViewItem(
        questionnaireItem = questionnaireItem,
        questionnaireResponseItem =
          QuestionnaireResponse.QuestionnaireResponseItemComponent().apply {
            linkId = "linkId-a"
            addAnswer(
              QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
                value = StringType(sampleQrCode1)
              },
            )
          },
        validationResult = NotValidated,
        answersChangedCallback = { _, _, _, _ -> },
      )

    Assert.assertTrue(
      QR_CODE_DIFF_ITEMCallBack.areItemsTheSame(questionnaireViewItem1, questionnaireViewItem2),
    )
  }

  @Test
  fun `areItemsTheSame() should return false if the questionnaire response item are the different`() {
    val questionnaireItem =
      Questionnaire.QuestionnaireItemComponent().apply {
        linkId = "linkId-a"
        repeats = true
        addExtension(
          Extension(
            "https://github.com/opensrp/android-fhir/StructureDefinition/qr-code-widget",
          ),
        )
      }
    val questionnaireViewItem1 =
      QuestionnaireViewItem(
        questionnaireItem = questionnaireItem,
        questionnaireResponseItem =
          QuestionnaireResponse.QuestionnaireResponseItemComponent().apply {
            linkId = "linkId-a"
            addAnswer(
              QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
                value = StringType(sampleQrCode1)
              },
            )
          },
        validationResult = NotValidated,
        answersChangedCallback = { _, _, _, _ -> },
      )

    val questionnaireViewItem2 =
      QuestionnaireViewItem(
        questionnaireItem = questionnaireItem,
        questionnaireResponseItem =
          QuestionnaireResponse.QuestionnaireResponseItemComponent().apply {
            linkId = "linkId-a"
            addAnswer(
              QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
                value = StringType(sampleQrCode2)
              },
            )
          },
        validationResult = NotValidated,
        answersChangedCallback = { _, _, _, _ -> },
      )

    Assert.assertFalse(
      QR_CODE_DIFF_ITEMCallBack.areItemsTheSame(questionnaireViewItem1, questionnaireViewItem2),
    )
  }

  @Test
  fun `areContentsTheSame() should return true if the questionnaire item and the questionnaire response item are the same`() {
    val questionnaireItem =
      Questionnaire.QuestionnaireItemComponent().apply {
        linkId = "linkId-a"
        repeats = true
        addExtension(
          Extension(
            "https://github.com/opensrp/android-fhir/StructureDefinition/qr-code-widget",
          ),
        )
      }

    val questionnaireViewItem1 =
      QuestionnaireViewItem(
        questionnaireItem = questionnaireItem,
        questionnaireResponseItem =
          QuestionnaireResponse.QuestionnaireResponseItemComponent().apply {
            linkId = "linkId-a"
            addAnswer(
              QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
                value = StringType(sampleQrCode1)
              },
            )
          },
        validationResult = NotValidated,
        answersChangedCallback = { _, _, _, _ -> },
      )

    val questionnaireViewItem2 =
      QuestionnaireViewItem(
        questionnaireItem = questionnaireItem,
        questionnaireResponseItem =
          QuestionnaireResponse.QuestionnaireResponseItemComponent().apply {
            linkId = "linkId-a"
            addAnswer(
              QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
                value = StringType(sampleQrCode1)
              },
            )
          },
        validationResult = NotValidated,
        answersChangedCallback = { _, _, _, _ -> },
      )

    Assert.assertTrue(
      QR_CODE_DIFF_ITEMCallBack.areContentsTheSame(questionnaireViewItem1, questionnaireViewItem2),
    )
  }

  @Test
  fun `areContentsTheSame() should return false if the questionnaire response item are the different`() {
    val questionnaireItem =
      Questionnaire.QuestionnaireItemComponent().apply {
        linkId = "linkId-a"
        repeats = true
        addExtension(
          Extension(
            "https://github.com/opensrp/android-fhir/StructureDefinition/qr-code-widget",
          ),
        )
      }
    val questionnaireViewItem1 =
      QuestionnaireViewItem(
        questionnaireItem = questionnaireItem,
        questionnaireResponseItem =
          QuestionnaireResponse.QuestionnaireResponseItemComponent().apply {
            linkId = "linkId-a"
            addAnswer(
              QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
                value = StringType(sampleQrCode1)
              },
            )
          },
        validationResult = NotValidated,
        answersChangedCallback = { _, _, _, _ -> },
      )

    val questionnaireViewItem2 =
      QuestionnaireViewItem(
        questionnaireItem = questionnaireItem,
        questionnaireResponseItem =
          QuestionnaireResponse.QuestionnaireResponseItemComponent().apply {
            linkId = "linkId-a"
            addAnswer(
              QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
                value = StringType(sampleQrCode2)
              },
            )
          },
        validationResult = NotValidated,
        answersChangedCallback = { _, _, _, _ -> },
      )

    Assert.assertFalse(
      QR_CODE_DIFF_ITEMCallBack.areContentsTheSame(questionnaireViewItem1, questionnaireViewItem2),
    )
  }

  @Test
  fun `onQrCodeChanged adds answer when previous answer is empty and repeats true`() = runTest {
    val viewHolder = EditTextQrCodeViewHolderFactory.create(parentView)
    val questionnaireViewItem =
      spyk(
        QuestionnaireViewItem(
          questionnaireItem =
            Questionnaire.QuestionnaireItemComponent().apply {
              linkId = "linkId-a"
              repeats = true
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
    viewHolder.bind(questionnaireViewItem)
    val qrCodeAdapter =
      viewHolder.itemView.findViewById<RecyclerView>(R.id.recycler_view_qr_codes).adapter
        as QrCodeViewItemAdapter
    val newAnswer =
      QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
        value = StringType(sampleQrCode1)
      }
    qrCodeAdapter.qrCodeAnswerChangeListener.onQrCodeChanged(null, newAnswer)
    coVerify { questionnaireViewItem.addAnswer(newAnswer) }
  }

  @Test
  fun `onQrCodeChanged sets answer when previous answer is empty and repeats false`() = runTest {
    val viewHolder = EditTextQrCodeViewHolderFactory.create(parentView)
    val questionnaireViewItem =
      spyk(
        QuestionnaireViewItem(
          questionnaireItem =
            Questionnaire.QuestionnaireItemComponent().apply {
              linkId = "linkId-a"
              repeats = false
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
    viewHolder.bind(questionnaireViewItem)
    val qrCodeAdapter =
      viewHolder.itemView.findViewById<RecyclerView>(R.id.recycler_view_qr_codes).adapter
        as QrCodeViewItemAdapter
    val newAnswer =
      QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
        value = StringType(sampleQrCode1)
      }
    qrCodeAdapter.qrCodeAnswerChangeListener.onQrCodeChanged(null, newAnswer)
    coVerify { questionnaireViewItem.setAnswer(newAnswer) }
  }

  @Test
  fun `onQrCodeChanged clears answer when new answer is empty and item does not repeat`() =
    runTest {
      val viewHolder = EditTextQrCodeViewHolderFactory.create(parentView)
      val previousQrAnswer =
        QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
          value = StringType(sampleQrCode1)
        }
      val questionnaireViewItem =
        spyk(
          QuestionnaireViewItem(
            questionnaireItem =
              Questionnaire.QuestionnaireItemComponent().apply {
                linkId = "linkId-a"
                repeats = false
                addExtension(
                  Extension(
                    "https://github.com/opensrp/android-fhir/StructureDefinition/qr-code-widget",
                  ),
                )
              },
            questionnaireResponseItem =
              QuestionnaireResponse.QuestionnaireResponseItemComponent().apply {
                linkId = "linkId-a"
                addAnswer(previousQrAnswer)
              },
            validationResult = NotValidated,
            answersChangedCallback = { _, _, _, _ -> },
          ),
        )
      viewHolder.bind(questionnaireViewItem)
      val qrCodeAdapter =
        viewHolder.itemView.findViewById<RecyclerView>(R.id.recycler_view_qr_codes).adapter
          as QrCodeViewItemAdapter
      qrCodeAdapter.qrCodeAnswerChangeListener.onQrCodeChanged(previousQrAnswer, null)
      coVerify { questionnaireViewItem.clearAnswer() }
    }

  @Test
  fun `onQrCodeChanged removes answer when new answer is empty and item repeats`() = runTest {
    val viewHolder = EditTextQrCodeViewHolderFactory.create(parentView)
    val previousQrAnswer =
      QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
        value = StringType(sampleQrCode1)
      }
    val questionnaireViewItem =
      spyk(
        QuestionnaireViewItem(
          questionnaireItem =
            Questionnaire.QuestionnaireItemComponent().apply {
              linkId = "linkId-a"
              repeats = true
              addExtension(
                Extension(
                  "https://github.com/opensrp/android-fhir/StructureDefinition/qr-code-widget",
                ),
              )
            },
          questionnaireResponseItem =
            QuestionnaireResponse.QuestionnaireResponseItemComponent().apply {
              linkId = "linkId-a"
              addAnswer(previousQrAnswer)
            },
          validationResult = NotValidated,
          answersChangedCallback = { _, _, _, _ -> },
        ),
      )
    viewHolder.bind(questionnaireViewItem)
    val qrCodeAdapter =
      viewHolder.itemView.findViewById<RecyclerView>(R.id.recycler_view_qr_codes).adapter
        as QrCodeViewItemAdapter
    qrCodeAdapter.qrCodeAnswerChangeListener.onQrCodeChanged(previousQrAnswer, null)
    coVerify { questionnaireViewItem.removeAnswer(previousQrAnswer) }
  }

  @Test
  fun `onQrCodeChanged swaps answer when previous and new answer are not empty`() = runTest {
    val viewHolder = EditTextQrCodeViewHolderFactory.create(parentView)
    val previousQrAnswer =
      QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
        value = StringType(sampleQrCode1)
      }
    val newQrAnswer =
      QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
        value = StringType(sampleQrCode2)
      }
    val questionnaireViewItem =
      spyk(
        QuestionnaireViewItem(
          questionnaireItem =
            Questionnaire.QuestionnaireItemComponent().apply {
              linkId = "linkId-a"
              repeats = false
              addExtension(
                Extension(
                  "https://github.com/opensrp/android-fhir/StructureDefinition/qr-code-widget",
                ),
              )
            },
          questionnaireResponseItem =
            QuestionnaireResponse.QuestionnaireResponseItemComponent().apply {
              linkId = "linkId-a"
              addAnswer(previousQrAnswer)
            },
          validationResult = NotValidated,
          answersChangedCallback = { _, _, _, _ -> },
        ),
      )
    viewHolder.bind(questionnaireViewItem)
    val qrCodeAdapter =
      viewHolder.itemView.findViewById<RecyclerView>(R.id.recycler_view_qr_codes).adapter
        as QrCodeViewItemAdapter
    qrCodeAdapter.qrCodeAnswerChangeListener.onQrCodeChanged(previousQrAnswer, newQrAnswer)
    coVerify {
      questionnaireViewItem.setAnswer(
        withArg { Assert.assertEquals(sampleQrCode2, it.valueStringType.value) },
      )
    }
  }
}
