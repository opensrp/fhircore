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
import android.content.Context
import ca.uhn.fhir.context.FhirContext
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.search.Order
import com.google.android.fhir.search.StringFilterModifier
import com.google.android.fhir.search.search
import com.google.android.fhir.sync.State
import com.google.android.fhir.sync.Sync
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableSharedFlow
import org.apache.commons.lang3.Validate.isAssignableFrom
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Resource
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.configuration.app.ConfigurableApplication
import org.smartregister.fhircore.engine.data.domain.util.PaginationUtil
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceService

suspend fun Application.runSync(flow: MutableSharedFlow<State>? = null) {
  if (this !is ConfigurableApplication)
    throw (IllegalStateException("Application should extend ConfigurableApplication interface"))
  val dataSource = buildDatasource(this.applicationConfiguration)

  Sync.basicSyncJob(this)
    .run(
      fhirEngine = this.fhirEngine,
      dataSource = dataSource,
      resourceSyncParams = resourceSyncParams,
      subscribeTo = flow
    )
}

fun Application.lastSyncDateTime(): String {
  val lastSyncDate = Sync.basicSyncJob(this).lastSyncTimestamp()
  return if (lastSyncDate == null) "" else lastSyncDate.asString()
}

fun <T> Application.loadConfig(id: String, clazz: Class<T>): T {
  val json = assets.open(id).bufferedReader().use { it.readText() }

  return if (Resource::class.java.isAssignableFrom(clazz)) FhirContext.forR4().newJsonParser().parseResource(json) as T
  else Gson().fromJson(json, clazz)
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
