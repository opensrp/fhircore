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

package org.smartregister.fhircore.engine.data.domain.util

import com.google.android.fhir.FhirEngine

/**
 * Implement [RegisterRepository] to query for FHIR resources of type [I]. The result should then be
 * transformed to [O] using the provided [domainMapper]. Queries are submitted through [FhirEngine]
 * search API
 */
interface RegisterRepository<I : Any, O> {

  val domainMapper: DomainMapper<I, O>

  val defaultPageSize: Int
    get() = PaginationUtil.DEFAULT_PAGE_SIZE

  val fhirEngine: FhirEngine

  /**
   * Method to read data from the local database. This method will return paginated data for the run
   * [query]
   */
  suspend fun loadData(
    query: String = "",
    pageNumber: Int,
  ): List<O>
}
