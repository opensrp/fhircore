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

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * @property resourceConfig The configuration for FHIR resource to be loaded
 * @property parentIdFhirPathExpression FhirPath expression for extracting the ID for the parent
 *   resource
 * @property contentFhirPathExpression FhirPath expression for extracting the content displayed on
 *   the multi select widget e.g. the name of the Location in a Location hierarchy
 * @property rootNodeFhirPathExpression A key value pair containing a FHIRPath expression for
 *   extracting the value used to identify if the current resource is Root. The key is the FHIRPath
 *   expression while value is the content to compare against.
 * @property viewActions The actions to be performed when the multiselect action button is pressed
 * @property mutuallyExclusive Setup the multi choice checkbox such that only a single (root level)
 *   selection can be performed at a time.
 */
@Serializable
@Parcelize
data class MultiSelectViewConfig(
  val resourceConfig: FhirResourceConfig,
  val parentIdFhirPathExpression: String,
  val contentFhirPathExpression: String,
  val rootNodeFhirPathExpression: KeyValueConfig,
  val viewActions: List<MultiSelectViewAction> = listOf(MultiSelectViewAction.FILTER_DATA),
  val mutuallyExclusive: Boolean = true,
) : java.io.Serializable, Parcelable

enum class MultiSelectViewAction {
  SYNC_DATA,
  FILTER_DATA,
}
