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

package org.smartregister.fhircore.eir.data

import com.google.android.fhir.FhirEngine
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.search
import javax.inject.Inject
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.eir.data.model.PatientItem
import org.smartregister.fhircore.eir.ui.patient.details.AdverseEventItem
import org.smartregister.fhircore.eir.ui.patient.register.PatientItemMapper
import org.smartregister.fhircore.engine.data.domain.util.RegisterRepository
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.extension.countActivePatients
import org.smartregister.fhircore.engine.util.extension.extractId
import org.smartregister.fhircore.engine.util.extension.loadResource
import org.smartregister.fhircore.engine.util.extension.searchActivePatients
import org.smartregister.fhircore.engine.util.extension.toHumanDisplay

class PatientRepository
@Inject
constructor(
  override val fhirEngine: FhirEngine,
  override val domainMapper: PatientItemMapper,
  val dispatcherProvider: DispatcherProvider
) : RegisterRepository<Pair<Patient, List<Immunization>>, PatientItem> {

  override suspend fun loadData(
    query: String,
    pageNumber: Int,
    loadAll: Boolean
  ): List<PatientItem> {
    return withContext(dispatcherProvider.io()) {
      val patients =
        fhirEngine.searchActivePatients(query = query, pageNumber = pageNumber, loadAll = loadAll)

      // Fetch immunization data for patient
      val patientImmunizations = mutableListOf<Pair<Patient, List<Immunization>>>()
      patients.forEach {
        val immunizations: List<Immunization> = getPatientImmunizations(it.logicalId)
        patientImmunizations.add(Pair(it, immunizations))
      }
      patientImmunizations.map { domainMapper.mapToDomainModel(it) }
    }
  }

  suspend fun getPatientImmunizations(patientId: String): List<Immunization> =
    withContext(dispatcherProvider.io()) {
      fhirEngine.search { filter(Immunization.PATIENT, { value = "Patient/$patientId" }) }
    }

  suspend fun fetchDemographics(patientId: String): Patient =
    withContext(dispatcherProvider.io()) {
      fhirEngine.get(ResourceType.Patient, patientId) as Patient
    }

  override suspend fun countAll(): Long =
    withContext(dispatcherProvider.io()) { fhirEngine.countActivePatients() }

  suspend fun getAdverseEvents(immunization: Immunization): List<AdverseEventItem> {
    return withContext(dispatcherProvider.io()) {
      val adverseEventItems = mutableListOf<AdverseEventItem>()
      immunization.reaction.forEach {
        val detailObservation = loadObservation(it.detail.extractId())
        adverseEventItems.add(
          AdverseEventItem(
            it.date.toHumanDisplay(),
            detailObservation?.code?.coding?.first()?.display ?: ""
          )
        )
      }
      adverseEventItems
    }
  }

  private suspend fun loadObservation(id: String): Observation? =
    withContext(dispatcherProvider.io()) { fhirEngine.loadResource(id) }
}
