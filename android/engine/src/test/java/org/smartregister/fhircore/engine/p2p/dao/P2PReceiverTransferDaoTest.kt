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
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.logicalId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.spyk
import java.util.Date
import org.hl7.fhir.r4.model.Address
import org.hl7.fhir.r4.model.ContactPoint
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.HumanName
import org.hl7.fhir.r4.model.Meta
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.StringType
import org.joda.time.LocalDate
import org.json.JSONArray
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.smartregister.fhircore.engine.app.fakes.Faker
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.p2p.sync.DataType

class P2PReceiverTransferDaoTest : RobolectricTest() {

  private val jsonParser: IParser = FhirContext.forCached(FhirVersionEnum.R4).newJsonParser()
  private lateinit var p2PReceiverTransferDao: P2PReceiverTransferDao
  private lateinit var configurationRegistry: ConfigurationRegistry
  private lateinit var defaultRepository: DefaultRepository
  private lateinit var fhirEngine: FhirEngine
  private val currentDate = Date()

  @Before
  fun setUp() {
    fhirEngine = mockk()
    configurationRegistry = Faker.buildTestConfigurationRegistry(mockk())
    defaultRepository = mockk()
    p2PReceiverTransferDao =
      spyk(
        P2PReceiverTransferDao(
          fhirEngine,
          DefaultDispatcherProvider(),
          configurationRegistry,
          defaultRepository
        )
      )
  }

  @Test
  fun `getP2PDataTypes() returns correct list of datatypes`() {
    val actualDataTypes = p2PReceiverTransferDao.getDataTypes()
    Assert.assertEquals(9, actualDataTypes.size)
    Assert.assertTrue(
      actualDataTypes.contains(DataType(ResourceType.Group.name, DataType.Filetype.JSON, 0))
    )
    Assert.assertTrue(
      actualDataTypes.contains(DataType(ResourceType.Patient.name, DataType.Filetype.JSON, 1))
    )
    Assert.assertTrue(
      actualDataTypes.contains(DataType(ResourceType.Questionnaire.name, DataType.Filetype.JSON, 2))
    )
    Assert.assertTrue(
      actualDataTypes.contains(
        DataType(ResourceType.QuestionnaireResponse.name, DataType.Filetype.JSON, 3)
      )
    )
    Assert.assertTrue(
      actualDataTypes.contains(DataType(ResourceType.Observation.name, DataType.Filetype.JSON, 4))
    )
    Assert.assertTrue(
      actualDataTypes.contains(DataType(ResourceType.Encounter.name, DataType.Filetype.JSON, 5))
    )
  }

  @Test
  fun `receiveJson() calls addOrUpdate() and returns correct maxLastUpdated`() {
    val expectedPatient = populateTestPatient()
    val jsonArray = populateTestJsonArray()
    val patientDataType = DataType(ResourceType.Patient.name, DataType.Filetype.JSON, 1)
    coEvery { defaultRepository.addOrUpdate(any()) } just runs
    p2PReceiverTransferDao.receiveJson(patientDataType, jsonArray)

    val resourceSlot = slot<Resource>()
    coVerify { defaultRepository.addOrUpdate(capture(resourceSlot)) }
    val actualPatient = resourceSlot.captured as Patient
    Assert.assertEquals(expectedPatient.logicalId, actualPatient.logicalId)
    Assert.assertEquals(expectedPatient.birthDate, actualPatient.birthDate)
    Assert.assertEquals(expectedPatient.gender, actualPatient.gender)
    Assert.assertEquals(expectedPatient.address[0].city, actualPatient.address[0].city)
    Assert.assertEquals(expectedPatient.address[0].country, actualPatient.address[0].country)
    Assert.assertEquals(expectedPatient.name[0].family, actualPatient.name[0].family)
    Assert.assertEquals(expectedPatient.meta.lastUpdated, actualPatient.meta.lastUpdated)
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

  private fun populateTestJsonArray(): JSONArray {
    val patient = populateTestPatient()
    val jsonArray = JSONArray()
    jsonArray.put(jsonParser.encodeResourceToString(patient))
    return jsonArray
  }
}
