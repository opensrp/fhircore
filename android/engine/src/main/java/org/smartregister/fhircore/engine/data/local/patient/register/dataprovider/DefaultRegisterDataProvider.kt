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

package org.smartregister.fhircore.engine.data.local.patient.register.dataprovider

import com.google.android.fhir.FhirEngine
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.Order
import com.google.android.fhir.search.search
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.engine.appfeature.AppFeature
import org.smartregister.fhircore.engine.data.domain.util.PaginationUtil
import org.smartregister.fhircore.engine.data.local.patient.PatientRegisterPagingSource.Companion.DEFAULT_PAGE_SIZE
import org.smartregister.fhircore.engine.domain.model.PatientProfileData
import org.smartregister.fhircore.engine.domain.model.RegisterRow
import org.smartregister.fhircore.engine.domain.repository.RegisterDataProvider
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.extension.extractAddress
import org.smartregister.fhircore.engine.util.extension.extractName

@Singleton
class DefaultRegisterDataProvider
@Inject
constructor(val fhirEngine: FhirEngine, val dispatcherProvider: DefaultDispatcherProvider) :
  RegisterDataProvider {

  override suspend fun provideRegisterData(appFeature: AppFeature?): List<RegisterRow> {
    return withContext(dispatcherProvider.io()) {
      val patients =
        fhirEngine.search<Patient> {
          filter(Patient.ACTIVE, { value = of(true) })
          sort(Patient.NAME, Order.ASCENDING)
          count = DEFAULT_PAGE_SIZE
          from = 0 * PaginationUtil.DEFAULT_PAGE_SIZE
        }

      patients.map {
        RegisterRow(it.logicalId, it.logicalId, it.extractName(), "Male", it.extractAddress())
      }
    }
  }

  override suspend fun provideProfileData(
    appFeature: AppFeature?,
    patientId: String
  ): PatientProfileData? {
    return null
  }
}
