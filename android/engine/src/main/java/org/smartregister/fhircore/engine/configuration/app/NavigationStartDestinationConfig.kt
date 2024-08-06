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

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import org.smartregister.fhircore.engine.domain.model.LauncherType

/**
 * This class configures the initial screen the application will be directed to upon launch. The
 * application currently supports [LauncherType.REGISTER] and [LauncherType.MAP] screen as the entry
 * point. This config defaults to launching a register with the rest of the properties obtained from
 * the first NavigationItemConfig of the NavigationConfiguration
 */
@Serializable
@Parcelize
data class NavigationStartDestinationConfig(
  val id: String? = null,
  val screenTitle: String? = null,
  val launcherType: LauncherType = LauncherType.REGISTER,
) : Parcelable
