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

import com.google.android.fhir.sync.DownloadWorkManager
import com.google.android.fhir.sync.download.DownloadRequest
import com.google.android.fhir.sync.download.ResourceParamsBasedDownloadWorkManager
import com.google.android.fhir.sync.download.ResourceSearchParams
import com.google.android.fhir.sync.download.UrlDownloadRequest
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.util.extension.updateLastUpdated

class OpenSrpDownloadManager(
  resourceSearchParams: ResourceSearchParams,
  val context: ResourceParamsBasedDownloadWorkManager.TimestampContext,
) : DownloadWorkManager {

  private val downloadWorkManager =
    ResourceParamsBasedDownloadWorkManager(resourceSearchParams, context)

  override suspend fun getNextRequest(): DownloadRequest? =
    downloadWorkManager.getNextRequest()?.let { request ->
      if (request is UrlDownloadRequest) {
        DownloadRequest.of(request.url.disablePrettyPrint(), request.headers)
      } else {
        request
      }
    }

  override suspend fun getSummaryRequestUrls(): Map<ResourceType, String> =
    downloadWorkManager.getSummaryRequestUrls()

  override suspend fun processResponse(response: Resource): Collection<Resource> {
    return downloadWorkManager.processResponse(response).onEach { it.updateLastUpdated() }
  }
}

/**
 * Forces the FHIR `_pretty` query parameter to `false` so the server returns compact JSON, keeping
 * sync payloads small. An existing `_pretty=<value>` (any case/value) is rewritten to
 * `_pretty=false`; when the parameter is absent it is appended with the correct separator.
 */
internal fun String.disablePrettyPrint(): String =
  if (contains("_pretty=", ignoreCase = true)) {
    replace(Regex("_pretty=[^&]*", RegexOption.IGNORE_CASE), "_pretty=false")
  } else {
    this + (if (contains('?')) '&' else '?') + "_pretty=false"
  }
