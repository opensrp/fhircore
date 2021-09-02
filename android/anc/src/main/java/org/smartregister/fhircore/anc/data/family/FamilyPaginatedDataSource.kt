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
import com.google.android.fhir.search.Search
import com.google.android.fhir.search.StringFilterModifier
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.anc.data.family.model.FamilyItem
import org.smartregister.fhircore.anc.ui.family.register.Family
import org.smartregister.fhircore.engine.data.domain.util.DomainMapper
import org.smartregister.fhircore.engine.data.domain.util.PaginatedDataSource

class FamilyPaginatedDataSource(
  private val familyTag: String,
  val fhirEngine: FhirEngine,
  val domainMapper: DomainMapper<Family, FamilyItem>
) : PaginatedDataSource<Family, FamilyItem>(FamilyPaginatedRepository(fhirEngine, domainMapper)) {

  var query: String = ""

  override suspend fun loadData(pageNumber: Int): List<FamilyItem> {
    return registerRepository.loadData(
      pageNumber = pageNumber,
      query = query,
      /* todo primaryFilterCallback = { search: Search -> search.filter(PatientExtended.TAG){
        modifier = StringFilterModifier.MATCHES_EXACTLY
        value = familyTag
      } },*/
      primaryFilterCallback = { search: Search ->
        search.filter(Patient.ADDRESS_CITY) {
          modifier = StringFilterModifier.CONTAINS
          value = "NAI"
        }
      },
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
