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
import timber.log.Timber

data class ResourceValidationResult(
  val resource: Resource,
  val validationResult: ValidationResult,
) {
  val errorMessages
    get() = buildString {
      val messages =
        validationResult.messages.filter {
          it.severity.ordinal >= ResultSeverityEnum.WARNING.ordinal
        }

      for (validationMsg in messages) {
        appendLine(
          "${validationMsg.message} - ${validationMsg.locationString} -- (${validationMsg.severity})",
        )
      }
    }
}

data class FhirValidatorResultsWrapper(val results: List<ResourceValidationResult> = emptyList()) {
  val errorMessages = results.map { it.errorMessages }
}

suspend fun FhirValidator.checkResourceValid(
  vararg resource: Resource,
  isDebug: Boolean = BuildConfig.DEBUG,
): FhirValidatorResultsWrapper {
  if (!isDebug) return FhirValidatorResultsWrapper()

  return withContext(coroutineContext) {
    FhirValidatorResultsWrapper(
      results =
        resource.map {
          val result = this@checkResourceValid.validateWithResult(it)
          ResourceValidationResult(it, result)
        },
    )
  }
}

fun FhirValidatorResultsWrapper.logErrorMessages() {
  results.forEach {
    if (it.errorMessages.isNotBlank()) {
      Timber.tag("$TAG (${it.resource.referenceValue()})").e(it.errorMessages)
    }
  }
}

private const val TAG = "FhirValidator"
