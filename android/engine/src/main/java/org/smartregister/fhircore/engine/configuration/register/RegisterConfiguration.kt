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

package org.smartregister.fhircore.engine.configuration.register

import kotlinx.serialization.Serializable
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.Configuration
import org.smartregister.fhircore.engine.configuration.navigation.NavigationMenuConfig
import org.smartregister.fhircore.engine.configuration.workflow.ActionTrigger
import org.smartregister.fhircore.engine.domain.model.ActionConfig
import org.smartregister.fhircore.engine.domain.model.FhirResourceConfig
import org.smartregister.fhircore.engine.domain.model.RuleConfig
import org.smartregister.fhircore.engine.domain.model.TopScreenSectionConfig

@Serializable
data class RegisterConfiguration(
  override var appId: String,
  override var configType: String = ConfigType.Register.name,
  val id: String,
  val registerTitle: String? = null,
  val fhirResource: FhirResourceConfig,
  val secondaryResources: List<FhirResourceConfig>? = null,
  val searchBar: RegisterContentConfig? = null,
  val registerCard: RegisterCardConfig = RegisterCardConfig(),
  val fabActions: List<NavigationMenuConfig> = emptyList(),
  val noResults: NoResultsConfig? = null,
  val pageSize: Int = 10,
  val activeResourceFilters: List<ActiveResourceFilterConfig> =
    listOf(
      ActiveResourceFilterConfig(resourceType = ResourceType.Patient, active = true),
      ActiveResourceFilterConfig(resourceType = ResourceType.Group, active = true),
    ),
  val configRules: List<RuleConfig>? = null,
  val registerFilter: RegisterFilterConfig? = null,
  val filterDataByRelatedEntityLocation: Boolean = false,
  val topScreenSection: TopScreenSectionConfig? = null,
  val onSearchByQrSingleResultActions: List<ActionConfig>? = null,
  val infiniteScroll: Boolean = false,
) : Configuration() {
  val onSearchByQrSingleResultValidActions =
    onSearchByQrSingleResultActions?.filter { it.trigger == ActionTrigger.ON_SEARCH_SINGLE_RESULT }

  val showSearchByQrCode =
    !onSearchByQrSingleResultValidActions.isNullOrEmpty() || searchBar?.searchByQrCode == true
}
