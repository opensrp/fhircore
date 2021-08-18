package org.smartregister.fhircore.engine.data.domain.util

import com.google.android.fhir.FhirEngine
import com.google.android.fhir.search.Search

/**
 * Implement [RegisterRepository] to query for FHIR resources of type [I]. The result should then be
 * transformed to [O] using the provided [domainMapper]. Queries are submitted through [FhirEngine]
 * search API
 */
interface RegisterRepository<I : Any, O> {
  val domainMapper: DomainMapper<I, O>
  val defaultPageSize: Int
  val fhirEngine: FhirEngine

  /**
   * Method to read data from the local database. This method will return paginated data for the run
   * [query]. It accepts two callbacks [primaryFilterCallback] and [secondaryFilterCallbacks].
   * [primaryFilterCallback] refers to the main filter applied to the query as expected by the
   * [FhirEngine] API e.g. filtering out only active clients. [secondaryFilterCallbacks] are
   * additional filters that are applied to the query to further filter the data
   */
  suspend fun loadData(
    query: String = "",
    pageNumber: Int,
    primaryFilterCallback: (Search) -> Unit,
    vararg secondaryFilterCallbacks: (String, Search) -> Unit
  ): List<O>
}
