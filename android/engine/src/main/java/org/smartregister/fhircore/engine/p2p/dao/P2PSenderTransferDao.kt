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
import java.util.TreeSet
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.json.JSONArray
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.p2p.dao.SenderTransferDao
import org.smartregister.p2p.search.data.JsonData
import org.smartregister.p2p.sync.DataType
import timber.log.Timber
import org.smartregister.fhircore.engine.util.extension.json

class P2PSenderTransferDao @Inject constructor(val defaultRepository: DefaultRepository) :
  BaseP2PTransferDao(), SenderTransferDao {

  override fun getP2PDataTypes(): TreeSet<DataType> {
    return getTypes()
  }

  override fun getJsonData(dataType: DataType, lastUpdated: Long, batchSize: Int): JsonData? {
    // TODO: complete  retrieval of data implementation
    // Find a way to make this generic
    Timber.e("Last updated at value is $lastUpdated")

    var highestRecordId = lastUpdated
    val records = runBlocking { defaultRepository.loadResources(lastRecordUpdatedAt = highestRecordId, batchSize = batchSize) }
    highestRecordId = if (records!!.isNotEmpty()) {
      records?.get(records.size - 1)?.meta?.lastUpdated?.time ?: highestRecordId
    } else {
      lastUpdated
    }

    var jsonArray = JSONArray()
    val jsonParser = FhirContext.forR4().newJsonParser()

    records?.forEach {
      jsonArray.put(jsonParser.encodeResourceToString(it))
      highestRecordId = if (it.meta?.lastUpdated?.time!! > highestRecordId) it.meta?.lastUpdated?.time!! else highestRecordId
    }

    Timber.e("New highest Last updated at value is $highestRecordId")
    return highestRecordId?.let { JsonData(jsonArray, it) }
  }

  fun genericGetJsonData(dataType: DataType, lastUpdated: Long, batchSize: Int) {
    val resourceTypes = ResourceType.values()

    val jsonParser = FhirContext.forR4().newJsonParser()
    resourceTypes.forEach {
      runBlocking {
        try {
          val RC = Class.forName("org.hl7.fhir.r4.model.${it}") as Class<out Resource>
          Timber.e("Fetch data for resource type ----> ${RC.name}")
          val records2 = defaultRepository.loadResources(lastUpdated, batchSize, RC)

          records2.forEachIndexed { index, resource ->
            Timber.e(
              "${index + 1}. ${resource.resourceType} -> ${jsonParser.encodeResourceToString(resource)}"
            )
          }
        } catch (ex: ClassNotFoundException) {
          Timber.e(ex)
        }
      }
    }
  }
}
