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

import java.util.TreeSet
import org.smartregister.fhircore.engine.p2p.dao.util.P2PConstants
import org.smartregister.p2p.sync.DataType

open class BaseP2PTransferDao {

  val patient = DataType(name = P2PConstants.P2PDataTypes.PATIENT, DataType.Filetype.JSON, 0)
  val questionnaire =
    DataType(name = P2PConstants.P2PDataTypes.QUESTIONNAIRE, DataType.Filetype.JSON, 1)
  val questionnaireResponse =
    DataType(name = P2PConstants.P2PDataTypes.QUESTIONNAIRE_RESPONSE, DataType.Filetype.JSON, 2)
  val observation =
    DataType(name = P2PConstants.P2PDataTypes.OBSERVATION, DataType.Filetype.JSON, 3)
  val encounter = DataType(name = P2PConstants.P2PDataTypes.ENCOUNTER, DataType.Filetype.JSON, 4)
  var dataTypes: TreeSet<DataType>? = null

  fun BaseP2PTransferDao() {
    dataTypes = TreeSet<DataType>()
    dataTypes!!.add(patient)
    dataTypes!!.add(questionnaire)
    dataTypes!!.add(questionnaireResponse)
    dataTypes!!.add(observation)
    dataTypes!!.add(encounter)
  }
}
