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
import com.google.android.fhir.search.count
import com.google.android.fhir.search.search
import javax.inject.Inject
import javax.inject.Singleton
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.Flag
import org.hl7.fhir.r4.model.Group
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.RelatedPerson
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.Task
import org.smartregister.fhircore.engine.appfeature.model.HealthModule.FAMILY
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.domain.model.ProfileData
import org.smartregister.fhircore.engine.domain.model.RegisterData
import org.smartregister.fhircore.engine.domain.repository.RegisterDao
import org.smartregister.fhircore.engine.domain.util.PaginationConstant
import org.smartregister.fhircore.engine.util.extension.extractId
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
      fhirEngine.search<Group> {
        getRegisterDataFilters(FAMILY.name).forEach { filterBy(it) }
        count =
          if (loadAll) countRegisterData(appFeatureName).toInt()
          else PaginationConstant.DEFAULT_PAGE_SIZE
        from = currentPage * PaginationConstant.DEFAULT_PAGE_SIZE
      }
    return families.filter { it.active && !it.name.isNullOrEmpty() }.map { family ->
      val members = loadFamilyMembers(family)
      val familyDetail = loadFamilyDetail(family, members)

      FamilyRegisterDataMapper.transformInputToOutputModel(familyDetail)
    }
  }

  override suspend fun loadProfileData(appFeatureName: String?, resourceId: String): ProfileData {
    val family = fhirEngine.load(Group::class.java, resourceId)
    val members = loadFamilyMembersDetails(family)
    val familyDetail = loadFamilyDetail(family, members)

    return FamilyProfileDataMapper.transformInputToOutputModel(familyDetail)
  }

  override suspend fun countRegisterData(appFeatureName: String?): Long {
    // TODO fix this workaround for groups count
    return fhirEngine
      .search<Group> { getRegisterDataFilters(FAMILY.name).forEach { filterBy(it) } }
      .filter { it.active && !it.name.isNullOrEmpty() }
      .size
      .toLong()
  }

  suspend fun loadFamilyDetail(family: Group, members: List<FamilyMemberDetail>): FamilyDetail {
    // In some cases the head could be null
    var head: FamilyMemberDetail? = null
    if (family.hasManagingEntity()) {
      head =
        family.managingEntity.let { reference ->
          fhirEngine
            .search<RelatedPerson> {
              filterByResourceTypeId(
                RelatedPerson.RES_ID,
                ResourceType.RelatedPerson,
                reference.extractId()
              )
            }
            .firstOrNull()
            ?.let { relatedPerson ->
              val patient =
                fhirEngine
                  .search<Patient> {
                    filterByResourceTypeId(
                      Patient.RES_ID,
                      ResourceType.Patient,
                      relatedPerson.patient.extractId()
                    )
                  }
                  .first()
              val conditions = loadMemberCondition(patient)
              val carePlans = loadMemberCarePlan(patient)
              val tasks = loadMemberTask(patient)

              FamilyMemberDetail(
                patient = patient,
                conditions = conditions,
                servicesDue = carePlans,
                tasks = tasks
              )
            }
        }
    }
    val familyServices =
      defaultRepository.searchResourceFor<CarePlan>(
        subjectId = family.logicalId,
        subjectParam = CarePlan.SUBJECT,
        filters = getRegisterDataFilters(FAMILY_CARE_PLAN)
      )

    return FamilyDetail(
      family = family,
      head = head,
      members = members,
      servicesDue = familyServices
    )
  }

  suspend fun loadFamilyMembers(family: Group) =
    family.member?.map { member ->
      fhirEngine.load(Patient::class.java, member.entity.extractId()).let { patient ->
        val conditions = loadMemberCondition(patient)
        val carePlans = loadMemberCarePlan(patient)
        val tasks = loadMemberTask(patient)

        FamilyMemberDetail(
          patient = patient,
          conditions = conditions,
          servicesDue = carePlans,
          tasks = tasks
        )
      }
    }
      ?: listOf()

  private suspend fun loadMemberCondition(patient: Patient) =
    fhirEngine.search<Condition> {
      filterByResourceTypeId(Condition.SUBJECT, ResourceType.Patient, patient.logicalId)
    }

  private suspend fun loadMemberCarePlan(patient: Patient) =
    fhirEngine.search<CarePlan> {
      filterByResourceTypeId(CarePlan.SUBJECT, ResourceType.Patient, patient.logicalId)
      filter(CarePlan.STATUS, { value = of(CarePlan.CarePlanStatus.ACTIVE.toCoding()) })
    }

  private suspend fun loadMemberTask(patient: Patient) =
    fhirEngine.search<Task> {
      filterByResourceTypeId(Task.SUBJECT, ResourceType.Patient, patient.logicalId)
    }

  suspend fun loadFamilyMembersDetails(family: Group) =
    loadFamilyMembers(family).map {
      val flags =
        fhirEngine.search<Flag> {
          filterByResourceTypeId(Flag.SUBJECT, ResourceType.Patient, it.patient.id)
        }

      FamilyMemberDetail(
        patient = it.patient,
        conditions = it.conditions,
        flags = flags,
        servicesDue = it.servicesDue
      )
    }

  private fun getRegisterDataFilters(id: String) =
    configurationRegistry.retrieveDataFilterConfiguration(id)

  companion object {
    const val FAMILY_CARE_PLAN = "family_care_plan"
  }
}
