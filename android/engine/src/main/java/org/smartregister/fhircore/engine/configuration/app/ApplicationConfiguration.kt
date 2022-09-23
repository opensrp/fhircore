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

import kotlinx.serialization.Serializable
import org.hl7.fhir.r4.model.Coding
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.Configuration
import org.smartregister.fhircore.engine.sync.SyncStrategy
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.extractLogicalIdUuid

@Serializable
data class ApplicationConfiguration(
  override var appId: String,
  override var configType: String = ConfigType.Application.name,
  val theme: String = "",
  val appTitle: String = "",
  val remoteSyncPageSize: Int = 100,
  val languages: List<String> = listOf("en"),
  val useDarkTheme: Boolean = false,
  val syncInterval: Long = 30,
  val syncStrategy: List<String> = listOf(),
  val loginConfig: LoginConfig = LoginConfig(),
  val deviceToDeviceSync: DeviceToDeviceSyncConfig? = null
) : Configuration() {

  fun getMandatoryTags(
    sharedPreferencesHelper: SharedPreferencesHelper,
    providedSyncStrategy: SyncStrategy
  ): List<Coding> {
    val tags = mutableListOf<Coding>()
    syncStrategy.forEach { strategy ->
      when (strategy) {
        providedSyncStrategy.careTeamTag.type -> {
          sharedPreferencesHelper.read<List<String>>(strategy)?.forEach { id ->
            providedSyncStrategy.careTeamTag.tag?.let { tags.add(it.copy().apply { code = id }) }
          }
        }
        providedSyncStrategy.locationTag.type -> {
          sharedPreferencesHelper.read<List<String>>(strategy)?.forEach { id ->
            providedSyncStrategy.locationTag.tag?.let { tags.add(it.copy().apply { code = id }) }
          }
        }
        providedSyncStrategy.organizationTag.type -> {
          sharedPreferencesHelper.read<List<String>>(strategy)?.forEach { id ->
            providedSyncStrategy.organizationTag.tag?.let {
              tags.add(it.copy().apply { code = id })
            }
          }
        }
        providedSyncStrategy.practitionerTag.type -> {
          sharedPreferencesHelper.read(SharedPreferenceKey.PRACTITIONER_ID.name, null)?.let { id ->
            providedSyncStrategy.practitionerTag.tag?.let {
              tags.add(it.copy().apply { code = id.extractLogicalIdUuid() })
            }
          }
        }
      }
    }
    return tags
  }
}
