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

package org.smartregister.fhircore.engine

import com.google.android.fhir.FhirEngine
import com.google.android.fhir.LocalChange
import com.google.android.fhir.SearchResult
import com.google.android.fhir.db.LocalChangeResourceReference
import com.google.android.fhir.search.Search
import com.google.android.fhir.sync.ConflictResolver
import com.google.android.fhir.sync.upload.SyncUploadProgress
import com.google.android.fhir.sync.upload.UploadRequestResult
import com.google.android.fhir.sync.upload.UploadStrategy
import java.time.OffsetDateTime
import kotlinx.coroutines.flow.Flow
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType

class FhirEngineWrapper(fhirEngineProducer: () -> FhirEngine) : FhirEngine {

  private val fhirEngine by lazy {
    return@lazy fhirEngineProducer.invoke()
  }

  override suspend fun clearDatabase() = fhirEngine.clearDatabase()

  override suspend fun count(search: Search): Long = fhirEngine.count(search)

  override suspend fun create(vararg resource: Resource, isLocalOnly: Boolean): List<String> =
    fhirEngine.create(*resource, isLocalOnly = isLocalOnly)

  override suspend fun delete(type: ResourceType, id: String) = fhirEngine.delete(type, id)

  override suspend fun get(type: ResourceType, id: String): Resource = fhirEngine.get(type, id)

  override suspend fun getLastSyncTimeStamp(): OffsetDateTime? = fhirEngine.getLastSyncTimeStamp()

  override suspend fun getLocalChanges(type: ResourceType, id: String): List<LocalChange> =
    fhirEngine.getLocalChanges(type, id)

  override suspend fun getUnsyncedLocalChanges(): List<LocalChange> =
    fhirEngine.getUnsyncedLocalChanges()

  override suspend fun purge(type: ResourceType, id: String, forcePurge: Boolean) =
    fhirEngine.purge(type, id, forcePurge)

  override suspend fun purge(type: ResourceType, ids: Set<String>, forcePurge: Boolean) =
    fhirEngine.purge(type, ids)

  override suspend fun <R : Resource> search(search: Search): List<SearchResult<R>> =
    fhirEngine.search<R>(search)

  @Deprecated("To be deprecated.")
  override suspend fun syncDownload(
    conflictResolver: ConflictResolver,
    download: suspend () -> Flow<List<Resource>>,
  ) = fhirEngine.syncDownload(conflictResolver, download)

  @Deprecated("To be deprecated.")
  override suspend fun syncUpload(
    uploadStrategy: UploadStrategy,
    upload:
      suspend (List<LocalChange>, List<LocalChangeResourceReference>) -> Flow<UploadRequestResult>,
  ): Flow<SyncUploadProgress> = fhirEngine.syncUpload(uploadStrategy, upload)

  override suspend fun update(vararg resource: Resource) = fhirEngine.update(*resource)

  override suspend fun withTransaction(block: suspend FhirEngine.() -> Unit) =
    fhirEngine.withTransaction(block)
}
