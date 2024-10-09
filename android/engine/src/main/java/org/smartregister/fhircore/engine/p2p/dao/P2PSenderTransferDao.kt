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

package org.smartregister.fhircore.engine.p2p.dao

import ca.uhn.fhir.context.FhirContext
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.datacapture.extensions.logicalId
import java.util.TreeSet
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.extension.resourceClassType
import org.smartregister.p2p.dao.SenderTransferDao
import org.smartregister.p2p.model.RecordCount
import org.smartregister.p2p.search.data.JsonData
import org.smartregister.p2p.sync.DataType
import timber.log.Timber

class P2PSenderTransferDao
@Inject
constructor(
  fhirEngine: FhirEngine,
  dispatcherProvider: DefaultDispatcherProvider,
  configurationRegistry: ConfigurationRegistry,
) : BaseP2PTransferDao(fhirEngine, dispatcherProvider, configurationRegistry), SenderTransferDao {

  override fun getP2PDataTypes(): TreeSet<DataType> = getDataTypes()

  override fun getTotalRecordCount(highestRecordIdMap: HashMap<String, Long>): RecordCount {
    return runBlocking { countTotalRecordsForSync(highestRecordIdMap) }
  }

  override fun getJsonData(
    dataType: DataType,
    lastUpdated: Long,
    batchSize: Int,
    offset: Int,
  ): JsonData? {
    // TODO: complete  retrieval of data implementation
    Timber.e("Last updated at value is $lastUpdated")
    var highestRecordId = lastUpdated

    val records = runBlocking {
      dataType.name.resourceClassType().let { classType ->
        loadResources(
          lastRecordUpdatedAt = highestRecordId,
          batchSize = batchSize,
          offset = offset,
          classType,
        )
      }
    }

    Timber.i("Fetching resources from base dao of type  $dataType.name")
    highestRecordId =
      (if (records.isNotEmpty()) {
        records.last().resource.meta?.lastUpdated?.time ?: highestRecordId
      } else {
        lastUpdated
      })

    val jsonArray = JSONArray()
    records.forEach {
      jsonArray.put(FhirContext.forR4Cached().newJsonParser().encodeResourceToString(it.resource))
      highestRecordId =
        if (it.resource.meta?.lastUpdated?.time!! > highestRecordId) {
          it.resource.meta?.lastUpdated?.time!!
        } else {
          highestRecordId
        }
      Timber.i(
        "Sending ${it.resource.resourceType} with id ====== ${it.resource.logicalId} and lastUpdated = ${it.resource.meta?.lastUpdated?.time!!}",
      )
    }

    Timber.e("New highest Last updated at value is $highestRecordId")
    return JsonData(jsonArray, highestRecordId)
  }
}
