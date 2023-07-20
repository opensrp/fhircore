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
import com.google.android.fhir.search.Order
import com.google.android.fhir.search.search
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.engine.util.extension.extractName
import timber.log.Timber

class AppDataStore @Inject constructor(private val fhirEngine: FhirEngine) {
  private val jsonParser = FhirContext.forCached(FhirVersionEnum.R4).newJsonParser()

  suspend fun loadPatients(): List<PatientItem> {
    // TODO: replace with _tag search when update is out
    return fhirEngine
      .search<Patient> {
        filter(Patient.ACTIVE, { value = of(true) })
        sort(Patient.NAME, Order.ASCENDING)
      }
      .map { inputModel ->
        Timber.e(jsonParser.encodeResourceToString(inputModel))
        inputModel.toPatientItem()
      }
  }

  suspend fun getPatient(patientId: String): PatientItem {
    val patient = fhirEngine.get<Patient>(patientId)
    return patient.toPatientItem()
  }
}

data class PatientItem(
  val id: String,
  val resourceId: String,
  val name: String,
  val gender: String,
  val dob: LocalDate? = null,
  val phone: String,
  val isActive: Boolean,
)

internal fun Patient.toPatientItem(): PatientItem {
  val phone = if (hasTelecom()) telecom[0].value else "N/A"
  val isActive = active
  val gender = if (hasGenderElement()) genderElement.valueAsString else ""
  val dob =
    if (hasBirthDateElement())
      LocalDate.parse(birthDateElement.valueAsString, DateTimeFormatter.ISO_DATE)
    else null
  return PatientItem(
    id = this.identifierFirstRep?.value ?: "N/A",
    resourceId = this.logicalId,
    name = this.extractName(),
    dob = dob,
    gender = gender ?: "",
    phone = phone ?: "N/A",
    isActive = isActive,
  )
}
