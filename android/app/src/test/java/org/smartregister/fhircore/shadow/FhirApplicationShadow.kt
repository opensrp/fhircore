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

package org.smartregister.fhircore.shadow

import com.google.android.fhir.FhirEngine
import com.google.android.fhir.SyncDownloadContext
import com.google.android.fhir.db.impl.dao.LocalChangeToken
import com.google.android.fhir.db.impl.dao.SquashedLocalChange
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.Search
import org.hl7.fhir.r4.model.Resource
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowApplication
import org.smartregister.fhircore.FhirApplication

@Implements(FhirApplication::class)
class FhirApplicationShadow : ShadowApplication() {

  private val dataMap = mutableMapOf<String, Resource>()

  inner class FhirEngineImpl : FhirEngine {
    override suspend fun count(search: Search): Long {
      return -1
    }

    override suspend fun <R : Resource> load(clazz: Class<R>, id: String): R {
      return dataMap[id] as R
    }

    override suspend fun <R : Resource> remove(clazz: Class<R>, id: String) {
      dataMap.remove(id)
    }

    override suspend fun <R : Resource> save(vararg resource: R) {
      dataMap[resource[0].logicalId] = resource[0]
    }

    override suspend fun <R : Resource> search(search: Search): List<R> {
      return mutableListOf()
    }

    override suspend fun syncDownload(download: suspend (SyncDownloadContext) -> List<Resource>) {}

    override suspend fun syncUpload(
      upload: suspend (List<SquashedLocalChange>) -> List<LocalChangeToken>
    ) {}

    override suspend fun <R : Resource> update(resource: R) {
      dataMap[resource.logicalId] = resource
    }
  }

  @Implementation
  fun constructFhirEngine(): FhirEngine {
    return FhirEngineImpl()
  }
}
