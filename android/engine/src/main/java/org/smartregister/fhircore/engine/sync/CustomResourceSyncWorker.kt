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
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.net.UnknownHostException
import kotlinx.coroutines.withContext
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.model.location.LocationHierarchy
import retrofit2.HttpException

@HiltWorker
class CustomResourceSyncWorker
@AssistedInject
constructor(
  @Assisted appContext: Context,
  @Assisted workerParams: WorkerParameters,
  val configurationRegistry: ConfigurationRegistry,
  val dispatcherProvider: DispatcherProvider,
  val defaultRepository: DefaultRepository,
  val fhirResourceDataSource: FhirResourceDataSource,
) : CoroutineWorker(appContext, workerParams) {
  override suspend fun doWork(): Result {
    return withContext(dispatcherProvider.io()) {
      try {
        val urlList = configurationRegistry.fetchURLForCustomResource()
        val bundle = fhirResourceDataSource.getResource(urlList.first())
        bundle.entry.forEach { entry ->
          val locationHierarchy = entry.resource as? LocationHierarchy?
          locationHierarchy?.let { defaultRepository.create(false, it) }
        }
        Result.success()
      } catch (httpException: HttpException) {
        Result.failure()
      } catch (unknownHostException: UnknownHostException) {
        Result.failure()
      }
    }
  }

  companion object {
    const val WORK_ID = "CustomResourceSyncWorker"
  }
}
