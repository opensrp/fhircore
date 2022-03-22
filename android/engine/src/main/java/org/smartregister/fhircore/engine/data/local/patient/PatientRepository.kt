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
import org.smartregister.fhircore.engine.appfeature.AppFeature
import org.smartregister.fhircore.engine.appfeature.model.HealthModule
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.data.local.patient.register.PatientRegisterDataProviderFactory
import org.smartregister.fhircore.engine.domain.model.PatientProfileData
import org.smartregister.fhircore.engine.domain.model.RegisterRow
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
    appFeature: AppFeature?,
    healthModule: HealthModule?
  ): List<RegisterRow> =
    when (healthModule) {
      null -> emptyList()
      HealthModule.ANC ->
        registerDataProviderFactory.ancRegisterDataProvider.provideRegisterData(appFeature)
      HealthModule.RDT, HealthModule.PNC, HealthModule.FAMILY_PLANNING ->
        registerDataProviderFactory.defaultRegisterDataProvider.provideRegisterData(appFeature)
      HealthModule.FAMILY ->
        registerDataProviderFactory.familyRegisterDataProvider.provideRegisterData(appFeature)
      HealthModule.CHILD ->
        registerDataProviderFactory.eirRegisterDataProvider.provideRegisterData(appFeature)
    }

  override suspend fun countRegisterData(
    appFeature: AppFeature?,
    healthModule: HealthModule?
  ): Long = 0

  override suspend fun loadPatientProfileData(
    appFeature: AppFeature?,
    healthModule: HealthModule?,
    patientId: String
  ): PatientProfileData? =
    when (healthModule) {
      null -> null
      HealthModule.ANC ->
        registerDataProviderFactory.ancRegisterDataProvider.provideProfileData(
          appFeature = appFeature,
          patientId = patientId
        )
      HealthModule.RDT, HealthModule.PNC, HealthModule.FAMILY_PLANNING ->
        registerDataProviderFactory.defaultRegisterDataProvider.provideProfileData(
          appFeature = appFeature,
          patientId = patientId
        )
      HealthModule.FAMILY ->
        registerDataProviderFactory.familyRegisterDataProvider.provideProfileData(
          appFeature = appFeature,
          patientId = patientId
        )
      HealthModule.CHILD ->
        registerDataProviderFactory.eirRegisterDataProvider.provideProfileData(
          appFeature = appFeature,
          patientId = patientId
        )
    }
}
