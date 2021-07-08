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

import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.SyncDownloadContext
import com.google.android.fhir.db.impl.dao.LocalChangeToken
import com.google.android.fhir.db.impl.dao.SquashedLocalChange
import com.google.android.fhir.search.ReferenceFilter
import com.google.android.fhir.search.Search
import org.hl7.fhir.r4.model.Resource
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowApplication
import org.smartregister.fhircore.FhirApplication

@Implements(FhirApplication::class)
class FhirApplicationShadow : ShadowApplication() {

  private val dataMap = mutableMapOf<String, MutableList<Resource>>()

  inner class FhirEngineImpl : FhirEngine {
    override suspend fun count(search: Search): Long {
      return -1
    }

    override suspend fun <R : Resource> load(clazz: Class<R>, id: String): R {
      if (dataMap.containsKey(id)) {
        return dataMap[id]?.first() as R
      } else {
        throw ResourceNotFoundException("")
      }
    }

    override suspend fun <R : Resource> remove(clazz: Class<R>, id: String) {
      if (dataMap.containsKey(id)) {
        if (dataMap[id]!!.isNotEmpty()) {
          dataMap[id]!!.removeLast()
        }

        if (dataMap[id]!!.isEmpty()) {
          dataMap.remove(id)
        }
      }
    }

    override suspend fun <R : Resource> save(vararg resource: R) {
      resource.forEach {
        if (dataMap.containsKey(it.id)) {
          dataMap[it.id]?.add(it)
        } else {
          dataMap[it.id] = mutableListOf(it)
        }
      }
    }

    override suspend fun <R : Resource> search(search: Search): List<R> {
      val referenceFilter = getListOfFilters<ReferenceFilter>(search, "referenceFilters")

      val result = mutableListOf<Resource>()

      if (referenceFilter.isNotEmpty()) {
        dataMap.forEach {
          if (it.key == referenceFilter.first().value) {
            result.addAll(it.value)
          }
        }
      }

      return result as List<R>
    }

    override suspend fun syncDownload(download: suspend (SyncDownloadContext) -> List<Resource>) {}

    override suspend fun syncUpload(
      upload: suspend (List<SquashedLocalChange>) -> List<LocalChangeToken>
    ) {}

    override suspend fun <R : Resource> update(resource: R) {
      dataMap.forEach {
        if (it.key == resource.id) {
          if (it.value.isNotEmpty()) {
            it.value[0] = resource
          }
        }
      }
    }
  }

  @Implementation
  fun constructFhirEngine(): FhirEngine {
    return FhirEngineImpl()
  }

  private fun <T> getListOfFilters(search: Search, filterName: String): MutableList<T> {
    val field = search.javaClass.getDeclaredField(filterName)
    field.isAccessible = true
    return field.get(search) as MutableList<T>
  }
}
