package org.smartregister.fhircore.engine.data.local.repository.patient

import com.google.android.fhir.FhirEngine
import com.google.android.fhir.search.Search
import com.google.android.fhir.search.StringFilterModifier
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.engine.data.domain.util.DomainMapper
import org.smartregister.fhircore.engine.data.domain.util.PaginatedDataSource
import org.smartregister.fhircore.engine.data.local.repository.model.PatientItem

class PatientPaginatedDataSource(val fhirEngine: FhirEngine, domainMapper: DomainMapper<Pair<Patient, List<Immunization>>, PatientItem>) :
  PaginatedDataSource<Pair<Patient, List<Immunization>>, PatientItem>(PatientRepository(fhirEngine, domainMapper)) {

  private var query: String = ""

  override suspend fun loadData(pageNumber: Int): List<PatientItem> {
    return fhirRepository.loadData(
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
