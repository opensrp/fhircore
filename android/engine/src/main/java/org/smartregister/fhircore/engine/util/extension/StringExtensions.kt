/*
 * Copyright 2021 Ona Systems, Inc
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

import org.apache.commons.text.StringSubstitutor
import java.text.MessageFormat
import java.util.Locale
import java.util.LinkedList

/**
 * This function replaces the content enclosed within [substitutionPair] with the value obtained
 * from the [valuesMap]. If the map does not contain the key, the key is returned instead. Default
 * template is @{key}
 *
 * Examples: Given the string "HIV status: @{hivResult}" with a map of {'hivResult': "+ve"}, the
 * resulting string will be:
 *
 * "HIV status: +ve"
 *
 * If the key is not available in the map the resulting string will be:
 *
 * "HIV status: @{hivResult}"
 */
@Deprecated("Use the other extension method")
fun String.interpolateDepr(
  valuesMap: Map<String, Any>,
  substitutionPair: Pair<String, String> = Pair("@{", "}")
): String {
  val (substitutionPrefix, substitutionSuffix) = substitutionPair
  val wordsList = LinkedList<String>()
  val delimiter = " "

  // First remove extra white spaces then split
  val splitWords = this.replace("\\s+".toRegex(), delimiter).split(delimiter)

  var index = 0
  while (index < splitWords.size) {
    val word = splitWords[index]
    if (word.startsWith(substitutionPrefix) && word.endsWith(substitutionSuffix)) {
      val startIndex = substitutionPrefix.length
      val endIndex = word.length - substitutionSuffix.length
      val key = word.substring(startIndex, endIndex).trim()
      if (valuesMap.containsKey(key)) {
        wordsList.addLast(valuesMap.getValue(key).toString())
      } else wordsList.addLast(word)
      index++
    } else if (word == substitutionPrefix) {
      var nextIndex = index.inc()

      // Combine all words after substitutionPrefix into one and use as key
      val keyStringBuilder = StringBuilder()
      while (nextIndex < splitWords.size && splitWords[nextIndex] != substitutionSuffix) {
        keyStringBuilder.append(splitWords[nextIndex])
        nextIndex++
      }
      val key = keyStringBuilder.toString()
      if (valuesMap.containsKey(key)) {
        wordsList.addLast(valuesMap.getValue(key).toString())
      } else {
        wordsList.addLast(substitutionPrefix + key + substitutionSuffix)
      }
      index = nextIndex.inc()
    } else {
      wordsList.addLast(word)
      index++
    }
  }
  return wordsList.joinToString(delimiter)
}
/**
 * Sample template string: { "saveFamilyButtonText" : {{family.button.save}} } Sample properties
 * file content: family.button.save=Save Family
 *
 * @param lookupMap The Map with the key value items to be used for interpolation
 * @param prefix The prefix of the key variable to interpolate. In the above example it is {{.
 * Default is @{
 * @param suffix The prefix of the key/variable to interpolate. In the above example it is }}.
 * Default is }
 *
 * @return String with the interpolated value. For the sample case above this would be: {
 * "saveFamilyButtonText" : "Save Family" }
 */
fun String.interpolate(
  lookupMap: Map<String, Any>,
  prefix: String = "@{",
  suffix: String = "}"
): String {
  return StringSubstitutor(lookupMap, prefix, suffix).replace(this)
}


/**
 * Wrapper method around the Java text formatter
 *
 * Example string format: Name {0} {1}, Age {2}
 *
 * @param locale this is the Locale to use e.g. Locale.ENGLISH
 * @param arguments this is a variable number of values to replace placeholders in order
 *
 * @return the interpolated string with the placeholder variables replaced with the arguments values.
 *
 * In the example above, the result for passing arguments John, Doe, 35 would be: Name John Doe, Age 35
 */
fun String.messageFormat(locale: Locale?, vararg arguments: Any?): String? =
  MessageFormat(this, locale).format(arguments)
