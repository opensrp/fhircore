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

package org.smartregister.fhircore.engine.configuration.app

import kotlinx.serialization.Serializable
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.Configuration
import org.smartregister.fhircore.engine.configuration.event.EventWorkflow
import org.smartregister.fhircore.engine.domain.model.LauncherType
import org.smartregister.fhircore.engine.util.extension.DEFAULT_FORMAT_SDF_DD_MM_YYYY

@Serializable
data class ApplicationConfiguration(
  override var appId: String,
  override var configType: String = ConfigType.Application.name,
  val appTitle: String = "",
  val remoteSyncPageSize: Int = 100,
  val languages: List<String> = listOf("en"),
  val useDarkTheme: Boolean = false,
  val syncInterval: Long = 15,
  val syncStrategy: List<SyncStrategy> = listOf(),
  val loginConfig: LoginConfig = LoginConfig(),
  val deviceToDeviceSync: DeviceToDeviceSyncConfig? = null,
  val snackBarTheme: SnackBarThemeConfig = SnackBarThemeConfig(),
  val reportRepeatTime: String = "",
  val taskStatusUpdateJobDuration: String = "PT15M",
  val taskExpireJobDuration: String = "PT30M",
  val taskCompleteCarePlanJobDuration: String = "PT60M",
  val showLogo: Boolean = true,
  val taskBackgroundWorkerBatchSize: Int = 500,
  val eventWorkflows: List<EventWorkflow> = emptyList(),
  val settingsScreenMenuOptions: List<SettingsOptions> =
    listOf(
      SettingsOptions.MANUAL_SYNC,
      SettingsOptions.SWITCH_LANGUAGES,
      SettingsOptions.RESET_DATA,
      SettingsOptions.INSIGHTS,
    ),
  val logGpsLocation: List<LocationLogOptions> = emptyList(),
  val usePractitionerAssignedLocationOnSync: Boolean = true,
  val navigationStartDestination: NavigationStartDestinationConfig =
    NavigationStartDestinationConfig(
      launcherType = LauncherType.REGISTER,
      id = null,
    ),
  val codingSystems: List<CodingSystemConfig> = emptyList(),
  var dateFormat: String = DEFAULT_FORMAT_SDF_DD_MM_YYYY,
) : Configuration()

enum class SyncStrategy {
  Location,
  CareTeam,
  RelatedEntityLocation,
  Organization,
  Practitioner,
}

enum class LocationLogOptions {
  QUESTIONNAIRE,
  CALCULATE_DISTANCE_RULE_EXECUTOR,
}

enum class SettingsOptions {
  MANUAL_SYNC,
  OFFLINE_MAPS,
  SWITCH_LANGUAGES,
  RESET_DATA,
  INSIGHTS,
  CONTACT_HELP,
}
