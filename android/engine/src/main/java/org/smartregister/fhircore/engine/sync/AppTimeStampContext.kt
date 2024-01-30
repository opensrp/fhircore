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

import com.google.android.fhir.sync.download.ResourceParamsBasedDownloadWorkManager
import javax.inject.Inject
import javax.inject.Singleton
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.datastore.TimeStampDataStore

@Singleton
class AppTimeStampContext @Inject constructor(private val dataStore: TimeStampDataStore) :
  ResourceParamsBasedDownloadWorkManager.TimestampContext {

  override suspend fun getLasUpdateTimestamp(resourceType: ResourceType): String? {
    return dataStore.readOnce(resourceType)
  }

  // TODO: KELVIN ask Elly why nullable string is allowed. if no timestamp, why save?
  override suspend fun saveLastUpdatedTimestamp(resourceType: ResourceType, timestamp: String?) {
    dataStore.write(resourceType, timestamp ?: "")
  }
}
