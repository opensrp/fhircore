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

package org.dtree.fhircore.dataclerk.ui.main

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.get
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.Operation
import com.google.android.fhir.search.Order
import com.google.android.fhir.search.Search
import com.google.android.fhir.search.StringFilterModifier
import com.google.android.fhir.search.search
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import javax.inject.Inject
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.AppConfigClassification
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.domain.model.HealthStatus
import org.smartregister.fhircore.engine.util.extension.extractAddress
import org.smartregister.fhircore.engine.util.extension.extractGeneralPractitionerReference
import org.smartregister.fhircore.engine.util.extension.extractHealthStatusFromMeta
import org.smartregister.fhircore.engine.util.extension.extractName
import org.smartregister.fhircore.engine.util.extension.extractOfficialIdentifier
import org.smartregister.fhircore.engine.util.extension.extractWithFhirPath
import timber.log.Timber

class AppDataStore
@Inject
constructor(
  private val fhirEngine: FhirEngine,
  private val configurationRegistry: ConfigurationRegistry,
  val defaultRepository: DefaultRepository
) {
  private val jsonParser = FhirContext.forCached(FhirVersionEnum.R4).newJsonParser()

  private fun getApplicationConfiguration(): ApplicationConfiguration {
    return configurationRegistry.retrieveConfiguration(AppConfigClassification.APPLICATION)
  }

  suspend fun loadPatients(page: Int = 1): List<PatientItem> {
    Timber.e("Page: $page")
    // TODO: replace with _tag search when update is out
    return fhirEngine
      .search<Patient> {
        filter(Patient.ACTIVE, { value = of(true) })
        sort(Patient.NAME, Order.ASCENDING)
        count = 20
        from = (page - 1) * 20
      }
      .map { inputModel ->
        //        Timber.e(jsonParser.encodeResourceToString(inputModel))
        inputModel.toPatientItem(getApplicationConfiguration())
      }
  }

  suspend fun getPatient(patientId: String): PatientItem {
    val patient = fhirEngine.get<Patient>(patientId)
    return patient.toPatientItem(getApplicationConfiguration())
  }

  suspend fun getResource(resourceId: String): Resource {
    return defaultRepository.loadResource(Reference().apply { this.reference = resourceId })
  }

  suspend fun patientCount(): Long {
    return fhirEngine.count(
      Search(ResourceType.Patient).apply { filter(Patient.ACTIVE, { value = of(true) }) }
    )
  }

  suspend fun search(text: String): List<PatientItem> {
    return fhirEngine
      .search<Patient> {
        filter(
          Patient.NAME,
          {
            modifier = StringFilterModifier.CONTAINS
            value = text
          }
        )
        filter(Patient.IDENTIFIER, { value = of(Identifier().apply { value = text }) })
        operation = Operation.OR
        sort(Patient.NAME, Order.ASCENDING)
      }
      .map { inputModel ->
        //        Timber.e(jsonParser.encodeResourceToString(inputModel))
        inputModel.toPatientItem(getApplicationConfiguration())
      }
  }
}

data class PatientItem(
  val id: String,
  val resourceId: String,
  val name: String,
  val gender: String,
  val dob: LocalDate? = null,
  val addressData: AddressData,
  val phone: String,
  val isActive: Boolean,
  val chwAssigned: String,
  val healthStatus: HealthStatus,
  val practitioners: List<Reference>? = null,
  val dateCreated: Date? = null
)

data class AddressData(
  val district: String = "",
  val state: String = "",
  val text: String = "",
  val fullAddress: String = ""
)

internal fun Patient.toPatientItem(configuration: ApplicationConfiguration): PatientItem {
  val phone = if (hasTelecom()) telecom[0].value else "N/A"
  val isActive = active
  val gender = if (hasGenderElement()) genderElement.valueAsString else ""
  val dob =
    if (hasBirthDateElement())
      LocalDate.parse(birthDateElement.valueAsString, DateTimeFormatter.ISO_DATE)
    else null
  return PatientItem(
    id = this.extractOfficialIdentifier() ?: "N/A",
    resourceId = this.logicalId,
    name = this.extractName(),
    dob = dob,
    gender = gender ?: "",
    phone = phone ?: "N/A",
    isActive = isActive,
    healthStatus =
      this.extractHealthStatusFromMeta(configuration.patientTypeFilterTagViaMetaCodingSystem),
    chwAssigned = this.extractGeneralPractitionerReference(),
    practitioners = this.generalPractitioner,
    addressData =
      AddressData(
        district = this.extractWithFhirPath("Patient.address.district"),
        state = this.extractWithFhirPath("Patient.address.state"),
        text = this.extractWithFhirPath("Patient.address.text"),
        fullAddress = this.extractAddress()
      ),
    dateCreated = this.meta.lastUpdated
  )
}
