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
import com.google.android.fhir.datacapture.utilities.SimpleWorkerContextProvider
import com.google.android.fhir.db.ResourceNotFoundException
import com.google.android.fhir.search.Order
import com.google.android.fhir.search.StringFilterModifier
import com.google.android.fhir.search.count
import com.google.android.fhir.search.search
import com.google.android.fhir.sync.State
import com.google.android.fhir.sync.Sync
import com.google.gson.Gson
import java.io.IOException
import kotlinx.coroutines.flow.MutableSharedFlow
import org.hl7.fhir.r4.context.SimpleWorkerContext
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.RelatedPerson
import org.hl7.fhir.r4.model.Resource
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.configuration.app.ConfigurableApplication
import org.smartregister.fhircore.engine.data.domain.util.PaginationUtil
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceService
import timber.log.Timber

suspend fun Application.runSync(syncStateFlow: MutableSharedFlow<State>? = null) {
  if (this !is ConfigurableApplication)
    throw (IllegalStateException("Application should extend ConfigurableApplication interface"))
  val dataSource = buildDatasource(this.applicationConfiguration)

  Sync.basicSyncJob(this)
    .run(
      fhirEngine = fhirEngine,
      dataSource = dataSource,
      resourceSyncParams = resourceSyncParams,
      subscribeTo = syncStateFlow
    )
}

fun Application.lastSyncDateTime(): String {
  val lastSyncDate = Sync.basicSyncJob(this).lastSyncTimestamp()
  return lastSyncDate?.asString() ?: ""
}

fun <T> Application.loadResourceTemplate(
  id: String,
  clazz: Class<T>,
  data: Map<String, String?>
): T {
  var json = assets.open(id).bufferedReader().use { it.readText() }

  data.entries.forEach { it.value?.let { v -> json = json.replace(it.key, v) } }

  return if (Resource::class.java.isAssignableFrom(clazz))
    FhirContext.forR4().newJsonParser().parseResource(json) as T
  else Gson().fromJson(json, clazz)
}

fun Application.buildDatasource(
  applicationConfiguration: ApplicationConfiguration
): FhirResourceDataSource {
  return FhirResourceDataSource(
    FhirResourceService.create(FhirContext.forR4().newJsonParser(), this, applicationConfiguration)
  )
}

suspend fun FhirEngine.searchActivePatients(
  query: String,
  pageNumber: Int,
  loadAll: Boolean = false
) =
  this.search<Patient> {
    filter(Patient.ACTIVE, true)
    if (query.isNotBlank()) {
      filter(Patient.NAME) {
        modifier = StringFilterModifier.CONTAINS
        value = query.trim()
      }
    }
    sort(Patient.NAME, Order.ASCENDING)
    count =
      if (loadAll) this@searchActivePatients.countActivePatients().toInt()
      else PaginationUtil.DEFAULT_PAGE_SIZE
    from = pageNumber * PaginationUtil.DEFAULT_PAGE_SIZE
  }

suspend fun FhirEngine.countActivePatients(): Long =
  this.count<Patient> { filter(Patient.ACTIVE, true) }

suspend inline fun <reified T : Resource> FhirEngine.loadResource(resourceId: String): T? {
  return try {
    this@loadResource.load(T::class.java, resourceId)
  } catch (resourceNotFoundException: ResourceNotFoundException) {
    null
  }
}

suspend fun FhirEngine.loadRelatedPersons(patientId: String): List<RelatedPerson>? {
  return try {
    this@loadRelatedPersons.search {
      filter(RelatedPerson.PATIENT) { value = "Patient/$patientId" }
    }
  } catch (resourceNotFoundException: ResourceNotFoundException) {
    null
  }
}

suspend fun FhirEngine.loadImmunizations(patientId: String): List<Immunization>? {
  return try {
    this@loadImmunizations.search { filter(Immunization.PATIENT) { value = "Patient/$patientId" } }
  } catch (resourceNotFoundException: ResourceNotFoundException) {
    null
  }
}

suspend fun Application.initializeWorkerContext(): SimpleWorkerContext? {
  return try {
    SimpleWorkerContextProvider.loadSimpleWorkerContext(this@initializeWorkerContext)
  } catch (ioException: IOException) {
    Timber.e(ioException)
    null
  }
}
