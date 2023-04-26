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

import com.google.android.fhir.sync.download.ResourceParamsBasedDownloadWorkManager
import javax.inject.Inject
import javax.inject.Singleton
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper

@Singleton
class AppTimeStampContext
@Inject
constructor(private val sharedPreference: SharedPreferencesHelper) :
  ResourceParamsBasedDownloadWorkManager.TimestampContext {

  override suspend fun getLasUpdateTimestamp(resourceType: ResourceType): String? {
    return sharedPreference.read(timestampKey(resourceType), null)
  }

  override suspend fun saveLastUpdatedTimestamp(resourceType: ResourceType, timestamp: String?) {
    sharedPreference.write(timestampKey(resourceType), timestamp)
  }

  private fun timestampKey(resourceType: ResourceType) =
    "${resourceType.name.uppercase()}_${SharedPreferenceKey.LAST_SYNC_TIMESTAMP.name}"
}
