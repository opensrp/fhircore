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
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.data.local.register.dao.HivRegisterDao
import org.smartregister.fhircore.engine.data.local.register.dao.RegisterDaoFactory
import org.smartregister.fhircore.engine.domain.model.ProfileData
import org.smartregister.fhircore.engine.domain.model.RegisterData
import org.smartregister.fhircore.engine.domain.repository.RegisterRepository
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider

class PatientRegisterRepository
@Inject
constructor(
  override val fhirEngine: FhirEngine,
  override val dispatcherProvider: DefaultDispatcherProvider,
  val registerDaoFactory: RegisterDaoFactory
) :
  RegisterRepository,
  DefaultRepository(fhirEngine = fhirEngine, dispatcherProvider = dispatcherProvider) {

  override suspend fun loadRegisterData(
    currentPage: Int,
    loadAll: Boolean,
    appFeatureName: String?,
    healthModule: HealthModule
  ): List<RegisterData> =
    withContext(dispatcherProvider.io()) {
      registerDaoFactory.registerDaoMap[healthModule]?.loadRegisterData(
        currentPage = currentPage,
        appFeatureName = appFeatureName
      )
        ?: emptyList()
    }

  override suspend fun countRegisterData(
    appFeatureName: String?,
    healthModule: HealthModule
  ): Long =
    withContext(dispatcherProvider.io()) {
      registerDaoFactory.registerDaoMap[healthModule]?.countRegisterData(appFeatureName) ?: 0
    }

  override suspend fun loadPatientProfileData(
    appFeatureName: String?,
    healthModule: HealthModule,
    patientId: String
  ): ProfileData? =
    withContext(dispatcherProvider.io()) {
      registerDaoFactory.registerDaoMap[healthModule]?.loadProfileData(
        appFeatureName = appFeatureName,
        resourceId = patientId
      )
    }

  suspend fun loadChildrenRegisterData(
    healthModule: HealthModule,
    otherPatientResource: List<Resource>
  ): List<RegisterData> =
    withContext(dispatcherProvider.io()) {
      val dataList: ArrayList<Patient> = arrayListOf()
      val hivRegisterDao = registerDaoFactory.registerDaoMap[healthModule] as HivRegisterDao

      for (item: Resource in otherPatientResource) {
        val itemPatient = item as Patient
        dataList.add(itemPatient)
      }
      hivRegisterDao.transformChildrenPatientToRegisterData(dataList)
    }
}
