/*
 * Copyright 2021-2023 Ona Systems, Inc
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

import com.google.android.fhir.sync.DownloadWorkManager
import com.google.android.fhir.sync.Request
import com.google.android.fhir.sync.download.ResourceParamsBasedDownloadWorkManager
import com.google.android.fhir.sync.download.ResourceSearchParams
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.util.extension.updateLastUpdated
import org.smartregister.fhircore.engine.util.logTimeTaken

/** Created by Ephraim Kigamba - nek.eam@gmail.com on 13-06-2023. */
class FhirCoreDownloadManager(
  syncParams: ResourceSearchParams,
  val context: ResourceParamsBasedDownloadWorkManager.TimestampContext,
  val defaultRepository: DefaultRepository
) : DownloadWorkManager {

  val downloadWorkManager = ResourceParamsBasedDownloadWorkManager(syncParams, context)

  override suspend fun getNextRequest(): Request? = downloadWorkManager.getNextRequest()

  override suspend fun getSummaryRequestUrls(): Map<ResourceType, String> =
    downloadWorkManager.getSummaryRequestUrls()

  override suspend fun processResponse(response: Resource): Collection<Resource> {
    val resourcesList = logTimeTaken { downloadWorkManager.processResponse(response) }
    resourcesList.forEach { it.updateLastUpdated() }

    // Reduces the amount of time it takes to insert since it's all done in a single transaction
    // Multiple transactions will be expensive
    logTimeTaken { defaultRepository.fhirEngine.update(*resourcesList.toTypedArray()) }

    // TODO: Test if this resource list has changed the meta.lastUpdated
    return resourcesList
  }
}
