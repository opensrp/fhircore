/*
 * Copyright 2021-2023 Ona Systems, Inc
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

package org.smartregister.fhircore.engine.util.fhirpath

import javax.inject.Inject
import javax.inject.Singleton
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.utils.FHIRLexer
import org.hl7.fhir.r4.utils.FHIRPathEngine
import timber.log.Timber

@Singleton
class FhirPathDataExtractor @Inject constructor(val fhirPathEngine: FHIRPathEngine) {

  fun extractData(base: Base, expressions: Map<String, String>): Map<String, List<Base>> =
    expressions.map { Pair(it.key, this.extractData(base, it.value)) }.toMap()

  /**
   * Function to extract value based on the provided FHIR path [expression] on the given [base].
   *
   * @return the value of the first item in the returned list of [Base] as String, empty otherwise.
   */
  fun extractValue(base: Base?, expression: String): String {
    if (base == null) return ""
    return this.extractData(base, expression).firstOrNull()?.primitiveValue() ?: ""
  }

  /**
   * Function to extract value for the [base] using the on the provided FHIR path [expression].
   *
   * @return a list of [Base].
   */
  fun extractData(base: Base, expression: String): List<Base> {
    return try {
      fhirPathEngine.evaluate(base, expression)
    } catch (exception: FHIRLexer.FHIRLexerException) {
      Timber.e(exception)
      emptyList()
    }
  }
}
