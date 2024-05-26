package org.smartregister.fhircore.engine.pdf

import com.google.android.fhir.datacapture.extensions.allItems
import org.hl7.fhir.r4.model.BaseDateTimeType
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.engine.util.extension.formatDate
import org.smartregister.fhircore.engine.util.extension.makeItReadable
import org.smartregister.fhircore.engine.util.extension.valueToString

/**
 * Class responsible for populating HTML content with data from a [QuestionnaireResponse].
 *
 * This class provides methods to:
 * - Hide UI elements based on certain conditions.
 * - Populate answers as a list or a single value.
 * - Populate the submitted date in the HTML content.
 *
 * @param questionnaireResponse The [QuestionnaireResponse] object containing the data to be populated in the HTML.
 */
class HtmlPopulator(
    private val questionnaireResponse: QuestionnaireResponse
) {

    private val questionnaireResponseItemMap = questionnaireResponse.allItems.associateBy(
        keySelector = { it.linkId },
        valueTransform = { it.answer }
    )

  /**
   * Populates the given HTML string with data from the [QuestionnaireResponse].
   *
   * @param html The raw HTML string to be populated.
   * @return The populated HTML string.
   */
  fun populateHtml(html: String): String {
    return html.processIsNotEmpty().processIsEnabled().populateAnswerAsList().populateAnswer().populateSubmittedDate().processContains()
  }

    /**
     * Hides the contained UI elements if the referred link ID has no answer.
     *
     * Example:
     * ```
     * If the answer is empty:
     * "@is-not-empty('link-id')<p>Text</p>@is-not-empty" -> (nothing)
     *
     * If the answer is not empty:
     * "@is-not-empty('link-id')<p>Text</p>@is-not-empty" -> <p>Text</p>
     * ```
     *
     * @receiver The raw HTML string to be processed.
     * @return The processed HTML string with the elements hidden or shown based on the condition.
     */
    private fun String.processIsNotEmpty(): String {
        var html = this
        while (html.contains("@is-not-empty('")) {
            val linkId = html.substringAfter("@is-not-empty('").substringBefore("')")
            val htmlWithoutTag = html.substringAfter("@is-not-empty('$linkId')").substringBefore("@is-not-empty('$linkId')")
            html =  if (questionnaireResponseItemMap.getOrDefault(linkId, listOf()).isNotEmpty()) {
                html.replace("@is-not-empty('$linkId')$htmlWithoutTag@is-not-empty('$linkId')", htmlWithoutTag)
            } else {
                html.replace("@is-not-empty('$linkId')$htmlWithoutTag@is-not-empty('$linkId')", "")
            }
        }
        return html
    }

    private fun String.processIsEnabled(): String {
        var html = this
        while (html.contains("@is-enabled('")) {
            val linkId = html.substringAfter("@is-enabled('").substringBefore("')")
            val htmlWithoutTag = html.substringAfter("@is-enabled('$linkId')").substringBefore("@is-enabled")
            html =  if (questionnaireResponseItemMap.containsKey(linkId)) {
                html.replace("@is-enabled('$linkId')$htmlWithoutTag@is-enabled", htmlWithoutTag)
            } else {
                html.replace("@is-enabled('$linkId')$htmlWithoutTag@is-enabled", "")
            }
        }
        return html
    }


    /**
     * Populates answers as a list in the HTML.
     *
     * Example:
     * ```
     * "</ul>@answer-as-list('link-id')</ul>" -> <ul><li>answer 1</li><li>answer 2</li></ul>
     * ```
     *
     * @receiver The raw HTML string to be processed.
     * @return The HTML string with answers populated as list items.
     */
    private fun String.populateAnswerAsList(): String {
        var html = this
        while (html.contains("@answer-as-list('")) {
            val linkId = html.substringAfter("@answer-as-list('").substringBefore("')")
            val answerAsList = questionnaireResponseItemMap.getOrDefault(linkId, listOf()).joinToString(separator = "") { answer -> "<li>${answer.value.valueToString()}</li>" }
            html = html.replace("@answer-as-list('$linkId')", answerAsList)
        }
        return html
    }

    /**
     * Populates the answer from the referenced link ID in the HTML.
     *
     * Example:
     * ```
     * "@answer('link-id')" -> (the answer)
     *
     * "@answer('link-id')" -> 14-May-2024 (if it's a date type)
     *
     * "@answer('link-id','MMMM d, yyyy')" -> May 14, 2024
     * ```
     *
     * @receiver The raw HTML string to be processed.
     * @return The HTML string with the answer populated.
     */
    private fun String.populateAnswer(): String {
        var html = this
        while (html.contains("@answer('")) {
            val linkId = html.substringAfter("@answer('").substringBefore("'")
            val dateFormat = html.substringAfter("@answer('$linkId','", "").substringBefore("')")
            val answer = questionnaireResponseItemMap.getOrDefault(linkId, listOf()).joinToString { answer ->
                if (dateFormat.isEmpty()) answer.value.valueToString() else answer.value.valueToString(dateFormat)
            }
            html = if (dateFormat.isEmpty()) {
                html.replace("@answer('$linkId')", answer)
            } else {
                html.replace("@answer('$linkId','$dateFormat')", answer)
            }
        }
        return html
    }

  /**
   * Populates the submitted date from [QuestionnaireResponse.meta.lastUpdated].
   *
   * Example:
   * ```
   * "@submitted-date" -> 14-May-2024
   *
   * "@submitted-date('MMMM d, yyyy')" -> May 14, 2024
   * ```
   *
   * @return The HTML string with the submitted date populated.
   * @receiver The raw HTML string to be processed.
   */
  private fun String.populateSubmittedDate(): String {
    var html = this
    while (html.contains("@submitted-date")) {
      val dateFormat = html.substringAfter("@submitted-date('", "").substringBefore("')")
      val formattedDate =
        if (dateFormat.isEmpty()) {
          questionnaireResponse.meta.lastUpdated.formatDate()
        } else {
          questionnaireResponse.meta.lastUpdated.formatDate(dateFormat)
        }
      html =
        if (dateFormat.isEmpty()) {
          html.replace("@submitted-date", formattedDate)
        } else {
          html.replace("@submitted-date('$dateFormat')", formattedDate)
        }
    }
    return html
  }

  private fun String.processContains(): String {
    var html = this
    while (html.contains("@contains('")) {
      val linkId = html.substringAfter("@contains('").substringBefore("','")
      val indicator = html.substringAfter("@contains('$linkId','").substringBefore("')")
      val content = html.substringAfter("@contains('$linkId','$indicator')").substringBefore("@contains('$linkId')")

      val shouldShow = questionnaireResponseItemMap.getOrDefault(linkId, listOf()).any {
        when {
          it.hasValueCoding() -> it.valueCoding.code == indicator
          it.hasValueStringType() -> it.valueStringType.value == indicator
          it.hasValueIntegerType() -> it.valueIntegerType.value == indicator.toInt()
          it.hasValueDecimalType() -> it.valueDecimalType.value == indicator.toBigDecimal()
          it.hasValueBooleanType() -> it.valueBooleanType.value == indicator.toBoolean()
          it.hasValueQuantity() -> it.valueQuantity.value.toPlainString() == indicator
          it.isDateTime -> (it.value as BaseDateTimeType).value.makeItReadable() == indicator
          else -> false
        }
      }
      html = if (shouldShow) {
        html.replace("@contains('$linkId','$indicator')$content@contains('$linkId')", content)
      } else {
        html.replace("@contains('$linkId','$indicator')$content@contains('$linkId')", "")
      }
    }
    return html
  }
}