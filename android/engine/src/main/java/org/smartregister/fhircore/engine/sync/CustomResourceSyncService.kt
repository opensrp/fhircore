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

package org.smartregister.fhircore.engine.sync

import com.google.android.fhir.sync.CurrentSyncJobStatus
import com.google.android.fhir.sync.SyncJobStatus
import com.google.android.fhir.sync.SyncOperation
import com.google.android.fhir.sync.concatParams
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry.Companion.PAGINATION_NEXT
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.util.DispatcherProvider
import timber.log.Timber

@Singleton
class CustomResourceSyncService
@Inject
constructor(
  val configurationRegistry: ConfigurationRegistry,
  val dispatcherProvider: DispatcherProvider,
  val fhirResourceDataSource: FhirResourceDataSource,
  val syncListenerManager: SyncListenerManager,
) {
  suspend fun runCustomResourceSync() {
    val (resourceSearchParams, _) = configurationRegistry.loadResourceSearchParams()
    if (resourceSearchParams.isEmpty()) return

    val resourceUrls =
      resourceSearchParams
        .asIterable()
        .filter { it.value.isNotEmpty() }
        .map { "${it.key}?${it.value.concatParams()}" }

    val summaryCount = fetchSummaryCount(resourceUrls).values.sumOf { it ?: 0 }

    resourceUrls.forEach { url ->
      fetchCustomResources(
        gatewayModeHeaderValue = ConfigurationRegistry.FHIR_GATEWAY_MODE_HEADER_VALUE,
        url = url,
        totalCounts = summaryCount,
      )
    }
  }

  private suspend fun fetchCustomResources(
    gatewayModeHeaderValue: String? = null,
    url: String,
    totalCounts: Int = 0,
    completedRecords: Int = 0,
  ) {
    runCatching {
        Timber.d("Setting state: Running")
        syncListenerManager.emitSyncStatus(
          SyncState(
            counter = SYNC_COUNTER_1,
            currentSyncJobStatus =
              CurrentSyncJobStatus.Running(
                SyncJobStatus.InProgress(
                  syncOperation = SyncOperation.DOWNLOAD,
                  total = totalCounts,
                  completed = completedRecords,
                ),
              ),
          ),
        )
        Timber.d("Fetching page with URL: $url")
        if (gatewayModeHeaderValue.isNullOrEmpty()) {
          fhirResourceDataSource.getResource(url)
        } else {
          fhirResourceDataSource.getResourceWithGatewayModeHeader(gatewayModeHeaderValue, url)
        }
      }
      .onFailure { throwable ->
        Timber.e("Error occurred while retrieving resource via URL $url", throwable)
        syncListenerManager.emitSyncStatus(
          SyncState(
            counter = SYNC_COUNTER_1,
            currentSyncJobStatus = CurrentSyncJobStatus.Failed(OffsetDateTime.now()),
          ),
        )
        return
      }
      .onSuccess { resultBundle ->
        configurationRegistry.processResultBundleEntries(resultBundle.entry)
        val newCompletedRecords = completedRecords + resultBundle.entry.size
        syncListenerManager.emitSyncStatus(
          SyncState(
            counter = SYNC_COUNTER_1,
            currentSyncJobStatus =
              CurrentSyncJobStatus.Running(
                SyncJobStatus.InProgress(
                  syncOperation = SyncOperation.DOWNLOAD,
                  total = totalCounts,
                  completed = newCompletedRecords,
                ),
              ),
          ),
        )

        val nextPageUrl = resultBundle.getLink(PAGINATION_NEXT)?.url

        if (!nextPageUrl.isNullOrEmpty()) {
          fetchCustomResources(
            gatewayModeHeaderValue = gatewayModeHeaderValue,
            url = nextPageUrl,
            totalCounts = totalCounts,
            completedRecords = newCompletedRecords,
          )
        } else {
          Timber.d("Fetch complete. Emitting SyncStatus.Succeeded.")
          syncListenerManager.emitSyncStatus(
            SyncState(
              counter = SYNC_COUNTER_1,
              currentSyncJobStatus = CurrentSyncJobStatus.Succeeded(OffsetDateTime.now()),
            ),
          )
        }
      }
  }

  /** Fetch summary counts for the provided [resourceUrls] */
  private suspend fun fetchSummaryCount(resourceUrls: List<String>): Map<String, Int?> =
    resourceUrls
      .associate { url ->
        val summaryUrl = "$url&summary=count"
        val total: Int? =
          runCatching { fhirResourceDataSource.getResource(summaryUrl) }
            .onFailure { Timber.e(it, "Failed to fetch summary for $summaryUrl") }
            .getOrNull()
            ?.total
        summaryUrl to total
      }
      .also { summaries -> Timber.i("Summary fetch results: $summaries") }
}
