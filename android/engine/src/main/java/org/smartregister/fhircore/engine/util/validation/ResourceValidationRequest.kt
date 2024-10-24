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

package org.smartregister.fhircore.engine.util.validation

import ca.uhn.fhir.validation.FhirValidator
import ca.uhn.fhir.validation.ResultSeverityEnum
import ca.uhn.fhir.validation.ValidationResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Resource
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.extension.referenceValue
import timber.log.Timber

data class ResourceValidationRequest(val resources: List<Resource>) {
  constructor(vararg resource: Resource) : this(resource.toList())
}

class ResourceValidationRequestHandler(
  private val fhirValidator: FhirValidator,
  private val dispatcherProvider: DispatcherProvider,
) {
  fun handleResourceValidationRequest(request: ResourceValidationRequest) {
    CoroutineScope(dispatcherProvider.io()).launch {
      val resources = request.resources
      fhirValidator.checkResources(resources).logErrorMessages()
    }
  }
}

internal data class ResourceValidationResult(
  val resource: Resource,
  val validationResult: ValidationResult,
) {
  val errorMessages
    get() = buildString {
      val messages =
        validationResult.messages.filter {
          it.severity.ordinal >= ResultSeverityEnum.WARNING.ordinal
        }
      if (messages.isNotEmpty()) {
        appendLine(resource.referenceValue())
      }
      for (validationMsg in messages) {
        appendLine(
          "${validationMsg.locationString} - ${validationMsg.message} -- (${validationMsg.severity})",
        )
      }
    }
}

internal class FhirValidatorResultsWrapper(
  val results: List<ResourceValidationResult> = emptyList(),
) {
  val errorMessages = results.map { it.errorMessages }

  fun logErrorMessages() {
    results.forEach {
      if (it.errorMessages.isNotBlank()) {
        Timber.tag(TAG).e(it.errorMessages)
      }
    }
  }

  companion object {
    private const val TAG = "FhirValidatorResult"
  }
}

internal fun FhirValidator.checkResources(
  resources: List<Resource>,
): FhirValidatorResultsWrapper {
  return FhirValidatorResultsWrapper(
    results =
      resources.map {
        val result = this@checkResources.validateWithResult(it)
        ResourceValidationResult(it, result)
      },
  )
}
