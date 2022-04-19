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

package org.smartregister.fhircore.engine.data.local.patient.dao.register.family

import com.google.android.fhir.FhirEngine
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.Order
import com.google.android.fhir.search.count
import com.google.android.fhir.search.search
import org.hl7.fhir.r4.model.*
import javax.inject.Inject
import javax.inject.Singleton
import org.smartregister.fhircore.engine.appfeature.model.HealthModule
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.domain.model.ProfileData
import org.smartregister.fhircore.engine.domain.model.RegisterData
import org.smartregister.fhircore.engine.domain.repository.RegisterDao
import org.smartregister.fhircore.engine.domain.util.PaginationConstant
import org.smartregister.fhircore.engine.util.extension.filterBy
import org.smartregister.fhircore.engine.util.extension.filterByResourceTypeId
import org.smartregister.fhircore.engine.util.extension.toCoding

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
    val families =
      fhirEngine.search<Group>{
        getRegisterDataFilters().forEach(){ filterBy(it) }
        filter(Group.CODE, { value = of("{ \"system\": \"https://www.snomed.org\", \"code\": \"35359004\", \"display\": \"Family\" }") })

        count =
          if (loadAll) countRegisterData(appFeatureName).toInt()
          else PaginationConstant.DEFAULT_PAGE_SIZE
        from = currentPage * PaginationConstant.DEFAULT_PAGE_SIZE
      }

    return families.map { family ->
      val members = loadFamilyMembers(family)
      val familyServices = loadFamilyServices(family.logicalId)

      FamilyRegisterDataMapper.transformInputToOutputModel(
        FamilyDetail(family, members, familyServices)
      )
    }
  }

  override suspend fun loadProfileData(appFeatureName: String?, familyId: String): ProfileData {
    val family = fhirEngine.load(Group::class.java, familyId)

    val members = loadFamilyMembers(family)

    val familyServices = loadFamilyServices(family.logicalId)

    return FamilyProfileDataMapper.transformInputToOutputModel(
      FamilyDetail(family, members, familyServices)
    )
  }

  override suspend fun countRegisterData(appFeatureName: String?): Long {
    return fhirEngine.count<Patient> {
      getRegisterDataFilters().forEach { filterBy(it) }
      filter(Patient.ACTIVE, { value = of(true) })
    }
  }

  suspend fun loadFamilyMembers(family: Group) =
    family.member.map {

      val patient = fhirEngine.load(Patient::class.java, it.id)

      val conditions =
        fhirEngine.search<Condition> {
          filterByResourceTypeId(Condition.SUBJECT, ResourceType.Patient, patient.logicalId)
        }
      val carePlans =
        fhirEngine.search<CarePlan> {
          filterByResourceTypeId(CarePlan.SUBJECT, ResourceType.Patient, patient.logicalId)
          filter(CarePlan.STATUS, { value = of(CarePlan.CarePlanStatus.ACTIVE.toCoding()) })
        }
      FamilyMemberDetail(patient = patient, conditions = conditions, servicesDue = carePlans)
    }

  suspend fun loadFamilyServices(familyId: String) =
    defaultRepository.searchResourceFor<CarePlan>(
      subjectId = familyId,
      subjectParam = CarePlan.SUBJECT,
      filters = getRegisterDataFilters()
    )

  private fun getRegisterDataFilters() =
    configurationRegistry.retrieveDataFilterConfiguration(HealthModule.FAMILY.name)
}
