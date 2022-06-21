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

import ca.uhn.fhir.rest.param.ParamPrefixEnum
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.db.ResourceNotFoundException
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.Search
import com.google.android.fhir.search.filter.DateParamFilterCriterion
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import java.util.Date
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Address
import org.hl7.fhir.r4.model.ContactPoint
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Group
import org.hl7.fhir.r4.model.HumanName
import org.hl7.fhir.r4.model.Meta
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.StringType
import org.joda.time.LocalDate
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.engine.app.fakes.Faker
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.p2p.sync.DataType

class BaseP2PTransferDaoTest : RobolectricTest() {

  private lateinit var baseP2PTransferDao: BaseP2PTransferDao
  lateinit var configurationRegistry: ConfigurationRegistry
  private lateinit var defaultRepository: DefaultRepository
  private lateinit var fhirEngine: FhirEngine
  private val currentDate = Date()

  @Before
  fun setUp() {
    fhirEngine = mockk(relaxed = true)
    defaultRepository = mockk()
    configurationRegistry = Faker.buildTestConfigurationRegistry(mockk())
    baseP2PTransferDao =
      spyk(P2PReceiverTransferDao(fhirEngine, DefaultDispatcherProvider(), configurationRegistry))
  }

  @Test
  fun `getDataTypes() returns correct list of datatypes`() {
    Faker.loadTestConfigurationRegistryData(defaultRepository, configurationRegistry)

    val actualDataTypes = baseP2PTransferDao.getDataTypes()
    Assert.assertEquals(6, actualDataTypes.size)
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
  fun `addOrUpdate() calls fhirEngine#update() when resource already exists`() {
    val expectedPatient = populateTestPatient()

    coEvery { fhirEngine.get(ResourceType.Patient, expectedPatient.logicalId) } returns
      expectedPatient
    runBlocking { baseP2PTransferDao.addOrUpdate(expectedPatient) }

    val resourceSlot = slot<Resource>()
    coVerify { fhirEngine.update(capture(resourceSlot)) }
    val actualPatient = resourceSlot.captured as Patient
    Assert.assertEquals(expectedPatient.logicalId, actualPatient.logicalId)
    Assert.assertEquals(expectedPatient.birthDate, actualPatient.birthDate)
    Assert.assertEquals(expectedPatient.gender, actualPatient.gender)
    Assert.assertEquals(expectedPatient.address[0].city, actualPatient.address[0].city)
    Assert.assertEquals(expectedPatient.address[0].country, actualPatient.address[0].country)
    Assert.assertEquals(expectedPatient.name[0].family, actualPatient.name[0].family)
    Assert.assertEquals(expectedPatient.meta.lastUpdated, actualPatient.meta.lastUpdated)
  }

  @Test
  fun `addOrUpdate() calls fhirEngine#create() when resource does not exist`() {
    val expectedPatient = populateTestPatient()
    val resourceNotFoundException = ResourceNotFoundException("", "")
    coEvery { fhirEngine.get(ResourceType.Patient, expectedPatient.logicalId) } throws
      resourceNotFoundException
    runBlocking { baseP2PTransferDao.addOrUpdate(expectedPatient) }

    val resourceSlot = slot<Resource>()
    coVerify { fhirEngine.create(capture(resourceSlot)) }
    val actualPatient = resourceSlot.captured as Patient
    Assert.assertEquals(expectedPatient.logicalId, actualPatient.logicalId)
    Assert.assertEquals(expectedPatient.birthDate, actualPatient.birthDate)
    Assert.assertEquals(expectedPatient.gender, actualPatient.gender)
    Assert.assertEquals(expectedPatient.address[0].city, actualPatient.address[0].city)
    Assert.assertEquals(expectedPatient.address[0].country, actualPatient.address[0].country)
    Assert.assertEquals(expectedPatient.name[0].family, actualPatient.name[0].family)
    Assert.assertEquals(expectedPatient.meta.lastUpdated, actualPatient.meta.lastUpdated)
  }

  @Test
  fun `loadResources() calls fhirEngine#search()`() {

    val patientDataType = DataType("Patient", DataType.Filetype.JSON, 1)
    val classType = baseP2PTransferDao.resourceClassType(patientDataType)
    runBlocking {
      baseP2PTransferDao.loadResources(
        lastRecordUpdatedAt = 0,
        batchSize = 25,
        classType = classType
      )
    }

    val searchSlot = slot<Search>()
    coVerify { fhirEngine.search<Patient>(capture(searchSlot)) }
    Assert.assertEquals(25, searchSlot.captured.count)
    Assert.assertEquals(ResourceType.Patient, searchSlot.captured.type)

    val dateTimeFilterCriterion: MutableList<Any> =
      ReflectionHelpers.getField(searchSlot.captured, "dateTimeFilterCriteria")
    val tokenFilters: MutableList<DateParamFilterCriterion> =
      ReflectionHelpers.getField(dateTimeFilterCriterion[0], "filters")
    Assert.assertEquals("_lastUpdated", tokenFilters[0].parameter.paramName)
    Assert.assertEquals(ParamPrefixEnum.GREATERTHAN, tokenFilters[0].prefix)
  }

  @Test
  fun `resourceClassType() returns correct resource class type for data type`() {
    Assert.assertEquals(
      Group::class.java,
      baseP2PTransferDao.resourceClassType(
        DataType(ResourceType.Group.name, DataType.Filetype.JSON, 0)
      )
    )
    Assert.assertEquals(
      Encounter::class.java,
      baseP2PTransferDao.resourceClassType(
        DataType(ResourceType.Encounter.name, DataType.Filetype.JSON, 0)
      )
    )
    Assert.assertEquals(
      Observation::class.java,
      baseP2PTransferDao.resourceClassType(
        DataType(ResourceType.Observation.name, DataType.Filetype.JSON, 0)
      )
    )
    Assert.assertEquals(
      Patient::class.java,
      baseP2PTransferDao.resourceClassType(
        DataType(ResourceType.Patient.name, DataType.Filetype.JSON, 0)
      )
    )
    Assert.assertEquals(
      Questionnaire::class.java,
      baseP2PTransferDao.resourceClassType(
        DataType(ResourceType.Questionnaire.name, DataType.Filetype.JSON, 0)
      )
    )
    Assert.assertEquals(
      QuestionnaireResponse::class.java,
      baseP2PTransferDao.resourceClassType(
        DataType(ResourceType.QuestionnaireResponse.name, DataType.Filetype.JSON, 0)
      )
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
