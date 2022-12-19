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

import android.os.Parcelable
import com.google.android.fhir.search.Order
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * Represents FHIR resources used on the register. The [baseResource] is the main resource used
 * which can be accompanied by [relatedResources].
 */
@Serializable
@Parcelize
data class FhirResourceConfig(
  val baseResource: ResourceConfig,
  val relatedResources: List<ResourceConfig> = emptyList()
) : Parcelable

/**
 * This is the data class used to hold configurations for FHIR resources used on the register. The
 * type of FHIR resource is represented by the property [resource]. [name] property is used as the
 * variable name in the RulesFactory engine fact's map.An optional [searchParameter] that is used to
 * query the related resource. The [dataQueries] are optional configurations used to filter data.
 * [fhirPathExpression] is used when you want to extract data from a FHIR resource with nested
 * reference, e.g. a Group or Composition resource .
 *
 * Examples of valid expressions for [fhirPathExpression] property:
 * 1. "Group.member.entity" - extract members from Group resource as References
 * 2. "Composition.section.focus" - extract section objects from Composition resource as Reference
 *
 * Nested [ResourceConfig] are proved via the [relatedResources] property.
 */
@Serializable
@Parcelize
data class ResourceConfig(
  val name: String? = null,
  val resource: String,
  val searchParameter: String? = null,
  val fhirPathExpression: String? = null,
  val dataQueries: List<DataQuery>? = null,
  val relatedResources: List<ResourceConfig> = emptyList(),
  val sortConfigs: List<SortConfig> = emptyList()
) : Parcelable

@Serializable
@Parcelize
data class SortConfig(
  val paramName: String,
  val dataType: DataType,
  val order: Order = Order.ASCENDING
) : Parcelable
