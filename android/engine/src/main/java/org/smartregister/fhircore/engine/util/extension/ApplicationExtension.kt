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
import ca.uhn.fhir.context.FhirContext
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.db.ResourceNotFoundException
import com.google.android.fhir.get
import com.google.android.fhir.sync.FhirSyncWorker
import com.google.android.fhir.sync.PeriodicSyncConfiguration
import com.google.android.fhir.sync.RepeatInterval
import com.google.android.fhir.sync.SyncJob
import com.google.gson.Gson
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Resource
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.domain.model.Language
import org.smartregister.fhircore.engine.sync.SyncBroadcaster

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
