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

package org.smartregister.fhircore.quest.configuration.view

import androidx.compose.runtime.Stable
import androidx.ui.core.Direction
import kotlinx.serialization.Serializable
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.Configuration

@Stable
@Serializable
class PatientRegisterRowViewConfiguration(
  override val appId: String,
  override val classification: String,
  val filters: List<Filter>? = null
) : Configuration

@Stable
@Serializable
data class Filter(
  val resourceType: Enumerations.ResourceType,
  val key: String,
  val valuePrefix: String? = null,
  val label: String? = null,
  val valueType: Enumerations.DataType,
  val valueCoding: Code?,
  val valueString: String? = null,
  val dynamicColors: List<DynamicColor>? = null,
  val properties: Properties? = null
)

@Stable
@Serializable
data class Code(val system: String? = null, val code: String? = null, val display: String? = null)

@Stable
@Serializable
data class Properties(
  val label: Property? = null,
  val value: Property? = null,
  val valueFormatter: Map<String, String>? = null,
  val labelDirection: Direction = Direction.LEFT
)

@Stable @Serializable data class Property(val color: String? = null, val textSize: Int? = null)

@Stable @Serializable data class DynamicColor(val valueEqual: String, val useColor: String)

/**
 * @param appId Set unique identifier for this configuration
 * @param classification set the classification
 * @param showG6pdStatus enable of disable the status
 */
@Stable
fun patientRegisterRowViewConfigurationOf(
  appId: String = "quest",
  classification: String = "patient_list_row"
) = PatientRegisterRowViewConfiguration(appId = appId, classification = classification)
