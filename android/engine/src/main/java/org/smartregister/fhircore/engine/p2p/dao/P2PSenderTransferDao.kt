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

import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.p2p.dao.SenderTransferDao
import org.smartregister.p2p.search.data.JsonData
import org.smartregister.p2p.sync.DataType
import java.util.TreeSet
import javax.inject.Inject

class P2PSenderTransferDao
  @Inject
  constructor(
    val defaultRepository: DefaultRepository
        ) : BaseP2PTransferDao(), SenderTransferDao {

  override fun getP2PDataTypes(): TreeSet<DataType> {
    return TreeSet<DataType>()
  }

  override fun getJsonData(dataType: DataType, lastUpdated: Long, batchSize: Int): JsonData? {
    // TODO complete  retrieval of data implementation
    // Find a way to make this generic
    var jsonData:JsonData
    var jsonArray: JSONArray
    val records = runBlocking { defaultRepository.loadPatients(lastRecordUpdatedAt = lastUpdated, batchSize = batchSize) }
    return null
  }
}
