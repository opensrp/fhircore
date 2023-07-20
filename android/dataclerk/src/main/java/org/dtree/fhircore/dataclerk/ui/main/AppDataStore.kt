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
import com.google.android.fhir.search.Order
import com.google.android.fhir.search.search
import javax.inject.Inject
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.engine.util.extension.extractAge
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
}

data class PatientItem(
  val id: String? = null,
  val resourceId: String? = null,
  val name: String,
  val age: String
)

internal fun Patient.toPatientItem(): PatientItem {
  return PatientItem(
    id = this.identifierFirstRep.value,
    resourceId = this.id,
    name = this.extractName(),
    age = this.extractAge()
  )
}
