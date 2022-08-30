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

import android.content.Context
import android.content.res.AssetManager
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.gclient.ReferenceClientParam
import ca.uhn.fhir.rest.gclient.TokenClientParam
import ca.uhn.fhir.util.UrlUtil
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.db.ResourceNotFoundException
import com.google.android.fhir.delete
import com.google.android.fhir.get
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.search
import com.google.android.fhir.workflow.FhirOperator
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.Binary
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Composition
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.DataRequirement
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Group
import org.hl7.fhir.r4.model.IdType
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Library
import org.hl7.fhir.r4.model.Measure
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.RelatedArtifact
import org.hl7.fhir.r4.model.RelatedPerson
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.domain.model.DataQuery
import org.smartregister.fhircore.engine.sync.SyncStrategy
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.addMandatoryTags
import org.smartregister.fhircore.engine.util.extension.asReference
import org.smartregister.fhircore.engine.util.extension.extractId
import org.smartregister.fhircore.engine.util.extension.filterBy
import org.smartregister.fhircore.engine.util.extension.filterByResourceTypeId
import org.smartregister.fhircore.engine.util.extension.generateMissingId
import org.smartregister.fhircore.engine.util.extension.isIn
import org.smartregister.fhircore.engine.util.extension.loadResource
import org.smartregister.fhircore.engine.util.extension.updateFrom
import org.smartregister.fhircore.engine.util.extension.updateLastUpdated
import org.smartregister.model.practitioner.KeycloakUserDetails
import timber.log.Timber

@Singleton
open class DefaultRepository
@Inject
constructor(
  open val fhirEngine: FhirEngine,
  open val dispatcherProvider: DispatcherProvider,
  open val sharedPreferencesHelper: SharedPreferencesHelper
) {

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

  suspend fun create(vararg resource: Resource): List<String> {
    val syncStrategy = mutableMapOf<String, List<String>>()

    sharedPreferencesHelper.read<List<String>>(
        key = SharedPreferenceKey.PRACTITIONER_DETAILS_CARE_TEAM_IDS.name
      )
      ?.let { careTeamIds -> syncStrategy[SyncStrategy.CARE_TEAM.value] = careTeamIds }

    sharedPreferencesHelper.read<List<String>>(
        key = SharedPreferenceKey.PRACTITIONER_DETAILS_ORGANIZATION_IDS.name
      )
      ?.let { organizationIds -> syncStrategy[SyncStrategy.ORGANIZATION.value] = organizationIds }

    sharedPreferencesHelper.read<List<String>>(
        key = SharedPreferenceKey.PRACTITIONER_DETAILS_LOCATION_IDS.name
      )
      ?.let { locationIds -> syncStrategy[SyncStrategy.LOCATION.value] = locationIds }

    sharedPreferencesHelper.read<KeycloakUserDetails>(
        key = SharedPreferenceKey.PRACTITIONER_DETAILS_USER_DETAIL.name
      )
      ?.let { practitioner ->
        syncStrategy[SyncStrategy.PRACTITIONER.value] = listOf(practitioner.id)
      }

    return withContext(dispatcherProvider.io()) {
      resource.map {
        it.generateMissingId()
        it.addMandatoryTags(syncStrategy)
      }
      fhirEngine.create(*resource)
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
        create(resource)
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

    create(relatedPerson)
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

  /** Remove member of a group using the provided [patientId] */
  suspend fun removeGroupMember(patientId: String, groupId: String?) {
    // TODO refactor to work with any resource type
    loadResource<Patient>(patientId)?.let { patient ->
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
              if (relatedPerson.patient.id == patientId) {
                delete(relatedPerson)
                group.managingEntity = null
              }
            }
        }
      }
      addOrUpdate(patient)
    }
  }

  suspend fun loadCqlLibraryBundle(
    context: Context,
    fhirOperator: FhirOperator,
    resourcesBundlePath: String
  ) =
    try {
      val jsonParser = FhirContext.forR4().newJsonParser()
      val savedResources =
        sharedPreferencesHelper.read(SharedPreferenceKey.MEASURE_RESOURCES_LOADED.name, "")

      context.assets.open(resourcesBundlePath, AssetManager.ACCESS_RANDOM).bufferedReader().use {
        val bundle = jsonParser.parseResource(it) as Bundle
        bundle.entry.forEach { entry ->
          if (entry.resource.resourceType == ResourceType.Library) {
            fhirOperator.loadLib(entry.resource as Library)
          } else {
            if (!savedResources!!.contains(resourcesBundlePath)) {
              create(entry.resource)
              sharedPreferencesHelper.write(
                SharedPreferenceKey.MEASURE_RESOURCES_LOADED.name,
                savedResources.plus(",").plus(resourcesBundlePath)
              )
            }
          }
        }
      }
    } catch (exception: Exception) {
      Timber.e(exception)
    }

  suspend fun loadLibraryAtPath(fhirOperator: FhirOperator, path: String) {
    // resource path could be Library/123 OR something like http://fhir.labs.common/Library/123
    val library =
      if (!UrlUtil.isValid(path)) fhirEngine.get<Library>(IdType(path).idPart)
      else fhirEngine.search<Library> { filter(Library.URL, { value = path }) }.firstOrNull()

    library?.let {
      fhirOperator.loadLib(it)

      it.relatedArtifact.forEach { loadLibraryAtPath(fhirOperator, it) }
    }
  }

  suspend fun loadLibraryAtPath(fhirOperator: FhirOperator, relatedArtifact: RelatedArtifact) {
    if (relatedArtifact.type.isIn(
        RelatedArtifact.RelatedArtifactType.COMPOSEDOF,
        RelatedArtifact.RelatedArtifactType.DEPENDSON
      )
    )
      loadLibraryAtPath(fhirOperator, relatedArtifact.resource)
  }

  suspend fun loadCqlLibraryBundle(fhirOperator: FhirOperator, measurePath: String) =
    try {
      // resource path could be Measure/123 OR something like http://fhir.labs.common/Measure/123
      val measure =
        if (UrlUtil.isValid(measurePath))
          fhirEngine.search<Measure> { filter(Measure.URL, { value = measurePath }) }.first()
        else fhirEngine.get(measurePath)

      measure.relatedArtifact.forEach { loadLibraryAtPath(fhirOperator, it) }

      measure.library.map { it.value }.forEach { path -> loadLibraryAtPath(fhirOperator, path) }
    } catch (exception: Exception) {
      Timber.e(exception)
    }
}
