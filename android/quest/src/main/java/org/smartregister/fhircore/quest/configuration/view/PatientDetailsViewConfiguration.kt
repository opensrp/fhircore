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
import kotlinx.serialization.Serializable
import org.smartregister.fhircore.engine.configuration.Configuration
import org.smartregister.fhircore.quest.data.patient.model.AdditionalData

@Stable
@Serializable
data class PatientDetailsViewConfiguration(
  override val appId: String,
  override val classification: String,
  val contentTitle: String = "Responses",
  val valuePrefix: String = "G6PD ",
  val dynamicRows: List<List<Filter>>
) : Configuration

@Stable
fun patientDetailsViewConfigurationOf(
  appId: String = "quest",
  classification: String = "patient_details",
  contentTitle: String = "Responses",
  valuePrefix: String = "G6PD ",
  dynamicRows: List<List<Filter>> = mutableListOf()
) =
  PatientDetailsViewConfiguration(
    appId = appId,
    classification = classification,
    contentTitle = contentTitle,
    valuePrefix = valuePrefix,
    dynamicRows = dynamicRows
  )
