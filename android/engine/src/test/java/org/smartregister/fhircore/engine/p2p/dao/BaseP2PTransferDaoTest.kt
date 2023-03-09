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

package org.smartregister.fhircore.engine.p2p.dao

import com.google.android.fhir.FhirEngine
import com.google.android.fhir.search.SearchQuery
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import java.util.Date
import java.util.TreeSet
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.Address
import org.hl7.fhir.r4.model.ContactPoint
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Group
import org.hl7.fhir.r4.model.HumanName
import org.hl7.fhir.r4.model.ListResource
import org.hl7.fhir.r4.model.Meta
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.StringType
import org.joda.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.smartregister.fhircore.engine.app.fakes.Faker
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.extension.resourceClassType
import org.smartregister.p2p.sync.DataType

class BaseP2PTransferDaoTest : RobolectricTest() {

  private lateinit var baseP2PTransferDao: BaseP2PTransferDao

  private val configurationRegistry: ConfigurationRegistry = Faker.buildTestConfigurationRegistry()

  private val fhirEngine: FhirEngine = mockk(relaxed = true)

  private val currentDate = Date()

  @Before
  fun setUp() {
    baseP2PTransferDao =
      spyk(
        P2PReceiverTransferDao(
          fhirEngine,
          DefaultDispatcherProvider(),
          configurationRegistry,
          mockk()
        )
      )
  }

  @Test
  fun `getDataTypes() returns correct list of datatypes`() {

    val actualDataTypes = baseP2PTransferDao.getDataTypes()
    assertEquals(9, actualDataTypes.size)
    assertTrue(
      actualDataTypes.contains(DataType(ResourceType.Group.name, DataType.Filetype.JSON, 0))
    )
    assertTrue(
      actualDataTypes.contains(DataType(ResourceType.Patient.name, DataType.Filetype.JSON, 1))
    )
    assertTrue(
      actualDataTypes.contains(DataType(ResourceType.Questionnaire.name, DataType.Filetype.JSON, 2))
    )
    assertTrue(
      actualDataTypes.contains(
        DataType(ResourceType.QuestionnaireResponse.name, DataType.Filetype.JSON, 3)
      )
    )
    assertTrue(
      actualDataTypes.contains(DataType(ResourceType.Observation.name, DataType.Filetype.JSON, 4))
    )
    assertTrue(
      actualDataTypes.contains(DataType(ResourceType.Encounter.name, DataType.Filetype.JSON, 5))
    )
  }

  @Test
  fun `getDynamicDataTypes() returns correct list of datatypes`() {
    val resourceList =
      listOf(
        ResourceType.Group.name,
        ResourceType.Patient.name,
        ResourceType.Questionnaire.name,
        ResourceType.QuestionnaireResponse.name,
        ResourceType.Observation.name,
        ResourceType.Encounter.name
      )
    val actualDataTypes = baseP2PTransferDao.getDynamicDataTypes(resourceList)
    assertEquals(6, actualDataTypes.size)
    assertTrue(
      actualDataTypes.contains(DataType(ResourceType.Group.name, DataType.Filetype.JSON, 0))
    )
    assertTrue(
      actualDataTypes.contains(DataType(ResourceType.Patient.name, DataType.Filetype.JSON, 1))
    )
    assertTrue(
      actualDataTypes.contains(DataType(ResourceType.Questionnaire.name, DataType.Filetype.JSON, 2))
    )
    assertTrue(
      actualDataTypes.contains(
        DataType(ResourceType.QuestionnaireResponse.name, DataType.Filetype.JSON, 3)
      )
    )
    assertTrue(
      actualDataTypes.contains(DataType(ResourceType.Observation.name, DataType.Filetype.JSON, 4))
    )
    assertTrue(
      actualDataTypes.contains(DataType(ResourceType.Encounter.name, DataType.Filetype.JSON, 5))
    )
  }

  @Test
  fun `loadResources() calls fhirEngine#search()`() {
    val expectedQuery =
      "SELECT a.serializedResource\n" +
        "  FROM ResourceEntity a\n" +
        "  LEFT JOIN DateIndexEntity b\n" +
        "  ON a.resourceType = b.resourceType AND a.resourceUuid = b.resourceUuid \n" +
        "  LEFT JOIN DateTimeIndexEntity c\n" +
        "  ON a.resourceType = c.resourceType AND a.resourceUuid = c.resourceUuid\n" +
        "  WHERE a.resourceUuid IN (\n" +
        "  SELECT resourceUuid FROM DateTimeIndexEntity\n" +
        "  WHERE resourceType = 'Patient' AND index_name = '_lastUpdated' AND index_to >= ?\n" +
        "  )\n" +
        "  AND (b.index_name = '_lastUpdated' OR c.index_name = '_lastUpdated')\n" +
        "  ORDER BY c.index_from ASC, a.id ASC\n" +
        "  LIMIT ? OFFSET ?"

    val patientDataType = DataType("Patient", DataType.Filetype.JSON, 1)
    val classType = patientDataType.name.resourceClassType()
    runBlocking {
      baseP2PTransferDao.loadResources(
        lastRecordUpdatedAt = 0,
        batchSize = 25,
        offset = 0,
        classType = classType
      )
    }

    val searchQuerySlot = slot<SearchQuery>()
    coVerify { fhirEngine.search<Patient>(capture(searchQuerySlot)) }
    assertEquals(25, searchQuerySlot.captured.args[1])
    assertEquals(expectedQuery, searchQuerySlot.captured.query)
  }

  @Test
  fun `resourceClassType() returns correct resource class type for data type`() {
    assertEquals(
      Group::class.java,
      DataType(ResourceType.Group.name, DataType.Filetype.JSON, 0).name.resourceClassType()
    )
    assertEquals(
      Encounter::class.java,
      DataType(ResourceType.Encounter.name, DataType.Filetype.JSON, 0).name.resourceClassType()
    )
    assertEquals(
      Observation::class.java,
      DataType(ResourceType.Observation.name, DataType.Filetype.JSON, 0).name.resourceClassType()
    )
    assertEquals(
      Patient::class.java,
      DataType(ResourceType.Patient.name, DataType.Filetype.JSON, 0).name.resourceClassType()
    )
    assertEquals(
      Questionnaire::class.java,
      DataType(ResourceType.Questionnaire.name, DataType.Filetype.JSON, 0).name.resourceClassType()
    )
    assertEquals(
      QuestionnaireResponse::class.java,
      DataType(ResourceType.QuestionnaireResponse.name, DataType.Filetype.JSON, 0)
        .name
        .resourceClassType()
    )
    assertEquals(
      ListResource::class.java,
      DataType(ResourceType.List.name, DataType.Filetype.JSON, 0).name.resourceClassType()
    )
  }

  @Test
  fun `countTotalRecordsForSync() calls fhirEngine#count`() = runTest {
    every { baseP2PTransferDao.getDataTypes() } returns
      TreeSet<DataType>().apply {
        add(DataType(ResourceType.Patient.name, DataType.Filetype.JSON, 1))
      }

    coEvery { fhirEngine.count(any()) } returns 1

    assertEquals(1, baseP2PTransferDao.countTotalRecordsForSync(HashMap()))
  }

  @Test
  fun `getSearchObjectForCount() create search filter in fhirEngine`() {
    val search = baseP2PTransferDao.getSearchObjectForCount(1656663911, Patient::class.java)
    assertEquals("Patient", search.type.name)
  }

  @Test
  fun getDefaultDataTypesReturnsCorrectListOfDataTypes() {

    val actualDataTypes = baseP2PTransferDao.getDefaultDataTypes()
    assertEquals(6, actualDataTypes.size)
    assertTrue(
      actualDataTypes.contains(DataType(ResourceType.Group.name, DataType.Filetype.JSON, 0))
    )
    assertTrue(
      actualDataTypes.contains(DataType(ResourceType.Patient.name, DataType.Filetype.JSON, 1))
    )
    assertTrue(
      actualDataTypes.contains(DataType(ResourceType.Questionnaire.name, DataType.Filetype.JSON, 2))
    )
    assertTrue(
      actualDataTypes.contains(
        DataType(ResourceType.QuestionnaireResponse.name, DataType.Filetype.JSON, 3)
      )
    )
    assertTrue(
      actualDataTypes.contains(DataType(ResourceType.Observation.name, DataType.Filetype.JSON, 4))
    )
    assertTrue(
      actualDataTypes.contains(DataType(ResourceType.Encounter.name, DataType.Filetype.JSON, 5))
    )
  }

  private fun populateTestPatient(): Patient {
    val patientId = "patient-123456"
    val patient: Patient =
      Patient().apply {
        id = patientId
        active = true
        birthDate = LocalDate.parse("1999-10-03").toDate()
        gender = Enumerations.AdministrativeGender.MALE
        address =
          listOf(
            Address().apply {
              city = "Nairobi"
              country = "Kenya"
            }
          )
        name =
          listOf(
            HumanName().apply {
              given = mutableListOf(StringType("Kiptoo"))
              family = "Maina"
            }
          )
        telecom = listOf(ContactPoint().apply { value = "12345" })
        meta = Meta().apply { lastUpdated = currentDate }
      }
    return patient
  }
}
