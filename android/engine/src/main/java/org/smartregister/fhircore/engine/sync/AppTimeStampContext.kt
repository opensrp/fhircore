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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.datastore.PreferenceDataStore

@Singleton
class AppTimeStampContext
@Inject
constructor(private val preferenceDatastore: PreferenceDataStore) :
  ResourceParamsBasedDownloadWorkManager.TimestampContext {

  override suspend fun getLasUpdateTimestamp(resourceType: ResourceType): String? {
    return preferenceDatastore.read(PreferenceDataStore.resourceTimestampKey(resourceType)).firstOrNull()
  }

  override suspend fun saveLastUpdatedTimestamp(resourceType: ResourceType, timestamp: String?) {
    if (timestamp != null) {
      preferenceDatastore.write(PreferenceDataStore.resourceTimestampKey(resourceType), timestamp)
    }
  }

}
