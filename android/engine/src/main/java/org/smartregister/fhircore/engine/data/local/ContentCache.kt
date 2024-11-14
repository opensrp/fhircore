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

package org.smartregister.fhircore.engine.data.local

import androidx.collection.LruCache
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.util.DispatcherProvider

@Singleton
class ContentCache @Inject constructor(private val dispatcherProvider: DispatcherProvider) {
  private val maxMemory: Int = (Runtime.getRuntime().maxMemory() / 1024).toInt()
  private val cacheSize: Int = maxMemory / 8
  private val cache = LruCache<String, Resource>(cacheSize)
  private val mutex = Mutex()

  suspend fun <T : Resource> saveResource(resource: T): T {
    val key = "${resource.resourceType.name}/${resource.idPart}"
    return withContext(dispatcherProvider.io()) {
      mutex.withLock { cache.put(key, resource.copy()) }
      @Suppress("UNCHECKED_CAST")
      getResource(resource.resourceType, resource.idPart)!! as T
    }
  }

  fun getResource(type: ResourceType, id: String) = cache["$type/$id"]?.copy()

  suspend fun invalidate() =
    withContext(dispatcherProvider.io()) { mutex.withLock { cache.evictAll() } }
}
