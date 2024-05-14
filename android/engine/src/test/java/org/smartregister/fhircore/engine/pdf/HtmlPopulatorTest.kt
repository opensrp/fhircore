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

package org.smartregister.fhircore.engine.pdf

import java.util.Calendar
import java.util.Date
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Meta
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent
import org.hl7.fhir.r4.model.StringType
import org.junit.Assert
import org.junit.Test

class HtmlPopulatorTest {

  @Test
  fun populateHtmlShouldShowUiElements() {
    val html = "@is-not-empty('link-a')<p>Text</p>@is-not-empty"
    val questionnaireResponse =
      QuestionnaireResponse().apply {
        addItem().apply {
          linkId = "link-a"
          answer = buildList {
            add(
              QuestionnaireResponseItemAnswerComponent().apply {
                value = Coding("system 1", "code 1", "display 1")
              },
            )
          }
        }
      }
    val htmlPopulator = HtmlPopulator(questionnaireResponse)
    val populatedHtml = htmlPopulator.populateHtml(html)
    Assert.assertEquals("<p>Text</p>", populatedHtml)
  }

  @Test
  fun populateHtmlShouldHideUiElement() {
    val html = "@is-not-empty('link-a')<p>Text</p>@is-not-empty"
    val questionnaireResponse =
      QuestionnaireResponse().apply { addItem().apply { linkId = "link-a" } }
    val htmlPopulator = HtmlPopulator(questionnaireResponse)
    val populatedHtml = htmlPopulator.populateHtml(html)
    Assert.assertTrue(populatedHtml.isEmpty())
  }

  @Test
  fun populateHtmlShouldPopulateAnswerAsList() {
    val html = "<ul>@answer-as-list('link-a')</ul>"
    val questionnaireResponse =
      QuestionnaireResponse().apply {
        addItem().apply {
          linkId = "link-a"
          answer = buildList {
            add(
              QuestionnaireResponseItemAnswerComponent().apply {
                value = Coding("system 1", "code 1", "display 1")
              },
            )
            add(
              QuestionnaireResponseItemAnswerComponent().apply {
                value = Coding("system 2", "code 2", "display 2")
              },
            )
          }
        }
      }
    val htmlPopulator = HtmlPopulator(questionnaireResponse)
    val populatedHtml = htmlPopulator.populateHtml(html)
    Assert.assertEquals("<ul><li>display 1</li><li>display 2</li></ul>", populatedHtml)
  }

  @Test
  fun populateHtmlShouldPopulateEmptyAnswerAsList() {
    val html = "<ul>@answer-as-list('link-a')</ul>"
    val questionnaireResponse =
      QuestionnaireResponse().apply { addItem().apply { linkId = "link-a" } }
    val htmlPopulator = HtmlPopulator(questionnaireResponse)
    val populatedHtml = htmlPopulator.populateHtml(html)
    Assert.assertEquals("<ul></ul>", populatedHtml)
  }

  @Test
  fun populateHtmlShouldPopulateAnswer() {
    val html = "<p>@answer('link-a')</p>"
    val questionnaireResponse =
      QuestionnaireResponse().apply {
        addItem().apply {
          linkId = "link-a"
          answer = buildList {
            add(
              QuestionnaireResponseItemAnswerComponent().apply { value = StringType("string 1") },
            )
          }
        }
      }
    val htmlPopulator = HtmlPopulator(questionnaireResponse)
    val populatedHtml = htmlPopulator.populateHtml(html)
    Assert.assertEquals("<p>string 1</p>", populatedHtml)
  }

  @Test
  fun populateHtmlShouldPopulateEmptyAnswer() {
    val html = "<p>@answer('link-a')</p>"
    val questionnaireResponse =
      QuestionnaireResponse().apply { addItem().apply { linkId = "link-a" } }
    val htmlPopulator = HtmlPopulator(questionnaireResponse)
    val populatedHtml = htmlPopulator.populateHtml(html)
    Assert.assertEquals("<p></p>", populatedHtml)
  }

  @Test
  fun populateHtmlShouldPopulateDateAnswer() {
    val calendar = Calendar.getInstance().apply { set(2024, Calendar.MAY, 14) }
    val specificDate: Date = calendar.time
    val html = "<p>@answer('link-a')</p>"
    val questionnaireResponse =
      QuestionnaireResponse().apply {
        addItem().apply {
          linkId = "link-a"
          answer = buildList {
            add(
              QuestionnaireResponseItemAnswerComponent().apply {
                value = DateTimeType(specificDate)
              },
            )
          }
        }
      }
    val htmlPopulator = HtmlPopulator(questionnaireResponse)
    val populatedHtml = htmlPopulator.populateHtml(html)
    Assert.assertEquals("<p>14-May-2024</p>", populatedHtml)
  }

  @Test
  fun populateHtmlShouldPopulateDateAnswerWithFormat() {
    val calendar = Calendar.getInstance().apply { set(2024, Calendar.MAY, 14) }
    val specificDate: Date = calendar.time
    val html = "<p>@answer('link-a','MMMM d, yyyy')</p>"
    val questionnaireResponse =
      QuestionnaireResponse().apply {
        addItem().apply {
          linkId = "link-a"
          answer = buildList {
            add(
              QuestionnaireResponseItemAnswerComponent().apply {
                value = DateTimeType(specificDate)
              },
            )
          }
        }
      }
    val htmlPopulator = HtmlPopulator(questionnaireResponse)
    val populatedHtml = htmlPopulator.populateHtml(html)
    Assert.assertEquals("<p>May 14, 2024</p>", populatedHtml)
  }

  @Test
  fun populateHtmlShouldPopulateSubmittedDate() {
    val calendar = Calendar.getInstance().apply { set(2024, Calendar.MAY, 14) }
    val specificDate: Date = calendar.time
    val html = "<p>@submitted-date</p>"
    val questionnaireResponse =
      QuestionnaireResponse().apply { meta = Meta().apply { lastUpdated = specificDate } }
    val htmlPopulator = HtmlPopulator(questionnaireResponse)
    val populatedHtml = htmlPopulator.populateHtml(html)
    Assert.assertEquals("<p>14-May-2024</p>", populatedHtml)
  }

  @Test
  fun populateHtmlShouldPopulateSubmittedDateWithFormat() {
    val calendar = Calendar.getInstance().apply { set(2024, Calendar.MAY, 14) }
    val specificDate: Date = calendar.time
    val html = "<p>@submitted-date('MMMM d, yyyy')</p>"
    val questionnaireResponse =
      QuestionnaireResponse().apply { meta = Meta().apply { lastUpdated = specificDate } }
    val htmlPopulator = HtmlPopulator(questionnaireResponse)
    val populatedHtml = htmlPopulator.populateHtml(html)
    Assert.assertEquals("<p>May 14, 2024</p>", populatedHtml)
  }
}
