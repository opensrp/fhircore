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

package org.smartregister.fhircore.engine.data.local

import ca.uhn.fhir.rest.gclient.ReferenceClientParam
import ca.uhn.fhir.rest.gclient.TokenClientParam
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.db.ResourceNotFoundException
import com.google.android.fhir.delete
import com.google.android.fhir.get
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.search
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.Binary
import org.hl7.fhir.r4.model.Composition
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.DataRequirement
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Group
import org.hl7.fhir.r4.model.IdType
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.RelatedPerson
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.domain.model.DataQuery
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.extension.asReference
import org.smartregister.fhircore.engine.util.extension.extractId
import org.smartregister.fhircore.engine.util.extension.filterBy
import org.smartregister.fhircore.engine.util.extension.filterByResourceTypeId
import org.smartregister.fhircore.engine.util.extension.generateMissingId
import org.smartregister.fhircore.engine.util.extension.loadResource
import org.smartregister.fhircore.engine.util.extension.updateFrom
import org.smartregister.fhircore.engine.util.extension.updateLastUpdated

@Singleton
open class DefaultRepository
@Inject
constructor(open val fhirEngine: FhirEngine, open val dispatcherProvider: DispatcherProvider) {

  suspend inline fun <reified T : Resource> loadResource(resourceId: String): T? {
    return withContext(dispatcherProvider.io()) { fhirEngine.loadResource(resourceId) }
  }

  suspend fun loadResource(reference: Reference) =
    withContext(dispatcherProvider.io()) {
      IdType(reference.reference).let {
        fhirEngine.get(ResourceType.fromCode(it.resourceType), it.idPart)
      }
    }

  suspend inline fun <reified T : Resource> searchResourceFor(
    subjectId: String,
    subjectType: ResourceType = ResourceType.Patient,
    subjectParam: ReferenceClientParam,
    filters: List<DataQuery>? = null
  ): List<T> =
    fhirEngine.search {
      filterByResourceTypeId(subjectParam, subjectType, subjectId)
      filters?.forEach { filterBy(it) }
    }

  suspend inline fun <reified T : Resource> searchResourceFor(
    token: TokenClientParam,
    subjectType: ResourceType,
    subjectId: String,
    filters: List<DataQuery> = listOf()
  ): List<T> =
    withContext(dispatcherProvider.io()) {
      fhirEngine.search {
        filterByResourceTypeId(token, subjectType, subjectId)
        filters.forEach { filterBy(it) }
      }
    }

  suspend fun search(dataRequirement: DataRequirement) =
    when (dataRequirement.type) {
      Enumerations.ResourceType.CONDITION.toCode() ->
        fhirEngine.search<Condition> {
          dataRequirement.codeFilter.forEach {
            filter(TokenClientParam(it.path), { value = of(it.codeFirstRep) })
          }
          // TODO handle date filter
        }
      else -> listOf()
    }

  suspend fun searchCompositionByIdentifier(identifier: String): Composition? =
    fhirEngine
      .search<Composition> {
        filter(Composition.IDENTIFIER, { value = of(Identifier().apply { value = identifier }) })
      }
      .firstOrNull()

  suspend fun getBinary(id: String): Binary = fhirEngine.get(id)

  suspend fun save(resource: Resource) {
    return withContext(dispatcherProvider.io()) {
      resource.generateMissingId()
      fhirEngine.create(resource)
    }
  }

  suspend fun delete(resource: Resource) {
    return withContext(dispatcherProvider.io()) { fhirEngine.delete<Resource>(resource.logicalId) }
  }

  suspend fun <R : Resource> addOrUpdate(resource: R) {
    return withContext(dispatcherProvider.io()) {
      resource.updateLastUpdated()
      try {
        fhirEngine.get(resource.resourceType, resource.logicalId).run {
          fhirEngine.update(updateFrom(resource))
        }
      } catch (resourceNotFoundException: ResourceNotFoundException) {
        resource.generateMissingId()
        fhirEngine.create(resource)
      }
    }
  }

  suspend fun loadManagingEntity(group: Group) =
    group.managingEntity?.let { reference ->
      searchResourceFor<RelatedPerson>(
        token = RelatedPerson.RES_ID,
        subjectType = ResourceType.RelatedPerson,
        subjectId = reference.extractId()
      )
        .firstOrNull()
        ?.let { relatedPerson ->
          searchResourceFor<Patient>(
              token = Patient.RES_ID,
              subjectType = ResourceType.Patient,
              subjectId = relatedPerson.patient.extractId()
            )
            .firstOrNull()
        }
    }

  suspend fun changeManagingEntity(newManagingEntityId: String, groupId: String) {

    val patient = fhirEngine.get<Patient>(newManagingEntityId)

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
    val group =
      fhirEngine.get<Group>(groupId).apply {
        managingEntity = relatedPerson.asReference()
        name = relatedPerson.name.first().nameAsSingleString
      }
    fhirEngine.update(group)
  }

  suspend fun removeGroup(groupId: String, isDeactivateMembers: Boolean?) {
    loadResource<Group>(groupId)?.let { group ->
      if (!group.active) throw IllegalStateException("Group already deleted")
      group
        .managingEntity
        ?.let { reference ->
          searchResourceFor<RelatedPerson>(
            token = RelatedPerson.RES_ID,
            subjectType = ResourceType.RelatedPerson,
            subjectId = reference.extractId()
          )
        }
        ?.firstOrNull()
        ?.let { relatedPerson -> delete(relatedPerson) }

      group.apply {
        managingEntity = null
        isDeactivateMembers?.let {
          if (it) {
            member.map { thisMember ->
              loadResource<Patient>(thisMember.entity.extractId())?.let { patient ->
                patient.active = false
                addOrUpdate(patient)
              }
            }
          }
        }
        member.clear()
        active = false
      }
      addOrUpdate(group)
    }
  }

  /** Remove member of a group using the provided [memberId] */
  suspend fun removeGroupMember(memberId: String, groupId: String?) {
    // TODO refactor to work with any resource type
    // TODO provide resourceType as param
    loadResource<Patient>(memberId)?.let { patient ->
      if (!patient.active) throw IllegalStateException("Patient already deleted")
      patient.active = false

      if (groupId != null) {
        loadResource<Group>(groupId)?.let { group ->
          group.member.run {
            remove(this.find { it.entity.reference == "Patient/${patient.logicalId}" })
          }
          group
            .managingEntity
            ?.let { reference ->
              searchResourceFor<RelatedPerson>(
                token = RelatedPerson.RES_ID,
                subjectType = ResourceType.RelatedPerson,
                subjectId = reference.extractId()
              )
            }
            ?.firstOrNull()
            ?.let { relatedPerson ->
              if (relatedPerson.patient.id == memberId) {
                delete(relatedPerson)
                group.managingEntity = null
              }
            }
        }
      }
      addOrUpdate(patient)
    }
  }
}
