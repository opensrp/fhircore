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

package org.smartregister.fhircore.engine.datastore

import androidx.collection.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.Resource

object ContentCache {
  private val maxMemory: Int = (Runtime.getRuntime().maxMemory() / 1024).toInt()
  private val cacheSize: Int = maxMemory / 8
  private val cache = LruCache<String, Resource>(cacheSize)

  @JvmStatic
  suspend fun saveResource(resource: Resource) =
    withContext(Dispatchers.IO) {
      cache.put("${resource.resourceType.name}/${resource.idPart}", resource.copy())
    }

  @JvmStatic fun getResource(resourceId: String) = cache[resourceId]?.copy()

  suspend fun invalidate() = withContext(Dispatchers.IO) { cache.evictAll() }
}
