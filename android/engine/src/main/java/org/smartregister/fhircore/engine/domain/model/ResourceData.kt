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

import java.util.LinkedList
import org.hl7.fhir.r4.model.Resource

/**
 * Represent the resource types that are used on a Register.
 * @property baseResource is the main resource used on the register
 * @property relatedResourcesMap are the other/extra resources accompanying the [baseResource]. For
 * each [baseResource] return associated [relatedResourcesMap].
 * @property computedValuesMap Contains data extracted from the resources to be used on the UI
 *
 * For example. For every Patient resource we return also their Immunization and Observation
 * resources
 */
data class ResourceData(
  val baseResource: Resource,
  val relatedResourcesMap: Map<String, List<Resource>> = emptyMap(),
  val computedValuesMap: Map<String, Any> = emptyMap()
)

/**
 * @property resource A valid FHIR resource
 * @property relatedResources Nested list of [RelatedResourceData]
 */
data class RelatedResourceData(
  val resource: Resource,
  val relatedResources: LinkedList<RelatedResourceData> = LinkedList()
)
