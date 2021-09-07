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

package org.smartregister.fhircore.anc.data

import com.google.android.fhir.FhirEngine
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.Order
import com.google.android.fhir.search.StringFilterModifier
import com.google.android.fhir.search.search
import java.util.Date
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Period
import org.smartregister.fhircore.anc.data.model.AncPatientItem
import org.smartregister.fhircore.anc.ui.anccare.register.Anc
import org.smartregister.fhircore.engine.data.domain.util.DomainMapper
import org.smartregister.fhircore.engine.data.domain.util.RegisterRepository
import org.smartregister.fhircore.engine.sdk.PatientExtended
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireUtils
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.extension.searchPatients

class AncPatientRepository(
  override val fhirEngine: FhirEngine,
  override val domainMapper: DomainMapper<Anc, AncPatientItem>,
  private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider
) : RegisterRepository<Anc, AncPatientItem> {

  override suspend fun loadData(
    query: String,
    pageNumber: Int,
  ): List<AncPatientItem> {
    return withContext(dispatcherProvider.io()) {
      val patients =
        fhirEngine.search<Patient> {
          filter(PatientExtended.TAG) {
            this.value = "Pregnant"
            this.modifier = StringFilterModifier.CONTAINS
          }

          if (query.isNotEmpty() && query.isNotBlank()) {
            filter(Patient.NAME) {
              value = query.trim()
              modifier = StringFilterModifier.CONTAINS
            }
          }
          sort(Patient.NAME, Order.ASCENDING)
          count = defaultPageSize
          from = pageNumber * defaultPageSize
        }

      patients.map {
        val head =
          kotlin
            .runCatching {
              fhirEngine.load(Patient::class.java, it.link[0].id.replace("Patient/", ""))
            }
            .getOrNull()

        val carePlans = searchCarePlan(it.logicalId)
        domainMapper.mapToDomainModel(Anc(it, head, carePlans))
      }
    }
  }

  suspend fun searchCarePlan(id: String): List<CarePlan> {
    return fhirEngine.search { filter(CarePlan.SUBJECT) { this.value = "Patient/$id" } }
  }

  // todo add careplan data from server or scheduled service
  suspend fun enrollIntoAnc(patientId: String) {
    for (i in 1..8) {
      val carePlan =
        CarePlan().apply {
          this.category.add(
            CodeableConcept().apply {
              this.text = "ANC Visit"
              this.addCoding(Coding("tempsystem", "anc visit code", "anc visit"))
            }
          )
          this.id = QuestionnaireUtils.getUniqueId()
          this.intent = CarePlan.CarePlanIntent.PLAN
          this.period =
            Period().apply {
              this.end = Date(System.currentTimeMillis() + (i * 1000 * 60 * 60 * 24 * 90L))
              this.start = Date()
            }
          this.status = CarePlan.CarePlanStatus.ACTIVE
          this.subject = QuestionnaireUtils.asPatientReference(patientId)
          this.title = "ANC Visit CP $i"
        }

      fhirEngine.save(carePlan)
    }
  }
}
