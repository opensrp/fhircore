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
