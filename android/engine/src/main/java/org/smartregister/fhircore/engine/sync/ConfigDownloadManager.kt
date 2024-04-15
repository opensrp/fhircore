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
import org.hl7.fhir.r4.model.Meta
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import java.util.Date
import java.util.LinkedList

class ConfigDownloadManager(
  syncParams: ResourceSearchParams,
  val context: ResourceParamsBasedDownloadWorkManager.TimestampContext,
  private val queries: List<String> = listOf(""), // set empty
) : DownloadWorkManager {
  private var urls = LinkedList(queries)

  public fun setupQueries(queries: List<String>){
    urls = LinkedList(queries)
  }
  override suspend fun getNextRequest(): DownloadRequest? =
    urls.poll()?.let { DownloadRequest.of(it) }

  override suspend fun getSummaryRequestUrls() =
    queries
      .stream()
      .map { ResourceType.fromCode(it.substringBefore("?")) to it.plus("?_summary=count") }
      .toList()
      .toMap()

  override suspend fun processResponse(response: Resource): Collection<Resource> {
    val patient = Patient().setMeta(Meta().setLastUpdated(Date()))
    return listOf(patient)
  }

}