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

package org.smartregister.fhircore.engine.configuration.view

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import org.smartregister.fhircore.engine.configuration.Configuration

@Stable
@Serializable
class PinViewConfiguration(
  override val appId: String = "",
  override val classification: String = "",
  val applicationName: String = "",
  val appLogoIconResourceFile: String = "",
  val enablePin: Boolean = false,
  val showLogo: Boolean = true
) : Configuration
/**
 * A function providing a DSL for configuring [PinViewConfiguration]. The configurations provided by
 * this method are used on the register calling this method
 * @param appId Set unique identifier for this configuration
 * @param applicationName Set the application name
 * @param appLogoIconResourceFile Sets the application logo thumb icon, this must be png file inside
 * drawable folder
 * @param enablePin provides PIN login feature
 * @param showLogo Show login logo for the app otherwise
 */
@Stable
fun pinViewConfigurationOf(
  appId: String = "appId",
  classification: String = "pin",
  applicationName: String = "FHIR Engine App",
  appLogoIconResourceFile: String = "ic_launcher",
  enablePin: Boolean = false,
  showLogo: Boolean = true,
) =
  PinViewConfiguration(
    appId = appId,
    classification = classification,
    applicationName = applicationName,
    appLogoIconResourceFile = appLogoIconResourceFile,
    showLogo = showLogo,
    enablePin = enablePin
  )
