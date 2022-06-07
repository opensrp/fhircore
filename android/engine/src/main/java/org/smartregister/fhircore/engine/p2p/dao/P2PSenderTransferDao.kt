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

import com.google.android.fhir.FhirEngine
import com.google.android.fhir.logicalId
import java.util.TreeSet
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.p2p.dao.SenderTransferDao
import org.smartregister.p2p.search.data.JsonData
import org.smartregister.p2p.sync.DataType
import timber.log.Timber

class P2PSenderTransferDao
@Inject
constructor(fhirEngine: FhirEngine, dispatcherProvider: DefaultDispatcherProvider) :
  BaseP2PTransferDao(fhirEngine, dispatcherProvider), SenderTransferDao {

  override fun getP2PDataTypes(): TreeSet<DataType> = getDataTypes()

  override fun getJsonData(dataType: DataType, lastUpdated: Long, batchSize: Int): JsonData? {
    // TODO: complete  retrieval of data implementation
    Timber.e("Last updated at value is $lastUpdated")
    var highestRecordId = lastUpdated

    val records =
      runBlocking {
        resourceClassType(dataType)?.let { classType ->
          loadResources(lastRecordUpdatedAt = highestRecordId, batchSize = batchSize, classType)
        }
      }
        ?: listOf()

    Timber.e("Fetching resources from base dao of type  $dataType.name")
    highestRecordId =
      (if (records.isNotEmpty()) records.last().meta?.lastUpdated?.time ?: highestRecordId
      else lastUpdated)

    val jsonArray = JSONArray()
    records.forEach {
      jsonArray.put(jsonParser.encodeResourceToString(it))
      highestRecordId =
        if (it.meta?.lastUpdated?.time!! > highestRecordId) it.meta?.lastUpdated?.time!!
        else highestRecordId
      Timber.e("Sending ${it.resourceType} with id ====== ${it.logicalId}")
    }

    Timber.e("New highest Last updated at value is $highestRecordId")
    return JsonData(jsonArray, highestRecordId)
  }
}
