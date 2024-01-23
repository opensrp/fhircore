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

import ca.uhn.fhir.validation.FhirValidator
import ca.uhn.fhir.validation.ResultSeverityEnum
import ca.uhn.fhir.validation.ValidationResult
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.Resource
import org.smartregister.fhircore.engine.BuildConfig

suspend fun FhirValidator.checkResourceValid(
  vararg resource: Resource,
  isDebug: Boolean = BuildConfig.BUILD_TYPE.contains("debug", ignoreCase = true),
): List<ValidationResult> {
  if (!isDebug) return emptyList()

  return withContext(coroutineContext) {
    resource.map { this@checkResourceValid.validateWithResult(it) }
  }
}

val ValidationResult.errorMessages
  get() = buildString {
    for (validationMsg in
      messages.filter { it.severity.ordinal >= ResultSeverityEnum.WARNING.ordinal }) {
      appendLine("${validationMsg.message} - ${validationMsg.locationString}")
    }
  }
