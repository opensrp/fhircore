package org.smartregister.fhircore.engine.util.extension

import android.app.Application
import ca.uhn.fhir.context.FhirContext
import com.google.android.fhir.sync.Result
import com.google.android.fhir.sync.Sync
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.configuration.app.ConfigurableApplication
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceService

suspend fun Application.syncData(applicationConfiguration: ApplicationConfiguration): Result {
  if (this !is ConfigurableApplication)
    throw (IllegalStateException("Application should extend ConfigurableApplication interface"))
  val dataSource =
    FhirResourceService.create(
      FhirContext.forR4().newJsonParser(),
      applicationContext,
      applicationConfiguration
    )
  return Sync.oneTimeSync(
    fhirEngine = (this as ConfigurableApplication).fhirEngine,
    dataSource = FhirResourceDataSource(dataSource),
    resourceSyncParams = resourceSyncParams
  )
}
