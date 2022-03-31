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

package org.smartregister.fhircore.engine.domain.model

import androidx.compose.runtime.Stable
import org.smartregister.fhircore.engine.appfeature.model.HealthModule

/**
 * @property appFeatureName Name of the feature
 * @property healthModule Optional healthModule property
 * @property iconResource Android drawable resource used as icon for menu option
 * @property titleResource Android translatable string resource used as the menu option title
 * @property count The current count for the menu item. Default is 0
 * @property showCount Show clients count against the menu option queries for resources other than
 * Patient
 */
@Stable
data class SideMenuOption(
  val appFeatureName: String,
  val healthModule: HealthModule = HealthModule.DEFAULT,
  val iconResource: Int,
  val titleResource: Int,
  val count: Long = 0,
  val showCount: Boolean = true,
)
