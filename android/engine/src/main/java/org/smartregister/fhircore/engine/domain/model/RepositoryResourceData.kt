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

import androidx.compose.runtime.Stable
import java.util.concurrent.ConcurrentHashMap
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType

/**
 * This represent the outcome of a query performed via the Repository. The query performed can
 * either return a count or map of [Resource]'s (including nested resources flattened in the map).
 * The optional property [resourceConfigId] can be used as the key in the rules factory facts map
 * (each fact is represented as a key-value pair). The key for the [relatedResourcesMap] will either
 * be the configured unique id for representing the resource(s) in Rules engine Facts map or the
 * [ResourceType]. [secondaryRepositoryResourceData] returns a list of independent resources (which
 * may include nested resource(s)) that have NO relationship with the base [resource].
 */
@Stable
data class RepositoryResourceData(
  val resourceConfigId: String? = null,
  val resource: Resource,
  val relatedResourcesMap: ConcurrentHashMap<String, List<Resource>> = ConcurrentHashMap(),
  val relatedResourcesCountMap: ConcurrentHashMap<String, List<RelatedResourceCount>> =
    ConcurrentHashMap(),
  var secondaryRepositoryResourceData: List<RepositoryResourceData>? = null,
)

/**
 * This model represent a count result for [RepositoryResourceData]. The [parentResourceId] refers
 * to the id of the parent resource that we are interested in counting it's related resources.
 *
 * Example: Count all Task resources for a Patient identified by 'abcxyz'. The response will be
 * represented as RelatedResourceCount(relatedResourceType= 'Task', relatedResourceId = "abcxyz",
 * count = 0)
 */
@Stable
data class RelatedResourceCount(
  val relatedResourceType: ResourceType? = null,
  val parentResourceId: String? = null,
  val count: Long,
)
