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

package org.smartregister.fhircore.engine.configuration.register

import kotlinx.serialization.Serializable
import org.smartregister.fhircore.engine.domain.model.DataQuery

/**
 * Represents FHIR resources used on the register. The [baseResource] is the main resource used
 * which can be accompanied by [relatedResources]
 */
@Serializable
data class FhirResourceConfig(
  val baseResource: ResourceConfig,
  val relatedResources: List<ResourceConfig> = emptyList()
)

/**
 * Defines the name of the [resource], an optional [searchParameter] that is useful for querying
 * related resources. The [dataQueries] are optional configurations used to filter data.
 */
@Serializable
data class ResourceConfig(
  val resource: String,
  val searchParameter: String? = null,
  val dataQueries: List<DataQuery>? = null
)
