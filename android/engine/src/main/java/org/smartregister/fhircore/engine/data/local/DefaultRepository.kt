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
import com.google.android.fhir.getResourceType
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
import javax.inject.Singleton
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
import org.smartregister.fhircore.engine.domain.model.MultiSelectViewAction
import org.smartregister.fhircore.engine.domain.model.RelatedResourceCount
import org.smartregister.fhircore.engine.domain.model.RepositoryResourceData
import org.smartregister.fhircore.engine.domain.model.ResourceConfig
import org.smartregister.fhircore.engine.domain.model.ResourceFilterExpression
import org.smartregister.fhircore.engine.domain.model.RuleConfig
import org.smartregister.fhircore.engine.domain.model.SortConfig
import org.smartregister.fhircore.engine.rulesengine.RulesExecutor
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
import org.smartregister.fhircore.engine.util.extension.retrieveRelatedEntitySyncLocationState
import org.smartregister.fhircore.engine.util.extension.updateFrom
import org.smartregister.fhircore.engine.util.extension.updateLastUpdated
import org.smartregister.fhircore.engine.util.fhirpath.FhirPathDataExtractor
import timber.log.Timber

typealias SearchQueryResultQueue =
  ArrayDeque<Triple<List<String>, ResourceConfig, Map<String, String>>>

typealias RelatedResourcesQueue = ArrayDeque<Triple<List<String>, ResourceConfig, String>>

@Singleton
open class DefaultRepository
@Inject
constructor(
  open val fhirEngine: FhirEngine,
  open val dispatcherProvider: DispatcherProvider,
  open val sharedPreferencesHelper: SharedPreferencesHelper,
  open val configurationRegistry: ConfigurationRegistry,
  open val configService: ConfigService,
  open val rulesExecutor: RulesExecutor,
  open val fhirPathDataExtractor: FhirPathDataExtractor,
  open val parser: IParser,
  @ApplicationContext open val context: Context,
  open val contentCache: ContentCache,
) {

  suspend inline fun <reified T : Resource> loadResource(resourceId: String): T? =
    fhirEngine.loadResource(resourceId)

  @Throws(ResourceNotFoundException::class)
  suspend fun loadResource(resourceId: String, resourceType: ResourceType): Resource =
    fhirEngine.get(resourceType, resourceId)

  @Throws(ResourceNotFoundException::class)
  suspend fun loadResource(reference: Reference) =
    IdType(reference.reference).let {
      fhirEngine.get(ResourceType.fromCode(it.resourceType), it.idPart)
    }

  suspend inline fun <reified T : Resource> loadResourceFromCache(resourceId: String): T? {
    val resourceType = getResourceType(T::class.java)
    val resource =
      contentCache.getResource(resourceType, resourceId)
        ?: fhirEngine.loadResource<T>(resourceId)?.let { contentCache.saveResource(it) }
    return resource as? T
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

  suspend fun applyDbTransaction(block: suspend () -> Unit) {
    fhirEngine.withTransaction { block.invoke() }
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

  private suspend fun computeCountForEachRelatedResource(
    resources: List<Resource>,
    resourceConfig: ResourceConfig,
    configComputedRuleValues: Map<String, Any>,
    repositoryResourceData: RepositoryResourceData,
  ) {
    val relatedResourceCountList = LinkedList<RelatedResourceCount>()
    resources.forEach { resource ->
      val countSearch =
        Search(resourceConfig.resource).apply {
          filter(
            ReferenceClientParam(resourceConfig.searchParameter),
            { value = resource.logicalId.asReference(resource.resourceType).reference },
          )
          applyConfiguredSortAndFilters(
            resourceConfig = resourceConfig,
            sortData = false,
            configComputedRuleValues = configComputedRuleValues,
          )
        }
      countSearch.count(
        onSuccess = {
          relatedResourceCountList.add(
            RelatedResourceCount(
              relatedResourceType = resourceConfig.resource,
              parentResourceId = resource.logicalId,
              count = it,
            ),
          )
        },
        onFailure = { throwable ->
          Timber.e(
            throwable,
            "Error retrieving count for ${resource.asReference().reference} for related resource identified ID ${resourceConfig.id ?: resourceConfig.resource.name}",
          )
        },
      )
    }

    if (relatedResourceCountList.isNotEmpty()) {
      val key = resourceConfig.id ?: resourceConfig.resource.name
      repositoryResourceData.relatedResourcesCountMap.apply { put(key, relatedResourceCountList) }
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
      val configRules =
        rulesExecutor.rulesFactory.generateRules(resourceConfig.configRules ?: listOf())
      val configComputedRuleValues =
        rulesExecutor.computeConfigRules(rules = configRules, baseResource = subject).mapValues {
          entry,
          ->
          val initialValue = entry.value.toString()
          if (initialValue.contains('/')) {
            """${initialValue.substringBefore("/")}/${initialValue.extractLogicalIdUuid()}"""
          } else {
            initialValue
          }
        }

      val repositoryResourceDataList =
        searchNestedResources(
          baseResourceIds = null,
          fhirResourceConfig = FhirResourceConfig(resourceConfig, resourceConfig.relatedResources),
          configComputedRuleValues = configComputedRuleValues,
          activeResourceFilters = null,
          currentPage = null,
          pageSize = null,
        )

      repositoryResourceDataList.forEach { entry ->
        val repoResourceData = entry.value
        filterResourcesByFhirPathExpression(
            resourceFilterExpressions = eventWorkflow.resourceFilterExpressions,
            resources = listOf(repoResourceData.resource),
          )
          .forEach {
            Timber.i("Closing Resource type ${it.resourceType.name} and id ${it.id}")
            closeResource(resource = it, eventWorkflow = eventWorkflow)
          }

        repoResourceData.relatedResourcesMap.forEach { resourcesMap ->
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
      repositoryResourceDataList.clear()
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
    val resourceFilterExpression =
      resourceFilterExpressions?.firstOrNull {
        it.resourceType == resources.firstOrNull()?.resourceType
      }
    if (resourceFilterExpression == null) return resources
    val (conditionalFhirPathExpressions, matchAll, _) = resourceFilterExpression
    return resources.filter { resource ->
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

  suspend fun searchNestedResources(
    baseResourceIds: List<String>?,
    fhirResourceConfig: FhirResourceConfig,
    configComputedRuleValues: Map<String, Any>,
    activeResourceFilters: List<ActiveResourceFilterConfig>?,
    filterByRelatedEntityLocationMetaTag: Boolean = false,
    currentPage: Int?,
    pageSize: Int?,
  ): MutableMap<String, RepositoryResourceData> {
    val resultsDataMap = mutableMapOf<String, RepositoryResourceData>()
    if (filterByRelatedEntityLocationMetaTag) {
      val locationIds =
        context
          .retrieveRelatedEntitySyncLocationState(MultiSelectViewAction.FILTER_DATA)
          .map { it.locationId }
          .map { retrieveFlattenedSubLocations(it).map { subLocation -> subLocation.logicalId } }
          .asSequence()
          .flatten()
          .chunked(SQL_WHERE_CLAUSE_LIMIT)

      if (currentPage != null && pageSize != null) {
        for (ids in locationIds) {
          if (resultsDataMap.size == pageSize) return resultsDataMap
          val searchResults =
            searchResources(
              baseResourceIds = ids,
              baseResourceConfig = fhirResourceConfig.baseResource,
              relatedResourcesConfigs = fhirResourceConfig.relatedResources,
              activeResourceFilters = activeResourceFilters,
              configComputedRuleValues = configComputedRuleValues,
              currentPage = currentPage,
              pageSize = pageSize,
              relTagCodeSystem =
                context.getString(R.string.sync_strategy_related_entity_location_system),
            )
          processSearchResult(
            searchResults = searchResults,
            resultsDataMap = resultsDataMap,
            fhirResourceConfig = fhirResourceConfig,
            configComputedRuleValues = configComputedRuleValues,
            activeResourceFilters = activeResourceFilters,
            pageSizeLimit = pageSize,
          )
        }
      } else {
        for (ids in locationIds) {
          val searchResults =
            searchResources(
              baseResourceIds = ids,
              baseResourceConfig = fhirResourceConfig.baseResource,
              relatedResourcesConfigs = fhirResourceConfig.relatedResources,
              activeResourceFilters = activeResourceFilters,
              configComputedRuleValues = configComputedRuleValues,
              currentPage = null,
              pageSize = null,
              relTagCodeSystem =
                context.getString(R.string.sync_strategy_related_entity_location_system),
            )
          processSearchResult(
            searchResults = searchResults,
            resultsDataMap = resultsDataMap,
            fhirResourceConfig = fhirResourceConfig,
            configComputedRuleValues = configComputedRuleValues,
            activeResourceFilters = activeResourceFilters,
            pageSizeLimit = null,
          )
        }
      }
    } else {
      val searchResults =
        searchResources(
          baseResourceIds = baseResourceIds,
          baseResourceConfig = fhirResourceConfig.baseResource,
          relatedResourcesConfigs = fhirResourceConfig.relatedResources,
          activeResourceFilters = activeResourceFilters,
          configComputedRuleValues = configComputedRuleValues,
          currentPage = currentPage,
          pageSize = pageSize,
        )
      processSearchResult(
        searchResults = searchResults,
        resultsDataMap = resultsDataMap,
        fhirResourceConfig = fhirResourceConfig,
        configComputedRuleValues = configComputedRuleValues,
        activeResourceFilters = activeResourceFilters,
      )
    }
    return resultsDataMap
  }

  private suspend fun processSearchResult(
    searchResults: List<SearchResult<Resource>>,
    resultsDataMap: MutableMap<String, RepositoryResourceData>,
    fhirResourceConfig: FhirResourceConfig,
    configComputedRuleValues: Map<String, Any>,
    activeResourceFilters: List<ActiveResourceFilterConfig>?,
    pageSizeLimit: Int? = null,
  ) {
    val processedSearchResults =
      handleSearchResults(
        searchResults = searchResults,
        repositoryResourceDataResultMap = resultsDataMap,
        repositoryResourceDataMap = null,
        relatedResourceConfigs = fhirResourceConfig.relatedResources,
        baseResourceConfigId = fhirResourceConfig.baseResource.id,
        configComputedRuleValues = configComputedRuleValues,
        limit = pageSizeLimit,
      )

    while (processedSearchResults.isNotEmpty()) {
      val (newBaseResourceIds, newResourceConfig, repositoryResourceDataMap) =
        processedSearchResults.removeFirst()
      val newSearchResults =
        searchResources(
          baseResourceIds = newBaseResourceIds,
          baseResourceConfig = newResourceConfig,
          relatedResourcesConfigs = newResourceConfig.relatedResources,
          activeResourceFilters = activeResourceFilters,
          configComputedRuleValues = configComputedRuleValues,
          currentPage = null,
          pageSize = null,
        )

      val newProcessedSearchResults =
        handleSearchResults(
          searchResults = newSearchResults,
          repositoryResourceDataResultMap = resultsDataMap,
          repositoryResourceDataMap = repositoryResourceDataMap,
          relatedResourceConfigs = newResourceConfig.relatedResources,
          baseResourceConfigId = fhirResourceConfig.baseResource.id,
          configComputedRuleValues = configComputedRuleValues,
          limit = null,
        )
      processedSearchResults.addAll(newProcessedSearchResults)
    }
  }

  private suspend fun searchResources(
    baseResourceIds: List<String>?,
    baseResourceConfig: ResourceConfig,
    relatedResourcesConfigs: List<ResourceConfig>,
    activeResourceFilters: List<ActiveResourceFilterConfig>?,
    configComputedRuleValues: Map<String, Any>,
    relTagCodeSystem: String? = null,
    currentPage: Int?,
    pageSize: Int?,
  ): List<SearchResult<Resource>> {
    val search =
      createSearch(
        baseResourceIds = baseResourceIds,
        baseResourceConfig = baseResourceConfig,
        filterActiveResources = activeResourceFilters,
        configComputedRuleValues = configComputedRuleValues,
        currentPage = currentPage,
        count = pageSize,
        relTagCodeSystem = relTagCodeSystem,
      )

    val (forwardIncludes, reverseIncludes) =
      relatedResourcesConfigs.filter { !it.resultAsCount }.partition { !it.isRevInclude }

    search.apply {
      reverseIncludes.forEach { resourceConfig ->
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
      forwardIncludes.forEach { resourceConfig ->
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
    return fhirEngine.batchedSearch(search)
  }

  private suspend fun handleSearchResults(
    searchResults: List<SearchResult<Resource>>,
    repositoryResourceDataResultMap: MutableMap<String, RepositoryResourceData>,
    repositoryResourceDataMap: Map<String, String>?,
    relatedResourceConfigs: List<ResourceConfig>,
    baseResourceConfigId: String?,
    configComputedRuleValues: Map<String, Any>,
    limit: Int?,
  ): SearchQueryResultQueue {
    val relatedResourcesQueue = RelatedResourcesQueue()
    val (forwardIncludes, reverseIncludes) =
      relatedResourceConfigs
        .asSequence()
        .filter { !it.resultAsCount }
        .partition { !it.isRevInclude }
    val forwardIncludesMap =
      forwardIncludes.groupBy { it.searchParameter!! }.mapValues { it.value.first() }
    val reverseIncludesMap =
      reverseIncludes
        .groupBy { "${it.resource.name}_${it.searchParameter}".lowercase() }
        .mapValues { it.value.first() }
    val includedResourcesCountConfigs =
      relatedResourceConfigs
        .asSequence()
        .flatMap { it.relatedResources }
        .filter { it.resultAsCount && !it.searchParameter.isNullOrEmpty() }
        .toList()

    searchResults.forEach { searchResult: SearchResult<Resource> ->
      // Create new repository data if none exist (subsequent queries will have repository data)
      // First get the key for the repository data, then proceed to retrieve it from the result map
      val repositoryResourceDataMapId =
        repositoryResourceDataMap?.get(searchResult.resource.logicalId)
      val repositoryResourceData =
        repositoryResourceDataResultMap[repositoryResourceDataMapId]
          ?: RepositoryResourceData(
            resource = searchResult.resource,
            resourceConfigId = baseResourceConfigId,
          )

      searchResult.included?.forEach { entry ->
        // Add the forward included resources to the relatedResourcesMap
        val relatedResourceConfig = forwardIncludesMap[entry.key]
        updateRepositoryResourceData(
          resources = entry.value,
          relatedResourceConfig = relatedResourceConfig,
          repositoryResourceData = repositoryResourceData,
          relatedResourcesQueue = relatedResourcesQueue,
        )
        if (entry.value.isNotEmpty()) {
          handleCountResults(
            resources = entry.value,
            repositoryResourceData = repositoryResourceData,
            countConfigs = includedResourcesCountConfigs,
            configComputedRuleValues = configComputedRuleValues,
          )
        }
      }
      searchResult.revIncluded?.forEach { entry ->
        val (resourceType, searchParam) = entry.key
        val name = "${resourceType.name}_$searchParam".lowercase()
        val relatedResourceConfig = reverseIncludesMap[name]
        // Add the reverse included resources to the relatedResourcesMap
        updateRepositoryResourceData(
          resources = entry.value,
          relatedResourceConfig = relatedResourceConfig,
          repositoryResourceData = repositoryResourceData,
          relatedResourcesQueue = relatedResourcesQueue,
        )
        if (entry.value.isNotEmpty()) {
          handleCountResults(
            resources = entry.value,
            repositoryResourceData = repositoryResourceData,
            countConfigs = includedResourcesCountConfigs,
            configComputedRuleValues = configComputedRuleValues,
          )
        }
      }
      if (
        repositoryResourceDataMap == null &&
          (limit == null || repositoryResourceDataResultMap.size < limit)
      ) {
        repositoryResourceDataResultMap[searchResult.resource.logicalId] = repositoryResourceData
      }
    }
    return groupAndBatchQueriedResources(relatedResourcesQueue)
  }

  private fun updateRepositoryResourceData(
    resources: List<Resource>,
    relatedResourceConfig: ResourceConfig?,
    repositoryResourceData: RepositoryResourceData?,
    relatedResourcesQueue: RelatedResourcesQueue,
  ) {
    if (resources.isNotEmpty() && repositoryResourceData != null) {
      val key = relatedResourceConfig?.id ?: relatedResourceConfig?.resource?.name
      if (!key.isNullOrBlank()) {
        repositoryResourceData.apply {
          relatedResourcesMap
            .getOrPut(key = key) { mutableListOf() }
            .apply { (this as MutableList).addAll(resources.distinctBy { it.logicalId }) }
        }
      }

      if (!relatedResourceConfig?.relatedResources.isNullOrEmpty()) {
        // Track the next nested resource to be fetched. ID for base resources is the unique key
        relatedResourcesQueue.addLast(
          Triple(
            first = resources.map { it.logicalId }.distinct(),
            second = relatedResourceConfig!!,
            third = repositoryResourceData.resource.logicalId,
          ),
        )
      }
    }
  }

  /**
   * Count the related resources that references the provided [resources]. The count is updated in
   * the [repositoryResourceData].
   */
  private suspend fun handleCountResults(
    resources: List<Resource>,
    repositoryResourceData: RepositoryResourceData,
    countConfigs: List<ResourceConfig>,
    configComputedRuleValues: Map<String, Any>,
  ) {
    if (countConfigs.isEmpty()) return
    resources.chunked(RESOURCE_BATCH_SIZE).forEach { theResources ->
      countRelatedResources(
        resources = theResources,
        repositoryResourceData = repositoryResourceData,
        countConfigs = countConfigs,
        configComputedRuleValues = configComputedRuleValues,
      )
    }
  }

  private suspend fun countRelatedResources(
    resources: List<Resource>,
    repositoryResourceData: RepositoryResourceData,
    countConfigs: List<ResourceConfig>,
    configComputedRuleValues: Map<String, Any>,
  ) {
    countConfigs.forEach { resourceConfig ->
      if (resourceConfig.countResultConfig?.sumCounts == true) {
        // Count all the related resources. E.g. count all members (Patient) of household (Group)
        val countSearch =
          Search(resourceConfig.resource).apply {
            val filters =
              resources.map {
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
              sortData = false,
              configComputedRuleValues = configComputedRuleValues,
            )
          }
        val key = resourceConfig.id ?: resourceConfig.resource.name
        countSearch.count(
          onSuccess = {
            repositoryResourceData.apply {
              relatedResourcesCountMap
                .getOrPut(key) { mutableListOf() }
                .apply {
                  (this as MutableList).add(
                    RelatedResourceCount(
                      count = it,
                      relatedResourceType = resourceConfig.resource,
                    ),
                  )
                }
            }
          },
          onFailure = {
            Timber.e(
              it,
              "Error retrieving total count for all related resources identified by $key",
            )
          },
        )
      } else {
        // Count each related resources, e.g. number of visits (Encounter) for every Patient
        computeCountForEachRelatedResource(
          resources = resources,
          resourceConfig = resourceConfig,
          configComputedRuleValues = configComputedRuleValues,
          repositoryResourceData = repositoryResourceData,
        )
      }
    }
  }

  protected fun createSearch(
    baseResourceIds: List<String>? = null,
    baseResourceConfig: ResourceConfig,
    filterActiveResources: List<ActiveResourceFilterConfig>?,
    configComputedRuleValues: Map<String, Any>,
    currentPage: Int?,
    count: Int?,
    relTagCodeSystem: String?,
  ): Search {
    val search =
      Search(type = baseResourceConfig.resource).apply {
        if (!baseResourceIds.isNullOrEmpty()) {
          val filters =
            baseResourceIds.map {
              val apply: TokenParamFilterCriterion.() -> Unit = { value = of(it) }
              apply
            }
          if (!relTagCodeSystem.isNullOrBlank()) {
            filter(
              when (baseResourceConfig.resource) {
                ResourceType.Location -> Resource.RES_ID
                else -> TokenClientParam(TAG)
              },
              *filters.toTypedArray(),
            )
          } else {
            filter(Resource.RES_ID, *filters.toTypedArray())
          }
        }
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

  /**
   * Groups resources by their [ResourceConfig] and batches them into groups of up to a specified
   * size.
   *
   * This function combines resources across multiple triples with the same [ResourceConfig],
   * ensuring all resources are grouped together while maintaining their association with the
   * respective [RepositoryResourceData]. Each batch contains a mapping of resource logicalId to
   * their corresponding [RepositoryResourceData], allowing traceability.
   *
   * @param relatedResourcesQueue An [ArrayDeque] containing triples of:
   * - A list of [Resource.logicalId]s to be grouped and batched.
   * - A [ResourceConfig] shared by all resources in the triple.
   * - A [RepositoryResourceData] shared by all resources in the triple.
   *
   * @param batchSize The maximum number of resources in each batch. Must be greater than 0.
   * @return A [ArrayDeque] of triples where each triple contains:
   * - A list of [Resource.logicalId]s grouped into batches of up to [batchSize].
   * - The [ResourceConfig] shared by the batch.
   * - A map of [Resource.logicalId] to its corresponding [RepositoryResourceData], ensuring
   *   traceability.
   *
   * @throws IllegalArgumentException if [batchSize] is less than or equal to 0.
   *
   * ```
   */
  private fun groupAndBatchQueriedResources(
    relatedResourcesQueue: RelatedResourcesQueue,
    batchSize: Int = RESOURCE_BATCH_SIZE,
  ): SearchQueryResultQueue {
    require(batchSize > 0) { "Batch size must be greater than 0" }
    val resultQueue = SearchQueryResultQueue()
    val bufferMap = mutableMapOf<ResourceConfig, MutableList<Pair<String, String>>>()

    while (relatedResourcesQueue.isNotEmpty()) {
      val (resourceIds, config, data) = relatedResourcesQueue.removeFirst()
      val buffer = bufferMap.getOrPut(config) { mutableListOf() }

      resourceIds.forEach { id -> buffer.add(id to data) }

      // Create and add batches to the result queue
      while (buffer.size >= batchSize) {
        val batch = List(batchSize) { buffer.removeFirst() }
        resultQueue.addLast(
          Triple(
            first = batch.map { it.first },
            second = config,
            third = batch.associate { it.first to it.second },
          ),
        )
      }
    }

    // Add any remaining items in the buffers
    for ((config, buffer) in bufferMap) {
      if (buffer.isNotEmpty()) {
        val batch = buffer.toList()
        resultQueue.addLast(
          Triple(
            first = batch.map { it.first },
            second = config,
            third = batch.associate { it.first to it.second },
          ),
        )
        buffer.clear()
      }
    }
    return resultQueue
  }

  protected fun List<RuleConfig>?.configRulesComputedValues(): Map<String, Any> {
    if (this == null) return emptyMap()
    val rules = rulesExecutor.rulesFactory.generateRules(this)
    return rulesExecutor.computeConfigRules(rules = rules, null)
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

  companion object {
    const val RESOURCE_BATCH_SIZE = 50
    const val SQL_WHERE_CLAUSE_LIMIT = 200 // Hard limit for WHERE CLAUSE items is 1000
    const val SNOMED_SYSTEM = "http://hl7.org/fhir/R4B/valueset-condition-clinical.html"
    const val PATIENT_CONDITION_RESOLVED_CODE = "resolved"
    const val PATIENT_CONDITION_RESOLVED_DISPLAY = "Resolved"
    const val TAG = "_tag"
    const val LAST_UPDATED = "_lastUpdated"
    const val ACTIVE = "active"
  }
}
