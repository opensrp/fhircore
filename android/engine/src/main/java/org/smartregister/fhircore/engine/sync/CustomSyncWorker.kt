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

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.fhir.sync.CurrentSyncJobStatus
import com.google.android.fhir.sync.concatParams
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.net.UnknownHostException
import java.time.OffsetDateTime
import kotlinx.coroutines.withContext
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.util.DispatcherProvider
import retrofit2.HttpException
import retrofit2.Response
import timber.log.Timber

@HiltWorker
class CustomSyncWorker
@AssistedInject
constructor(
  @Assisted appContext: Context,
  @Assisted workerParams: WorkerParameters,
  val configurationRegistry: ConfigurationRegistry,
  val dispatcherProvider: DispatcherProvider,
  val fhirResourceDataSource: FhirResourceDataSource,
) : CoroutineWorker(appContext, workerParams) {

  override suspend fun doWork(): Result {
    return withContext(dispatcherProvider.io()) {
      try {
        with(configurationRegistry) {
          val (resourceSearchParams, _) = loadResourceSearchParams()
          Timber.i("Custom resource sync parameters $resourceSearchParams")

          // Process resource URLs
          val resourceUrls =
            resourceSearchParams
              .asIterable()
              .filter { it.value.isNotEmpty() }
              .map { "${it.key}?${it.value.concatParams()}" }

          // Fetch summary count first

          val summaryCount = fetchSummaryCount(resourceUrls).values.sumOf { it ?: 0 }
          Timber.d("Fetched summary count: $summaryCount")

          // Fetch resources
          resourceUrls.forEach { url ->
            fetchCustomResources(
              gatewayModeHeaderValue = ConfigurationRegistry.FHIR_GATEWAY_MODE_HEADER_VALUE,
              url = url,
              totalCustomRecords = summaryCount,
            )
          }
        }
        Result.success()
      } catch (httpException: HttpException) {
        Timber.e(httpException)
        val response: Response<*>? = httpException.response()
        if (response != null && (400..503).contains(response.code())) {
          Timber.e("HTTP exception ${response.code()} -> ${response.errorBody()}")
        }
        Result.failure()
      } catch (unknownHostException: UnknownHostException) {
        Timber.e(unknownHostException)
        Result.failure()
      } catch (exception: Exception) {
        Timber.e(exception)
        configurationRegistry.setSyncState(
          CurrentSyncJobStatus.Failed(OffsetDateTime.now()),
        )
        Result.failure()
      }
    }
  }

  /** Fetch summary counts for the provided URLs with summary=count query added. */
  private suspend fun fetchSummaryCount(resourceUrls: List<String>): Map<String, Int?> =
    resourceUrls
      .associate { url ->
        // Modify URL to include 'summary=count'
        val summaryUrl = "$url&summary=count"
        val total: Int? =
          runCatching {
              // Explicitly fetch resource and ensure result can be cast to Bundle
              fhirResourceDataSource.getResource(summaryUrl)
            }
            .onFailure { Timber.e(it, "Failed to fetch summary for $summaryUrl") }
            .getOrNull()
            ?.total
        summaryUrl to total
      }
      .also { summaries -> Timber.i("Summary fetch results: $summaries") }

  companion object {
    const val WORK_ID = "CustomResourceSyncWorker"
  }
}
