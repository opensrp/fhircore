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

package org.smartregister.fhircore.engine.sync

import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.ResourceType

/**
 * SyncStrategy defines whether to sync resource based on the IDs of CareTeam, Location,
 * Organization and Practitioner. Each SyncStrategy represents a meta tag that is used by all synced
 * resource.
 */
data class SyncStrategy(
  var careTeamTag: SyncStrategyTag = SyncStrategyTag(type = ResourceType.CareTeam.name),
  var locationTag: SyncStrategyTag = SyncStrategyTag(type = ResourceType.Location.name),
  var organizationTag: SyncStrategyTag = SyncStrategyTag(type = ResourceType.Organization.name),
  var practitionerTag: SyncStrategyTag = SyncStrategyTag(type = ResourceType.Practitioner.name)
)

data class SyncStrategyTag(val type: String, var tag: Coding? = null)
