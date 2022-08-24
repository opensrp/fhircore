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

package org.smartregister.fhircore.engine.configuration

/**
 * Types of configurations supported
 * @property name A unique name for configuration type. Typically provided in camelCase.
 * @property parseAsResource Instructs that this configuration should be parsed into a FHIR resource
 * (for configurations saved as Binary resource but can be directly converted to a eligible FHIR
 * resource); generally a custom hard coded is used.
 * @property multiConfig Denotes that we can have multiple instances of this type of config.
 */
sealed class ConfigType(
  val name: String,
  val parseAsResource: Boolean = false,
  val multiConfig: Boolean = false
) {
  object Application : ConfigType("application")
  object Sync : ConfigType("sync", true)
  object Navigation : ConfigType("navigation")
  object Register : ConfigType("register", multiConfig = true)
  object MeasureReport : ConfigType("measureReport")
  object Profile : ConfigType("profile", multiConfig = true)
}
