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
import org.smartregister.fhircore.engine.configuration.Configuration

@Stable
class LoginViewConfiguration(
  val showPowered: Boolean = true,
  val applicationName: String = "App Name",
  val applicationVersion: String = "0.0.1"
) : Configuration
/**
 * A function providing a DSL for configuring [LoginViewConfiguration]. The configurations provided
 * by this method are used on the register calling this method
 * @param showPowered Show or hide the powered by section
 * @param applicationName Set the application name
 * @param applicationVersion Set the application version
 */
@Stable
fun loginViewConfigurationOf(
  showPowered: Boolean = true,
  applicationName: String = "App Name",
  applicationVersion: String = "0.0.1"
) =
  LoginViewConfiguration(
    showPowered = showPowered,
    applicationName = applicationName,
    applicationVersion = applicationVersion
  )
