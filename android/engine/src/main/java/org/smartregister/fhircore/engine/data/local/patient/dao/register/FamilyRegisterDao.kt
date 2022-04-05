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
import javax.inject.Inject
import javax.inject.Singleton
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.engine.appfeature.model.HealthModule
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.domain.model.ProfileData
import org.smartregister.fhircore.engine.domain.model.RegisterData
import org.smartregister.fhircore.engine.domain.repository.RegisterDao
import org.smartregister.fhircore.engine.domain.util.PaginationConstant
import org.smartregister.fhircore.engine.util.extension.filterBy

@Singleton
class FamilyRegisterDao
@Inject
constructor(
  val fhirEngine: FhirEngine,
  val defaultRepository: DefaultRepository,
  val configurationRegistry: ConfigurationRegistry
) : RegisterDao {

  override suspend fun loadRegisterData(
    currentPage: Int,
    loadAll: Boolean,
    appFeatureName: String?
  ): List<RegisterData> {
    val patients =
      fhirEngine.search<Patient> {
        getRegisterDataFilters().forEach { filterBy(it) }
        filter(Patient.ACTIVE, { value = of(true) })
        // filterByPatientName(query) TODO enable for search

        sort(Patient.NAME, Order.ASCENDING)
        count =
          if (loadAll) countRegisterData(appFeatureName).toInt()
          else PaginationConstant.DEFAULT_PAGE_SIZE
        from = currentPage * PaginationConstant.DEFAULT_PAGE_SIZE
      }

    return patients.map { p ->
      val members = defaultRepository.loadFamilyMembers(p.logicalId)
      val familyServices = defaultRepository.loadCarePlans(p.logicalId, getRegisterDataFilters())

      FamilyMapper.transformInputToOutputModel(Family(p, members, familyServices))
    }
  }

  override suspend fun loadProfileData(appFeatureName: String?, patientId: String): ProfileData {
    val family = fhirEngine.load(Patient::class.java, patientId)
    val members = defaultRepository.loadFamilyMembersDetails(family.logicalId)
    val familyServices = defaultRepository.loadCarePlans(family.logicalId, getRegisterDataFilters())

    return FamilyProfileMapper.transformInputToOutputModel(
      FamilyDetail(family, members, familyServices)
    )
  }

  override suspend fun countRegisterData(appFeatureName: String?): Long {
    return fhirEngine.count<Patient> {
      getRegisterDataFilters().forEach { filterBy(it) }
      filter(Patient.ACTIVE, { value = of(true) })
      // filterByPatientName(query) TODO enable for search
    }
  }

  private fun getRegisterDataFilters() =
    configurationRegistry.retrieveDataFilterConfiguration(HealthModule.FAMILY.name)
}
