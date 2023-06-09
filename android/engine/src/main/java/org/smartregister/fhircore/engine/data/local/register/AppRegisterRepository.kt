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

package org.smartregister.fhircore.engine.data.local.register

import com.google.android.fhir.FhirEngine
import javax.inject.Inject
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Resource
import org.smartregister.fhircore.engine.appfeature.model.HealthModule
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.data.local.AppointmentRegisterFilter
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.data.local.RegisterFilter
import org.smartregister.fhircore.engine.data.local.TracingRegisterFilter
import org.smartregister.fhircore.engine.data.local.register.dao.HivRegisterDao
import org.smartregister.fhircore.engine.data.local.register.dao.RegisterDaoFactory
import org.smartregister.fhircore.engine.domain.model.HealthStatus
import org.smartregister.fhircore.engine.domain.model.ProfileData
import org.smartregister.fhircore.engine.domain.model.RegisterData
import org.smartregister.fhircore.engine.domain.repository.RegisterRepository
import org.smartregister.fhircore.engine.trace.PerformanceReporter
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper

class AppRegisterRepository
@Inject
constructor(
  override val fhirEngine: FhirEngine,
  override val dispatcherProvider: DefaultDispatcherProvider,
  override val sharedPreferencesHelper: SharedPreferencesHelper,
  override val configurationRegistry: ConfigurationRegistry,
  val registerDaoFactory: RegisterDaoFactory,
  override val configService: ConfigService,
  val tracer: PerformanceReporter
) :
  RegisterRepository,
  DefaultRepository(
    fhirEngine = fhirEngine,
    dispatcherProvider = dispatcherProvider,
    sharedPreferencesHelper = sharedPreferencesHelper,
    configurationRegistry = configurationRegistry,
    configService = configService
  ) {

  override suspend fun loadRegisterData(
    currentPage: Int,
    loadAll: Boolean,
    appFeatureName: String?,
    healthModule: HealthModule
  ): List<RegisterData> =
    withContext(dispatcherProvider.io()) {
      tracer.traceSuspend("${healthModule.name.camelCase()}.loadRegisterData") {
        if (currentPage != 0) it.putAttribute("Page", "${currentPage + 1}")

        registerDaoFactory.registerDaoMap[healthModule]?.loadRegisterData(
          currentPage = currentPage,
          appFeatureName = appFeatureName
        )
          ?: emptyList()
      }
    }

  override suspend fun searchByName(
    nameQuery: String,
    currentPage: Int,
    appFeatureName: String?,
    healthModule: HealthModule
  ): List<RegisterData> =
    withContext(dispatcherProvider.io()) {
      tracer.traceSuspend("${healthModule.name.camelCase()}.searchByName") {
        if (currentPage != 0) it.putAttribute("Page", "${currentPage + 1}")

        registerDaoFactory.registerDaoMap[healthModule]?.searchByName(
          currentPage = currentPage,
          appFeatureName = appFeatureName,
          nameQuery = nameQuery
        )
          ?: emptyList()
      }
    }

  override suspend fun loadRegisterFiltered(
    currentPage: Int,
    loadAll: Boolean,
    appFeatureName: String?,
    healthModule: HealthModule,
    filters: RegisterFilter
  ): List<RegisterData> {
    return withContext(dispatcherProvider.io()) {
      tracer.traceSuspend("${healthModule.name.camelCase()}.loadRegisterFiltered") {
        when (filters) {
          is AppointmentRegisterFilter -> {
            it.putAttribute(
              "SelectedFilters",
              "${filters.dateOfAppointment}, ${if (filters.myPatients) "Assigned patients" else "All patients"}, ${if (filters.patientCategory?.any() == true) filters.patientCategory.map(HealthStatus::name).joinToString(prefix = "(", postfix = ")") else "All"}, ${filters.reasonCode ?: "All reason codes"}"
            )
          }
          is TracingRegisterFilter -> {
            it.putAttribute(
              "SelectedFilters",
              " ${if (filters.isAssignedToMe) "Assigned patients" else "All patients"}, ${if (filters.patientCategory?.any() == true) filters.patientCategory.map(HealthStatus::name).joinToString(prefix = "(", postfix = ")") else "All"}, ${filters.reasonCode ?: "All reason codes"}, ${filters.age?.name ?: "All Ages"}"
            )
          }
        }
        if (currentPage != 0) it.putAttribute("Page", "${currentPage + 1}")

        registerDaoFactory.registerDaoMap[healthModule]?.loadRegisterFiltered(
          currentPage,
          loadAll,
          appFeatureName,
          filters
        )
          ?: emptyList()
      }
    }
  }

  override suspend fun countRegisterFiltered(
    appFeatureName: String?,
    healthModule: HealthModule,
    filters: RegisterFilter
  ): Long {
    return withContext(dispatcherProvider.io()) {
      tracer.traceSuspend("${healthModule.name.camelCase()}.countRegisterFiltered") {
        when (filters) {
          is AppointmentRegisterFilter -> {
            it.putAttribute(
              "SelectedFilters",
              "${filters.dateOfAppointment}, ${if (filters.myPatients) "Assigned patients" else "All patients"}, ${if (filters.patientCategory?.any() == true) filters.patientCategory.map(HealthStatus::name).joinToString(prefix = "(", postfix = ")") else "All"}, ${filters.reasonCode ?: "All reason codes"}"
            )
          }
          is TracingRegisterFilter -> {
            it.putAttribute(
              "SelectedFilters",
              " ${if (filters.isAssignedToMe) "Assigned patients" else "All patients"}, ${if (filters.patientCategory?.any() == true) filters.patientCategory.map(HealthStatus::name).joinToString(prefix = "(", postfix = ")") else "All"}, ${filters.reasonCode ?: "All reason codes"}, ${filters.age?.name ?: "All Ages"}"
            )
          }
        }

        registerDaoFactory.registerDaoMap[healthModule]?.countRegisterFiltered(
          appFeatureName,
          filters
        )
          ?: 0
      }
    }
  }

  override suspend fun countRegisterData(
    appFeatureName: String?,
    healthModule: HealthModule
  ): Long =
    withContext(dispatcherProvider.io()) {
      tracer.traceSuspend("${healthModule.name.camelCase()}.countRegisterData") {
        registerDaoFactory.registerDaoMap[healthModule]?.countRegisterData(appFeatureName) ?: 0
      }
    }

  override suspend fun loadPatientProfileData(
    appFeatureName: String?,
    healthModule: HealthModule,
    patientId: String
  ): ProfileData? =
    withContext(dispatcherProvider.io()) {
      tracer.traceSuspend("${healthModule.name.camelCase()}.loadPatientProfileData") {
        registerDaoFactory.registerDaoMap[healthModule]?.loadProfileData(
          appFeatureName = appFeatureName,
          resourceId = patientId
        )
      }
    }

  suspend fun loadChildrenRegisterData(
    healthModule: HealthModule,
    otherPatientResource: List<Resource>
  ): List<RegisterData> =
    withContext(dispatcherProvider.io()) {
      tracer.traceSuspend("${healthModule.name.camelCase()}}.loadChildrenRegisterData") {
        val dataList: ArrayList<Patient> = arrayListOf()
        val hivRegisterDao = registerDaoFactory.registerDaoMap[healthModule] as HivRegisterDao

        for (item: Resource in otherPatientResource) {
          val itemPatient = item as Patient
          dataList.add(itemPatient)
        }
        hivRegisterDao.transformChildrenPatientToRegisterData(dataList)
      }
    }

  private fun String.camelCase() =
    this.let {
      val u = indexOf('_')
      val newString = lowercase().replaceFirstChar { it.uppercase() }
      return@let if (u != -1)
        newString.replaceRange(u until u + 2, this.elementAt(u + 1).uppercase())
      else newString
    }
}
