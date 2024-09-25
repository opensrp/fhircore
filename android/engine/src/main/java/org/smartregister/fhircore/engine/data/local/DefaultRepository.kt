/*
 * Copyright 2021-2024 Ona Systems, Inc
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
import androidx.annotation.VisibleForTesting
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.IParser
import ca.uhn.fhir.rest.gclient.DateClientParam
import ca.uhn.fhir.rest.gclient.NumberClientParam
import ca.uhn.fhir.rest.gclient.ReferenceClientParam
import ca.uhn.fhir.rest.gclient.StringClientParam
import ca.uhn.fhir.rest.gclient.TokenClientParam
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.SearchResult
import com.google.android.fhir.datacapture.extensions.logicalId
import com.google.android.fhir.db.ResourceNotFoundException
import com.google.android.fhir.get
import com.google.android.fhir.search.Order
import com.google.android.fhir.search.Search
import com.google.android.fhir.search.filter.ReferenceParamFilterCriterion
import com.google.android.fhir.search.filter.TokenParamFilterCriterion
import com.google.android.fhir.search.has
import com.google.android.fhir.search.include
import com.google.android.fhir.search.revInclude
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.Option
import com.jayway.jsonpath.PathNotFoundException
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.LinkedList
import java.util.UUID
import javax.inject.Inject
import kotlin.math.min
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Group
import org.hl7.fhir.r4.model.IdType
import org.hl7.fhir.r4.model.Location
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.RelatedPerson
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.UniqueIdAssignmentConfig
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.configuration.event.EventWorkflow
import org.smartregister.fhircore.engine.configuration.profile.ManagingEntityConfig
import org.smartregister.fhircore.engine.configuration.register.ActiveResourceFilterConfig
import org.smartregister.fhircore.engine.domain.model.Code
import org.smartregister.fhircore.engine.domain.model.DataQuery
import org.smartregister.fhircore.engine.domain.model.FhirResourceConfig
import org.smartregister.fhircore.engine.domain.model.RelatedResourceCount
import org.smartregister.fhircore.engine.domain.model.RepositoryResourceData
import org.smartregister.fhircore.engine.domain.model.ResourceConfig
import org.smartregister.fhircore.engine.domain.model.ResourceFilterExpression
import org.smartregister.fhircore.engine.domain.model.RuleConfig
import org.smartregister.fhircore.engine.domain.model.SortConfig
import org.smartregister.fhircore.engine.rulesengine.ConfigRulesExecutor
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.asReference
import org.smartregister.fhircore.engine.util.extension.batchedSearch
import org.smartregister.fhircore.engine.util.extension.encodeResourceToString
import org.smartregister.fhircore.engine.util.extension.extractId
import org.smartregister.fhircore.engine.util.extension.extractLogicalIdUuid
import org.smartregister.fhircore.engine.util.extension.filterBy
import org.smartregister.fhircore.engine.util.extension.filterByResourceTypeId
import org.smartregister.fhircore.engine.util.extension.generateMissingId
import org.smartregister.fhircore.engine.util.extension.loadResource
import org.smartregister.fhircore.engine.util.extension.retrieveRelatedEntitySyncLocationIds
import org.smartregister.fhircore.engine.util.extension.updateFrom
import org.smartregister.fhircore.engine.util.extension.updateLastUpdated
import org.smartregister.fhircore.engine.util.fhirpath.FhirPathDataExtractor
import org.smartregister.fhircore.engine.util.pmap
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
  open val parser: IParser,
  @ApplicationContext open val context: Context,
) {

  suspend inline fun <reified T : Resource> loadResource(resourceId: String): T? =
    fhirEngine.loadResource(resourceId)

  suspend fun loadResource(resourceId: String, resourceType: ResourceType): Resource =
    fhirEngine.get(resourceType, resourceId)

  suspend fun loadResource(reference: Reference) =
    IdType(reference.reference).let {
      fhirEngine.get(ResourceType.fromCode(it.resourceType), it.idPart)
    }

  suspend inline fun <reified T : Resource> searchResourceFor(
    token: TokenClientParam,
    subjectType: ResourceType,
    subjectId: String,
    dataQueries: List<DataQuery> = listOf(),
    configComputedRuleValues: Map<String, Any>,
  ): List<T> =
    fhirEngine
      .batchedSearch<T> {
        filterByResourceTypeId(token, subjectType, subjectId)
        dataQueries.forEach {
          filterBy(
            dataQuery = it,
            configComputedRuleValues = configComputedRuleValues,
          )
        }
      }
      .map { it.resource }

  suspend inline fun <reified R : Resource> search(search: Search) =
    fhirEngine.batchedSearch<R>(search).map { it.resource }

  suspend inline fun count(search: Search) = fhirEngine.count(search)

  /**
   * Saves a resource in the database. It also updates the [Resource.meta] _lastUpdated and
   * generates the [Resource.id] if it is missing before saving the resource.
   *
   * By default, mandatory Resource tags for sync are added but this can be disabled through the
   * param [addResourceTags]
   */
  suspend fun create(addResourceTags: Boolean = true, vararg resource: Resource): List<String> {
    preProcessResources(addResourceTags, *resource)
    return fhirEngine.create(*resource)
  }

  suspend fun createRemote(addResourceTags: Boolean = true, vararg resource: Resource) {
    preProcessResources(addResourceTags, *resource)
    fhirEngine.create(*resource, isLocalOnly = true)
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

  suspend fun delete(
    resourceType: ResourceType,
    resourceId: String,
    softDelete: Boolean = false,
  ) {
    if (softDelete) {
      val resource = fhirEngine.get(resourceType, resourceId)
      softDelete(resource)
    } else {
      fhirEngine.delete(resourceType, resourceId)
    }
  }

  suspend fun delete(resource: Resource, softDelete: Boolean = false) {
    if (softDelete) {
      softDelete(resource)
    } else {
      fhirEngine.delete(resource.resourceType, resource.logicalId)
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

  suspend fun <R : Resource> update(resource: R) {
    resource.updateLastUpdated()
    fhirEngine.update(resource)
  }

  suspend fun loadManagingEntity(group: Group) =
    group.managingEntity?.let { reference ->
      fhirEngine
        .batchedSearch<RelatedPerson> {
          filter(RelatedPerson.RES_ID, { value = of(reference.extractId()) })
        }
        .map { it.resource }
        .firstOrNull()
        ?.let { relatedPerson ->
          fhirEngine
            .batchedSearch<Patient> {
              filter(
                Patient.RES_ID,
                { value = of(relatedPerson.patient.extractId()) },
              )
            }
            .map { it.resource }
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

      updateRelatedPersonDetails(
        relatedPerson,
        newPatient,
        managingEntityConfig.relationshipCode,
      )

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
      filter(TokenClientParam(ACTIVE), { value = of(true) })
    }

    resourceConfig.dataQueries?.forEach { dataQuery ->
      filterBy(dataQuery = dataQuery, configComputedRuleValues = configComputedRuleValues)
    }

    resourceConfig.nestedSearchResources?.forEach {
      has(it.resourceType, ReferenceClientParam((it.referenceParam))) {
        it.dataQueries?.forEach { dataQuery ->
          (this as Search).filterBy(
            dataQuery = dataQuery,
            configComputedRuleValues = configComputedRuleValues,
          )
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
              "Unsupported data type: '${sortConfig.dataType}'. Only ${
                                listOf(
                                    Enumerations.DataType.INTEGER,
                                    Enumerations.DataType.DATE,
                                    Enumerations.DataType.STRING,
                                )
                            } types are supported for DB level sorting.",
            )
        }
      }
    }
  }

  protected suspend fun retrieveRelatedResources(
    resource: Resource,
    relatedResourcesConfigs: List<ResourceConfig>?,
    configComputedRuleValues: Map<String, Any>,
  ): RelatedResourceWrapper {
    val relatedResourceWrapper = RelatedResourceWrapper()
    val relatedResourcesQueue =
      ArrayDeque<Pair<List<Resource>, List<ResourceConfig>?>>().apply {
        addFirst(Pair(listOf(resource), relatedResourcesConfigs))
      }
    while (relatedResourcesQueue.isNotEmpty()) {
      val (currentResources, currentRelatedResourceConfigs) = relatedResourcesQueue.removeFirst()
      val relatedResourceCountConfigs =
        currentRelatedResourceConfigs
          ?.asSequence()
          ?.filter { it.resultAsCount && !it.searchParameter.isNullOrEmpty() }
          ?.toList()

      relatedResourceCountConfigs?.forEach { resourceConfig ->
        val search =
          Search(resourceConfig.resource).apply {
            val filters =
              currentResources.map {
                val apply: ReferenceParamFilterCriterion.() -> Unit = {
                  value = it.logicalId.asReference(it.resourceType).reference
                }
                apply
              }
            filter(
              ReferenceClientParam(resourceConfig.searchParameter),
              *filters.toTypedArray(),
            )
            applyConfiguredSortAndFilters(
              resourceConfig = resourceConfig,
              sortData = true,
              configComputedRuleValues = configComputedRuleValues,
            )
          }

        val key = resourceConfig.id ?: resourceConfig.resource.name
        if (resourceConfig.countResultConfig?.sumCounts == true) {
          search.count(
            onSuccess = {
              relatedResourceWrapper.relatedResourceCountMap
                .getOrPut(key) { mutableListOf() }
                .apply { add(RelatedResourceCount(count = it)) }
            },
            onFailure = {
              Timber.e(
                it,
                "Error retrieving total count for all related resources identified by $key",
              )
            },
          )
        } else {
          computeCountForEachRelatedResource(
            resources = currentResources,
            resourceConfig = resourceConfig,
            relatedResourceWrapper = relatedResourceWrapper,
            configComputedRuleValues = configComputedRuleValues,
          )
        }
      }

      val searchResults =
        searchIncludedResources(
          relatedResourcesConfigs = currentRelatedResourceConfigs,
          resources = currentResources,
          configComputedRuleValues = configComputedRuleValues,
        )

      val fwdIncludedRelatedConfigsMap =
        currentRelatedResourceConfigs
          ?.revIncludeRelatedResourceConfigs(false)
          ?.groupBy { it.searchParameter!! }
          ?.mapValues { it.value.first() }

      val revIncludedRelatedConfigsMap =
        currentRelatedResourceConfigs
          ?.revIncludeRelatedResourceConfigs(true)
          ?.groupBy { "${it.resource.name}_${it.searchParameter}".lowercase() }
          ?.mapValues { it.value.first() }

      searchResults.forEach { searchResult ->
        searchResult.included?.forEach { entry ->
          updateResourceWrapperAndQueue(
            key = entry.key,
            defaultKey = entry.value.firstOrNull()?.resourceType?.name,
            resources = entry.value,
            relatedResourcesConfigsMap = fwdIncludedRelatedConfigsMap,
            relatedResourceWrapper = relatedResourceWrapper,
            relatedResourcesQueue = relatedResourcesQueue,
          )
        }
        searchResult.revIncluded?.forEach { entry ->
          val (resourceType, searchParam) = entry.key
          val key = "${resourceType.name}_$searchParam".lowercase()
          updateResourceWrapperAndQueue(
            key = key,
            defaultKey = entry.value.firstOrNull()?.resourceType?.name,
            resources = entry.value,
            relatedResourcesConfigsMap = revIncludedRelatedConfigsMap,
            relatedResourceWrapper = relatedResourceWrapper,
            relatedResourcesQueue = relatedResourcesQueue,
          )
        }
      }
    }
    return relatedResourceWrapper
  }

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
            "Error retrieving count for ${
                            baseResource.logicalId.asReference(
                                baseResource.resourceType,
                            )
                        } for related resource identified ID $key",
          )
        },
      )
    }

    // Add each related resource count query result to map
    relatedResourceWrapper.relatedResourceCountMap[key] = relatedResourceCountLinkedList
  }

  private fun updateResourceWrapperAndQueue(
    key: String,
    defaultKey: String?,
    resources: List<Resource>,
    relatedResourcesConfigsMap: Map<String, ResourceConfig>?,
    relatedResourceWrapper: RelatedResourceWrapper,
    relatedResourcesQueue: ArrayDeque<Pair<List<Resource>, List<ResourceConfig>?>>,
  ) {
    val resourceConfigs = relatedResourcesConfigsMap?.get(key)
    val id = resourceConfigs?.id ?: defaultKey
    if (!id.isNullOrBlank()) {
      relatedResourceWrapper.relatedResourceMap[id] =
        relatedResourceWrapper.relatedResourceMap
          .getOrPut(id) { mutableListOf() }
          .apply { addAll(resources.distinctBy { it.logicalId }) }
      resources.chunked(DEFAULT_BATCH_SIZE) { item ->
        with(resourceConfigs?.relatedResources) {
          if (!this.isNullOrEmpty()) {
            relatedResourcesQueue.addLast(Pair(item, this))
          }
        }
      }
    }
  }

  protected suspend fun Search.count(
    onSuccess: (Long) -> Unit = {},
    onFailure: (Throwable) -> Unit = { throwable ->
      Timber.e(
        throwable,
        "Error counting data",
      )
    },
  ): Long =
    kotlin
      .runCatching { fhirEngine.count(this@count) }
      .onSuccess { count -> onSuccess(count) }
      .onFailure { throwable -> onFailure(throwable) }
      .getOrDefault(0)

  /**
   * This function searches for reverse/forward included resources as per the configuration;
   * [RelatedResourceWrapper] data class is then used to wrap the maps used to store Search Query
   * results. The [relatedResourcesConfigs] configures which resources to load.
   */
  private suspend fun searchIncludedResources(
    relatedResourcesConfigs: List<ResourceConfig>?,
    resources: List<Resource>,
    configComputedRuleValues: Map<String, Any>,
  ): List<SearchResult<Resource>> {
    val search =
      Search(resources.first().resourceType).apply {
        val filters =
          resources.map {
            val apply: TokenParamFilterCriterion.() -> Unit = { value = of(it.logicalId) }
            apply
          }
        filter(Resource.RES_ID, *filters.toTypedArray())
      }

    // Forward include related resources e.g. a member or managingEntity of a Group resource
    val forwardIncludeResourceConfigs =
      relatedResourcesConfigs?.revIncludeRelatedResourceConfigs(false)

    // Reverse include related resources e.g. all CarePlans, Immunizations for Patient resource
    val reverseIncludeResourceConfigs =
      relatedResourcesConfigs?.revIncludeRelatedResourceConfigs(true)

    search.apply {
      reverseIncludeResourceConfigs?.forEach { resourceConfig ->
        revInclude(
          resourceConfig.resource,
          ReferenceClientParam(resourceConfig.searchParameter),
        ) {
          (this as Search).applyConfiguredSortAndFilters(
            resourceConfig = resourceConfig,
            sortData = true,
            configComputedRuleValues = configComputedRuleValues,
          )
        }
      }

      forwardIncludeResourceConfigs?.forEach { resourceConfig ->
        include(
          resourceConfig.resource,
          ReferenceClientParam(resourceConfig.searchParameter),
        ) {
          (this as Search).applyConfiguredSortAndFilters(
            resourceConfig = resourceConfig,
            sortData = true,
            configComputedRuleValues = configComputedRuleValues,
          )
        }
      }
    }
    return kotlin
      .runCatching { fhirEngine.batchedSearch<Resource>(search) }
      .onFailure { Timber.e(it, "Error fetching related resources") }
      .getOrDefault(emptyList())
  }

  private fun List<ResourceConfig>.revIncludeRelatedResourceConfigs(isRevInclude: Boolean) =
    if (isRevInclude) {
      this.filter { it.isRevInclude && !it.resultAsCount }
    } else {
      this.filter { !it.isRevInclude && !it.resultAsCount }
    }

  /**
   * Data queries for retrieving resources require the id to be provided in the format
   * [ResourceType/UUID] e.g Group/0acda8c9-3fa3-40ae-abcd-7d1fba7098b4. When resources are synced
   * up to the server the id is updated with history information e.g
   * Group/0acda8c9-3fa3-40ae-abcd-7d1fba7098b4/_history/1 This needs to be formatted to
   * [ResourceType/UUID] format and updated in the computedValuesMap
   */
  suspend fun updateResourcesRecursively(
    resourceConfig: ResourceConfig,
    subject: Resource? = null,
    eventWorkflow: EventWorkflow,
  ) {
    withContext(dispatcherProvider.io()) {
      val configRules = configRulesExecutor.generateRules(resourceConfig.configRules ?: listOf())
      val computedValuesMap =
        configRulesExecutor.fireRules(rules = configRules, baseResource = subject).mapValues {
          entry,
          ->
          val initialValue = entry.value.toString()
          if (initialValue.contains('/')) {
            """${initialValue.substringBefore("/")}/${initialValue.extractLogicalIdUuid()}"""
          } else {
            initialValue
          }
        }

      val search =
        Search(resourceConfig.resource).apply {
          applyConfiguredSortAndFilters(
            resourceConfig = resourceConfig,
            sortData = false,
            filterActiveResources = null,
            configComputedRuleValues = computedValuesMap,
          )
        }
      val resources = fhirEngine.batchedSearch<Resource>(search).map { it.resource }
      val filteredResources =
        filterResourcesByFhirPathExpression(
          resourceFilterExpressions = eventWorkflow.resourceFilterExpressions,
          resources = resources,
        )
      filteredResources.forEach {
        Timber.i("Closing Resource type ${it.resourceType.name} and id ${it.id}")
        closeResource(resource = it, eventWorkflow = eventWorkflow)
      }

      resources.forEach { resource ->
        val retrievedRelatedResources =
          retrieveRelatedResources(
            resource = resource,
            relatedResourcesConfigs = resourceConfig.relatedResources,
            configComputedRuleValues = computedValuesMap,
          )
        retrievedRelatedResources.relatedResourceMap.forEach { resourcesMap ->
          val filteredRelatedResources =
            filterResourcesByFhirPathExpression(
              resourceFilterExpressions = eventWorkflow.resourceFilterExpressions,
              resources = resourcesMap.value,
            )

          filteredRelatedResources.forEach { resource ->
            Timber.i(
              "Closing related Resource type ${resource.resourceType.name} and id ${resource.id}",
            )
            if (filterRelatedResource(resource, resourceConfig)) {
              closeResource(resource = resource, eventWorkflow = eventWorkflow)
            }
          }
        }
      }
    }
  }

  /**
   * This function filters event management resources using a filter expression. For example when
   * closing tasks we do not want to close `completed`tasks. The filter expression will be used to
   * filter out completed tasks from the resources list
   *
   * @param resourceFilterExpressions - Contains the list of conditional FhirPath expressions used
   *   for filtering resources. It also specifies the resource type that the filter expressions will
   *   be applied to
   * @param resources - The list of resources to be filtered. Note that it only contains resources
   *   of a single type.
   */
  private fun filterResourcesByFhirPathExpression(
    resourceFilterExpressions: List<ResourceFilterExpression>?,
    resources: List<Resource>,
  ): List<Resource> {
    val resourceFilterExpressionForCurrentResourceType =
      resourceFilterExpressions?.firstOrNull {
        resources.isNotEmpty() && (resources[0].resourceType == it.resourceType)
      }
    return with(resourceFilterExpressionForCurrentResourceType) {
      if ((this == null) || conditionalFhirPathExpressions.isEmpty()) {
        resources
      } else {
        resources.filter { resource ->
          if (matchAll) {
            conditionalFhirPathExpressions.all {
              fhirPathDataExtractor.extractValue(resource, it).toBoolean()
            }
          } else {
            conditionalFhirPathExpressions.any {
              fhirPathDataExtractor.extractValue(resource, it).toBoolean()
            }
          }
        }
      }
    }
  }

  @VisibleForTesting
  suspend fun closeResource(resource: Resource, eventWorkflow: EventWorkflow) {
    val conf: Configuration =
      Configuration.defaultConfiguration().apply { addOptions(Option.DEFAULT_PATH_LEAF_TO_NULL) }
    val jsonParse = JsonPath.using(conf).parse(resource.encodeResourceToString())

    val updatedResourceDocument =
      jsonParse.apply {
        eventWorkflow.updateValues
          .filter { it.resourceType == resource.resourceType }
          .forEach { updateExpression ->
            try {
              val updateValue =
                getJsonContent(
                  updateExpression.value,
                )
              // Expression stars with '$' (JSONPath) or ResourceType like in FHIRPath
              if (
                updateExpression.jsonPathExpression.startsWith("\$") &&
                  updateExpression.value != JsonNull
              ) {
                set(updateExpression.jsonPathExpression, updateValue)
              }
              if (
                updateExpression.jsonPathExpression.startsWith(
                  resource.resourceType.name,
                  ignoreCase = true,
                ) && updateExpression.value != JsonNull
              ) {
                set(
                  updateExpression.jsonPathExpression.replace(
                    resource.resourceType.name,
                    "\$",
                    ignoreCase = true,
                  ),
                  updateValue,
                )
              }
            } catch (pathNotFoundException: PathNotFoundException) {
              Timber.e(
                "Error updating ${resource.resourceType.name} with ID ${resource.id} using jsonPath ${updateExpression.jsonPathExpression} and value ${updateExpression.value} ",
              )
            }
          }
      }

    val resourceDefinition: Class<out IBaseResource>? =
      FhirContext.forR4Cached().getResourceDefinition(resource).implementingClass

    val updatedResource =
      parser.parseResource(resourceDefinition, updatedResourceDocument.jsonString())
    updatedResource.setId(updatedResource.idElement.idPart)
    fhirEngine.update(updatedResource as Resource)
  }

  private fun getJsonContent(jsonElement: JsonElement): Any? {
    return when (jsonElement) {
      is JsonPrimitive -> jsonElement.jsonPrimitive.content
      is JsonObject -> jsonElement.jsonObject
      is JsonArray -> jsonElement.jsonArray
      else -> {
        null
      }
    }
  }

  /**
   * Filtering the Related Resources is achieved by use of the filterFhirPathExpression
   * configuration. It specifies which field and values to filter the resources by.
   */
  fun filterRelatedResource(resource: Resource, resourceConfig: ResourceConfig): Boolean {
    return if (resourceConfig.filterFhirPathExpressions?.isEmpty() == true) {
      true
    } else {
      resourceConfig.filterFhirPathExpressions?.any { filterFhirPathExpression ->
        fhirPathDataExtractor.extractValue(resource, filterFhirPathExpression.key) ==
          filterFhirPathExpression.value
      } == true
    }
  }

  suspend fun purge(resource: Resource, forcePurge: Boolean) {
    try {
      fhirEngine.purge(resource.resourceType, resource.logicalId, forcePurge)
    } catch (resourceNotFoundException: ResourceNotFoundException) {
      Timber.e(
        "Purge failed -> Resource with ID ${resource.logicalId} does not exist",
        resourceNotFoundException,
      )
    }
  }

  suspend fun countResources(
    filterByRelatedEntityLocation: Boolean,
    baseResourceConfig: ResourceConfig,
    filterActiveResources: List<ActiveResourceFilterConfig>,
    configComputedRuleValues: Map<String, Any>,
  ) =
    if (filterByRelatedEntityLocation) {
      val syncLocationIds = context.retrieveRelatedEntitySyncLocationIds()
      val locationIds =
        syncLocationIds
          .map { retrieveFlattenedSubLocations(it).map { subLocation -> subLocation.logicalId } }
          .asSequence()
          .flatten()
          .toHashSet()
      val countSearch =
        Search(baseResourceConfig.resource).apply {
          applyConfiguredSortAndFilters(
            resourceConfig = baseResourceConfig,
            sortData = false,
            filterActiveResources = filterActiveResources,
            configComputedRuleValues = configComputedRuleValues,
          )
        }
      val totalCount = fhirEngine.count(countSearch)
      var searchResultsCount = 0L
      var pageNumber = 0
      var count = 0
      while (count < totalCount) {
        val baseResourceSearch =
          createSearch(
            baseResourceConfig = baseResourceConfig,
            filterActiveResources = filterActiveResources,
            configComputedRuleValues = configComputedRuleValues,
            currentPage = pageNumber,
            count = DEFAULT_BATCH_SIZE,
          )
        searchResultsCount +=
          fhirEngine
            .search<Resource>(baseResourceSearch)
            .asSequence()
            .map { it.resource }
            .filter { resource ->
              when (resource.resourceType) {
                ResourceType.Location -> locationIds.contains(resource.logicalId)
                else ->
                  resource.meta.tag.any {
                    it.system ==
                      context.getString(R.string.sync_strategy_related_entity_location_system) &&
                      locationIds.contains(it.code)
                  }
              }
            }
            .count()
            .toLong()
        count += DEFAULT_BATCH_SIZE
        pageNumber++
      }
      searchResultsCount
    } else {
      val search =
        Search(baseResourceConfig.resource).apply {
          applyConfiguredSortAndFilters(
            resourceConfig = baseResourceConfig,
            sortData = false,
            filterActiveResources = filterActiveResources,
            configComputedRuleValues = configComputedRuleValues,
          )
        }
      search.count(
        onFailure = {
          Timber.e(it, "Error counting resources ${baseResourceConfig.resource.name}")
        },
      )
    }

  suspend fun searchResourcesRecursively(
    filterByRelatedEntityLocationMetaTag: Boolean,
    filterActiveResources: List<ActiveResourceFilterConfig>?,
    fhirResourceConfig: FhirResourceConfig,
    secondaryResourceConfigs: List<FhirResourceConfig>?,
    currentPage: Int? = null,
    pageSize: Int? = null,
    configRules: List<RuleConfig>?,
  ): List<RepositoryResourceData> {
    return withContext(dispatcherProvider.io()) {
      val baseResourceConfig = fhirResourceConfig.baseResource
      val relatedResourcesConfig = fhirResourceConfig.relatedResources
      val configComputedRuleValues = configRules.configRulesComputedValues()

      if (filterByRelatedEntityLocationMetaTag) {
        val syncLocationIds = context.retrieveRelatedEntitySyncLocationIds()
        val locationIds =
          syncLocationIds
            .map { retrieveFlattenedSubLocations(it).map { subLocation -> subLocation.logicalId } }
            .flatten()
            .toHashSet()
        val countSearch =
          Search(baseResourceConfig.resource).apply {
            applyConfiguredSortAndFilters(
              resourceConfig = baseResourceConfig,
              sortData = false,
              filterActiveResources = filterActiveResources,
              configComputedRuleValues = configComputedRuleValues,
            )
          }
        val totalCount = fhirEngine.count(countSearch)
        val searchResults = ArrayDeque<SearchResult<Resource>>()
        var pageNumber = 0
        var count = 0
        while (count < totalCount) {
          val baseResourceSearch =
            createSearch(
              baseResourceConfig = baseResourceConfig,
              filterActiveResources = filterActiveResources,
              configComputedRuleValues = configComputedRuleValues,
              currentPage = pageNumber,
              count = DEFAULT_BATCH_SIZE,
            )
          val result = fhirEngine.batchedSearch<Resource>(baseResourceSearch)
          searchResults.addAll(
            result.filter { searchResult ->
              when (baseResourceConfig.resource) {
                ResourceType.Location -> locationIds.contains(searchResult.resource.logicalId)
                else ->
                  searchResult.resource.meta.tag.any {
                    it.system ==
                      context.getString(R.string.sync_strategy_related_entity_location_system) &&
                      locationIds.contains(it.code)
                  }
              }
            },
          )
          count += DEFAULT_BATCH_SIZE
          pageNumber++
          if (currentPage != null && pageSize != null) {
            val maxPageCount = (currentPage + 1) * pageSize
            if (searchResults.size >= maxPageCount) break
          }
        }

        if (currentPage != null && pageSize != null) {
          val fromIndex = currentPage * pageSize
          val toIndex = (currentPage + 1) * pageSize
          val maxSublistIndex = min(toIndex, searchResults.size)

          if (fromIndex < maxSublistIndex) {
            with(searchResults.subList(fromIndex, maxSublistIndex)) {
              mapResourceToRepositoryResourceData(
                relatedResourcesConfig = relatedResourcesConfig,
                configComputedRuleValues = configComputedRuleValues,
                secondaryResourceConfigs = secondaryResourceConfigs,
                filterActiveResources = filterActiveResources,
                baseResourceConfig = baseResourceConfig,
              )
            }
          } else {
            emptyList()
          }
        } else {
          searchResults.mapResourceToRepositoryResourceData(
            relatedResourcesConfig = relatedResourcesConfig,
            configComputedRuleValues = configComputedRuleValues,
            secondaryResourceConfigs = secondaryResourceConfigs,
            filterActiveResources = filterActiveResources,
            baseResourceConfig = baseResourceConfig,
          )
        }
      } else {
        val baseFhirResources: List<SearchResult<Resource>> =
          kotlin
            .runCatching {
              val search =
                createSearch(
                  baseResourceConfig = baseResourceConfig,
                  filterActiveResources = filterActiveResources,
                  configComputedRuleValues = configComputedRuleValues,
                  currentPage = currentPage,
                  count = pageSize,
                )
              fhirEngine.batchedSearch<Resource>(search)
            }
            .onFailure {
              Timber.e(
                t = it,
                message = "Error retrieving resources. Empty list returned by default",
              )
            }
            .getOrDefault(emptyList())
        baseFhirResources.mapResourceToRepositoryResourceData(
          relatedResourcesConfig = relatedResourcesConfig,
          configComputedRuleValues = configComputedRuleValues,
          secondaryResourceConfigs = secondaryResourceConfigs,
          filterActiveResources = filterActiveResources,
          baseResourceConfig = baseResourceConfig,
        )
      }
        as List<RepositoryResourceData>
    }
  }

  private suspend fun List<SearchResult<Resource>>.mapResourceToRepositoryResourceData(
    relatedResourcesConfig: List<ResourceConfig>,
    configComputedRuleValues: Map<String, Any>,
    secondaryResourceConfigs: List<FhirResourceConfig>?,
    filterActiveResources: List<ActiveResourceFilterConfig>?,
    baseResourceConfig: ResourceConfig,
  ) =
    this.pmap { searchResult ->
      val retrievedRelatedResources =
        retrieveRelatedResources(
          resource = searchResult.resource,
          relatedResourcesConfigs = relatedResourcesConfig,
          configComputedRuleValues = configComputedRuleValues,
        )
      val secondaryRepositoryResourceData =
        secondaryResourceConfigs.retrieveSecondaryRepositoryResourceData(filterActiveResources)
      RepositoryResourceData(
        resourceRulesEngineFactId = baseResourceConfig.id ?: baseResourceConfig.resource.name,
        resource = searchResult.resource,
        relatedResourcesMap = retrievedRelatedResources.relatedResourceMap,
        relatedResourcesCountMap = retrievedRelatedResources.relatedResourceCountMap,
        secondaryRepositoryResourceData = secondaryRepositoryResourceData,
      )
    }

  protected fun createSearch(
    baseResourceConfig: ResourceConfig,
    filterActiveResources: List<ActiveResourceFilterConfig>?,
    configComputedRuleValues: Map<String, Any>,
    currentPage: Int?,
    count: Int?,
  ): Search {
    val search =
      Search(type = baseResourceConfig.resource).apply {
        applyConfiguredSortAndFilters(
          resourceConfig = baseResourceConfig,
          filterActiveResources = filterActiveResources,
          sortData = true,
          configComputedRuleValues = configComputedRuleValues,
        )
        if (currentPage != null && count != null) {
          this.count = count
          from = currentPage * count
        }
      }
    return search
  }

  protected fun List<RuleConfig>?.configRulesComputedValues(): Map<String, Any> {
    if (this == null) return emptyMap()
    val configRules = configRulesExecutor.generateRules(this)
    return configRulesExecutor.fireRules(configRules)
  }

  /** This function fetches other resources that are not linked to the base/primary resource. */
  protected suspend fun List<FhirResourceConfig>?.retrieveSecondaryRepositoryResourceData(
    filterActiveResources: List<ActiveResourceFilterConfig>?,
  ): List<RepositoryResourceData> {
    val secondaryRepositoryResourceDataList = mutableListOf<RepositoryResourceData>()
    this?.forEach {
      secondaryRepositoryResourceDataList.addAll(
        searchResourcesRecursively(
          fhirResourceConfig = it,
          filterActiveResources = filterActiveResources,
          secondaryResourceConfigs = null,
          configRules = null,
          filterByRelatedEntityLocationMetaTag = false,
        ),
      )
    }
    return secondaryRepositoryResourceDataList
  }

  suspend fun retrieveUniqueIdAssignmentResource(
    uniqueIdAssignmentConfig: UniqueIdAssignmentConfig?,
    computedValuesMap: Map<String, Any>,
  ): Resource? {
    if (uniqueIdAssignmentConfig != null) {
      val search =
        Search(uniqueIdAssignmentConfig.resource).apply {
          uniqueIdAssignmentConfig.dataQueries.forEach {
            filterBy(dataQuery = it, configComputedRuleValues = computedValuesMap)
          }
          if (uniqueIdAssignmentConfig.sortConfigs != null) {
            sort(uniqueIdAssignmentConfig.sortConfigs)
          } else {
            sort(
              DateClientParam(LAST_UPDATED),
              Order.DESCENDING,
            )
          }
        }

      val resources = search<Resource>(search)
      val idResources =
        if (uniqueIdAssignmentConfig.resourceFilterExpression != null) {
          resources.filter { resource ->
            val (conditionalFhirPathExpressions, matchAll) =
              uniqueIdAssignmentConfig.resourceFilterExpression
            if (matchAll) {
              conditionalFhirPathExpressions.all {
                fhirPathDataExtractor.extractValue(resource, it).toBoolean()
              }
            } else {
              conditionalFhirPathExpressions.any {
                fhirPathDataExtractor.extractValue(resource, it).toBoolean()
              }
            }
          }
        } else {
          resources
        }
      return idResources.firstOrNull()
    }
    return null
  }

  suspend fun retrieveFlattenedSubLocations(locationId: String): ArrayDeque<Location> {
    val locations = ArrayDeque<Location>()
    val resources: ArrayDeque<Location> = retrieveSubLocations(locationId)
    while (resources.isNotEmpty()) {
      val currentResource = resources.removeFirst()
      locations.add(currentResource)
      retrieveSubLocations(currentResource.logicalId).forEach(resources::addLast)
    }
    loadResource<Location>(locationId)?.let { parentLocation -> locations.addFirst(parentLocation) }
    return locations
  }

  private suspend fun retrieveSubLocations(locationId: String): ArrayDeque<Location> =
    fhirEngine
      .batchedSearch<Location>(
        Search(type = ResourceType.Location).apply {
          filter(
            Location.PARTOF,
            { value = locationId.asReference(ResourceType.Location).reference },
          )
        },
      )
      .mapTo(ArrayDeque()) { it.resource }

  /**
   * A wrapper data class to hold search results. All related resources are flattened into one Map
   * including the nested related resources as required by the Rules Engine facts.
   */
  data class RelatedResourceWrapper(
    val relatedResourceMap: MutableMap<String, MutableList<Resource>> = mutableMapOf(),
    val relatedResourceCountMap: MutableMap<String, MutableList<RelatedResourceCount>> =
      mutableMapOf(),
  )

  companion object {
    const val DEFAULT_BATCH_SIZE = 250
    const val SNOMED_SYSTEM = "http://hl7.org/fhir/R4B/valueset-condition-clinical.html"
    const val PATIENT_CONDITION_RESOLVED_CODE = "resolved"
    const val PATIENT_CONDITION_RESOLVED_DISPLAY = "Resolved"
    const val TAG = "_tag"
    const val LAST_UPDATED = "_lastUpdated"
    const val ACTIVE = "active"
  }
}
