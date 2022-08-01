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

package org.smartregister.fhircore.engine.p2p.dao

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import ca.uhn.fhir.parser.IParser
import ca.uhn.fhir.rest.gclient.DateClientParam
import ca.uhn.fhir.rest.param.ParamPrefixEnum
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.db.ResourceNotFoundException
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.Search
import com.google.android.fhir.sync.SyncDataParams
import java.util.Date
import java.util.TreeSet
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.extension.generateMissingId
import org.smartregister.fhircore.engine.util.extension.isValidResourceType
import org.smartregister.fhircore.engine.util.extension.resourceClassType
import org.smartregister.fhircore.engine.util.extension.updateFrom
import org.smartregister.fhircore.engine.util.extension.updateLastUpdated
import org.smartregister.p2p.sync.DataType

open class BaseP2PTransferDao
constructor(
  open val fhirEngine: FhirEngine,
  open val dispatcherProvider: DispatcherProvider,
  open val configurationRegistry: ConfigurationRegistry
) {

  protected val jsonParser: IParser = FhirContext.forCached(FhirVersionEnum.R4).newJsonParser()

  open fun getDataTypes(): TreeSet<DataType> {
    val appRegistry =
      configurationRegistry.retrieveConfiguration<ApplicationConfiguration>(ConfigType.Application)
    val deviceToDeviceSyncConfigs = appRegistry.deviceToDeviceSync

    return if (deviceToDeviceSyncConfigs?.resourcesToSync != null &&
        deviceToDeviceSyncConfigs.resourcesToSync.isNotEmpty()
    ) {
      getDynamicDataTypes(deviceToDeviceSyncConfigs.resourcesToSync)
    } else {
      getDefaultDataTypes()
    }
  }

  open fun getDefaultDataTypes(): TreeSet<DataType> =
    TreeSet<DataType>(
      listOf(
        ResourceType.Group,
        ResourceType.Patient,
        ResourceType.Questionnaire,
        ResourceType.QuestionnaireResponse,
        ResourceType.Observation,
        ResourceType.Encounter
      )
        .mapIndexed { index, resourceType ->
          DataType(name = resourceType.name, DataType.Filetype.JSON, index)
        }
    )

  open fun getDynamicDataTypes(resourceList: List<String>): TreeSet<DataType> =
    TreeSet<DataType>(
      resourceList.filter { isValidResourceType(it) }.mapIndexed { index, resource ->
        DataType(name = resource, DataType.Filetype.JSON, index)
      }
    )

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

  suspend fun loadResources(
    lastRecordUpdatedAt: Long,
    batchSize: Int,
    classType: Class<out Resource>
  ): List<Resource> {
    return withContext(dispatcherProvider.io()) {
      // TODO FIX search order by _lastUpdated; SearchQuery no longer allowed in search API

      /*  val searchQuery =
        SearchQuery(
          """
      SELECT a.serializedResource, b.index_to
      FROM ResourceEntity a
      LEFT JOIN DateTimeIndexEntity b
      ON a.resourceType = b.resourceType AND a.resourceId = b.resourceId AND b.index_name = '_lastUpdated'
      WHERE a.resourceType = '${classType.newInstance().resourceType}'
      AND a.resourceId IN (
      SELECT resourceId FROM DateTimeIndexEntity
      WHERE resourceType = '${classType.newInstance().resourceType}' AND index_name = '_lastUpdated' AND index_to > ?
      )
      ORDER BY b.index_from ASC
      LIMIT ?
          """.trimIndent(),
          listOf(lastRecordUpdatedAt, batchSize)
        )

      fhirEngine.search(searchQuery)*/

      val search =
        Search(type = classType.newInstance().resourceType).apply {
          filter(
            DateClientParam(SyncDataParams.LAST_UPDATED_KEY),
            {
              value = of(DateTimeType(Date(lastRecordUpdatedAt)))
              prefix = ParamPrefixEnum.GREATERTHAN
            }
          )

          // sort(StringClientParam("_lastUpdated"), Order.ASCENDING)
          count = batchSize
        }
      fhirEngine.search(search)
    }
  }

  suspend fun countTotalRecordsForSync(highestRecordIdMap: HashMap<String, Long>): Long {
    var recordCount: Long = 0

    getDataTypes().forEach {
      it.name.resourceClassType().let { classType ->
        val lastRecordId = highestRecordIdMap[it.name] ?: 0L
        val searchCount = getSearchObjectForCount(lastRecordId, classType)
        recordCount += fhirEngine.count(searchCount)
      }
    }
    return recordCount
  }

  fun getSearchObjectForCount(lastRecordUpdatedAt: Long, classType: Class<out Resource>): Search {
    return Search(type = classType.newInstance().resourceType).apply {
      filter(
        DateClientParam(SyncDataParams.LAST_UPDATED_KEY),
        {
          value = of(DateTimeType(Date(lastRecordUpdatedAt)))
          prefix = ParamPrefixEnum.GREATERTHAN
        }
      )
    }
  }
}
