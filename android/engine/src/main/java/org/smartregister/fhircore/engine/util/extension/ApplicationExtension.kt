package org.smartregister.fhircore.engine.util.extension

import android.app.Application
import ca.uhn.fhir.context.FhirContext
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.search.Order
import com.google.android.fhir.search.StringFilterModifier
import com.google.android.fhir.search.search
import com.google.android.fhir.sync.Result
import com.google.android.fhir.sync.Sync
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.configuration.app.ConfigurableApplication
import org.smartregister.fhircore.engine.data.domain.util.PaginationUtil
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceService

suspend fun Application.runSync(): Result {
  if (this !is ConfigurableApplication)
    throw (IllegalStateException("Application should extend ConfigurableApplication interface"))
  val dataSource =
    FhirResourceService.create(
      FhirContext.forR4().newJsonParser(),
      applicationContext,
      this.applicationConfiguration
    )
  return Sync.oneTimeSync(
    fhirEngine = (this as ConfigurableApplication).fhirEngine,
    dataSource = FhirResourceDataSource(dataSource),
    resourceSyncParams = resourceSyncParams
  )
}

fun Application.buildDatasource(
  applicationConfiguration: ApplicationConfiguration
): FhirResourceDataSource {
  return FhirResourceDataSource(
    FhirResourceService.create(FhirContext.forR4().newJsonParser(), this, applicationConfiguration)
  )
}

suspend fun FhirEngine.searchPatients(query: String, pageNumber: Int) =
  this.search<Patient> {
    filter(Patient.ACTIVE, true)
    if (query.isNotBlank()) {
      filter(Patient.NAME) {
        modifier = StringFilterModifier.CONTAINS
        value = query.trim()
      }
    }
    sort(Patient.NAME, Order.ASCENDING)
    count = PaginationUtil.DEFAULT_PAGE_SIZE
    from = pageNumber * PaginationUtil.DEFAULT_PAGE_SIZE
  }
