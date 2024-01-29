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

package org.smartregister.fhircore.engine.domain.model

import kotlinx.serialization.Serializable
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.data.remote.model.response.LocationHierarchyInfo
import org.smartregister.fhircore.engine.data.remote.model.response.UserInfo


// TODO: KELVIN rename to practitionerdatastore
@Serializable
data class PractitionerDataStore(
  val careTeamIds: List<String>? = null,
  val careTeamNames: List<String>? = null,
  val locationIds: List<String>? = null,
  val locationNames: List<String>? = null,
  val locationHierarchies: List<LocationHierarchyInfo>? = null,
  val organizationIds: List<String>? = null,
  val organizationNames: List<String>? = null,
  val remoteSyncResources: List<ResourceType>? = null,
  val userInfo: UserInfo? = null,
)
