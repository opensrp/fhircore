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
import com.google.android.fhir.search.Order
import com.google.android.fhir.search.Search
import com.google.android.fhir.search.search
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.engine.data.domain.util.DomainMapper
import org.smartregister.fhircore.engine.data.domain.util.RegisterRepository
import org.smartregister.fhircore.anc.data.model.AncItem
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.DispatcherProvider

class AncRepository(
    override val fhirEngine: FhirEngine,
    override val domainMapper: DomainMapper<Patient, AncItem>,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider
) : RegisterRepository<Patient, AncItem> {

  override val defaultPageSize: Int
    get() = 50

  override suspend fun loadData(
    query: String,
    pageNumber: Int,
    primaryFilterCallback: (Search) -> Unit,
    vararg secondaryFilterCallbacks: (String, Search) -> Unit
  ): List<AncItem> {
    return withContext(dispatcherProvider.io()) {
      val patients =
        fhirEngine.search<Patient> {
          primaryFilterCallback(this)
          secondaryFilterCallbacks.forEach { filterCallback: (String, Search) -> Unit ->
            if (query.isNotEmpty() && query.isNotBlank()) {
              filterCallback(query, this)
            }
          }
          sort(Patient.NAME, Order.ASCENDING)
          count = defaultPageSize
          from = pageNumber * defaultPageSize
        }

      patients.map { domainMapper.mapToDomainModel(it) }
    }
  }
}
