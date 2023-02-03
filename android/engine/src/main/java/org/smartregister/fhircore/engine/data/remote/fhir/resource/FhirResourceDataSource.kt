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

package org.smartregister.fhircore.engine.data.remote.fhir.resource

import javax.inject.Inject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.OperationOutcome
import org.hl7.fhir.r4.model.Resource

/** Interact with HAPI FHIR server */
class FhirResourceDataSource @Inject constructor(private val resourceService: FhirResourceService) {

  suspend fun getResource(path: String): Bundle {
    return resourceService.getResource(path)
  }

  suspend fun insert(resourceType: String, resourceId: String, payload: String): Resource {
    return resourceService.insertResource(
      resourceType,
      resourceId,
      payload.toRequestBody("application/fhir+json".toMediaType())
    )
  }

  suspend fun update(resourceType: String, resourceId: String, payload: String): OperationOutcome {
    return resourceService.updateResource(
      resourceType,
      resourceId,
      payload.toRequestBody("application/json-patch+json".toMediaType())
    )
  }

  suspend fun delete(resourceType: String, resourceId: String): OperationOutcome {
    return resourceService.deleteResource(resourceType, resourceId)
  }

  suspend fun search(resourceType: String, searchParameters: Map<String, String>): Bundle =
    resourceService.searchResource(resourceType, searchParameters)
}
