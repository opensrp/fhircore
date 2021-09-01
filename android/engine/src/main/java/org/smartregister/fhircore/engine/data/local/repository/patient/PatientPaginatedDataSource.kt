package org.smartregister.fhircore.engine.data.local.repository.patient

import com.google.android.fhir.FhirEngine
import com.google.android.fhir.search.Search
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.engine.data.domain.util.DomainMapper
import org.smartregister.fhircore.engine.data.domain.util.PaginatedDataSource
import org.smartregister.fhircore.engine.data.local.repository.patient.model.PatientItem

class PatientPaginatedDataSource(
  val fhirEngine: FhirEngine,
  domainMapper: DomainMapper<Pair<Patient, List<Immunization>>, PatientItem>
) :
  PaginatedDataSource<Pair<Patient, List<Immunization>>, PatientItem>(
    PatientRepository(fhirEngine, domainMapper)
  ) {

  override suspend fun loadData(pageNumber: Int): List<PatientItem> {
    return registerRepository.loadData(
      pageNumber = pageNumber,
      query = "",
      primaryFilterCallback = { search: Search -> search.filter(Patient.ACTIVE, true) },
      secondaryFilterCallbacks = arrayOf()
    )
  }
}
