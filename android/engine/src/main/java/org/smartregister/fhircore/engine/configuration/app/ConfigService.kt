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

package org.smartregister.fhircore.engine.configuration.app

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.fhir.sync.FhirSyncWorker
import com.google.android.fhir.sync.PeriodicSyncConfiguration
import com.google.android.fhir.sync.RepeatInterval
import com.google.android.fhir.sync.SyncJob
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Parameters
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.SearchParameter
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.FhirConfiguration
import org.smartregister.fhircore.engine.data.remote.model.response.UserInfo
import org.smartregister.fhircore.engine.sync.SyncBroadcaster
import org.smartregister.fhircore.engine.task.FhirTaskPlanWorker
import timber.log.Timber

/** An interface that provides the application configurations. */
interface ConfigService {

  /** Provide [AuthConfiguration] for the Application */
  fun provideAuthConfiguration(): AuthConfiguration

  /**
   * Schedule periodic sync periodically as defined in the [configurationRegistry] application
   * config interval. The [syncBroadcaster] will broadcast the sync status to its listeners
   */
  fun schedulePeriodicSync(
    syncJob: SyncJob,
    configurationRegistry: ConfigurationRegistry,
    syncBroadcaster: SyncBroadcaster,
    syncInterval: Long = 30,
  ) {
    CoroutineScope(Dispatchers.Main).launch {
      syncBroadcaster.sharedSyncStatus.emitAll(syncJob.stateFlow())
    }

    syncJob.poll(
      periodicSyncConfiguration =
        PeriodicSyncConfiguration(repeat = RepeatInterval(syncInterval, TimeUnit.MINUTES)),
      clazz = FhirSyncWorker::class.java
    )
  }

  fun schedulePlan(context: Context) {
    WorkManager.getInstance(context)
      .enqueueUniquePeriodicWork(
        FhirTaskPlanWorker.WORK_ID,
        ExistingPeriodicWorkPolicy.REPLACE,
        PeriodicWorkRequestBuilder<FhirTaskPlanWorker>(12, TimeUnit.HOURS).build()
      )
  }

  fun unschedulePlan(context: Context) {
    WorkManager.getInstance(context).cancelUniqueWork(FhirTaskPlanWorker.WORK_ID)
  }

  /** Retrieve registry sync params */
  fun loadRegistrySyncParams(
    configurationRegistry: ConfigurationRegistry,
    authenticatedUserInfo: UserInfo?,
  ): Map<ResourceType, Map<String, String>> {
    val pairs = mutableListOf<Pair<ResourceType, Map<String, String>>>()

    val syncConfig =
      configurationRegistry.retrieveConfiguration<FhirConfiguration<Parameters>>(
        AppConfigClassification.SYNC
      )

    val appConfig =
      configurationRegistry.retrieveConfiguration<ApplicationConfiguration>(
        AppConfigClassification.APPLICATION
      )

    // TODO Does not support nested parameters i.e. parameters.parameters...
    // TODO: expressionValue supports for Organization and Publisher literals for now
    syncConfig.resource.parameter.map { it.resource as SearchParameter }.forEach { sp ->
      val paramName = sp.name // e.g. organization
      val paramLiteral = "#$paramName" // e.g. #organization in expression for replacement
      val paramExpression = sp.expression
      val expressionValue =
        when (paramName) {
          ConfigurationRegistry.ORGANIZATION -> authenticatedUserInfo?.organization
          ConfigurationRegistry.PUBLISHER -> authenticatedUserInfo?.questionnairePublisher
          ConfigurationRegistry.ID -> paramExpression
          ConfigurationRegistry.COUNT -> appConfig.count
          else -> null
        }?.let {
          // replace the evaluated value into expression for complex expressions
          // e.g. #organization -> 123
          // e.g. patient.organization eq #organization -> patient.organization eq 123
          paramExpression.replace(paramLiteral, it)
        }

      // for each entity in base create and add param map
      // [Patient=[ name=Abc, organization=111 ], Encounter=[ type=MyType, location=MyHospital ],..]
      sp.base.forEach { base ->
        val resourceType = ResourceType.fromCode(base.code)
        val pair = pairs.find { it.first == resourceType }
        if (pair == null) {
          pairs.add(
            Pair(
              resourceType,
              expressionValue?.let { mapOf(sp.code to expressionValue) } ?: mapOf()
            )
          )
        } else {
          expressionValue?.let {
            // add another parameter if there is a matching resource type
            // e.g. [(Patient, {organization=105})] to [(Patient, {organization=105, _count=100})]
            val updatedPair = pair.second.toMutableMap().apply { put(sp.code, expressionValue) }
            val index = pairs.indexOfFirst { it.first == resourceType }
            pairs.set(index, Pair(resourceType, updatedPair))
          }
        }
      }
    }

    Timber.i("SYNC CONFIG $pairs")

    return mapOf(*pairs.toTypedArray())
  }
}
