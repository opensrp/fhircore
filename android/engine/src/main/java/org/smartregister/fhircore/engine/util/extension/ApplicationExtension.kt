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

package org.smartregister.fhircore.engine.util.extension

import android.content.Context
import android.content.res.AssetManager
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.util.UrlUtil
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.db.ResourceNotFoundException
import com.google.android.fhir.get
import com.google.android.fhir.search.search
import com.google.android.fhir.sync.FhirSyncWorker
import com.google.android.fhir.sync.PeriodicSyncConfiguration
import com.google.android.fhir.sync.RepeatInterval
import com.google.android.fhir.sync.SyncJob
import com.google.android.fhir.workflow.FhirOperator
import com.google.gson.Gson
import java.net.URL
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.IdType
import org.hl7.fhir.r4.model.Library
import org.hl7.fhir.r4.model.Measure
import org.hl7.fhir.r4.model.RelatedArtifact
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.domain.model.Language
import org.smartregister.fhircore.engine.sync.SyncBroadcaster
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import timber.log.Timber

fun <T> Context.loadResourceTemplate(id: String, clazz: Class<T>, data: Map<String, String?>): T {
  var json = assets.open(id).bufferedReader().use { it.readText() }

  data.entries.forEach { it.value?.let { v -> json = json.replace(it.key, v) } }

  return if (Resource::class.java.isAssignableFrom(clazz))
    FhirContext.forR4Cached().newJsonParser().parseResource(json) as T
  else Gson().fromJson(json, clazz)
}

suspend inline fun <reified T : Resource> FhirEngine.loadResource(resourceId: String): T? {
  return try {
    this.get(resourceId)
  } catch (resourceNotFoundException: ResourceNotFoundException) {
    null
  }
}

suspend fun FhirEngine.loadCqlLibraryBundle(
  context: Context,
  sharedPreferencesHelper: SharedPreferencesHelper,
  fhirOperator: FhirOperator,
  resourcesBundlePath: String
) =
  try {
    val jsonParser = FhirContext.forR4().newJsonParser()
    val savedResources =
      sharedPreferencesHelper.read(SharedPreferenceKey.MEASURE_RESOURCES_LOADED.name, "")

    context.assets.open(resourcesBundlePath, AssetManager.ACCESS_RANDOM).bufferedReader().use {
      val bundle = jsonParser.parseResource(it) as Bundle
      bundle.entry.forEach { entry ->
        if (entry.resource.resourceType == ResourceType.Library) {
          fhirOperator.loadLib(entry.resource as Library)
        } else {
          if (!savedResources!!.contains(resourcesBundlePath)) {
            create(entry.resource)
            sharedPreferencesHelper.write(
              SharedPreferenceKey.MEASURE_RESOURCES_LOADED.name,
              savedResources.plus(",").plus(resourcesBundlePath)
            )
          }
        }
      }
    }
  } catch (exception: Exception) {
    Timber.e(exception)
  }

suspend fun FhirEngine.loadLibraryAtPath(fhirOperator: FhirOperator, path: String) {
  // resource path could be Library/123 OR something like http://fhir.labs.common/Library/123
  val library =
    if (!UrlUtil.isValid(path)) get<Library>(IdType(path).idPart)
    else search<Library> { filter(Library.URL, { value = path }) }.firstOrNull()

  library?.let {
    fhirOperator.loadLib(it)

    it.relatedArtifact.forEach { loadLibraryAtPath(fhirOperator, it) }
  }
}

suspend fun FhirEngine.loadLibraryAtPath(
  fhirOperator: FhirOperator,
  relatedArtifact: RelatedArtifact
) {
  if (relatedArtifact.type.isIn(
      RelatedArtifact.RelatedArtifactType.COMPOSEDOF,
      RelatedArtifact.RelatedArtifactType.DEPENDSON
    )
  )
    loadLibraryAtPath(fhirOperator, relatedArtifact.resource)
}

suspend fun FhirEngine.loadCqlLibraryBundle(fhirOperator: FhirOperator, measurePath: String) =
  try {
    // resource path could be Measure/123 OR something like http://fhir.labs.common/Measure/123
    val measure =
      if (UrlUtil.isValid(measurePath))
        search<Measure> { filter(Measure.URL, { value = measurePath }) }.first()
      else get(measurePath)

    measure.relatedArtifact.forEach { loadLibraryAtPath(fhirOperator, it) }

    measure.library.map { it.value }.forEach { path -> loadLibraryAtPath(fhirOperator, path) }
  } catch (exception: Exception) {
    Timber.e(exception)
  }

fun ConfigurationRegistry.fetchLanguages() =
  this.retrieveConfiguration<ApplicationConfiguration>(ConfigType.Application)
    .run { this.languages }
    .map { Language(it, Locale.forLanguageTag(it).displayName) }

/**
 * Schedule periodic sync periodically as defined in the [configurationRegistry] application config
 * interval. The [syncBroadcaster] will broadcast the sync status to its listeners
 */
fun SyncJob.schedulePeriodicSync(
  configurationRegistry: ConfigurationRegistry, // TODO Obtain sync interval from app config
  syncBroadcaster: SyncBroadcaster,
  syncInterval: Long = 30
) {
  CoroutineScope(Dispatchers.Main).launch {
    syncBroadcaster.sharedSyncStatus.emitAll(this@schedulePeriodicSync.stateFlow())
  }
  this.poll(
    periodicSyncConfiguration =
      PeriodicSyncConfiguration(repeat = RepeatInterval(syncInterval, TimeUnit.MINUTES)),
    clazz = FhirSyncWorker::class.java
  )
}
