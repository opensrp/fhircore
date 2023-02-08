/*
 * Copyright 2021-2023 Ona Systems, Inc
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
import com.google.android.fhir.get
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.search
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.DataRequirement
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Group
import org.hl7.fhir.r4.model.IdType
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.RelatedPerson
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.domain.model.DataQuery
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.addTags
import org.smartregister.fhircore.engine.util.extension.asReference
import org.smartregister.fhircore.engine.util.extension.extractId
import org.smartregister.fhircore.engine.util.extension.extractLogicalIdUuid
import org.smartregister.fhircore.engine.util.extension.filterBy
import org.smartregister.fhircore.engine.util.extension.filterByResourceTypeId
import org.smartregister.fhircore.engine.util.extension.generateMissingId
import org.smartregister.fhircore.engine.util.extension.loadResource
import org.smartregister.fhircore.engine.util.extension.resourceClassType
import org.smartregister.fhircore.engine.util.extension.updateFrom
import org.smartregister.fhircore.engine.util.extension.updateLastUpdated

@Singleton
open class DefaultRepository
@Inject
constructor(
  open val fhirEngine: FhirEngine,
  open val dispatcherProvider: DispatcherProvider,
  open val sharedPreferencesHelper: SharedPreferencesHelper,
  open val configurationRegistry: ConfigurationRegistry,
  open val configService: ConfigService
) {

  suspend inline fun <reified T : Resource> loadResource(resourceId: String): T? {
    return withContext(dispatcherProvider.io()) { fhirEngine.loadResource(resourceId) }
  }

  suspend fun loadResource(resourceId: String, resourceType: ResourceType): Resource =
    withContext(dispatcherProvider.io()) { fhirEngine.get(resourceType, resourceId) }

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

  suspend fun create(addResourceTags: Boolean = true, vararg resource: Resource): List<String> {
    return withContext(dispatcherProvider.io()) {
      resource.onEach {
        it.generateMissingId()
        if (addResourceTags) {
          it.addTags(configService.provideResourceTags(sharedPreferencesHelper))
        }
      }

      fhirEngine.create(*resource)
    }
  }

  suspend fun delete(resource: Resource) {
    return withContext(dispatcherProvider.io()) {
      fhirEngine.delete(resource.resourceType, resource.logicalId)
    }
  }

  suspend fun <R : Resource> addOrUpdate(addMandatoryTags: Boolean = true, resource: R) {
    return withContext(dispatcherProvider.io()) {
      resource.updateLastUpdated()
      try {
        fhirEngine.get(resource.resourceType, resource.logicalId).run {
          fhirEngine.update(updateFrom(resource))
        }
      } catch (resourceNotFoundException: ResourceNotFoundException) {
        create(addMandatoryTags, resource)
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
        this.relationshipFirstRep.codingFirstRep.code = "99990006"
        this.relationshipFirstRep.codingFirstRep.display = "Family Head"
        this.patient = patient.asReference()
        this.id = UUID.randomUUID().toString()
      }

    create(true, relatedPerson)
    val group =
      fhirEngine.get<Group>(groupId).apply {
        managingEntity = relatedPerson.asReference()
        name = relatedPerson.name.firstOrNull()?.family
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
                addOrUpdate(resource = patient)
              }
            }
          }
        }
        member.clear()
        active = false
      }
      addOrUpdate(resource = group)
    }
  }

  /** Remove member of a group using the provided [memberId] and [groupMemberResourceType] */
  suspend fun removeGroupMember(
    memberId: String,
    groupId: String?,
    groupMemberResourceType: String?
  ) {
    val memberResourceType =
      groupMemberResourceType?.resourceClassType()?.newInstance()?.resourceType
    val fhirResource: Resource? =
      try {
        if (memberResourceType == null) {
          return
        }
        fhirEngine.get(memberResourceType, memberId.extractLogicalIdUuid())
      } catch (resourceNotFoundException: ResourceNotFoundException) {
        null
      }

    fhirResource?.let { resource ->
      if (resource is Patient) {
        resource.active = false
      }

      if (groupId != null) {
        loadResource<Group>(groupId)?.let { group ->
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
              if (relatedPerson.patient.id.extractLogicalIdUuid() == memberId) {
                delete(relatedPerson)
                group.managingEntity = null
              }
            }

          // Update this group resource
          addOrUpdate(resource = group)
        }
      }
      addOrUpdate(resource = resource)
    }
  }

  suspend fun delete(resourceType: String, resourceId: String) {
    fhirEngine.delete(resourceType.resourceClassType().newInstance().resourceType, resourceId)
  }
}
