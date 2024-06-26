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

package org.smartregister.fhircore.engine.util.extension

import org.hl7.fhir.r4.model.IntegerType
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.StringType
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class QuestionnaireResponseExtensionTest {
  private lateinit var questionnaireResponse: QuestionnaireResponse

  @Before
  fun setup() {
    questionnaireResponse =
      QuestionnaireResponse().apply {
        addItem().apply {
          linkId = "1"
          text = "Text 1"
          addItem().apply {
            linkId = "2"
            text = "Text 2"
          }
        }
      }
  }

  @Test
  fun testClearText() {
    questionnaireResponse.clearText()
    val item1 = questionnaireResponse.itemFirstRep
    Assert.assertNull(item1.text)
    val item2 = item1.itemFirstRep
    Assert.assertNull(item2.text)
  }

  @Test
  fun testQuestionnaireResponsePackingRepeatedGroups() {
    val unPackedRepeatingGroupQuestionnaireResponse =
      QuestionnaireResponse().apply {
        addItem(
          QuestionnaireResponse.QuestionnaireResponseItemComponent(StringType("page-1")).apply {
            addItem(
              QuestionnaireResponse.QuestionnaireResponseItemComponent(
                  StringType("repeating-group"),
                )
                .apply {
                  addItem(
                    QuestionnaireResponse.QuestionnaireResponseItemComponent(StringType("bp"))
                      .apply {
                        addAnswer(
                          QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
                            value =
                              IntegerType(
                                124,
                              )
                          },
                        )
                      },
                  )
                },
            )

            addItem(
              QuestionnaireResponse.QuestionnaireResponseItemComponent(
                  StringType("repeating-group"),
                )
                .apply {
                  addItem(
                    QuestionnaireResponse.QuestionnaireResponseItemComponent(StringType("bp"))
                      .apply {
                        addAnswer(
                          QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
                            value =
                              IntegerType(
                                104,
                              )
                          },
                        )
                      },
                  )
                },
            )

            addItem(
              QuestionnaireResponse.QuestionnaireResponseItemComponent(
                  StringType("repeating-group"),
                )
                .apply {
                  addItem(
                    QuestionnaireResponse.QuestionnaireResponseItemComponent(StringType("bp"))
                      .apply {
                        addAnswer(
                          QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
                            value =
                              IntegerType(
                                138,
                              )
                          },
                        )
                      },
                  )
                },
            )
          },
        )
      }
    Assert.assertEquals(3, unPackedRepeatingGroupQuestionnaireResponse.itemFirstRep.item.size)
    val packedRepeatingGroupsQuestionnaireResponse =
      unPackedRepeatingGroupQuestionnaireResponse.copy().apply { this.packRepeatedGroups() }
    Assert.assertEquals(1, packedRepeatingGroupsQuestionnaireResponse.itemFirstRep.item.size)
    Assert.assertEquals(
      3,
      packedRepeatingGroupsQuestionnaireResponse.itemFirstRep.itemFirstRep.answer.size,
    )
  }
}
