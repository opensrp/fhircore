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
fun String.interpolate(
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
