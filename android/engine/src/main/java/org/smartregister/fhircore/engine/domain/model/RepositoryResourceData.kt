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
 * @property id A unique name retrieved from the configs used to identify the data represented by
 * this data class; more like a variable name used to access the object in rules engine facts map
 * @property queryResult The response returned from the database query. The response can either be a
 * [QueryResult.Search] or [QueryResult.Count]. [QueryResult.Search] includes a [Resource] and
 * optionally a list of related [RepositoryResourceData], typically this result is produced when a
 * SELECT query returns FHIR [Resource]'s. [QueryResult.Count] represents the count for the
 * configured resources
 */
@Stable
data class RepositoryResourceData(val id: String? = null, val queryResult: QueryResult) {
  sealed class QueryResult {
    data class Search(
      val resource: Resource,
      val relatedResources: LinkedList<RepositoryResourceData> = LinkedList()
    ) : QueryResult()
    data class Count(val resourceType: ResourceType, val count: Long) : QueryResult()
  }
}
