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

package org.smartregister.fhircore.engine.data.local.patient

import com.google.android.fhir.FhirEngine
import javax.inject.Inject
import org.smartregister.fhircore.engine.appfeature.model.HealthModule
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.data.local.patient.register.PatientRegisterDataProviderFactory
import org.smartregister.fhircore.engine.domain.model.PatientProfileData
import org.smartregister.fhircore.engine.domain.model.RegisterRowData
import org.smartregister.fhircore.engine.domain.repository.RegisterRepository
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider

class PatientRepository
@Inject
constructor(
  override val fhirEngine: FhirEngine,
  override val dispatcherProvider: DefaultDispatcherProvider,
  val registerDataProviderFactory: PatientRegisterDataProviderFactory
) :
  RegisterRepository,
  DefaultRepository(fhirEngine = fhirEngine, dispatcherProvider = dispatcherProvider) {

  override suspend fun loadRegisterData(
    currentPage: Int,
    loadAll: Boolean,
    appFeatureName: String?,
    healthModule: HealthModule?
  ): List<RegisterRowData> =
    when (healthModule) {
      null ->
        registerDataProviderFactory.defaultRegisterDataProvider.provideRegisterData(
          currentPage = currentPage,
          appFeatureName = appFeatureName
        )
      HealthModule.ANC ->
        registerDataProviderFactory.ancRegisterDataProvider.provideRegisterData(
          currentPage = currentPage,
          appFeatureName = appFeatureName
        )
      HealthModule.FAMILY ->
        registerDataProviderFactory.familyRegisterDataProvider.provideRegisterData(
          currentPage = currentPage,
          appFeatureName = appFeatureName
        )
      HealthModule.CHILD, HealthModule.RDT, HealthModule.PNC, HealthModule.FAMILY_PLANNING ->
        emptyList()
    }

  override suspend fun countRegisterData(
    appFeatureName: String?,
    healthModule: HealthModule?
  ): Long =
    when (healthModule) {
      null ->
        registerDataProviderFactory.defaultRegisterDataProvider.provideRegisterDataCount(
          appFeatureName
        )
      HealthModule.ANC ->
        registerDataProviderFactory.ancRegisterDataProvider.provideRegisterDataCount(appFeatureName)
      HealthModule.FAMILY ->
        registerDataProviderFactory.familyRegisterDataProvider.provideRegisterDataCount(
          appFeatureName
        )
      HealthModule.RDT, HealthModule.PNC, HealthModule.FAMILY_PLANNING, HealthModule.CHILD -> 0
    }

  override suspend fun loadPatientProfileData(
    appFeatureName: String?,
    healthModule: HealthModule?,
    patientId: String
  ): PatientProfileData? =
    when (healthModule) {
      null ->
        registerDataProviderFactory.defaultRegisterDataProvider.provideProfileData(
          appFeatureName = appFeatureName,
          patientId = patientId
        )
      HealthModule.ANC ->
        registerDataProviderFactory.ancRegisterDataProvider.provideProfileData(
          appFeatureName = appFeatureName,
          patientId = patientId
        )
      HealthModule.FAMILY ->
        registerDataProviderFactory.familyRegisterDataProvider.provideProfileData(
          appFeatureName = appFeatureName,
          patientId = patientId
        )
      HealthModule.CHILD, HealthModule.RDT, HealthModule.PNC, HealthModule.FAMILY_PLANNING -> null
    }
}
