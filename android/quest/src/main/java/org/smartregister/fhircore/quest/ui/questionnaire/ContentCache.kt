package org.smartregister.fhircore.quest.ui.questionnaire

import androidx.collection.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.Resource

object ContentCache {
    private val maxMemory: Int = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize: Int = maxMemory / 8
    private val cache = LruCache<String, Resource>(cacheSize)

    suspend fun saveResource(resourceId: String, resource: Resource) =
        withContext(Dispatchers.IO) { cache.put("${resource::class.simpleName}/$resourceId", resource) }

    fun getResource(resourceId: String) = cache[resourceId]

    suspend fun invalidate() = withContext(Dispatchers.IO) { cache.evictAll() }
}