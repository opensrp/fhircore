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
import ca.uhn.fhir.parser.IParser
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.db.ResourceNotFoundException
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.SearchQuery
import java.util.TreeSet
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Group
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.extension.generateMissingId
import org.smartregister.fhircore.engine.util.extension.updateFrom
import org.smartregister.p2p.sync.DataType

open class BaseP2PTransferDao
constructor(
  open val fhirEngine: FhirEngine,
  open val dispatcherProvider: DispatcherProvider,
  val fhirContext: FhirContext
) {

  protected val jsonParser: IParser = fhirContext.newJsonParser()

  open fun getDataTypes(): TreeSet<DataType> =
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

  suspend fun <R : Resource> addOrUpdate(resource: R) {
    return withContext(dispatcherProvider.io()) {
      try {
        fhirEngine.load(resource::class.java, resource.logicalId).run {
          fhirEngine.update(updateFrom(resource))
        }
      } catch (resourceNotFoundException: ResourceNotFoundException) {
        resource.generateMissingId()
        fhirEngine.save(resource)
      }
    }
  }

  suspend fun loadResources(lastRecordUpdatedAt: Long, batchSize: Int): List<Patient>? {
    // TODO remove harcoded strings
    return withContext(dispatcherProvider.io()) {
      /*fhirEngine.search<Patient> {

        sort(DateClientParam("_lastUpdated"), Order.ASCENDING)
        filter(DateClientParam("_lastUpdated"), {
          value = of(DateTimeType(Date(lastRecordUpdatedAt)))
          prefix = ParamPrefixEnum.GREATERTHAN_OR_EQUALS
        })

        //sort(DateClientParam("_lastUpdated"), Order.ASCENDING)
        count = batchSize
      }*/

      val searchQuery =
        SearchQuery(
          """
      SELECT a.serializedResource, b.index_to
      FROM ResourceEntity a
      LEFT JOIN DateTimeIndexEntity b
      ON a.resourceType = b.resourceType AND a.resourceId = b.resourceId AND b.index_name = '_lastUpdated'
      WHERE a.resourceType = 'Patient'
      AND a.resourceId IN (
      SELECT resourceId FROM DateTimeIndexEntity
      WHERE resourceType = 'Patient' AND index_name = '_lastUpdated' AND index_to > ?
      )
      ORDER BY b.index_from ASC
      LIMIT ?
    """.trimIndent(),
          listOf(lastRecordUpdatedAt, batchSize)
        )

      fhirEngine.search(searchQuery)
    }
  }

  suspend fun loadResources(
    lastRecordUpdatedAt: Long,
    batchSize: Int,
    classType: Class<out Resource>
  ): List<Resource> {
    // TODO remove harcoded strings
    return withContext(dispatcherProvider.io()) {

      /*      val search = Search(type = classType.newInstance().resourceType)
      search.apply {
        filter(DateClientParam("_lastUpdated"), {
          value = of(DateTimeType(Date(lastRecordUpdatedAt)))
          prefix = ParamPrefixEnum.GREATERTHAN_OR_EQUALS})

        //sort(StringClientParam("_lastUpdated"), Order.ASCENDING)
        count = batchSize
      }
      fhirEngine.search(search)*/

      val searchQuery =
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

      fhirEngine.search(searchQuery)
    }
  }

  protected fun resourceClassType(type: DataType) =
    when (ResourceType.valueOf(type.name)) {
      ResourceType.Group -> Group::class.java
      ResourceType.Encounter -> Encounter::class.java
      ResourceType.Observation -> Observation::class.java
      ResourceType.Patient -> Patient::class.java
      ResourceType.Questionnaire -> Questionnaire::class.java
      ResourceType.QuestionnaireResponse -> QuestionnaireResponse::class.java
      else -> null /*TODO support other resource types*/
    }
}
