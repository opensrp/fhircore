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

package org.smartregister.fhircore.engine.data.local.register.dao

import com.google.android.fhir.FhirEngine
import com.google.android.fhir.get
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.search
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.Flag
import org.hl7.fhir.r4.model.Group
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.RelatedPerson
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.Task
import org.smartregister.fhircore.engine.appfeature.model.HealthModule.FAMILY
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.domain.model.FamilyMemberProfileData
import org.smartregister.fhircore.engine.domain.model.ProfileData
import org.smartregister.fhircore.engine.domain.model.RegisterData
import org.smartregister.fhircore.engine.domain.repository.RegisterDao
import org.smartregister.fhircore.engine.domain.util.PaginationConstant
import org.smartregister.fhircore.engine.util.extension.DAYS_IN_YEAR
import org.smartregister.fhircore.engine.util.extension.asReference
import org.smartregister.fhircore.engine.util.extension.daysPassed
import org.smartregister.fhircore.engine.util.extension.due
import org.smartregister.fhircore.engine.util.extension.extractAddress
import org.smartregister.fhircore.engine.util.extension.extractAge
import org.smartregister.fhircore.engine.util.extension.extractDeathDate
import org.smartregister.fhircore.engine.util.extension.extractId
import org.smartregister.fhircore.engine.util.extension.extractName
import org.smartregister.fhircore.engine.util.extension.filterBy
import org.smartregister.fhircore.engine.util.extension.filterByResourceTypeId
import org.smartregister.fhircore.engine.util.extension.hasActivePregnancy
import org.smartregister.fhircore.engine.util.extension.isFamilyHead
import org.smartregister.fhircore.engine.util.extension.lastSeenFormat
import org.smartregister.fhircore.engine.util.extension.overdue
import org.smartregister.fhircore.engine.util.extension.toAgeDisplay
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
      val members: List<RegisterData.FamilyMemberRegisterData> =
        family.member?.filter { it.hasEntity() && it.entity.hasReference() }?.mapNotNull {
          loadFamilyMemberRegisterData(it.entity.extractId())
        }
          ?: listOf()

      val familyHead = loadFamilyHead(family)

      RegisterData.FamilyRegisterData(
        logicalId = family.logicalId,
        name = family.name,
        identifier = family.extractOfficialIdentifier(),
        address = familyHead?.extractAddress() ?: "",
        head = familyHead?.let { loadFamilyMemberRegisterData(familyHead.logicalId) },
        members = members,
        servicesDue = members.sumOf { it.servicesDue ?: 0 },
        servicesOverdue = members.sumOf { it.servicesOverdue ?: 0 },
        lastSeen = family.meta?.lastUpdated.lastSeenFormat()
      )
    }
  }

  override suspend fun loadProfileData(appFeatureName: String?, resourceId: String): ProfileData {
    val family = defaultRepository.loadResource<Group>(resourceId)!!
    val familyHead = loadFamilyHead(family)

    return ProfileData.FamilyProfileData(
      logicalId = family.logicalId,
      name = family.name,
      identifier = family.extractOfficialIdentifier(),
      address = familyHead?.extractAddress() ?: "",
      age = familyHead?.extractAge() ?: "",
      head = familyHead?.let { loadFamilyMemberProfileData(familyHead.logicalId) },
      members =
        family.member?.filter { it.hasEntity() && it.entity.hasReference() }?.mapNotNull {
          loadFamilyMemberProfileData(it.entity.extractId())
        }
          ?: listOf(),
      services =
        defaultRepository.searchResourceFor(
          subjectId = family.logicalId,
          subjectParam = CarePlan.SUBJECT,
          filters = getRegisterDataFilters(FAMILY_CARE_PLAN)
        ),
      tasks =
        defaultRepository.searchResourceFor<Task>(
            subjectId = family.logicalId,
            subjectParam = Task.SUBJECT,
            subjectType = ResourceType.Group
          )
          .let { it.sortedBy { it.executionPeriod.start.time } }
    )
  }

  override suspend fun countRegisterData(appFeatureName: String?): Long {
    // TODO fix this workaround for groups count
    return fhirEngine
      .search<Group> { getRegisterDataFilters(FAMILY.name).forEach { filterBy(it) } }
      .filter { it.active && !it.name.isNullOrEmpty() }
      .size
      .toLong()
  }

  private suspend fun loadFamilyHead(family: Group) =
    family.managingEntity?.let { reference ->
      defaultRepository
        .searchResourceFor<RelatedPerson>(
          token = RelatedPerson.RES_ID,
          subjectType = ResourceType.RelatedPerson,
          subjectId = reference.extractId()
        )
        .firstOrNull()
        ?.let { relatedPerson ->
          defaultRepository
            .searchResourceFor<Patient>(
              token = Patient.RES_ID,
              subjectType = ResourceType.Patient,
              subjectId = relatedPerson.patient.extractId()
            )
            .firstOrNull()
        }
    }

  suspend fun changeFamilyHead(newFamilyHead: String, oldFamilyHead: String) {

    val patient = fhirEngine.get<Patient>(newFamilyHead)

    // TODO create a utility/extension function for creating RelatedPersonResource
    val relatedPerson =
      RelatedPerson().apply {
        this.active = true
        this.name = patient.name
        this.birthDate = patient.birthDate
        this.telecom = patient.telecom
        this.address = patient.address
        this.gender = patient.gender
        this.relationshipFirstRep.codingFirstRep.system =
          "http://hl7.org/fhir/ValueSet/relatedperson-relationshiptype"
        this.patient = patient.asReference()
        this.id = UUID.randomUUID().toString()
      }

    fhirEngine.create(relatedPerson)
    val family =
      fhirEngine.get<Group>(oldFamilyHead).apply {
        managingEntity = relatedPerson.asReference()
        name = relatedPerson.name.first().nameAsSingleString
      }
    fhirEngine.update(family)
  }

  suspend fun removeFamily(familyId: String, isDeactivateMembers: Boolean?) {
    defaultRepository.loadResource<Group>(familyId)?.let { family ->
      if (!family.active) throw IllegalStateException("Family already deleted")
      family
        .managingEntity
        ?.let { reference ->
          defaultRepository.searchResourceFor<RelatedPerson>(
            token = RelatedPerson.RES_ID,
            subjectType = ResourceType.RelatedPerson,
            subjectId = reference.extractId()
          )
        }
        ?.firstOrNull()
        ?.let { relatedPerson -> defaultRepository.delete(relatedPerson) }
      family.managingEntity = null
      isDeactivateMembers?.let {
        if (it) {
          family.member.map { member ->
            defaultRepository.loadResource<Patient>(member.entity.extractId())?.let { patient ->
              patient.active = false
              defaultRepository.addOrUpdate(patient)
            }
          }
        }
      }
      family.member.clear()
      family.active = false

      defaultRepository.addOrUpdate(family)
    }
  }

  suspend fun removeFamilyMember(patientId: String, familyId: String?) {
    defaultRepository.loadResource<Patient>(patientId)?.let { patient ->
      if (!patient.active) throw IllegalStateException("Patient already deleted")
      patient.active = false

      if (familyId != null) {
        defaultRepository.loadResource<Group>(familyId)?.let { family ->
          family.member.run {
            remove(this.find { it.entity.reference == "Patient/${patient.logicalId}" })
          }
          family
            .managingEntity
            ?.let { reference ->
              defaultRepository.searchResourceFor<RelatedPerson>(
                token = RelatedPerson.RES_ID,
                subjectType = ResourceType.RelatedPerson,
                subjectId = reference.extractId()
              )
            }
            ?.firstOrNull()
            ?.let { relatedPerson ->
              if (relatedPerson.patient.id == patientId) {
                defaultRepository.delete(relatedPerson)
                family.managingEntity = null
              }
            }
        }
      }
      defaultRepository.addOrUpdate(patient)
    }
  }

  private suspend fun loadFamilyMemberRegisterData(
    memberId: String
  ): RegisterData.FamilyMemberRegisterData? {
    return defaultRepository.loadResource<Patient>(memberId)?.let { patient ->
      if (!patient.active) return null
      val conditions = loadMemberCondition(patient.logicalId)
      val carePlans = loadMemberCarePlan(patient.logicalId)
      RegisterData.FamilyMemberRegisterData(
        logicalId = patient.logicalId,
        name = patient.extractName(),
        age = patient.birthDate?.let { it.daysPassed() / DAYS_IN_YEAR }?.toString(),
        birthdate = patient.birthDate,
        gender = patient.gender.display.first().toString(),
        pregnant = conditions.hasActivePregnancy(),
        isHead = patient.isFamilyHead(),
        deathDate = patient.extractDeathDate(),
        servicesDue = carePlans.filter { it.due() }.flatMap { it.activity }.size,
        servicesOverdue = carePlans.filter { it.overdue() }.flatMap { it.activity }.size
      )
    }
  }

  private suspend fun loadFamilyMemberProfileData(memberId: String): FamilyMemberProfileData? =
    defaultRepository.loadResource<Patient>(memberId)?.let { patient ->
      if (!patient.active) return null
      val conditions = loadMemberCondition(patient.logicalId)
      val carePlans = loadMemberCarePlan(patient.logicalId)
      val tasks = loadMemberTask(patient.logicalId)
      val flags = loadMemberFlags(patient.logicalId)

      FamilyMemberProfileData(
        id = patient.logicalId,
        name = patient.extractName(),
        age = patient.birthDate.toAgeDisplay(),
        birthdate = patient.birthDate,
        gender = patient.gender,
        pregnant = conditions.hasActivePregnancy(),
        isHead = patient.isFamilyHead(),
        deathDate = patient.extractDeathDate(),
        conditions = conditions,
        flags = flags,
        services = carePlans,
        tasks = tasks
      )
    }

  private suspend fun loadMemberCondition(patientId: String) =
    defaultRepository.searchResourceFor<Condition>(
      subjectId = patientId,
      subjectParam = Condition.SUBJECT,
      subjectType = ResourceType.Patient
    )

  private suspend fun loadMemberCarePlan(patientId: String) =
    fhirEngine.search<CarePlan> {
      filterByResourceTypeId(CarePlan.SUBJECT, ResourceType.Patient, patientId)
      filter(CarePlan.STATUS, { value = of(CarePlan.CarePlanStatus.ACTIVE.toCoding()) })
    }

  private suspend fun loadMemberTask(patientId: String) =
    defaultRepository.searchResourceFor<Task>(
        subjectId = patientId,
        subjectParam = Task.SUBJECT,
        subjectType = ResourceType.Patient
      )
      .let { it.sortedBy { it.executionPeriod.start.time } }

  private fun Group.extractOfficialIdentifier(): String? =
    if (this.hasIdentifier())
      this.identifier.firstOrNull { it.use == Identifier.IdentifierUse.OFFICIAL }?.value
    else null

  private suspend fun loadMemberFlags(patientId: String) =
    defaultRepository.searchResourceFor<Flag>(
      subjectId = patientId,
      subjectParam = Flag.SUBJECT,
      subjectType = ResourceType.Patient
    )

  private fun getRegisterDataFilters(id: String) =
    configurationRegistry.retrieveDataFilterConfiguration(id)

  companion object {
    const val FAMILY_CARE_PLAN = "family_care_plan"
  }
}
