package org.smartregister.fhircore.engine.data.local.repository.patient

import com.google.android.fhir.FhirEngine
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.Order
import com.google.android.fhir.search.Search
import com.google.android.fhir.search.count
import com.google.android.fhir.search.search
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.engine.data.domain.util.DomainMapper
import org.smartregister.fhircore.engine.data.domain.util.RegisterRepository
import org.smartregister.fhircore.engine.data.local.repository.patient.model.PatientItem
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.DispatcherProvider

class PatientRepository(
  override val fhirEngine: FhirEngine,
  override val domainMapper: DomainMapper<Pair<Patient, List<Immunization>>, PatientItem>,
  private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider
) : RegisterRepository<Pair<Patient, List<Immunization>>, PatientItem> {

  override suspend fun loadData(
    query: String,
    pageNumber: Int,
    primaryFilterCallback: (Search) -> Unit,
    vararg secondaryFilterCallbacks: (String, Search) -> Unit
  ): List<PatientItem> {
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

      // Fetch immunization data for patient
      val patientImmunizations = mutableListOf<Pair<Patient, List<Immunization>>>()
      patients.forEach {
        val immunizations: List<Immunization> = getPatientImmunizations(it.logicalId)
        patientImmunizations.add(Pair(it, immunizations))
      }
      patientImmunizations.map { domainMapper.mapToDomainModel(it) }
    }
  }

  suspend fun getPatientImmunizations(logicalId: String): List<Immunization> =
    withContext(dispatcherProvider.io()) {
      fhirEngine.search { filter(Immunization.PATIENT) { value = "Patient/${logicalId}" } }
    }

  suspend fun countAll(
    query: String = "",
    secondaryFilterCallbacks: Array<out (String, Search) -> Unit>
  ): Int {
    return fhirEngine
      .count<Patient> {
        filter(Patient.ACTIVE, true)
        secondaryFilterCallbacks.forEach { filterCallback: (String, Search) -> Unit ->
          if (query.isNotEmpty() && query.isNotBlank()) {
            filterCallback(query, this)
          }
        }
      }
      .toInt()
  }
}
