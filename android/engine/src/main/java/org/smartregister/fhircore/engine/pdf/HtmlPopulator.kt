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

import java.util.regex.Matcher
import java.util.regex.Pattern
import org.hl7.fhir.r4.model.BaseDateTimeType
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.engine.util.extension.allItems
import org.smartregister.fhircore.engine.util.extension.formatDate
import org.smartregister.fhircore.engine.util.extension.makeItReadable
import org.smartregister.fhircore.engine.util.extension.valueToString

class HtmlPopulator(
  private val questionnaireResponse: QuestionnaireResponse,
) {

  private val questionnaireResponseItemMap =
    questionnaireResponse.allItems.associateBy(
      keySelector = { it.linkId },
      valueTransform = { it.answer },
    )

  fun populateHtml(rawHtml: String): String {
    val html = StringBuilder(rawHtml)
    var i = 0
    while (i < html.length) {
      when {
        html.startsWith("@is-not-empty", i) -> {
          val matcher = isNotEmptyPattern.matcher(html.substring(i))
          if (matcher.find()) {
            processIsNotEmpty(i, html, matcher)
            // After replacement, the current index will be used twice, adding an increment/using
            // forEach
            // will skip a character right after the current index.

            // For example, the '<' symbol will be skipped, not really an issue here.
            // But it will be a problem if it's an '@', which means a tag will not be detected.
            // @is-not-empty('link')Text@is-not-empty('link')<br> -> No problem
            // @is-not-empty('link')Text@is-not-empty('link')@answer('link-b') -> The @answer tag
            // will not be replaced, hence it will stay as a tag

            // See below process:

            // Case 1
            // 1. Given: <p>@is-not-empty('link')Text@is-not-empty('link')</p>
            // 2. The first '@' will be detected, and it's index is 3
            // 3. If link answer exist, both tags will be replaced by 'Text'
            // 4. Since the index is not incremented, it stays 3 in the next iteration, but the
            // value of the index has changed from '@' to 'T'
            // 5. Index 3 with 'T' as it's value is detected
            // 6. The flow goes to the ELSE, then the index is incremented, from 3 to 4, with value
            // respectively 'T' to 'e'
            // 7. On the next iteration, index 4 will go to the ELSE, and the process continues

            // Case 2
            // 1. Given: <p>@is-not-empty('link')Text@is-not-empty('link')</p>
            // 2. The first '@' will be detected, and it's index is 3
            // 3. If link answer not exist, both tags will be replaced by ''
            // 4. Since the index is not incremented, it stays 3 in the next iteration, but the
            // value of the index has changed from '@' to '<'
            // 5. Index 3 with '<' as it's value is detected
            // 6. The flow goes to the ELSE, then the index is incremented, from 3 to 4, with value
            // respectively '<' to '/'
            // 7. On the next iteration, index 4 will go to the ELSE, and the process continues

            // Note 1: We are not simply matching the '@' symbol, but instead matching the tag
            // itself e.g. @is-not-empty
            // Note 2: An '@' symbol inside an email will not be affected because of Note 1 e.g.
            // sample@gmail.com
          } else {
            i++
            // Prevents an infinite loop where regex cannot find any match:
            // 1. There is only 1 tag, where it should be a pair
            // 2. There is a pair of tag, but each has different link id
            // 3. Other regex matching issues, but underline is, the @is-not-empty tag do exist
          }
        }
        html.startsWith("@answer-as-list", i) -> {
          val matcher = answerAsListPattern.matcher(html.substring(i))
          if (matcher.find()) processAnswerAsList(i, html, matcher) else i++
        }
        html.startsWith("@answer", i) -> {
          val matcher = answerPattern.matcher(html.substring(i))
          if (matcher.find()) processAnswer(i, html, matcher) else i++
        }
        html.startsWith("@submitted-date", i) -> {
          val matcher = submittedDatePattern.matcher(html.substring(i))
          if (matcher.find()) processSubmittedDate(i, html, matcher) else i++
        }
        html.startsWith("@contains", i) -> {
          val matcher = containsPattern.matcher(html.substring(i))
          if (matcher.find()) processContains(i, html, matcher) else i++
        }
        else -> i++
      }
    }
    return html.toString()
  }

  private fun processIsNotEmpty(i: Int, html: StringBuilder, matcher: Matcher) {
    val linkId = matcher.group(1)
    val content = matcher.group(2) ?: ""
    val doesAnswerExist = questionnaireResponseItemMap.getOrDefault(linkId, listOf()).isNotEmpty()
    if (doesAnswerExist) {
      html.replace(i, matcher.end() + i, content)
      // Start index is the index of '@' symbol, End index is the index after the ')' symbol.
      // For example: @is-not-empty('link')Text@is-not-empty('link')
      // The args we put the the replace function: The Start index is 0, the '@' symbol. The
      // End index is the index after the ')' symbol.
      // Note: The ones that are going to be replaced are from the Start index which is an '@' of
      // the first tag, until the index before the End index which is an ')' of the second tag.
    } else {
      html.replace(i, matcher.end() + i, "")
    }
  }

  private fun processAnswerAsList(i: Int, html: StringBuilder, matcher: Matcher) {
    val linkId = matcher.group(1)
    val answerAsList =
      questionnaireResponseItemMap.getOrDefault(linkId, listOf()).joinToString(separator = "") {
        answer ->
        "<li>${answer.value.valueToString()}</li>"
      }
    html.replace(i, matcher.end() + i, answerAsList)
  }

  private fun processAnswer(i: Int, html: StringBuilder, matcher: Matcher) {
    val linkId = matcher.group(1)
    val dateFormat = matcher.group(2)
    val answer =
      questionnaireResponseItemMap.getOrDefault(linkId, listOf()).joinToString { answer ->
        if (dateFormat == null) {
          answer.value.valueToString()
        } else answer.value.valueToString(dateFormat)
      }
    html.replace(i, matcher.end() + i, answer)
  }

  private fun processSubmittedDate(i: Int, html: StringBuilder, matcher: Matcher) {
    val dateFormat = matcher.group(1)
    val date =
      if (dateFormat == null) {
        questionnaireResponse.meta.lastUpdated.formatDate()
      } else {
        questionnaireResponse.meta.lastUpdated.formatDate(dateFormat)
      }
    html.replace(i, matcher.end() + i, date)
  }

  private fun processContains(i: Int, html: StringBuilder, matcher: Matcher) {
    val linkId = matcher.group(1)
    val indicator = matcher.group(2) ?: ""
    val content = matcher.group(3) ?: ""
    val doesAnswerExist =
      questionnaireResponseItemMap.getOrDefault(linkId, listOf()).any {
        when {
          it.hasValueCoding() -> it.valueCoding.code == indicator
          it.hasValueStringType() -> it.valueStringType.value.contains(indicator)
          it.hasValueIntegerType() -> it.valueIntegerType.value == indicator.toInt()
          it.hasValueDecimalType() -> it.valueDecimalType.value == indicator.toBigDecimal()
          it.hasValueBooleanType() -> it.valueBooleanType.value == indicator.toBoolean()
          it.hasValueQuantity() ->
            "${it.valueQuantity.value.toPlainString()} ${it.valueQuantity.unit}" == indicator
          it.hasValueDateType() || it.hasValueDateTimeType() ->
            (it.value as BaseDateTimeType).value.makeItReadable() == indicator
          else -> false
        }
      }
    if (doesAnswerExist) {
      html.replace(i, matcher.end() + i, content)
    } else {
      html.replace(i, matcher.end() + i, "")
    }
  }

  companion object {
    // Compile regex patterns for different tags
    private val isNotEmptyPattern =
      Pattern.compile("@is-not-empty\\('([^']+)'\\)((?s).*?)@is-not-empty\\('\\1'\\)")
    private val answerAsListPattern = Pattern.compile("@answer-as-list\\('([^']+)'\\)")
    private val answerPattern = Pattern.compile("@answer\\('([^']+)'(?:,'([^']+)')?\\)")
    private val submittedDatePattern = Pattern.compile("@submitted-date(?:\\('([^']+)'\\))?")
    private val containsPattern =
      Pattern.compile("@contains\\('([^']+)','([^']+)'\\)((?s).*?)@contains\\('\\1'\\)")
  }
}
