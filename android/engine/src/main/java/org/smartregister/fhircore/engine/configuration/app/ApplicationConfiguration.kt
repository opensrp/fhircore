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
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.Configuration
import org.smartregister.fhircore.engine.domain.model.Code
import org.smartregister.fhircore.engine.sync.SyncStrategy
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.model.practitioner.KeycloakUserDetails

@Serializable
data class ApplicationConfiguration(
  override var appId: String,
  override var configType: String = ConfigType.Application.name,
  val theme: String = "",
  val appTitle: String = "",
  val remoteSyncPageSize: Int = 100,
  val languages: List<String> = listOf("en"),
  val useDarkTheme: Boolean = false,
  val syncInterval: Int = 30,
  val syncStrategy: List<Code> = listOf(),
  val loginConfig: LoginConfig = LoginConfig(),
  val deviceToDeviceSync: DeviceToDeviceSyncConfig? = null
) : Configuration() {

  fun getMandatoryTags(sharedPreferencesHelper: SharedPreferencesHelper): List<Code> {
    val tags = mutableListOf<Code>()
    syncStrategy.forEach { coding ->
      when (coding.display) {
        SyncStrategy.CARE_TEAM.value,
        SyncStrategy.ORGANIZATION.value,
        SyncStrategy.LOCATION.value -> {
          sharedPreferencesHelper.read<List<String>>(coding.display!!)?.forEach { id ->
            tags.add(coding.apply { code = id }.copy())
          }
        }
        SyncStrategy.PRACTITIONER.value -> {
          sharedPreferencesHelper.read<KeycloakUserDetails>(coding.display!!)?.let { practitioner ->
            tags.add(coding.apply { code = practitioner.id })
          }
        }
      }
    }
    return tags
  }
}
