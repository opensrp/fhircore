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

import com.google.android.fhir.sync.DataSource
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.OperationOutcome
import org.hl7.fhir.r4.model.Resource

/** Implementation of the [DataSource] that communicates with hapi fhir. */
class FhirResourceDataSource(private val resourceService: FhirResourceService) : DataSource {

  override suspend fun loadData(path: String): Bundle {
    return resourceService.getResource(path)
  }

  override suspend fun insert(resourceType: String, resourceId: String, payload: String): Resource {
    return resourceService.insertResource(
      resourceType,
      resourceId,
      payload.toRequestBody("application/fhir+json".toMediaType())
    )
  }

  override suspend fun update(
    resourceType: String,
    resourceId: String,
    payload: String
  ): OperationOutcome {
    return resourceService.updateResource(
      resourceType,
      resourceId,
      payload.toRequestBody("application/json-patch+json".toMediaType())
    )
  }

  override suspend fun delete(resourceType: String, resourceId: String): OperationOutcome {
    return resourceService.deleteResource(resourceType, resourceId)
  }
}
