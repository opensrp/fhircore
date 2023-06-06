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

import ca.uhn.fhir.rest.gclient.DateClientParam
import ca.uhn.fhir.rest.gclient.NumberClientParam
import ca.uhn.fhir.rest.gclient.ReferenceClientParam
import ca.uhn.fhir.rest.gclient.StringClientParam
import ca.uhn.fhir.rest.gclient.TokenClientParam
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.db.ResourceNotFoundException
import com.google.android.fhir.get
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.Search
import com.google.android.fhir.search.filter.ReferenceParamFilterCriterion
import com.google.android.fhir.search.filter.TokenParamFilterCriterion
import com.google.android.fhir.search.has
import com.google.android.fhir.search.search
import java.util.LinkedList
import java.util.UUID
import javax.inject.Inject
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
import org.smartregister.fhircore.engine.configuration.profile.ManagingEntityConfig
import org.smartregister.fhircore.engine.configuration.register.ActiveResourceFilterConfig
import org.smartregister.fhircore.engine.data.local.register.RegisterRepository
import org.smartregister.fhircore.engine.domain.model.Code
import org.smartregister.fhircore.engine.domain.model.DataQuery
import org.smartregister.fhircore.engine.domain.model.RelatedResourceCount
import org.smartregister.fhircore.engine.domain.model.ResourceConfig
import org.smartregister.fhircore.engine.domain.model.SortConfig
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
import timber.log.Timber

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
    filters: List<DataQuery>? = null,
    configComputedRuleValues: Map<String, Any> = emptyMap()
  ): List<T> =
    withContext(dispatcherProvider.io()) {
      fhirEngine.search {
        filterByResourceTypeId(subjectParam, subjectType, subjectId)
        filters?.forEach { filterBy(it, configComputedRuleValues = configComputedRuleValues) }
      }
    }

  suspend inline fun <reified T : Resource> searchResourceFor(
    token: TokenClientParam,
    subjectType: ResourceType,
    subjectId: String,
    filters: List<DataQuery> = listOf(),
    configComputedRuleValues: Map<String, Any>
  ): List<T> =
    withContext(dispatcherProvider.io()) {
      fhirEngine.search {
        filterByResourceTypeId(token, subjectType, subjectId)
        filters.forEach { filterBy(it, configComputedRuleValues) }
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

  suspend fun loadManagingEntity(group: Group, configComputedRuleValues: Map<String, Any>) =
    group.managingEntity?.let { reference ->
      searchResourceFor<RelatedPerson>(
        token = RelatedPerson.RES_ID,
        subjectType = ResourceType.RelatedPerson,
        subjectId = reference.extractId(),
        configComputedRuleValues = configComputedRuleValues
      )
        .firstOrNull()
        ?.let { relatedPerson ->
          searchResourceFor<Patient>(
              token = Patient.RES_ID,
              subjectType = ResourceType.Patient,
              subjectId = relatedPerson.patient.extractId(),
              configComputedRuleValues = configComputedRuleValues
            )
            .firstOrNull()
        }
    }

  suspend fun changeManagingEntity(
    newManagingEntityId: String,
    groupId: String,
    managingEntityConfig: ManagingEntityConfig?
  ) {
    val group = fhirEngine.get<Group>(groupId)
    if (managingEntityConfig?.resourceType == ResourceType.Patient) {
      val relatedPerson =
        if (group.managingEntity.reference != null) {
          fhirEngine.get(group.managingEntity.reference.extractLogicalIdUuid())
        } else {
          RelatedPerson().apply { id = UUID.randomUUID().toString() }
        }
      val newPatient = fhirEngine.get<Patient>(newManagingEntityId)

      updateRelatedPersonDetails(relatedPerson, newPatient, managingEntityConfig.relationshipCode)

      addOrUpdate(resource = relatedPerson)

      group.managingEntity = relatedPerson.asReference()
      fhirEngine.update(group)
    }
  }

  private fun updateRelatedPersonDetails(
    existingPerson: RelatedPerson,
    newPatient: Patient,
    relationshipCode: Code?
  ) {
    existingPerson.apply {
      active = true
      name = newPatient.name
      birthDate = newPatient.birthDate
      telecom = newPatient.telecom
      address = newPatient.address
      gender = newPatient.gender
      patient = newPatient.asReference()
      relationshipFirstRep.codingFirstRep.system = relationshipCode?.system
      relationshipFirstRep.codingFirstRep.code = relationshipCode?.code
      relationshipFirstRep.codingFirstRep.display = relationshipCode?.display
    }
  }

  suspend fun removeGroup(
    groupId: String,
    isDeactivateMembers: Boolean?,
    configComputedRuleValues: Map<String, Any>
  ) {
    loadResource<Group>(groupId)?.let { group ->
      if (!group.active) throw IllegalStateException("Group already deleted")
      group
        .managingEntity
        ?.let { reference ->
          searchResourceFor<RelatedPerson>(
            token = RelatedPerson.RES_ID,
            subjectType = ResourceType.RelatedPerson,
            subjectId = reference.extractId(),
            configComputedRuleValues = configComputedRuleValues
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
    groupMemberResourceType: String?,
    configComputedRuleValues: Map<String, Any>
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
                subjectId = reference.extractId(),
                configComputedRuleValues = configComputedRuleValues
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

  suspend fun delete(resourceType: ResourceType, resourceId: String) {
    fhirEngine.delete(resourceType, resourceId)
  }

  protected fun Search.applyConfiguredSortAndFilters(
    resourceConfig: ResourceConfig,
    filterActiveResources: List<ActiveResourceFilterConfig>? = null,
    sortData: Boolean,
    configComputedRuleValues: Map<String, Any>
  ) {
    val activeResource = filterActiveResources?.find { it.resourceType == resourceConfig.resource }
    if (!filterActiveResources.isNullOrEmpty() && activeResource?.active == true) {
      filter(TokenClientParam(RegisterRepository.ACTIVE), { value = of(true) })
    }

    resourceConfig.dataQueries?.forEach { dataQuery ->
      filterBy(dataQuery = dataQuery, configComputedRuleValues = configComputedRuleValues)
    }

    resourceConfig.nestedSearchResources?.forEach {
      has(it.resourceType, ReferenceClientParam((it.referenceParam))) {
        it.dataQueries?.forEach { dataQuery ->
          filterBy(dataQuery = dataQuery, configComputedRuleValues = configComputedRuleValues)
        }
      }
    }

    if (sortData) sort(resourceConfig.sortConfigs)
  }

  private fun Search.sort(sortConfigs: List<SortConfig>) {
    sortConfigs.forEach { sortConfig ->
      when (sortConfig.dataType) {
        Enumerations.DataType.INTEGER ->
          sort(NumberClientParam(sortConfig.paramName), sortConfig.order)
        Enumerations.DataType.DATE -> sort(DateClientParam(sortConfig.paramName), sortConfig.order)
        Enumerations.DataType.STRING ->
          sort(StringClientParam(sortConfig.paramName), sortConfig.order)
        else -> {
          /*Unsupported data type*/
        }
      }
    }
  }

  protected suspend fun retrieveRelatedResources(
    resources: List<Resource>,
    relatedResourcesConfigs: List<ResourceConfig>?,
    relatedResourceWrapper: RelatedResourceWrapper,
    configComputedRuleValues: Map<String, Any>
  ): RelatedResourceWrapper {

    // Forward include related resources e.g. Members (Patient) referenced in Group resource
    val forwardIncludeResourceConfigs =
      relatedResourcesConfigs?.revIncludeRelatedResourceConfigs(false)
    if (!forwardIncludeResourceConfigs.isNullOrEmpty()) {
      searchWithRevInclude(
        isRevInclude = false,
        relatedResourcesConfigs = forwardIncludeResourceConfigs,
        resources = resources,
        relatedResourceWrapper = relatedResourceWrapper,
        configComputedRuleValues = configComputedRuleValues
      )
    }

    val countResourceConfigs = relatedResourcesConfigs?.filter { it.resultAsCount }
    countResourceConfigs?.forEach { resourceConfig ->
      if (resourceConfig.searchParameter.isNullOrEmpty()) {
        Timber.e("Search parameter require to perform count query. Current config: $resourceConfig")
      }

      // Count for each related resource or aggregate total count in one query; as configured
      if (resourceConfig.resultAsCount && !resourceConfig.searchParameter.isNullOrEmpty()) {
        if (resourceConfig.countResultConfig?.sumCounts == true) {
          val search =
            Search(resourceConfig.resource).apply {
              val filters =
                resources.map {
                  val apply: ReferenceParamFilterCriterion.() -> Unit = {
                    value = it.logicalId.asReference(it.resourceType).reference
                  }
                  apply
                }
              filter(ReferenceClientParam(resourceConfig.searchParameter), *filters.toTypedArray())
              applyConfiguredSortAndFilters(
                resourceConfig = resourceConfig,
                sortData = false,
                configComputedRuleValues = configComputedRuleValues
              )
            }
          val key = resourceConfig.id ?: resourceConfig.resource.name
          search.count(
            onSuccess = {
              relatedResourceWrapper.relatedResourceCountMap[key] =
                LinkedList<RelatedResourceCount>().apply { add(RelatedResourceCount(count = it)) }
            },
            onFailure = {
              Timber.e(
                it,
                "Error retrieving total count for all related resourced identified by $key"
              )
            }
          )
        } else {
          computeCountForEachRelatedResource(
            resources = resources,
            resourceConfig = resourceConfig,
            relatedResourceWrapper = relatedResourceWrapper,
            configComputedRuleValues = configComputedRuleValues
          )
        }
      }
    }

    // Reverse include related resources e.g. All CarePlans, Immunization for Patient resource
    val reverseIncludeResourceConfigs =
      relatedResourcesConfigs?.revIncludeRelatedResourceConfigs(true)
    if (!reverseIncludeResourceConfigs.isNullOrEmpty()) {
      searchWithRevInclude(
        isRevInclude = true,
        relatedResourcesConfigs = reverseIncludeResourceConfigs,
        resources = resources,
        relatedResourceWrapper = relatedResourceWrapper,
        configComputedRuleValues = configComputedRuleValues
      )
    }

    return relatedResourceWrapper
  }

  protected suspend fun Search.count(
    onSuccess: (Long) -> Unit = {},
    onFailure: (Throwable) -> Unit = { throwable -> Timber.e(throwable, "Error counting data") }
  ): Long =
    kotlin
      .runCatching { withContext(dispatcherProvider.io()) { fhirEngine.count(this@count) } }
      .onSuccess { count -> onSuccess(count) }
      .onFailure { throwable -> onFailure(throwable) }
      .getOrDefault(0)

  private suspend fun computeCountForEachRelatedResource(
    resources: List<Resource>,
    resourceConfig: ResourceConfig,
    relatedResourceWrapper: RelatedResourceWrapper,
    configComputedRuleValues: Map<String, Any>
  ) {
    val relatedResourceCountLinkedList = LinkedList<RelatedResourceCount>()
    val key = resourceConfig.id ?: resourceConfig.resource.name
    resources.forEach { baseResource ->
      val search =
        Search(type = resourceConfig.resource).apply {
          filter(
            ReferenceClientParam(resourceConfig.searchParameter),
            { value = baseResource.logicalId.asReference(baseResource.resourceType).reference }
          )
          applyConfiguredSortAndFilters(
            resourceConfig = resourceConfig,
            sortData = false,
            configComputedRuleValues = configComputedRuleValues
          )
        }
      search.count(
        onSuccess = {
          relatedResourceCountLinkedList.add(
            RelatedResourceCount(
              relatedResourceType = resourceConfig.resource,
              parentResourceId = baseResource.logicalId,
              count = it
            )
          )
        },
        onFailure = {
          Timber.e(
            it,
            "Error retrieving count for ${baseResource.logicalId.asReference(baseResource.resourceType)} for related resource identified ID $key"
          )
        }
      )
    }

    // Add each related resource count query result to map
    relatedResourceWrapper.relatedResourceCountMap[key] = relatedResourceCountLinkedList
  }

  /**
   * If [isRevInclude] is set to false, the forward include search API will be used; otherwise
   * reverse include is used to retrieve related resources. [relatedResourceWrapper] is a data class
   * that wraps the maps used to store Search Query results. The [relatedResourcesConfigs]
   * configures which resources to load.
   */
  private suspend fun searchWithRevInclude(
    isRevInclude: Boolean,
    relatedResourcesConfigs: List<ResourceConfig>?,
    resources: List<Resource>,
    relatedResourceWrapper: RelatedResourceWrapper,
    configComputedRuleValues: Map<String, Any>
  ) {
    val relatedResourcesConfigsMap = relatedResourcesConfigs?.groupBy { it.resource }

    if (!relatedResourcesConfigsMap.isNullOrEmpty()) {
      if (resources.isEmpty()) return

      val firstResourceType = resources.first().resourceType
      val search =
        Search(firstResourceType).apply {
          val filters =
            resources.map {
              val apply: TokenParamFilterCriterion.() -> Unit = { value = of(it.logicalId) }
              apply
            }
          filter(Resource.RES_ID, *filters.toTypedArray())
        }

      relatedResourcesConfigs.forEach { resourceConfig ->
        search.apply {
          applyConfiguredSortAndFilters(
            resourceConfig = resourceConfig,
            sortData = true,
            configComputedRuleValues = configComputedRuleValues
          )
          if (isRevInclude) {
            revInclude(
              resourceConfig.resource,
              ReferenceClientParam(resourceConfig.searchParameter)
            )
          } else {
            include(ReferenceClientParam(resourceConfig.searchParameter), resourceConfig.resource)
          }
        }
      }

      searchRelatedResources(
        isRevInclude = isRevInclude,
        search = search,
        relatedResourcesConfigsMap = relatedResourcesConfigsMap,
        relatedResourceWrapper = relatedResourceWrapper,
        configComputedRuleValues = configComputedRuleValues
      )
    }
  }

  private suspend fun searchRelatedResources(
    isRevInclude: Boolean,
    search: Search,
    relatedResourcesConfigsMap: Map<ResourceType, List<ResourceConfig>>,
    relatedResourceWrapper: RelatedResourceWrapper,
    configComputedRuleValues: Map<String, Any>
  ) {
    kotlin
      .runCatching { fhirEngine.searchWithRevInclude<Resource>(isRevInclude, search) }
      .onSuccess { searchResult ->
        searchResult.values.forEach { theRelatedResourcesMap: Map<ResourceType, List<Resource>> ->
          theRelatedResourcesMap.forEach { entry ->
            val currentResourceConfigs = relatedResourcesConfigsMap[entry.key]

            val key = // Use configured id as key otherwise default to ResourceType
              if (relatedResourcesConfigsMap.containsKey(entry.key)) {
                currentResourceConfigs?.firstOrNull()?.id ?: entry.key.name
              } else entry.key.name

            // All nested resources flattened to one map by adding to existing list
            relatedResourceWrapper.relatedResourceMap[key] =
              relatedResourceWrapper
                .relatedResourceMap
                .getOrPut(key) { LinkedList() }
                .plus(entry.value)

            currentResourceConfigs?.forEach { resourceConfig ->
              if (resourceConfig.relatedResources.isNotEmpty())
                retrieveRelatedResources(
                  resources = entry.value,
                  relatedResourcesConfigs = resourceConfig.relatedResources,
                  relatedResourceWrapper = relatedResourceWrapper,
                  configComputedRuleValues = configComputedRuleValues
                )
            }
          }
        }
      }
      .onFailure {
        Timber.e(it, "Error fetching configured related resources: $relatedResourcesConfigsMap")
      }
  }

  private fun List<ResourceConfig>.revIncludeRelatedResourceConfigs(isRevInclude: Boolean) =
    if (isRevInclude) this.filter { it.isRevInclude && !it.resultAsCount }
    else this.filter { !it.isRevInclude && !it.resultAsCount }

  suspend fun updateResourcesRecursively(resourceConfig: ResourceConfig) {
    val search =
      Search(resourceConfig.resource).apply {
        applyConfiguredSortAndFilters(
          resourceConfig = resourceConfig,
          sortData = false,
          filterActiveResources = null,
          configComputedRuleValues = emptyMap()
        )
      }
    val resources = fhirEngine.search<Resource>(search)
    Timber.e("Fetched careplans = ${resources.size} ++++")

    // recursive related resources
    val retrievedRelatedResources =
      withContext(dispatcherProvider.io()) {
        retrieveRelatedResources(
          resources = resources,
          relatedResourcesConfigs = resourceConfig.relatedResources,
          relatedResourceWrapper = RelatedResourceWrapper(),
          configComputedRuleValues = emptyMap()
        )
      }
    Timber.e("Fetched tasks = ${retrievedRelatedResources.relatedResourceMap.size} ++++")
  }

  /**
   * A wrapper data class to hold search results. All related resources are flattened into one Map
   * including the nested related resources as required by the Rules Engine facts.
   */
  data class RelatedResourceWrapper(
    val relatedResourceMap: MutableMap<String, List<Resource>> = mutableMapOf(),
    val relatedResourceCountMap: MutableMap<String, List<RelatedResourceCount>> = mutableMapOf()
  )
}
