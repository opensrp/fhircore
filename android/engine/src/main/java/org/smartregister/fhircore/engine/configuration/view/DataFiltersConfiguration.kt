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

package org.smartregister.fhircore.engine.configuration.view

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Extension
import org.smartregister.fhircore.engine.configuration.Configuration

const val INIT_PERSION_EXPRESSION_EXTENSION_URL =
  "http://hl7.org/fhir/StructureDefinition/cqf-initiatingPerson"
const val CQF_EXPRESSION_EXTENSION_URL = "http://hl7.org/fhir/StructureDefinition/cqf-expression"

fun List<Extension>.expressionExtension() =
  this.find { it.url!!.contentEquals(CQF_EXPRESSION_EXTENSION_URL) }

@Stable
@Serializable
class DataFiltersConfiguration(
  override val appId: String = "",
  override val classification: String = "",
  val filters: List<SearchFilter> = listOf()
) : Configuration

@Stable
@Serializable
/** Only TokenClientParam, and StringClientParam supported as Register Primary Filter. */
data class SearchFilter(
  val id: String = "",
  val key: String,
  val valueBoolean: Boolean? = null,
  val valueCoding: Code? = null,
  val valueReference: String? = null,
  val valueString: String? = null
)

@Stable
@Serializable
data class Code(val system: String? = null, val code: String? = null, val display: String? = null)

fun Code.asCoding() = Coding(this.system, this.code, this.display)

fun Coding.asCode() = Code(this.system, this.code, this.display)

fun Code.asCodeableConcept() =
  CodeableConcept().apply {
    addCoding(this@asCodeableConcept.asCoding())
    text = this@asCodeableConcept.display
  }

@Stable
fun dataFilterConfigurationOf(
  appId: String = "",
  classification: String = "form",
  filters: List<SearchFilter> = listOf()
) = DataFiltersConfiguration(appId = appId, classification = classification, filters = filters)
