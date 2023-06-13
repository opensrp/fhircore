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

import android.os.Parcelable
import com.google.android.fhir.search.Order
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import kotlinx.serialization.Serializable
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.ResourceType

/**
 * Represents FHIR resources used on the register. The [baseResource] is the main resource used
 * which can be accompanied by [relatedResources].
 */
@Serializable
@Parcelize
data class FhirResourceConfig(
  val baseResource: ResourceConfig,
  val relatedResources: List<ResourceConfig> = emptyList()
) : Parcelable, java.io.Serializable

/**
 * This is the data class used to hold configurations for FHIR resources used in Profile and
 * Registers. The property [resource] represents the [ResourceType]. [id] is a unique name used as
 * key in the rules engine fact's map. [resultAsCount] property is used to indicate whether to
 * perform count SQL query or not. Count queries return a count whereas search queries return the
 * result of SQL SELECT statement.
 *
 * Data filtering: [dataQueries] are used to apply conditions for filtering data in the WHERE clause
 * of the SQL statement run against the SQLite database. The data queries follow the pattern
 * provided by the Search DSL. [NestedSearchConfig] can be used to apply conditional filter against
 * other resources. Example: Retrieve all Patients with Condition Diabetes; result will be a list of
 * Patient (Condition resources will NOT be included in the returned results)
 *
 * Sorting: Some resource properties support sorting. To configure how to sort the data [SortConfig]
 * can be used.
 *
 * [isRevInclude] property is a required configuration (default: true) needed to determine whether
 * to use forward or reverse include operations of the Search DSL to include the configured resource
 * in the final result of the query.
 *
 * If [isRevInclude] is set to `false`, the configured resource will be retrieved via the referenced
 * property on the parent resource, provided via [searchParameter]. Usage example: Retrieve all
 * Group as well as the Patient resources referenced in the 'Group.member' property.
 *
 * If [isRevInclude] is set to `true`, the database will query for any of the configured resources
 * that include the parent resource in their references (the reverse of forward include). Usage
 * example: Retrieve all Patients including their Immunizations, Observation etc in the result.
 *
 * Both reverse and forward include require [searchParameter] which refers to the name of the
 * property or search parameter used to reference other resources.
 *
 * A [ResourceConfig] can have nested list of other [ResourceConfig] configured via
 * [relatedResources] property.
 *
 * [CountResultConfig] is used to configure how to compute the total counts returned. If
 * [CountResultConfig.sumCounts] is set to true, all the related resources counts are computed once
 * via one query. However there may be scenarios to return count for each related resource e.g. for
 * every Patient in a Group, return their Tasks count.
 */
@Serializable
@Parcelize
data class ResourceConfig(
  val id: String? = null,
  val resource: ResourceType,
  val searchParameter: String? = null,
  val isRevInclude: Boolean = true,
  val dataQueries: List<DataQuery>? = null,
  val relatedResources: List<ResourceConfig> = emptyList(),
  val sortConfigs: List<SortConfig> = emptyList(),
  val resultAsCount: Boolean = false,
  val countResultConfig: CountResultConfig? = CountResultConfig(),
  val nestedSearchResources: List<NestedSearchConfig>? = null,
  val configRules: @RawValue List<RuleConfig>? = emptyList(),
  val planDefinitions: List<String>? = null,
  val attributesToUpdate: List<KeyValueConfig>? = emptyList()
) : Parcelable, java.io.Serializable

@Serializable
@Parcelize
data class CountResultConfig(val sumCounts: Boolean = true) : Parcelable, java.io.Serializable

@Serializable
@Parcelize
data class SortConfig(
  val paramName: String? = null,
  val dataType: Enumerations.DataType,
  val order: Order = Order.ASCENDING,
  val fhirPathExpression: String = ""
) : Parcelable

@Serializable
@Parcelize
data class NestedSearchConfig(
  val resourceType: ResourceType,
  val referenceParam: String,
  val dataQueries: List<DataQuery>? = null
) : Parcelable
