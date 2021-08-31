package org.smartregister.fhircore.anc.ui.anccare

import com.google.android.fhir.FhirEngine
import com.google.android.fhir.search.Order
import com.google.android.fhir.search.Search
import com.google.android.fhir.search.search
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.engine.data.domain.util.DomainMapper
import org.smartregister.fhircore.engine.data.domain.util.RegisterRepository
import org.smartregister.fhircore.engine.data.local.repository.patient.model.AncItem
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
