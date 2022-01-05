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

package org.smartregister.fhircore.quest.util

import ca.uhn.fhir.rest.gclient.ReferenceClientParam
import ca.uhn.fhir.rest.gclient.TokenClientParam
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.search.search
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.quest.configuration.view.Code
import org.smartregister.fhircore.quest.configuration.view.Filter
import org.smartregister.fhircore.quest.configuration.view.PatientDetailsViewConfiguration
import org.smartregister.fhircore.quest.configuration.view.PatientRegisterRowViewConfiguration
import org.smartregister.fhircore.quest.configuration.view.Properties
import org.smartregister.fhircore.quest.configuration.view.Property
import org.smartregister.fhircore.quest.data.patient.model.AdditionalData

suspend fun loadAdditionalData(
  patientId: String,
  configurationRegistry: ConfigurationRegistry,
  fhirEngine: FhirEngine
): List<AdditionalData> {
  val result = mutableListOf<AdditionalData>()

  val patientRegisterRowViewConfiguration =
    configurationRegistry.retrieveConfiguration<PatientRegisterRowViewConfiguration>(
      configClassification = QuestConfigClassification.PATIENT_REGISTER_ROW
    )

  patientRegisterRowViewConfiguration.filters?.forEach { filter ->
    when (filter.resourceType) {
      Enumerations.ResourceType.CONDITION -> {
        val conditions =
          getSearchResults<Condition>("Patient/$patientId", Condition.SUBJECT, filter, fhirEngine)

        val sortedByDescending = conditions.maxByOrNull { it.recordedDate }
        sortedByDescending?.category?.forEach { cc ->
          cc.coding.firstOrNull { c -> c.code == filter.valueCoding!!.code }?.let {
            val status = sortedByDescending.code?.coding?.firstOrNull()?.display ?: ""
            result.add(
              AdditionalData(
                label = filter.label,
                value = status,
                valuePrefix = filter.valuePrefix,
                properties = propertiesMapping(status, filter)
              )
            )
          }
        }
      }
    }
  }

  return result
}

suspend fun loadAdditionalDataForPatientDetails(
  patientId: String,
  configurationRegistry: ConfigurationRegistry,
  fhirEngine: FhirEngine
): List<AdditionalData> {
  val result = mutableListOf<AdditionalData>()

  val patientDetailsViewConfiguration =
    configurationRegistry.retrieveConfiguration<PatientDetailsViewConfiguration>(
      configClassification = QuestConfigClassification.PATIENT_DETAILS_VIEW
    )

  patientDetailsViewConfiguration.filters?.forEach { filter ->
    when (filter.resourceType) {
      Enumerations.ResourceType.CONDITION -> {
        val conditions =
          getSearchResults<Condition>("Patient/$patientId", Condition.SUBJECT, filter, fhirEngine)

        val sortedByDescending = conditions.maxByOrNull { it.recordedDate }
        sortedByDescending?.category?.forEach { cc ->
          cc.coding.firstOrNull { c -> c.code == filter.valueCoding!!.code }?.let {
            val status = sortedByDescending.code?.coding?.firstOrNull()?.display ?: ""
            result.add(
              AdditionalData(
                label = filter.label,
                value = status,
                valuePrefix = filter.valuePrefix,
                properties = propertiesMapping(status, filter)
              )
            )
          }
        }
      }
    }
  }

  return result
}

suspend inline fun <reified T : Resource> getSearchResults(
  patientId: String,
  reference: ReferenceClientParam,
  filter: Filter,
  fhirEngine: FhirEngine
): List<T> {
  return fhirEngine.search {
    filter(reference) { this.value = "${ResourceType.Patient.name}/$patientId" }

    when (filter.valueType) {
      Enumerations.DataType.CODEABLECONCEPT -> {
        filter(
          TokenClientParam(filter.key),
          CodeableConcept().addCoding(filter.valueCoding!!.asCoding())
        )
      }
      Enumerations.DataType.CODING -> {
        filter(TokenClientParam(filter.key), filter.valueCoding!!.asCoding())
      }
    }
  }
}

fun propertiesMapping(value: String, filter: Filter): Properties {
  return Properties(
    label = filter.properties?.label,
    value =
      Property(
        color = filter.dynamicColors?.firstOrNull { it.valueEqual == value }?.useColor
            ?: filter.properties?.value?.color,
        textSize = filter.properties?.value?.textSize,
        fontWeight = filter.properties?.value?.fontWeight
      )
  )
}

fun Code.asCoding() = Coding(system, code, display)
