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

import android.content.Context
import android.content.res.AssetManager
import ca.uhn.fhir.context.FhirContext
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.db.ResourceNotFoundException
import com.google.android.fhir.search.Order
import com.google.android.fhir.search.StringFilterModifier
import com.google.android.fhir.search.count
import com.google.android.fhir.search.getQuery
import com.google.android.fhir.search.search
import com.google.android.fhir.sync.ResourceSyncParams
import com.google.android.fhir.sync.State
import com.google.android.fhir.sync.SyncJob
import com.google.gson.Gson
import java.util.Locale
import kotlinx.coroutines.flow.MutableSharedFlow
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.Library
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.RelatedPerson
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.AppConfigClassification
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.cql.FhirOperatorDecorator
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.domain.model.Language
import org.smartregister.fhircore.engine.domain.util.PaginationConstant
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import timber.log.Timber

suspend fun FhirEngine.runOneTimeSync(
  sharedSyncStatus: MutableSharedFlow<State>,
  syncJob: SyncJob,
  resourceSyncParams: ResourceSyncParams,
  fhirResourceDataSource: FhirResourceDataSource
) {

  // TODO run initial sync for binary and library resources

  syncJob.run(
    fhirEngine = this,
    dataSource = fhirResourceDataSource,
    resourceSyncParams = resourceSyncParams,
    subscribeTo = sharedSyncStatus
  )
}

fun <T> Context.loadResourceTemplate(id: String, clazz: Class<T>, data: Map<String, String?>): T {
  var json = assets.open(id).bufferedReader().use { it.readText() }

  data.entries.forEach { it.value?.let { v -> json = json.replace(it.key, v) } }

  return if (Resource::class.java.isAssignableFrom(clazz))
    FhirContext.forR4Cached().newJsonParser().parseResource(json) as T
  else Gson().fromJson(json, clazz)
}

suspend fun FhirEngine.searchActivePatients(
  query: String,
  pageNumber: Int,
  loadAll: Boolean = false
) =
  this.search<Patient> {
    filter(Patient.ACTIVE, { value = of(true) })
    if (query.isNotBlank()) {
      filter(
        stringParameter = Patient.NAME,
        {
          modifier = StringFilterModifier.CONTAINS
          value = query.trim()
        }
      )
    }
    sort(Patient.NAME, Order.ASCENDING)
    count =
      if (loadAll) this@searchActivePatients.countActivePatients().toInt()
      else PaginationConstant.DEFAULT_PAGE_SIZE
    from = pageNumber * PaginationConstant.DEFAULT_PAGE_SIZE
  }

suspend fun FhirEngine.countActivePatients(): Long =
  this.count<Patient> { apply { filter(Patient.ACTIVE, { value = of(true) }) }.getQuery(true) }

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
      apply { filter(RelatedPerson.PATIENT, { value = "Patient/$patientId" }) }.getQuery()
    }
  } catch (resourceNotFoundException: ResourceNotFoundException) {
    null
  }
}

suspend fun FhirEngine.loadPatientImmunizations(patientId: String): List<Immunization>? {
  return try {
    this@loadPatientImmunizations.search {
      filter(Immunization.PATIENT, { value = "Patient/$patientId" })
      apply { filter(Immunization.PATIENT, { value = "Patient/$patientId" }) }.getQuery()
    }
  } catch (resourceNotFoundException: ResourceNotFoundException) {
    null
  }
}

suspend fun FhirEngine.loadCqlLibraryBundle(
  context: Context,
  sharedPreferencesHelper: SharedPreferencesHelper,
  fhirOperator: FhirOperatorDecorator,
  resourcesBundlePath: String
) =
  try {
    val jsonParser = FhirContext.forR4Cached().newJsonParser()
    val savedResources =
      sharedPreferencesHelper.read(SharedPreferencesHelper.MEASURE_RESOURCES_LOADED, "")

    context.assets.open(resourcesBundlePath, AssetManager.ACCESS_RANDOM).bufferedReader().use {
      val bundle = jsonParser.parseResource(it) as Bundle
      bundle.entry.forEach { entry ->
        if (entry.resource.resourceType == ResourceType.Library) {
          fhirOperator.loadLib(entry.resource as Library)
        } else {
          if (!savedResources!!.contains(resourcesBundlePath)) {
            save(entry.resource)
            sharedPreferencesHelper.write(
              SharedPreferencesHelper.MEASURE_RESOURCES_LOADED,
              savedResources.plus(",").plus(resourcesBundlePath)
            )
          }
        }
      }
    }
  } catch (exception: Exception) {
    Timber.e(exception)
  }

fun ConfigurationRegistry.fetchLanguages() =
  this.retrieveConfiguration<ApplicationConfiguration>(AppConfigClassification.APPLICATION)
    .run { this.languages }
    .map { Language(it, Locale.forLanguageTag(it).displayName) }
