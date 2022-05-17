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
class LoginViewConfiguration(
  override val appId: String = "",
  override val classification: String = "",
  val applicationName: String = "App Name",
  val applicationVersion: String = "0.0.1",
  val applicationVersionCode: Int = 1,
  val darkMode: Boolean = true,
  val showLogo: Boolean = false,
  val enablePin: Boolean = false
) : Configuration
/**
 * A function providing a DSL for configuring [LoginViewConfiguration]. The configurations provided
 * by this method are used on the register calling this method
 * @param appId Set unique identifier for this configuration
 * @param applicationName Set the application name
 * @param applicationVersion Set the application version
 * @param applicationVersionCode Set the application version code
 * @param darkMode Change login theme; alter the background color to white when false dark blue
 * otherwise
 * @param showLogo Show login logo for the app otherwise
 * @param enablePin provides PIN login feature
 */
@Stable
fun loginViewConfigurationOf(
  appId: String = "loginId",
  classification: String = "login",
  applicationName: String = "FHIR App",
  applicationVersion: String = "0.0.1",
  applicationVersionCode: Int = 1,
  darkMode: Boolean = false,
  showLogo: Boolean = false,
  enablePin: Boolean = false
) =
  LoginViewConfiguration(
    appId = appId,
    classification = classification,
    applicationName = applicationName,
    applicationVersion = applicationVersion,
    applicationVersionCode = applicationVersionCode,
    darkMode = darkMode,
    showLogo = showLogo,
    enablePin = enablePin
  )
