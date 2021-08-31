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
import com.google.android.fhir.search.Search
import com.google.android.fhir.search.StringFilterModifier
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.anc.data.model.AncPatientItem
import org.smartregister.fhircore.engine.data.domain.util.DomainMapper
import org.smartregister.fhircore.engine.data.domain.util.PaginatedDataSource

class AncPatientPaginatedDataSource(
  val fhirEngine: FhirEngine,
  domainMapper: DomainMapper<Patient, AncPatientItem>
) : PaginatedDataSource<Patient, AncPatientItem>(AncPatientRepository(fhirEngine, domainMapper)) {

  private var query: String = ""

  override suspend fun loadData(pageNumber: Int): List<AncPatientItem> {
    return registerRepository.loadData(
      pageNumber = pageNumber,
      query = query,
      primaryFilterCallback = { search: Search -> search.filter(Patient.ACTIVE, true) },
      secondaryFilterCallbacks =
        arrayOf({ filterQuery: String, search: Search ->
          search.filter(Patient.NAME) {
            modifier = StringFilterModifier.CONTAINS
            value = filterQuery.trim()
          }
        })
    )
  }
}
