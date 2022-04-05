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
import com.google.android.fhir.search.count
import com.google.android.fhir.search.search
import org.hl7.fhir.r4.model.Patient
import javax.inject.Inject
import javax.inject.Singleton
import org.smartregister.fhircore.engine.domain.model.ProfileData
import org.smartregister.fhircore.engine.domain.model.RegisterData
import org.smartregister.fhircore.engine.domain.repository.RegisterDao
import org.smartregister.fhircore.engine.domain.util.PaginationConstant
import org.smartregister.fhircore.engine.util.extension.extractFamilyTag
import org.smartregister.fhircore.engine.util.extension.filterBy
import org.smartregister.fhircore.engine.util.extension.filterByPatientName

@Singleton
class FamilyRegisterDao @Inject constructor(val fhirEngine: FhirEngine) : RegisterDao {

  override suspend fun loadRegisterData(
    currentPage: Int,
    loadAll: Boolean,
    appFeatureName: String?
  ): List<RegisterData> {
    val patients =
            fhirEngine.search<Patient> {
              filterBy(registerConfig.primaryFilter!!)
              filter(Patient.ACTIVE, { value = of(true) })
              // filterByPatientName(query) TODO enable for search

              sort(Patient.NAME, Order.ASCENDING)
              count = if (loadAll) countRegisterData(appFeatureName).toInt() else PaginationConstant.DEFAULT_PAGE_SIZE
              from = currentPage * PaginationConstant.DEFAULT_PAGE_SIZE
            }

    return patients.map { p ->
      val members = searchFamilyMembers(p.logicalId)

      val familyServices = ancPatientRepository.searchCarePlan(p.logicalId, p.extractFamilyTag())
      dataMapper.transformInputToOutputModel(Family(p, members, familyServices))
    }

  }

  override suspend fun loadProfileData(appFeatureName: String?, patientId: String): ProfileData? {
    // TODO Load profile data for family
    return ProfileData.FamilyProfileData()
  }

  override suspend fun countRegisterData(appFeatureName: String?): Long {
      return fhirEngine.count<Patient> {
          filterBy(registerConfig.primaryFilter!!)
          filter(Patient.ACTIVE, { value = of(true) })
          // filterByPatientName(query) TODO enable for search
      }
  }
}
