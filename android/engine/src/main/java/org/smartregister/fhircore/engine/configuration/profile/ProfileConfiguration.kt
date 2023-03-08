/*
 * Copyright 2021-2023 Ona Systems, Inc
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

package org.smartregister.fhircore.engine.configuration.profile

import kotlinx.serialization.Serializable
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.Configuration
import org.smartregister.fhircore.engine.configuration.navigation.NavigationMenuConfig
import org.smartregister.fhircore.engine.configuration.view.ViewProperties
import org.smartregister.fhircore.engine.domain.model.FhirResourceConfig
import org.smartregister.fhircore.engine.domain.model.OverflowMenuItemConfig
import org.smartregister.fhircore.engine.domain.model.RuleConfig
import org.smartregister.fhircore.engine.domain.model.TopBarConfig
import org.smartregister.fhircore.engine.util.annotation.ExcludeFromJacocoGeneratedReport

@ExcludeFromJacocoGeneratedReport
@Serializable
data class ProfileConfiguration(
  override var appId: String,
  override var configType: String = ConfigType.Profile.name,
  val id: String,
  val fhirResource: FhirResourceConfig,
  val secondaryResources: List<FhirResourceConfig>? = null,
  val managingEntity: ManagingEntityConfig? = null,
  val profileParams: List<String> = emptyList(),
  val rules: List<RuleConfig> = emptyList(),
  val topAppBar: TopBarConfig? = null,
  val views: List<ViewProperties> = emptyList(),
  val fabActions: List<NavigationMenuConfig> = emptyList(),
  val overFlowMenuItems: List<OverflowMenuItemConfig> = emptyList()
) : Configuration()
