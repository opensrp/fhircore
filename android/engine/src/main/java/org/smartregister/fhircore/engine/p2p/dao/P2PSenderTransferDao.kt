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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Patient
import org.json.JSONArray
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.p2p.dao.SenderTransferDao
import org.smartregister.p2p.search.data.JsonData
import org.smartregister.p2p.sync.DataType
import java.util.TreeSet
import org.smartregister.fhircore.engine.util.extension.json
import javax.inject.Inject

class P2PSenderTransferDao
  @Inject
  constructor(
    val defaultRepository: DefaultRepository
        ) : BaseP2PTransferDao(), SenderTransferDao {

  override fun getP2PDataTypes(): TreeSet<DataType> {
    return getTypes()
  }

  override fun getJsonData(dataType: DataType, lastUpdated: Long, batchSize: Int): JsonData? {
    // TODO complete  retrieval of data implementation
    // Find a way to make this generic
    val records = runBlocking { defaultRepository.loadResources(lastRecordUpdatedAt = lastUpdated, batchSize = batchSize) }
    val highestRecordId = records?.get(records.size - 1)?.meta?.lastUpdated?.time

    var jsonArray = JSONArray()
    val jsonParser = FhirContext.forR4().newJsonParser()

    records?.forEach {
      jsonArray.put(jsonParser.encodeResourceToString(it))
    }

    return highestRecordId?.let { JsonData(jsonArray, it) }
  }
}
