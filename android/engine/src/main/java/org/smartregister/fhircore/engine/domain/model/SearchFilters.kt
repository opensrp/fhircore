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
import kotlinx.serialization.Serializable
import org.hl7.fhir.r4.model.Enumerations

@Serializable
/** Only TokenClientParam, and StringClientParam supported as Register Primary Filter. */
data class DataQuery(
  val id: String = "",
  val key: String,
  val filterType: Enumerations.SearchParamType,
  val valueType: Enumerations.DataType,
  val valueBoolean: Boolean? = null,
  val valueCoding: Code? = null,
  val valueString: String? = null
)

@Stable
@Serializable
data class Code(val system: String? = null, val code: String? = null, val display: String? = null)
