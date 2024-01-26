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

import androidx.annotation.VisibleForTesting
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.IParser
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
import com.google.android.fhir.search.include
import com.google.android.fhir.search.revInclude
import com.google.android.fhir.search.search
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.Option
import java.util.LinkedList
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.hl7.fhir.instance.model.api.IBaseResource
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
import org.smartregister.fhircore.engine.configuration.event.EventWorkflow
import org.smartregister.fhircore.engine.configuration.profile.ManagingEntityConfig
import org.smartregister.fhircore.engine.configuration.register.ActiveResourceFilterConfig
import org.smartregister.fhircore.engine.data.local.register.RegisterRepository
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
import org.smartregister.fhircore.engine.util.extension.encodeResourceToString
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
  open val parser: IParser,
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
    token: TokenClientParam,
    subjectType: ResourceType,
    subjectId: String,
    dataQueries: List<DataQuery> = listOf(),
    configComputedRuleValues: Map<String, Any>,
  ): List<T> =
    withContext(dispatcherProvider.io()) {
      fhirEngine
        .search<T> {
          filterByResourceTypeId(token, subjectType, subjectId)
          dataQueries.forEach {
            filterBy(
              dataQuery = it,
              configComputedRuleValues = configComputedRuleValues,
            )
          }
        }
        .map { it.resource }
    }

  suspend fun searchCondition(dataRequirement: DataRequirement) =
    when (dataRequirement.type) {
      Enumerations.ResourceType.CONDITION.toCode() ->
        fhirEngine
          .search<Condition> {
            dataRequirement.codeFilter.forEach {
              filter(TokenClientParam(it.path), { value = of(it.codeFirstRep) })
            }
            // TODO handle date filter
          }
          .map { it.resource }
      else -> listOf()
    }

  suspend inline fun <reified R : Resource> search(search: Search) =
    fhirEngine.search<R>(search).map { it.resource }

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
      fhirEngine.create(*resource, isLocalOnly = true)
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
      } else {
        fhirEngine.delete(resourceType, resourceId)
      }
    }
  }

  suspend fun delete(resource: Resource, softDelete: Boolean = false) {
    withContext(dispatcherProvider.io()) {
      if (softDelete) {
        softDelete(resource)
      } else {
        fhirEngine.delete(resource.resourceType, resource.logicalId)
      }
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

  suspend fun loadManagingEntity(group: Group) =
    group.managingEntity?.let { reference ->
      fhirEngine
        .search<RelatedPerson> {
          filter(RelatedPerson.RES_ID, { value = of(reference.extractId()) })
        }
        .map { it.resource }
        .firstOrNull()
        ?.let { relatedPerson ->
          fhirEngine
            .search<Patient> {
              filter(Patient.RES_ID, { value = of(relatedPerson.patient.extractId()) })
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
    resources: List<Resource>,
    relatedResourcesConfigs: List<ResourceConfig>?,
    relatedResourceWrapper: RelatedResourceWrapper,
    configComputedRuleValues: Map<String, Any>,
  ): RelatedResourceWrapper {
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

    searchIncludedResources(
      relatedResourcesConfigs = relatedResourcesConfigs,
      resources = resources,
      relatedResourceWrapper = relatedResourceWrapper,
      configComputedRuleValues = configComputedRuleValues,
    )

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
   * This function searches for reverse/forward included resources as per the configuration;
   * [RelatedResourceWrapper] data class is then used to wrap the maps used to store Search Query
   * results. The [relatedResourcesConfigs] configures which resources to load.
   */
  private suspend fun searchIncludedResources(
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

      // Forward include related resources e.g. Members (Patient) referenced in Group resource
      val forwardIncludeResourceConfigs =
        relatedResourcesConfigs.revIncludeRelatedResourceConfigs(false)

      // Reverse include related resources e.g. All CarePlans, Immunization for Patient resource
      val reverseIncludeResourceConfigs =
        relatedResourcesConfigs.revIncludeRelatedResourceConfigs(true)

      search.apply {
        reverseIncludeResourceConfigs.forEach { resourceConfig ->
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

        forwardIncludeResourceConfigs.forEach { resourceConfig ->
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

      searchRelatedResources(
        search = search,
        relatedResourcesConfigsMap = relatedResourcesConfigsMap,
        relatedResourceWrapper = relatedResourceWrapper,
        configComputedRuleValues = configComputedRuleValues,
      )
    }
  }

  private suspend fun searchRelatedResources(
    search: Search,
    relatedResourcesConfigsMap: Map<ResourceType, List<ResourceConfig>>,
    relatedResourceWrapper: RelatedResourceWrapper,
    configComputedRuleValues: Map<String, Any>,
  ) {
    kotlin
      .runCatching { fhirEngine.search<Resource>(search) }
      .onSuccess { searchResult ->
        searchResult.forEach { currentSearchResult ->
          val includedResources: Map<ResourceType, List<Resource>>? =
            currentSearchResult.included?.values?.flatten()?.groupBy { it.resourceType }
          val reverseIncludedResources: Map<ResourceType, List<Resource>>? =
            currentSearchResult.revIncluded?.values?.flatten()?.groupBy { it.resourceType }
          val theRelatedResourcesMap =
            mutableMapOf<ResourceType, List<Resource>>().apply {
              includedResources?.let { putAll(it) }
              reverseIncludedResources?.let { putAll(it) }
            }
          theRelatedResourcesMap.forEach { entry ->
            val currentResourceConfigs = relatedResourcesConfigsMap[entry.key]

            val key = // Use configured id as key otherwise default to ResourceType
              if (relatedResourcesConfigsMap.containsKey(entry.key)) {
                currentResourceConfigs?.firstOrNull()?.id ?: entry.key.name
              } else {
                entry.key.name
              }

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
    } else {
      this.filter { !it.isRevInclude && !it.resultAsCount }
    }

  suspend fun updateResourcesRecursively(
    resourceConfig: ResourceConfig,
    subject: Resource,
    eventWorkflow: EventWorkflow,
  ) {
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
        "${entry.value.toString().substringBefore("/")}/${
                    entry.value.toString().extractLogicalIdUuid()
                }"
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
    val resources = fhirEngine.search<Resource>(search).map { it.resource }
    val filteredResources =
      filterResourcesByFhirPathExpression(
        resourceFilterExpression = eventWorkflow.resourceFilterExpression,
        resources = resources,
      )
    filteredResources.forEach {
      Timber.i("Closing Resource type ${it.resourceType.name} and id ${it.id}")
      closeResource(resource = it, eventWorkflow = eventWorkflow)
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
      val filteredRelatedResources =
        filterResourcesByFhirPathExpression(
          resourceFilterExpression = eventWorkflow.resourceFilterExpression,
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

  fun filterResourcesByFhirPathExpression(
    resourceFilterExpression: ResourceFilterExpression?,
    resources: List<Resource>,
  ): List<Resource> {
    return with(resourceFilterExpression) {
      if ((this == null) || conditionalFhirPathExpressions.isNullOrEmpty()) {
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
    var conf: Configuration =
      Configuration.defaultConfiguration().apply { addOptions(Option.DEFAULT_PATH_LEAF_TO_NULL) }
    val jsonParse = JsonPath.using(conf).parse(resource.encodeResourceToString())

    val updatedResourceDocument =
      jsonParse.apply {
        eventWorkflow.updateValues.forEach { updateExpression ->
          val updateValue =
            getJsonContent(
              updateExpression.value,
            )
          // Expression stars with '$' (JSONPath) or ResourceType like in FHIRPath
          if (
            updateExpression.jsonPathExpression.startsWith("\$") && updateExpression.value != null
          ) {
            set(updateExpression.jsonPathExpression, updateValue)
          }
          if (
            updateExpression.jsonPathExpression.startsWith(
              resource.resourceType.name,
              ignoreCase = true,
            ) && updateExpression.value != null
          ) {
            set(
              updateExpression.jsonPathExpression.replace(resource.resourceType.name, "\$"),
              updateValue,
            )
          }
        }
      }

    val resourceDefinition: Class<out IBaseResource>? =
      FhirContext.forR4Cached().getResourceDefinition(resource).implementingClass

    val updatedResource =
      parser.parseResource(resourceDefinition, updatedResourceDocument.jsonString())
    updatedResource.setId(updatedResource.idElement.idPart)
    withContext(dispatcherProvider.io()) { fhirEngine.update(updatedResource as Resource) }
  }

  fun getJsonContent(jsonElement: JsonElement): Any? {
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
      withContext(dispatcherProvider.io()) {
        fhirEngine.purge(resource.resourceType, resource.logicalId, forcePurge)
      }
    } catch (resourceNotFoundException: ResourceNotFoundException) {
      Timber.e(
        "Purge failed -> Resource with ID ${resource.logicalId} does not exist",
        resourceNotFoundException,
      )
    }
  }

  suspend fun searchResourcesRecursively(
    filterActiveResources: List<ActiveResourceFilterConfig>?,
    fhirResourceConfig: FhirResourceConfig,
    secondaryResourceConfigs: List<FhirResourceConfig>?,
    currentPage: Int? = null,
    pageSize: Int? = null,
    configRules: List<RuleConfig>?,
  ): List<RepositoryResourceData> {
    val baseResourceConfig = fhirResourceConfig.baseResource
    val relatedResourcesConfig = fhirResourceConfig.relatedResources
    val configComputedRuleValues = configRules.configRulesComputedValues()
    val search =
      Search(type = baseResourceConfig.resource).apply {
        applyConfiguredSortAndFilters(
          resourceConfig = baseResourceConfig,
          filterActiveResources = filterActiveResources,
          sortData = true,
          configComputedRuleValues = configComputedRuleValues,
        )
        if (currentPage != null && pageSize != null) {
          count = pageSize
          from = currentPage * pageSize
        }
      }

    val baseFhirResources =
      kotlin
        .runCatching {
          withContext(dispatcherProvider.io()) { fhirEngine.search<Resource>(search) }
        }
        .onFailure { Timber.e(it, "Error retrieving resources. Empty list returned by default") }
        .getOrDefault(emptyList())

    return baseFhirResources.map { searchResult ->
      val retrievedRelatedResources =
        withContext(dispatcherProvider.io()) {
          retrieveRelatedResources(
            resources = listOf(searchResult.resource),
            relatedResourcesConfigs = relatedResourcesConfig,
            relatedResourceWrapper = RelatedResourceWrapper(),
            configComputedRuleValues = configComputedRuleValues,
          )
        }
      RepositoryResourceData(
        resourceRulesEngineFactId = baseResourceConfig.id ?: baseResourceConfig.resource.name,
        resource = searchResult.resource,
        relatedResourcesMap = retrievedRelatedResources.relatedResourceMap,
        relatedResourcesCountMap = retrievedRelatedResources.relatedResourceCountMap,
        secondaryRepositoryResourceData =
          withContext(dispatcherProvider.io()) {
            secondaryResourceConfigs.retrieveSecondaryRepositoryResourceData(
              filterActiveResources,
            )
          },
      )
    }
  }

  protected fun List<RuleConfig>?.configRulesComputedValues(): Map<String, Any> {
    if (this == null) return emptyMap()
    val configRules = configRulesExecutor.generateRules(this)
    return configRulesExecutor.fireRules(configRules)
  }

  /** This function fetches other resources that are not linked to the base/primary resource. */
  protected suspend fun List<FhirResourceConfig>?.retrieveSecondaryRepositoryResourceData(
    filterActiveResources: List<ActiveResourceFilterConfig>?,
  ): LinkedList<RepositoryResourceData> {
    val secondaryRepositoryResourceDataLinkedList = LinkedList<RepositoryResourceData>()
    this?.forEach {
      secondaryRepositoryResourceDataLinkedList.addAll(
        searchResourcesRecursively(
          fhirResourceConfig = it,
          filterActiveResources = filterActiveResources,
          secondaryResourceConfigs = null,
          configRules = null,
        ),
      )
    }
    return secondaryRepositoryResourceDataLinkedList
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
