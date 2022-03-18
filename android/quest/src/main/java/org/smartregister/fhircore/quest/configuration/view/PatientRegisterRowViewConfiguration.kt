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
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Enumerations
import org.smartregister.fhircore.engine.configuration.Configuration

@Stable
@Serializable
class PatientRegisterRowViewConfiguration(
  override val appId: String = "",
  override val classification: String = "",
  val filters: List<Filter>? = null
) : Configuration

@Stable
@Serializable
data class Filter(
  val resourceType: Enumerations.ResourceType,
  val key: String,
  val displayableProperty: String = key,
  val valuePrefix: String? = null,
  val valuePostfix: String? = null,
  val label: String? = null,
  val valueType: Enumerations.DataType,
  val valueCoding: Code? = null,
  val valueString: String? = null,
  val dynamicColors: List<DynamicColor>? = null,
  val properties: Properties? = null
)

@Stable
@Serializable
data class QuestionnaireItemFilter(
  val key: String,
  val label: String? = null,
  val index: Int? = null,
  val dynamicColors: List<DynamicColor>? = null,
  val properties: Properties? = null
)

@Stable
@Serializable
data class Code(val system: String? = null, val code: String? = null, val display: String? = null)

fun Code.isSimilar(coding: Coding) = this.code == coding.code && this.system == coding.system

fun Code.isSimilar(concept: CodeableConcept) =
  concept.coding.any { this.code == it.code && this.system == it.system }

@Stable
@Serializable
data class Properties(
  val label: Property? = null,
  val value: Property? = null,
  val valueFormatter: Map<String, String>? = null,
  val labelDirection: Direction = Direction.LEFT
)

@Stable
@Serializable
data class Property(
  val color: String? = null,
  val textSize: Int? = null,
  val fontWeight: FontWeight? = FontWeight.NORMAL
)

@Stable @Serializable data class DynamicColor(val valueEqual: String, val useColor: String)

enum class FontWeight {
  LIGHT(300),
  NORMAL(400),
  BOLD(700);

  val weight: Int

  constructor(weight: Int) {
    this.weight = weight
  }
}
