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

package org.smartregister.fhircore.engine.data.local.patient.dao.register

import com.google.android.fhir.FhirEngine
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.Order
import com.google.android.fhir.search.search
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.engine.domain.model.PatientProfileData
import org.smartregister.fhircore.engine.domain.model.RegisterData
import org.smartregister.fhircore.engine.domain.repository.RegisterDao
import org.smartregister.fhircore.engine.domain.util.PaginationConstant
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.extension.countActivePatients
import org.smartregister.fhircore.engine.util.extension.extractAge
import org.smartregister.fhircore.engine.util.extension.extractName

@Singleton
class AncPatientRegisterDao
@Inject
constructor(val fhirEngine: FhirEngine, val dispatcherProvider: DefaultDispatcherProvider) :
  RegisterDao {

  override suspend fun loadRegisterData(
    currentPage: Int,
    loadAll: Boolean,
    appFeatureName: String?
  ): List<RegisterData> =
    // TODO Refactor to load correct ANC content
    withContext(dispatcherProvider.io()) {
      val patients =
        fhirEngine.search<Patient> {
          filter(Patient.ACTIVE, { value = of(true) })
          sort(Patient.NAME, Order.ASCENDING)
          count =
            if (loadAll) countRegisterData(appFeatureName).toInt()
            else PaginationConstant.DEFAULT_PAGE_SIZE
          from = currentPage * PaginationConstant.DEFAULT_PAGE_SIZE
        }

      patients.map {
        RegisterData.DefaultRegisterData(
          id = it.logicalId,
          name = it.extractName(),
          gender = it.gender,
          age = it.extractAge()
        )
      }
    }

  override suspend fun countRegisterData(appFeatureName: String?): Long {
    // TODO "Return count for Anc register clients"
    return withContext(dispatcherProvider.io()) { fhirEngine.countActivePatients() }
  }

  override suspend fun loadProfileData(
    appFeatureName: String?,
    patientId: String
  ): PatientProfileData? = null
}
