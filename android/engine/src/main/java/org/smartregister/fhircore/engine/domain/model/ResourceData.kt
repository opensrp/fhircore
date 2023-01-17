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
import java.util.LinkedList
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType

/**
 * Represent the resource types that are used on a Register.
 * @property baseResourceId is the unique identifier for the main resource in the register
 * @property baseResourceType is the [ResourceType] for the main resource
 * @property computedValuesMap contains data extracted from the resources to be used on the UI
 * @property listResourceDataMap a map containing the pre-computed values for LIST views used
 *
 * For example. For every Patient resource we return also their Immunization and Observation
 * resources but precompute the values needed by firing the configured rules.
 */
@Stable
data class ResourceData(
  val baseResourceId: String,
  val baseResourceType: ResourceType,
  val computedValuesMap: Map<String, Any>,
  val listResourceDataMap: Map<String, List<ResourceData>>
)

/**
 * @property resource A valid FHIR resource
 * @property relatedResources Nested list of [RelatedResourceData]
 */
@Stable
data class RelatedResourceData(
  val resource: Resource,
  val relatedResources: LinkedList<RelatedResourceData> = LinkedList(),
  val resourceConfigId: String? = null
)