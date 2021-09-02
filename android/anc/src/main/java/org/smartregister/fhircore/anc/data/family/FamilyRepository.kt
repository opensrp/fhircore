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

package org.smartregister.fhircore.anc.data.family

import com.google.android.fhir.FhirEngine
import com.google.android.fhir.search.Order
import com.google.android.fhir.search.StringFilterModifier
import com.google.android.fhir.search.search
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.anc.data.family.model.FamilyItem
import org.smartregister.fhircore.anc.ui.family.register.Family
import org.smartregister.fhircore.anc.ui.family.register.FamilyItemMapper
import org.smartregister.fhircore.engine.data.domain.util.DomainMapper
import org.smartregister.fhircore.engine.data.domain.util.RegisterRepository
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.DispatcherProvider

class FamilyRepository(
  override val fhirEngine: FhirEngine,
  override val domainMapper: DomainMapper<Family, FamilyItem>,
  private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider
) : RegisterRepository<Family, FamilyItem> {

  override suspend fun loadData(query: String, pageNumber: Int): List<FamilyItem> {
    return withContext(dispatcherProvider.io()) {
      val patients =
        fhirEngine.search<Patient> {
          filter(Patient.ADDRESS_CITY) {
            modifier = StringFilterModifier.CONTAINS
            value = "NAIROBI"
          }
          if (query.isNotEmpty() && query.isNotBlank()) {
            filter(Patient.NAME) {
              modifier = StringFilterModifier.CONTAINS
              value = query.trim()
            }
          }
          sort(Patient.NAME, Order.ASCENDING)
          count = 500 // todo defaultPageSize
          from = pageNumber * defaultPageSize
        }

      patients
        .filter { // todo
          it.extension.any { it.value.toString().contains("family", true) }
        }
        .map { p ->
          val carePlans = searchCarePlan(p.id).toMutableList()

          val members = fhirEngine.search<Patient> { filter(Patient.LINK) { this.value = p.id } }

          members.forEach { carePlans.addAll(searchCarePlan(it.id)) }

          FamilyItemMapper.mapToDomainModel(Family(p, members, carePlans))
        }
    }
  }

  private suspend fun searchCarePlan(id: String): List<CarePlan> {
    return fhirEngine.search { filter(CarePlan.PATIENT) { this.value = id } }
  }
}
