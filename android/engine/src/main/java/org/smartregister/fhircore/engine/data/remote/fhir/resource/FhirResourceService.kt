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

import okhttp3.RequestBody
import okhttp3.ResponseBody
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.OperationOutcome
import org.hl7.fhir.r4.model.Resource
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.QueryMap
import retrofit2.http.Url

/** [Retrofit] Service for communication with HAPI FHIR server. Used for querying FHIR Resources */
interface FhirResourceService {

  @GET suspend fun getResource(@Url url: String): Bundle

  @PUT("{resourceType}/{id}")
  suspend fun insertResource(
    @Path("resourceType") resourceType: String,
    @Path("id") id: String,
    @Body body: RequestBody
  ): Resource

  @PATCH("{resourceType}/{id}")
  suspend fun updateResource(
    @Path("resourceType") resourceType: String,
    @Path("id") id: String,
    @Body body: RequestBody
  ): OperationOutcome

  @DELETE("{resourceType}/{id}")
  suspend fun deleteResource(
    @Path("resourceType") resourceType: String,
    @Path("id") id: String
  ): OperationOutcome

  @GET suspend fun fetchImage(@Url url: String): ResponseBody?

  @GET("{resourceType}/_search")
  suspend fun searchResource(
    @Path("resourceType") resourceType: String,
    @QueryMap(encoded = false) searchParameters: Map<String, String>
  ): Bundle
}
