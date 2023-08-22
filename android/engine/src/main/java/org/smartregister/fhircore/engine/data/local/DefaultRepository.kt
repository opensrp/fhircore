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

import androidx.annotation.VisibleForTesting
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
import java.util.Date
import java.util.LinkedList
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.DataRequirement
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Group
import org.hl7.fhir.r4.model.IdType
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Procedure
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.RelatedPerson
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.ServiceRequest
import org.hl7.fhir.r4.model.Task
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
import org.smartregister.fhircore.engine.rulesengine.ConfigRulesExecutor
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.asReference
import org.smartregister.fhircore.engine.util.extension.extractId
import org.smartregister.fhircore.engine.util.extension.extractLogicalIdUuid
import org.smartregister.fhircore.engine.util.extension.filterBy
import org.smartregister.fhircore.engine.util.extension.filterByResourceTypeId
import org.smartregister.fhircore.engine.util.extension.generateMissingId
import org.smartregister.fhircore.engine.util.extension.loadResource
import org.smartregister.fhircore.engine.util.extension.updateFrom
import org.smartregister.fhircore.engine.util.extension.updateLastUpdated
import org.smartregister.fhircore.engine.util.fhirpath.FhirPathDataExtractor
import timber.log.Timber

open class DefaultRepository
@Inject
constructor(
  open val fhirEngine: FhirEngine,
  open val dispatcherProvider: DispatcherProvider,
  open val sharedPreferencesHelper: SharedPreferencesHelper,
  open val configurationRegistry: ConfigurationRegistry,
  open val configService: ConfigService,
  open val configRulesExecutor: ConfigRulesExecutor,
  open val fhirPathDataExtractor: FhirPathDataExtractor,
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
    configComputedRuleValues: Map<String, Any> = emptyMap(),
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
    configComputedRuleValues: Map<String, Any>,
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

  suspend inline fun <reified R : Resource> search(search: Search) = fhirEngine.search<R>(search)

  /**
   * Saves a resource in the database. It also updates the [Resource.meta.lastUpdated] and generates
   * the [Resource.id] if it is missing before saving the resource.
   *
   * By default, mandatory Resource tags for sync are added but this can be disabled through the
   * param [addResourceTags]
   */
  suspend fun create(addResourceTags: Boolean = true, vararg resource: Resource): List<String> {
    return withContext(dispatcherProvider.io()) {
      preProcessResources(addResourceTags, *resource)
      fhirEngine.create(*resource)
    }
  }

  suspend fun createRemote(addResourceTags: Boolean = true, vararg resource: Resource) {
    return withContext(dispatcherProvider.io()) {
      preProcessResources(addResourceTags, *resource)
      fhirEngine.createRemote(*resource)
    }
  }

  private fun preProcessResources(addResourceTags: Boolean, vararg resource: Resource) {
    resource.onEach { currentResource ->
      currentResource.apply {
        updateLastUpdated()
        generateMissingId()
      }
      if (addResourceTags) {
        val tags = configService.provideResourceTags(sharedPreferencesHelper)
        tags.forEach {
          val existingTag = currentResource.meta.getTag(it.system, it.code)
          if (existingTag == null) {
            currentResource.meta.addTag(it)
          }
        }
      }
    }
  }

  suspend fun delete(resourceType: ResourceType, resourceId: String, softDelete: Boolean = false) {
    withContext(dispatcherProvider.io()) {
      if (softDelete) {
        val resource = fhirEngine.get(resourceType, resourceId)
        softDelete(resource)
      } else fhirEngine.delete(resourceType, resourceId)
    }
  }

  suspend fun delete(resource: Resource, softDelete: Boolean = false) {
    withContext(dispatcherProvider.io()) {
      if (softDelete) {
        softDelete(resource)
      } else fhirEngine.delete(resource.resourceType, resource.logicalId)
    }
  }

  private suspend fun softDelete(resource: Resource) {
    when (resource.resourceType) {
      ResourceType.Patient -> (resource as Patient).active = false
      ResourceType.Group -> (resource as Group).active = false
      else -> {
        /** TODO implement soft delete for other resource types */
      }
    }
    addOrUpdate(true, resource)
  }

  /**
   * This function upserts a resource into the database. This function also updates the
   * [Resource.meta] and generates the [Resource.id] if it is missing before upserting the resource.
   * The resource needs to already have a [Resource.id].
   *
   * The function benefits since it merges the resource in the database and what is provided. It
   * does this by filling in properties that are missing in the new resource but available in the
   * old resource. This is useful such as during form edits where the resource updates might only
   * contain data generated at this step
   *
   * By default, mandatory Resource tags for sync are added but this can be disabled through the
   * param [addMandatoryTags]
   */
  suspend fun <R : Resource> addOrUpdate(addMandatoryTags: Boolean = true, resource: R) {
    return withContext(dispatcherProvider.io()) {
      resource.updateLastUpdated()
      try {
        fhirEngine.get(resource.resourceType, resource.logicalId).run {
          val updateFrom = updateFrom(resource)
          fhirEngine.update(updateFrom)
        }
      } catch (resourceNotFoundException: ResourceNotFoundException) {
        create(addMandatoryTags, resource)
      }
    }
  }

  suspend fun <R : Resource> update(resource: R) {
    return withContext(dispatcherProvider.io()) {
      resource.updateLastUpdated()
      fhirEngine.update(resource)
    }
  }

  suspend fun loadManagingEntity(group: Group, configComputedRuleValues: Map<String, Any>) =
    group.managingEntity?.let { reference ->
      searchResourceFor<RelatedPerson>(
          token = RelatedPerson.RES_ID,
          subjectType = ResourceType.RelatedPerson,
          subjectId = reference.extractId(),
          configComputedRuleValues = configComputedRuleValues,
        )
        .firstOrNull()
        ?.let { relatedPerson ->
          searchResourceFor<Patient>(
              token = Patient.RES_ID,
              subjectType = ResourceType.Patient,
              subjectId = relatedPerson.patient.extractId(),
              configComputedRuleValues = configComputedRuleValues,
            )
            .firstOrNull()
        }
    }

  suspend fun changeManagingEntity(
    newManagingEntityId: String,
    groupId: String,
    managingEntityConfig: ManagingEntityConfig?,
  ) {
    val group = fhirEngine.get<Group>(groupId)
    if (managingEntityConfig?.resourceType == ResourceType.Patient) {
      val relatedPerson =
        if (
          group.managingEntity.reference != null &&
            group.managingEntity.reference.startsWith(ResourceType.RelatedPerson.name)
        ) {
          fhirEngine.get(group.managingEntity.reference.extractLogicalIdUuid())
        } else {
          RelatedPerson().apply { id = UUID.randomUUID().toString() }
        }
      val newPatient = fhirEngine.get<Patient>(newManagingEntityId)

      updateRelatedPersonDetails(relatedPerson, newPatient, managingEntityConfig.relationshipCode)

      addOrUpdate(resource = relatedPerson)

      group.managingEntity = relatedPerson.asReference()
      update(group)
    }
  }

  private fun updateRelatedPersonDetails(
    existingPerson: RelatedPerson,
    newPatient: Patient,
    relationshipCode: Code?,
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
    configComputedRuleValues: Map<String, Any>,
  ) {
    loadResource<Group>(groupId)?.let { group ->
      if (!group.active) throw IllegalStateException("Group already deleted")
      group.managingEntity
        ?.let { reference ->
          searchResourceFor<RelatedPerson>(
            token = RelatedPerson.RES_ID,
            subjectType = ResourceType.RelatedPerson,
            subjectId = reference.extractId(),
            configComputedRuleValues = configComputedRuleValues,
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
    groupMemberResourceType: ResourceType?,
    configComputedRuleValues: Map<String, Any>,
  ) {
    val fhirResource: Resource? =
      try {
        if (groupMemberResourceType == null) return
        fhirEngine.get(groupMemberResourceType, memberId.extractLogicalIdUuid())
      } catch (resourceNotFoundException: ResourceNotFoundException) {
        Timber.e("Group member with ID $memberId not found!")
        null
      }

    fhirResource?.let { resource ->
      if (resource is Patient) {
        resource.active = false
      }

      if (groupId != null) {
        loadResource<Group>(groupId)?.let { group ->
          group.managingEntity
            ?.let { reference ->
              searchResourceFor<RelatedPerson>(
                token = RelatedPerson.RES_ID,
                subjectType = ResourceType.RelatedPerson,
                subjectId = reference.extractId(),
                configComputedRuleValues = configComputedRuleValues,
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

  protected fun Search.applyConfiguredSortAndFilters(
    resourceConfig: ResourceConfig,
    filterActiveResources: List<ActiveResourceFilterConfig>? = null,
    sortData: Boolean,
    configComputedRuleValues: Map<String, Any>,
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
      if (!sortConfig.paramName.isNullOrEmpty()) {
        when (sortConfig.dataType) {
          Enumerations.DataType.INTEGER ->
            sort(NumberClientParam(sortConfig.paramName), sortConfig.order)
          Enumerations.DataType.DATE ->
            sort(DateClientParam(sortConfig.paramName), sortConfig.order)
          Enumerations.DataType.STRING ->
            sort(StringClientParam(sortConfig.paramName), sortConfig.order)
          else ->
            Timber.e(
              "Unsupported data type: '${sortConfig.dataType}'. Only ${listOf(Enumerations.DataType.INTEGER, Enumerations.DataType.DATE, Enumerations.DataType.STRING)} types are supported for DB level sorting.",
            )
        }
      }
    }
  }

  protected suspend fun retrieveRelatedResources(
    resources: List<Resource>,
    relatedResourcesConfigs: List<ResourceConfig>?,
    relatedResourceWrapper: RelatedResourceWrapper,
    configComputedRuleValues: Map<String, Any>,
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
        configComputedRuleValues = configComputedRuleValues,
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
                configComputedRuleValues = configComputedRuleValues,
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
                "Error retrieving total count for all related resourced identified by $key",
              )
            },
          )
        } else {
          computeCountForEachRelatedResource(
            resources = resources,
            resourceConfig = resourceConfig,
            relatedResourceWrapper = relatedResourceWrapper,
            configComputedRuleValues = configComputedRuleValues,
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
        configComputedRuleValues = configComputedRuleValues,
      )
    }

    return relatedResourceWrapper
  }

  protected suspend fun Search.count(
    onSuccess: (Long) -> Unit = {},
    onFailure: (Throwable) -> Unit = { throwable -> Timber.e(throwable, "Error counting data") },
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
    configComputedRuleValues: Map<String, Any>,
  ) {
    val relatedResourceCountLinkedList = LinkedList<RelatedResourceCount>()
    val key = resourceConfig.id ?: resourceConfig.resource.name
    resources.forEach { baseResource ->
      val search =
        Search(type = resourceConfig.resource).apply {
          filter(
            ReferenceClientParam(resourceConfig.searchParameter),
            { value = baseResource.logicalId.asReference(baseResource.resourceType).reference },
          )
          applyConfiguredSortAndFilters(
            resourceConfig = resourceConfig,
            sortData = false,
            configComputedRuleValues = configComputedRuleValues,
          )
        }
      search.count(
        onSuccess = {
          relatedResourceCountLinkedList.add(
            RelatedResourceCount(
              relatedResourceType = resourceConfig.resource,
              parentResourceId = baseResource.logicalId,
              count = it,
            ),
          )
        },
        onFailure = {
          Timber.e(
            it,
            "Error retrieving count for ${baseResource.logicalId.asReference(baseResource.resourceType)} for related resource identified ID $key",
          )
        },
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
    configComputedRuleValues: Map<String, Any>,
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
            configComputedRuleValues = configComputedRuleValues,
          )
          if (isRevInclude) {
            revInclude(
              resourceConfig.resource,
              ReferenceClientParam(resourceConfig.searchParameter),
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
        configComputedRuleValues = configComputedRuleValues,
      )
    }
  }

  private suspend fun searchRelatedResources(
    isRevInclude: Boolean,
    search: Search,
    relatedResourcesConfigsMap: Map<ResourceType, List<ResourceConfig>>,
    relatedResourceWrapper: RelatedResourceWrapper,
    configComputedRuleValues: Map<String, Any>,
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
              relatedResourceWrapper.relatedResourceMap
                .getOrPut(key) { LinkedList() }
                .plus(entry.value)

            currentResourceConfigs?.forEach { resourceConfig ->
              if (resourceConfig.relatedResources.isNotEmpty()) {
                retrieveRelatedResources(
                  resources = entry.value,
                  relatedResourcesConfigs = resourceConfig.relatedResources,
                  relatedResourceWrapper = relatedResourceWrapper,
                  configComputedRuleValues = configComputedRuleValues,
                )
              }
            }
          }
        }
      }
      .onFailure {
        Timber.e(it, "Error fetching configured related resources: $relatedResourcesConfigsMap")
      }
  }

  private fun List<ResourceConfig>.revIncludeRelatedResourceConfigs(isRevInclude: Boolean) =
    if (isRevInclude) {
      this.filter { it.isRevInclude && !it.resultAsCount }
    } else this.filter { !it.isRevInclude && !it.resultAsCount }

  suspend fun updateResourcesRecursively(resourceConfig: ResourceConfig, subject: Resource) {
    val configRules = configRulesExecutor.generateRules(resourceConfig.configRules ?: listOf())
    val initialComputedValuesMap =
      configRulesExecutor.fireRules(rules = configRules, baseResource = subject)

    /**
     * Data queries for retrieving resources require the id to be provided in the format
     * [ResourceType/UUID] e.g Group/0acda8c9-3fa3-40ae-abcd-7d1fba7098b4. When resources are synced
     * up to the server the id is updated with history information e.g
     * Group/0acda8c9-3fa3-40ae-abcd-7d1fba7098b4/_history/1 This needs to be formatted to
     * [ResourceType/UUID] format and updated in the computedValuesMap
     */
    val computedValuesMap = mutableMapOf<String, Any>()
    initialComputedValuesMap.forEach { entry ->
      computedValuesMap[entry.key] =
        "${entry.value.toString().substringBefore("/")}/${entry.value.toString().extractLogicalIdUuid()}"
    }

    Timber.i("Computed values map = ${computedValuesMap.values}")
    val search =
      Search(resourceConfig.resource).apply {
        applyConfiguredSortAndFilters(
          resourceConfig = resourceConfig,
          sortData = false,
          filterActiveResources = null,
          configComputedRuleValues = computedValuesMap,
        )
      }
    val resources = fhirEngine.search<Resource>(search)
    resources.forEach {
      Timber.i("Closing Resource type ${it.resourceType.name} and id ${it.id}")
      closeResource(it, resourceConfig)
    }

    // recursive related resources
    val retrievedRelatedResources =
      withContext(dispatcherProvider.io()) {
        retrieveRelatedResources(
          resources = resources,
          relatedResourcesConfigs = resourceConfig.relatedResources,
          relatedResourceWrapper = RelatedResourceWrapper(),
          configComputedRuleValues = emptyMap(),
        )
      }

    retrievedRelatedResources.relatedResourceMap.forEach { resourcesMap ->
      resourcesMap.value.forEach { resource ->
        Timber.i(
          "Closing related Resource type ${resource.resourceType.name} and id ${resource.id}",
        )
        if (filterRelatedResource(resource, resourceConfig)) {
          closeResource(resource, resourceConfig)
        }
      }
    }
  }

  @VisibleForTesting
  suspend fun closeResource(resource: Resource, resourceConfig: ResourceConfig) {
    when (resource) {
      is Task -> {
        if (resource.status != Task.TaskStatus.COMPLETED) {
          resource.status = Task.TaskStatus.CANCELLED
          resource.lastModified = Date()
        }
      }
      is CarePlan -> resource.status = CarePlan.CarePlanStatus.COMPLETED
      is Procedure -> resource.status = Procedure.ProcedureStatus.STOPPED
      is Condition -> {
        resource.clinicalStatus =
          CodeableConcept().apply {
            coding =
              listOf(
                Coding().apply {
                  system = SNOMED_SYSTEM
                  display = PATIENT_CONDITION_RESOLVED_DISPLAY
                  code = PATIENT_CONDITION_RESOLVED_CODE
                },
              )
          }
      }
      is ServiceRequest -> resource.status = ServiceRequest.ServiceRequestStatus.REVOKED
    }
    fhirEngine.update(resource)
  }

  fun filterRelatedResource(resource: Resource, resourceConfig: ResourceConfig): Boolean {
    return resourceConfig.filterFhirPathExpressions?.any { filterFhirPathExpression ->
      fhirPathDataExtractor.extractValue(resource, filterFhirPathExpression.key) ==
        filterFhirPathExpression.value
    } == true
  }

  /**
   * A wrapper data class to hold search results. All related resources are flattened into one Map
   * including the nested related resources as required by the Rules Engine facts.
   */
  data class RelatedResourceWrapper(
    val relatedResourceMap: MutableMap<String, List<Resource>> = mutableMapOf(),
    val relatedResourceCountMap: MutableMap<String, List<RelatedResourceCount>> = mutableMapOf(),
  )

  companion object {
    const val SNOMED_SYSTEM = "http://hl7.org/fhir/R4B/valueset-condition-clinical.html"
    const val PATIENT_CONDITION_RESOLVED_CODE = "resolved"
    const val PATIENT_CONDITION_RESOLVED_DISPLAY = "Resolved"
    const val PNC_CONDITION_TO_CLOSE_RESOURCE_ID = "pncConditionToClose"
    const val SICK_CHILD_CONDITION_TO_CLOSE_RESOURCE_ID = "sickChildConditionToClose"
  }
}
