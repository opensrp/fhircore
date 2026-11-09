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

import kotlin.math.abs
import org.hl7.fhir.r4.model.Attachment
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

  @Test
  fun testRemoveAttachmentDataDropsOnlyTheFileBytes() {
    val response =
      QuestionnaireResponse().apply {
        addItem().apply {
          linkId = "page"
          addItem().apply {
            linkId = "photo"
            addAnswer().apply {
              value =
                Attachment().apply {
                  contentType = "image/jpeg"
                  title = "TEST.jpg"
                  data = byteArrayOf(1, 2, 3)
                }
            }
          }
          addItem().apply {
            linkId = "has-photo"
            addAnswer().apply {
              value = StringType("yes")
              // An item nested in an answer
              addItem().apply {
                linkId = "consent"
                addAnswer().apply {
                  value =
                    Attachment().apply {
                      contentType = "application/pdf"
                      data = byteArrayOf(4, 5)
                    }
                }
              }
            }
          }
        }
      }

    response.removeAttachmentData()

    val items = response.allItems.associateBy { it.linkId }
    val photo = items.getValue("photo").answerFirstRep.valueAttachment
    Assert.assertFalse(photo.hasData())
    Assert.assertEquals("image/jpeg", photo.contentType)
    Assert.assertEquals("TEST.jpg", photo.title)
    val consent = items.getValue("consent").answerFirstRep.valueAttachment
    Assert.assertFalse(consent.hasData())
    Assert.assertEquals("application/pdf", consent.contentType)
    Assert.assertEquals("yes", items.getValue("has-photo").answerFirstRep.valueStringType.value)
  }

  @Test
  fun testReplaceAttachmentDataWithLabelLabelsEachFileAndDropsItsBytes() {
    val response =
      QuestionnaireResponse().apply {
        addItem().apply {
          linkId = "photo"
          addAnswer().apply {
            value =
              Attachment().apply {
                contentType = "image/jpeg"
                title = "TEST.jpg"
                data = byteArrayOf(1, 2, 3)
              }
          }
        }
        addItem().apply {
          linkId = "has-photo"
          addAnswer().apply {
            value = StringType("yes")
            // An item nested in an answer
            addItem().apply {
              linkId = "consent"
              addAnswer().apply {
                value =
                  Attachment().apply {
                    contentType = "application/pdf"
                    data = byteArrayOf(4, 5)
                  }
              }
            }
          }
        }
        addItem().apply {
          linkId = "linked"
          addAnswer().apply {
            value =
              Attachment().apply {
                url = "https://example.org/TEST.pdf"
                data = byteArrayOf(6)
              }
          }
        }
        addItem().apply {
          linkId = "no-bytes"
          addAnswer().apply { value = Attachment().apply { title = "TEST_empty.jpg" } }
        }
      }

    response.replaceAttachmentDataWithLabel { item, attachment ->
      "${item.linkId}: ${attachment.title} ${attachment.data.size}"
    }

    val items = response.allItems.associateBy { it.linkId }
    val photo = items.getValue("photo").answerFirstRep.valueAttachment
    Assert.assertFalse(photo.hasData())
    Assert.assertEquals("photo: TEST.jpg 3", photo.url)
    Assert.assertEquals("image/jpeg", photo.contentType)
    Assert.assertEquals("TEST.jpg", photo.title)
    val consent = items.getValue("consent").answerFirstRep.valueAttachment
    Assert.assertFalse(consent.hasData())
    Assert.assertEquals("consent: null 2", consent.url)
    val linked = items.getValue("linked").answerFirstRep.valueAttachment
    Assert.assertFalse(linked.hasData())
    Assert.assertEquals("https://example.org/TEST.pdf", linked.url)
    Assert.assertFalse(items.getValue("no-bytes").answerFirstRep.valueAttachment.hasUrl())
    Assert.assertEquals("yes", items.getValue("has-photo").answerFirstRep.valueStringType.value)
  }

  @Test
  fun testEstimatedJsonSizeMatchesTheJsonAndKeepsTheFileBytes() {
    val photo = ByteArray(30_001) { it.toByte() }
    val consent = byteArrayOf(4, 5)
    val response =
      QuestionnaireResponse().apply {
        addItem().apply {
          linkId = "photo"
          addAnswer().apply {
            value =
              Attachment().apply {
                contentType = "image/jpeg"
                title = "TEST.jpg"
                data = photo
              }
          }
        }
        addItem().apply {
          linkId = "has-photo"
          addAnswer().apply {
            value = StringType("yes")
            // An item nested in an answer
            addItem().apply {
              linkId = "consent"
              addAnswer().apply {
                value =
                  Attachment().apply {
                    contentType = "application/pdf"
                    data = consent
                  }
              }
            }
          }
        }
      }
    val items = response.allItems.associateBy { it.linkId }
    val photoElement = items.getValue("photo").answerFirstRep.valueAttachment.dataElement
    val consentElement = items.getValue("consent").answerFirstRep.valueAttachment.dataElement
    val jsonSize = response.encodeResourceToString().toByteArray(Charsets.UTF_8).size.toLong()

    val estimate = response.estimatedJsonSize()

    Assert.assertTrue("estimate $estimate, JSON $jsonSize", abs(estimate - jsonSize) <= 100)
    Assert.assertSame(
      photoElement,
      items.getValue("photo").answerFirstRep.valueAttachment.dataElement,
    )
    Assert.assertSame(
      consentElement,
      items.getValue("consent").answerFirstRep.valueAttachment.dataElement,
    )
    Assert.assertArrayEquals(photo, items.getValue("photo").answerFirstRep.valueAttachment.data)
    Assert.assertArrayEquals(consent, items.getValue("consent").answerFirstRep.valueAttachment.data)
  }
}
