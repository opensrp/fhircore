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
import com.google.android.fhir.datacapture.extensions.logicalId
import com.google.android.fhir.db.LocalChangeResourceReference
import com.google.android.fhir.delete
import com.google.android.fhir.search.Search
import com.google.android.fhir.sync.ConflictResolver
import com.google.android.fhir.sync.upload.HttpCreateMethod
import com.google.android.fhir.sync.upload.HttpUpdateMethod
import com.google.android.fhir.sync.upload.UploadRequestResult
import com.google.android.fhir.sync.upload.UploadStrategy
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Before
import org.junit.Test

class FhirEngineWrapperTest {

  private lateinit var fhirEngineWrapper: FhirEngineWrapper
  private lateinit var fhirEngine: FhirEngine

  @Before
  fun setUp() {
    fhirEngine = mockk(relaxed = true)
    fhirEngineWrapper = FhirEngineWrapper { fhirEngine }
  }

  @Test
  fun clearDatabase() = runTest {
    fhirEngineWrapper.clearDatabase()
    coVerify { fhirEngine.clearDatabase() }
  }

  @Test
  fun count() = runTest {
    val search = Search(ResourceType.Patient)
    fhirEngineWrapper.count(search)
    coVerify { fhirEngine.count(search) }
  }

  @Test
  fun create() = runTest {
    val resource = Patient()
    fhirEngineWrapper.create(resource, isLocalOnly = false)
    coVerify { fhirEngine.create(resource, isLocalOnly = false) }
  }

  @Test
  fun delete() = runTest {
    val resource = Patient().apply { id = "sample-id" }
    fhirEngineWrapper.delete(resource.resourceType, resource.logicalId)
    coVerify { fhirEngine.delete(resource.resourceType, "sample-id") }
  }

  @Test
  fun get() = runTest {
    fhirEngineWrapper.get(ResourceType.Patient, "test-id")
    coVerify { fhirEngine.get(ResourceType.Patient, "test-id") }
  }

  @Test
  fun getLastSyncTimeStamp() = runTest {
    fhirEngineWrapper.getLastSyncTimeStamp()
    coVerify { fhirEngine.getLastSyncTimeStamp() }
  }

  @Test
  fun getLocalChanges() = runTest {
    fhirEngineWrapper.getLocalChanges(ResourceType.Patient, "test-id")
    coVerify { fhirEngine.getLocalChanges(ResourceType.Patient, "test-id") }
  }

  @Test
  fun getUnsyncedLocalChanges() = runTest {
    fhirEngineWrapper.getUnsyncedLocalChanges()
    coVerify { fhirEngine.getUnsyncedLocalChanges() }
  }

  @Test
  fun purge() = runTest {
    fhirEngineWrapper.purge(ResourceType.Patient, "test-id", true)
    coVerify { fhirEngine.purge(ResourceType.Patient, "test-id", true) }
  }

  @Test
  fun purgeIdSets() = runTest {
    val ids = setOf("res-1", "res-2", "res-3")
    fhirEngineWrapper.purge(ResourceType.Patient, ids, true)
    coVerify { fhirEngine.purge(ResourceType.Patient, ids, true) }
  }

  @Test
  fun search() = runTest {
    val search = Search(ResourceType.Patient)
    fhirEngineWrapper.search<Patient>(search)
    coVerify { fhirEngine.search<Patient>(search) }
  }

  @Test
  fun syncDownload() = runTest {
    val conflictResolver = mockk<ConflictResolver>(relaxed = true)
    val downloadOp = suspend { emptyFlow<List<Patient>>() }
    fhirEngineWrapper.syncDownload(conflictResolver, downloadOp)
    coVerify { fhirEngine.syncDownload(conflictResolver, downloadOp) }
  }

  @Test
  fun syncUpload() = runTest {
    val uploadStrategy =
      UploadStrategy.forIndividualRequest(HttpCreateMethod.PUT, HttpUpdateMethod.PATCH, true)
    val uploadOp:
      suspend (List<LocalChange>, List<LocalChangeResourceReference>) -> Flow<UploadRequestResult> =
      { _, _ ->
        emptyFlow()
      }
    fhirEngineWrapper.syncUpload(uploadStrategy, uploadOp)
    coVerify { fhirEngine.syncUpload(uploadStrategy, uploadOp) }
  }

  @Test
  fun update() = runTest {
    val resource = Patient()
    fhirEngineWrapper.update(resource)
    coVerify { fhirEngine.update(resource) }
  }

  @Test
  fun withTransaction() = runTest {
    val block: suspend FhirEngine.() -> Unit = {}
    fhirEngineWrapper.withTransaction(block)
    coVerify { fhirEngine.withTransaction(block) }
  }
}
