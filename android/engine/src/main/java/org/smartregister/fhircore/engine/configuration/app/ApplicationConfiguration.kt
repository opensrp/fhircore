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
import org.smartregister.fhircore.engine.configuration.Configuration
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry

@Serializable
data class ApplicationConfiguration(
  override val appId: String = "",
  override val classification: String,
  var theme: String = "",
  var languages: List<String> = listOf("en"),
  var syncInterval: Long = 30,
  var scheduleDefaultPlanWorker: Boolean = true,
  var applicationName: String = "",
  var appLogoIconResourceFile: String = "ic_default_logo",
  var count: String = ConfigurationRegistry.DEFAULT_COUNT,
  var deviceToDeviceSync: DeviceToDeviceSyncConfiguration? = null
) : Configuration

/**
 * A function providing a DSL for configuring [ApplicationConfiguration] used in a FHIR application
 *
 * @param appId Set unique identifier for the app
 * @param classification Set the
 * @param languages Sets the languages for the app
 * @param syncInterval Sets the periodic sync interval in seconds. Default 30.
 * @param applicationName Sets the application display name
 * @param appLogoIconResourceFile Sets the application logo thumb icon, this must be png file inside
 * @param count Sets the application maximum records when downloading resource drawable folder
 */
fun applicationConfigurationOf(
  appId: String = "",
  classification: String = "",
  theme: String = "",
  languages: List<String> = listOf("en"),
  syncInterval: Long = 30,
  scheduleDefaultPlanWorker: Boolean = true,
  applicationName: String = "",
  appLogoIconResourceFile: String = "",
  count: String = ConfigurationRegistry.DEFAULT_COUNT
): ApplicationConfiguration =
  ApplicationConfiguration(
    appId = appId,
    classification = classification,
    theme = theme,
    languages = languages,
    syncInterval = syncInterval,
    scheduleDefaultPlanWorker = scheduleDefaultPlanWorker,
    applicationName = applicationName,
    appLogoIconResourceFile = appLogoIconResourceFile,
    count = count
  )
