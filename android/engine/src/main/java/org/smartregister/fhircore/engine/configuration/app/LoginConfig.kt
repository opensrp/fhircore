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

@Serializable
data class LoginConfig(
  val showLogo: Boolean = true,
  val enablePin: Boolean? = false,
  val pinLength: Int = 4,
  val pinLoginMessage: String? = null,
  val logoHeight: Int = 120,
  val logoWidth: Int = 140,
  val showAppTitle: Boolean = true,
  var supervisorContactNumber: String? = "",
  var countryCode: String? = "",
)
