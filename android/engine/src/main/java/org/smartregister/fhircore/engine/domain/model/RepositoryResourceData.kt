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

package org.smartregister.fhircore.engine.domain.model

import androidx.compose.runtime.Stable
import java.util.LinkedList
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType

/**
 * This represent the outcome of a query performed via the Repository. The query performed can
 * either return a count which is wrapped in the [RepositoryResourceData.Count] data class or return
 * a list of [Resource]'s (with optional nested [Resource]'s) wrapped in
 * [RepositoryResourceData.Search] data class. [RepositoryResourceData.Search] has an optional
 * property [RepositoryResourceData.Search.rulesFactsMapId] that can be used as the key in the rules
 * factory facts map (each fact is represented as a key-value pair). The key for the
 * [RepositoryResourceData.Search.relatedResources] will either be the configured unique id for
 * representing the resource(s) in Rules engine Facts map or the [ResourceType]
 */
@Stable
sealed class RepositoryResourceData {
  data class Search(
    val rulesFactsMapId: String? = null,
    val resource: Resource,
    val relatedResources: Map<String, LinkedList<RepositoryResourceData>> = mutableMapOf()
  ) : RepositoryResourceData()
  data class Count(val resourceType: ResourceType, val relatedResourceCount: RelatedResourceCount) :
    RepositoryResourceData()
}

/**
 * This model represent a count result for [RepositoryResourceData]. The [parentResourceId] refers
 * to the id of the parent resource that we are interested in counting it's related resources.
 *
 * Example: Count all Task resources for a Patient identified by 'abcxyz'. The response will be
 * represented as RelatedResourceCount(relatedResourceId = "abcxyz", count = 0)
 */
data class RelatedResourceCount(val parentResourceId: String, val count: Long = 0L)
