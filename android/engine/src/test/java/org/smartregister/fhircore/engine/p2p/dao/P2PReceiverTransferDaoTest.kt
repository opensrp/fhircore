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

import androidx.test.core.app.ApplicationProvider
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
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.geowidget.KujakuConversionInterface
import org.smartregister.p2p.sync.DataType

class P2PReceiverTransferDaoTest : RobolectricTest() {

  private val jsonParser: IParser = FhirContext.forCached(FhirVersionEnum.R4).newJsonParser()
  private lateinit var p2PReceiverTransferDao: P2PReceiverTransferDao
  private lateinit var configurationRegistry: ConfigurationRegistry
  private lateinit var fhirEngine: FhirEngine
  private val currentDate = Date()

  @Before
  fun setUp() {
    fhirEngine = mockk()
    configurationRegistry = Faker.buildTestConfigurationRegistry(mockk())
    p2PReceiverTransferDao =
      spyk(P2PReceiverTransferDao(fhirEngine, DefaultDispatcherProvider(), configurationRegistry))
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
    coEvery { p2PReceiverTransferDao.addOrUpdate(any()) } just runs
    p2PReceiverTransferDao.receiveJson(patientDataType, jsonArray)

    val resourceSlot = slot<Resource>()
    coVerify { p2PReceiverTransferDao.addOrUpdate(capture(resourceSlot)) }
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

  @Test
  fun testFeatureGeneration() {
    val locationJson =
      """{"resourceType":"Location","id":"136702","meta":{"versionId":"3","lastUpdated":"2022-07-28T18:21:39.739+00:00","source":"#18c074df71ca7366"},"status":"active","name":"Kenyatta Hospital Visitors Parking","description":"Parking Lobby","telecom":[{"system":"phone","value":"020 2726300"},{"system":"phone","value":"(+254)0709854000"},{"system":"phone","value":"(+254)0730643000"},{"system":"email","value":"knhadmin@knh.or.ke"}],"address":{"line":["P.O. Box 20723"],"city":"Nairobi","postalCode":"00202","country":"Kenya"},"physicalType":{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/location-physical-type","code":"area","display":"Area"}]},"position":{"longitude":36.80826008319855,"latitude":-1.301070677485388},"managingOrganization":{"reference":"Organization/400"},"partOf":{"reference":"Location/136710"}}"""
    val groupJson =
      """{"resourceType":"Group","id":"1122f50c-5499-4eaa-bd53-a5364371a2ba","meta":{"versionId":"5","lastUpdated":"2022-06-23T14:55:37.217+00:00","source":"#75f9db2107ef0977"},"identifier":[{"use":"official","value":"124"},{"use":"secondary","value":"c90cd5e3-a1c4-4040-9745-433aea9fe174"}],"active":true,"type":"person","code":{"coding":[{"system":"https://www.snomed.org","code":"35359004","display":"Family"}]},"name":"new family","managingEntity":{"reference":"Organization/105"},"member":[{"entity":{"reference":"Patient/7d84a2d0-8706-485a-85f5-8313f16bafa1"}},{"entity":{"reference":"Patient/0beaa1e3-64a9-436f-91af-36cbdaff5628"}},{"entity":{"reference":"Patient/a9e466a6-6237-46e0-bcda-c66036414aed"}},{"entity":{"reference":"Patient/7e62cc99-d992-484c-ace8-a43dba87ed22"}},{"entity":{"reference":"Patient/cd1c9616-bdfd-4947-907a-5f08e2bcd8a9"}}]}"""

    val jsonParser = FhirContext.forR4().newJsonParser()
    val location: Resource = jsonParser.parseResource(locationJson) as Resource
    val group: Resource = jsonParser.parseResource(groupJson) as Resource

    val resourceGroups = listOf<List<Resource>>(listOf(location, group))

    val conversionInterface = KujakuConversionInterface()

    val output =
      conversionInterface.generateFeatureCollection(
        ApplicationProvider.getApplicationContext(),
        resourceGroups
      )

    System.out.println("Output received")
  }
}
