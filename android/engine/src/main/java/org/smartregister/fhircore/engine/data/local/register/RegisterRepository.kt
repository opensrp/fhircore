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

package org.smartregister.fhircore.engine.data.local.register

import android.database.Cursor
import ca.uhn.fhir.rest.gclient.DateClientParam
import ca.uhn.fhir.rest.gclient.NumberClientParam
import ca.uhn.fhir.rest.gclient.ReferenceClientParam
import ca.uhn.fhir.rest.gclient.StringClientParam
import ca.uhn.fhir.rest.gclient.TokenClientParam
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.db.ResourceNotFoundException
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.Search
import com.google.android.fhir.search.SearchQuery
import java.util.LinkedList
import javax.inject.Inject
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.configuration.profile.ProfileConfiguration
import org.smartregister.fhircore.engine.configuration.register.RegisterConfiguration
import org.smartregister.fhircore.engine.configuration.view.CardViewProperties
import org.smartregister.fhircore.engine.configuration.view.ColumnProperties
import org.smartregister.fhircore.engine.configuration.view.ListProperties
import org.smartregister.fhircore.engine.configuration.view.ListResource
import org.smartregister.fhircore.engine.configuration.view.RowProperties
import org.smartregister.fhircore.engine.configuration.view.ViewProperties
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.domain.model.ActionParameter
import org.smartregister.fhircore.engine.domain.model.ActionParameterType
import org.smartregister.fhircore.engine.domain.model.DataQuery
import org.smartregister.fhircore.engine.domain.model.DataType
import org.smartregister.fhircore.engine.domain.model.ExtractedResource
import org.smartregister.fhircore.engine.domain.model.FhirResourceConfig
import org.smartregister.fhircore.engine.domain.model.RepositoryResourceData
import org.smartregister.fhircore.engine.domain.model.ResourceConfig
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.domain.model.SortConfig
import org.smartregister.fhircore.engine.domain.repository.Repository
import org.smartregister.fhircore.engine.performance.Timer
import org.smartregister.fhircore.engine.rulesengine.RulesFactory
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.extractId
import org.smartregister.fhircore.engine.util.extension.extractLogicalIdUuid
import org.smartregister.fhircore.engine.util.extension.filterBy
import org.smartregister.fhircore.engine.util.extension.filterByResourceTypeId
import org.smartregister.fhircore.engine.util.extension.resourceClassType
import org.smartregister.fhircore.engine.util.fhirpath.FhirPathDataExtractor
import org.smartregister.fhircore.engine.util.pmap
import timber.log.Timber

class RegisterRepository
@Inject
constructor(
  override val fhirEngine: FhirEngine,
  override val dispatcherProvider: DefaultDispatcherProvider,
  override val sharedPreferencesHelper: SharedPreferencesHelper,
  override val configurationRegistry: ConfigurationRegistry,
  override val configService: ConfigService,
  val fhirPathDataExtractor: FhirPathDataExtractor
) :
  Repository,
  DefaultRepository(
    fhirEngine = fhirEngine,
    dispatcherProvider = dispatcherProvider,
    sharedPreferencesHelper = sharedPreferencesHelper,
    configurationRegistry = configurationRegistry,
    configService = configService
  ) {

  override suspend fun loadRegisterData2(currentPage: Int, registerId: String): List<ResourceData> {

    val timer = Timer("currentPage $currentPage | registerID | $registerId", "loadRegisterData()")

    val resourceData = mutableListOf<ResourceData>()

    val searchQuery = SearchQuery("""
      SELECT * FROM RegisterFamilies ORDER BY lastUpdated DESC LIMIT 25 OFFSET ${currentPage.times(25)}
    """.trimIndent(),
      emptyList()
    )

    val cursor = withContext(dispatcherProvider.io()) {
      fhirEngine.getCursorAll(searchQuery)
    }

    while (cursor.moveToNext()) {
      /*
      "resourceUuid"	BLOB UNIQUE,
        	"resourceId"	TEXT,
        	"lastUpdated"	INTEGER,
        	"childCount" INTEGER,
        	"taskCount" INTEGER,
        	"taskStatus" TEXT,
        	"pregnantWomenCount" INTEGER,
        	"familyName" TEXT,
        	"householdNo" TEXT,
        	"householdLocation" TEXT,
        	PRIMARY KEY("resourceUuid")
       */

      val childrenCount = cursor.getInt("childCount")
      val pregnantWomenCount = cursor.getInt("pregnantWomenCount")
      val totalIcons = MutableList(childrenCount) {"CHILD"}
      totalIcons.addAll(MutableList(pregnantWomenCount) {"PREGNANT_WOMAN"})

      val computedValuesMap = mapOf<String, Any>(
        "familyName" to cursor.getString("familyName"),
        "familyId" to cursor.getString("householdNo"),
        "familyVillage" to cursor.getString("householdLocation"),
        "taskCount" to cursor.getInt("taskCount"),
        "serviceStatus" to cursor.getString("taskStatus"),
        "serviceMemberIcons" to totalIcons.joinToString(","),
      )
      val singleResourceData = ResourceData(
        cursor.getString("resourceId"),
        ResourceType.Group,
        computedValuesMap,
        emptyMap()
      )

      resourceData.add(singleResourceData)
    }

    timer.stop()
    return resourceData
  }

  fun Cursor.getString(fieldName: String) : String {
    val index = getColumnIndex(fieldName)
    return if (index != -1) {
      getString(index) ?: ""
    } else {
      ""
    }
  }


  fun Cursor.getInt(fieldName: String) : Int {
    val index = getColumnIndex(fieldName)
    return if (index != -1) {
      getInt(index)
    } else {
      0
    }
  }

  override suspend fun loadRegisterData(
                                        currentPage: Int,
                                        registerId: String,
                                        paramsMap: Map<String, String>?
  ): List<RepositoryResourceData> {
    val registerConfiguration = retrieveRegisterConfiguration(registerId, paramsMap)
    val baseResourceConfig = registerConfiguration.fhirResource.baseResource
    val relatedResourcesConfig = registerConfiguration.fhirResource.relatedResources
    val baseResourceClass = baseResourceConfig.resource.resourceClassType()
    val baseResourceType = baseResourceClass.newInstance().resourceType
    val secondaryResourceConfig = registerConfiguration.secondaryResources
    val secondaryResourceData = LinkedList<RepositoryResourceData>()

    // retrieve secondary ResourceData if secondaryResources are configured
    if (!secondaryResourceConfig.isNullOrEmpty()) {
      secondaryResourceData.addAll(retrieveSecondaryResources(secondaryResourceConfig))
    }

    val timer = Timer("currentPage $currentPage | registerID | $registerId", "loadRegisterData()")

    var timer2 = Timer(methodName = "searchResource()")
    val baseResources: List<Resource> =
      withContext(dispatcherProvider.io()) {
        searchResource(
          baseResourceClass = baseResourceClass,
          dataQueries = baseResourceConfig.dataQueries,
          sortConfigs = baseResourceConfig.sortConfigs,
          currentPage = currentPage,
          pageSize = registerConfiguration.pageSize
        )
      }

    timer2.stop()

    timer2 = Timer(methodName = "baseResources.forEach.searchRelatedResources()")

    // Retrieve data for each of the configured related resources
    // Also retrieve data for nested related resources for each of the related resource
    return baseResources.map { baseResource: Resource ->
      val currentRelatedResources = LinkedList<RepositoryResourceData>()

      relatedResourcesConfig.forEach { resourceConfig: ResourceConfig ->
        val relatedResources =
          withContext(dispatcherProvider.io()) {
            searchRelatedResources(
              resourceConfig = resourceConfig,
              baseResourceType = baseResourceType,
              baseResource = baseResource,
              fhirPathExpression = resourceConfig.fhirPathExpression
            )
          }
        currentRelatedResources.addAll(relatedResources)
      }


      // Include secondary resourceData in each row if secondaryResources are configured
      val repoData = RepositoryResourceData(
        configId = baseResourceConfig.id ?: baseResourceType.name,
        resource = baseResource,
        relatedResources = currentRelatedResources.apply { addAll(secondaryResourceData) }
      )

      timer2.stop()

      timer.stop()

      repoData
    }
  }

  private suspend fun searchRelatedResources(
    resourceConfig: ResourceConfig,
    baseResourceType: ResourceType,
    baseResource: Resource,
    fhirPathExpression: String?
  ): LinkedList<RepositoryResourceData> {

    val relatedResourceClass = resourceConfig.resource.resourceClassType()
    val relatedResourceType = relatedResourceClass.newInstance().resourceType
    val relatedResourcesData = LinkedList<RepositoryResourceData>()
    val startTime = System.currentTimeMillis()
    Timber.e(
      "Starting searchRelatedResources $baseResourceType | $resourceConfig | $fhirPathExpression"
    )

    if (fhirPathExpression.isNullOrEmpty()) {

      /*if (resourceConfig.sortConfigs.isEmpty() &&
          (resourceConfig.dataQueries == null || resourceConfig.dataQueries.isEmpty())
      ) {
        Timber.e("Running using the optimized query")
        val searchQuery = SearchQuery("""
            SELECT a.serializedResource
            FROM ResourceEntity a
            WHERE
            a.resourceUuid IN (
            SELECT resourceUuid FROM ReferenceIndexEntity
            WHERE resourceType = ? AND index_name = ? AND index_value = ?
            )
            """,
          listOf(baseResourceType.name, resourceConfig.searchParameter!!, baseResource.logicalId)
        )
        fhirEngine.search<Resource>(searchQuery).forEach { resource ->
          relatedResourcesData.addLast(
            RelatedResourceData(resource = resource, resourceConfigId = resourceConfig.id)
          )
        }
      } else {*/
      // TODO: Optimize this also
      Timber.e("Running using the non-optimized query")
      val relatedResourceSearch =
        Search(type = relatedResourceType).apply {
          filterByResourceTypeId(
            ReferenceClientParam(resourceConfig.searchParameter),
            baseResourceType,
            baseResource.logicalId
          )
          resourceConfig.dataQueries?.forEach { filterBy(it) }
          sort(resourceConfig.sortConfigs)
        }
      fhirEngine.search<Resource>(relatedResourceSearch).forEach { resource ->
        relatedResourcesData.addLast(
          RepositoryResourceData(
            configId = resourceConfig.id ?: resource.resourceType.name,
            resource = resource
          )
        )

        postProcessRelatedResourcesData(resourceConfig.relatedResources, relatedResourcesData)
      }
      //}
    } else {
      fhirPathDataExtractor
        .extractData(baseResource, fhirPathExpression)
        .takeWhile { it is Reference }
        .pmap {
          try {
            fhirEngine.get(
              resourceConfig.resource.resourceClassType().newInstance().resourceType,
              (it as Reference).extractId()
            )
          } catch (exception: ResourceNotFoundException) {
            Timber.e(exception)
            null
          }
        }
        .forEach { resource ->
          resource?.let {
            relatedResourcesData.addLast(
              RepositoryResourceData(
                configId = resourceConfig.id ?: resource.resourceType.name,
                resource = resource
              )
            )
          }
          postProcessRelatedResourcesData(resourceConfig.relatedResources, relatedResourcesData)
        }
    }

    val stopTime = System.currentTimeMillis()
    val timeTaken = stopTime - startTime
    Timber.e("Finished searchRelatedResources -> $timeTaken ms | ${timeTaken/1000} s  for $baseResourceType | $resourceConfig")

    return relatedResourcesData
  }

  private suspend fun postProcessRelatedResourcesData(
    relatedResources: List<ResourceConfig>,
    relatedResourcesData: LinkedList<RepositoryResourceData>
  ) {

    if (relatedResourcesData.size < 1) return

    relatedResources.forEach {
      val searchRelatedResources =
        searchRelatedResources(
          resourceConfig = it,
          baseResourceType = relatedResourcesData.last.resource.resourceType,
          baseResource = relatedResourcesData.last.resource,
          fhirPathExpression = it.fhirPathExpression
        )

      relatedResourcesData.last.relatedResources.addAll(searchRelatedResources)
    }
  }

  private suspend fun searchResource(
    baseResourceClass: Class<out Resource>,
    dataQueries: List<DataQuery>?,
    sortConfigs: List<SortConfig>,
    currentPage: Int? = null,
    pageSize: Int? = null
  ): List<Resource> {
    val resourceType = baseResourceClass.newInstance().resourceType
    val search =
      Search(type = resourceType).apply {
        dataQueries?.forEach { filterBy(it) }
        // For patient return only active members
        if (resourceType == ResourceType.Patient) {
          filter(TokenClientParam(ACTIVE), { value = of(true) })
        }
        sort(sortConfigs)
        if (currentPage != null && pageSize != null) {
          count = pageSize
          from = currentPage * pageSize
        }
      }
    return fhirEngine.search(search)
  }

  private fun Search.sort(sortConfigs: List<SortConfig>) {
    sortConfigs.forEach { sortConfig ->
      when (sortConfig.dataType) {
        DataType.INTEGER -> sort(NumberClientParam(sortConfig.paramName), sortConfig.order)
        DataType.DATE -> sort(DateClientParam(sortConfig.paramName), sortConfig.order)
        DataType.STRING -> sort(StringClientParam(sortConfig.paramName), sortConfig.order)
        else -> {
          /*Unsupported data type*/
        }
      }
    }
  }

  /** Count register data for the provided [registerId]. Use the configured base resource filters */
  override suspend fun countRegisterData(
    registerId: String,
    paramsMap: Map<String, String>?
  ): Long {
    val registerConfiguration = retrieveRegisterConfiguration(registerId, paramsMap)
    val baseResourceConfig = registerConfiguration.fhirResource.baseResource
    val baseResourceClass = baseResourceConfig.resource.resourceClassType()
    val resourceType = baseResourceClass.newInstance().resourceType

    val search =
      Search(resourceType).apply {
        baseResourceConfig.dataQueries?.forEach { filterBy(it) }
        // For patient return only active members count
        if (resourceType == ResourceType.Patient) {
          filter(TokenClientParam(ACTIVE), { value = of(true) })
        }
      }

    return fhirEngine.count(search)
  }

  override suspend fun loadProfileData(
    profileId: String,
    resourceId: String,
    fhirResourceConfig: FhirResourceConfig?,
    paramsList: Array<ActionParameter>?
  ): RepositoryResourceData {
    val paramsMap: Map<String, String> =
      paramsList
        ?.filter { it.paramType == ActionParameterType.PARAMDATA && !it.value.isNullOrEmpty() }
        ?.associate { it.key to it.value }
        ?: emptyMap()
    val profileConfiguration =
      configurationRegistry.retrieveConfiguration<ProfileConfiguration>(
        ConfigType.Profile,
        profileId,
        paramsMap
      )
    val resourceConfig = fhirResourceConfig ?: profileConfiguration.fhirResource
    val baseResourceConfig = resourceConfig.baseResource
    val relatedResourcesConfig = resourceConfig.relatedResources
    val baseResourceClass = baseResourceConfig.resource.resourceClassType()
    val baseResourceType = baseResourceClass.newInstance().resourceType
    val secondaryResourceConfig = profileConfiguration.secondaryResources

    val timer = Timer(methodName = "loadProfileData()", startString = "Args Profile Id = $profileId | resource id = $resourceId |  Fhir resource config = $fhirResourceConfig")

    val timer2 = Timer(methodName = "loadProfileData.fetchBaseResource $baseResourceType")
    val baseResource: Resource =
      withContext(dispatcherProvider.io()) {
        fhirEngine.get(baseResourceType, resourceId.extractLogicalIdUuid())
      }

    timer2.stop()
    val relatedResources = LinkedList<RepositoryResourceData>()

    relatedResourcesConfig.forEach { config: ResourceConfig ->
      val resources =
        withContext(dispatcherProvider.io()) {
          searchRelatedResources(
            resourceConfig = config,
            baseResourceType = baseResourceType,
            baseResource = baseResource,
            fhirPathExpression = config.fhirPathExpression
          )
        }
      relatedResources.addAll(resources)
    }

    if (!secondaryResourceConfig.isNullOrEmpty()) {
      relatedResources.addAll(retrieveSecondaryResources(secondaryResourceConfig))
    }

    val res = RepositoryResourceData(
      configId = baseResourceConfig.id ?: baseResourceType.name,
      resource = baseResource,
      relatedResources = relatedResources
    )

    timer.stop()
    return res
  }

  private suspend fun retrieveSecondaryResources(
    resourceConfigList: List<FhirResourceConfig>
  ): LinkedList<RepositoryResourceData> {
    val repositoryResourceData = LinkedList<RepositoryResourceData>()

    resourceConfigList.map { fhirResourceConfig: FhirResourceConfig ->
      val baseResources: List<Resource> =
        withContext(dispatcherProvider.io()) {
          searchResource(
            baseResourceClass = fhirResourceConfig.baseResource.resource.resourceClassType(),
            dataQueries = fhirResourceConfig.baseResource.dataQueries,
            sortConfigs = fhirResourceConfig.baseResource.sortConfigs
          )
        }

      baseResources.map { baseResource: Resource ->
        val baseRelatedResourceList = LinkedList<RepositoryResourceData>()
        fhirResourceConfig.relatedResources.forEach { resourceConfig: ResourceConfig ->
          val currentRelatedResources =
            withContext(dispatcherProvider.io()) {
              searchRelatedResources(
                resourceConfig = resourceConfig,
                baseResourceType = baseResource.resourceType,
                baseResource = baseResource,
                fhirPathExpression = resourceConfig.fhirPathExpression
              )
            }
          baseRelatedResourceList.addAll(currentRelatedResources)
        }
        repositoryResourceData.add(
          RepositoryResourceData(
            configId = fhirResourceConfig.baseResource.id,
            resource = baseResource,
            relatedResources = baseRelatedResourceList
          )
        )
      }
    }

    return repositoryResourceData
  }

  fun retrieveRegisterConfiguration(
    registerId: String,
    paramsMap: Map<String, String>?
  ): RegisterConfiguration =
    configurationRegistry.retrieveConfiguration(ConfigType.Register, registerId, paramsMap)

  companion object {
    const val ACTIVE = "active"
  }
}
