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

package org.smartregister.fhircore.engine.data.local.register

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.Search
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.Group
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.app.fakes.Faker
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.domain.model.ActionParameter
import org.smartregister.fhircore.engine.domain.model.ActionParameterType
import org.smartregister.fhircore.engine.domain.model.DataType
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.rulesengine.RulesFactory
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.fhirpath.FhirPathDataExtractor

@HiltAndroidTest
class RegisterRepositoryTest : RobolectricTest() {

  @get:Rule(order = 0) var hiltRule = HiltAndroidRule(this)

  var context: Context = ApplicationProvider.getApplicationContext()

  private val fhirEngine: FhirEngine = mockk()

  @Inject lateinit var rulesFactory: RulesFactory

  private val configurationRegistry: ConfigurationRegistry = Faker.buildTestConfigurationRegistry()

  private lateinit var registerRepository: RegisterRepository

  private val fhirPathDataExtractor: FhirPathDataExtractor = mockk()

  private val patient = Faker.buildPatient("12345")

  @Before
  fun setUp() {
    hiltRule.inject()
    registerRepository =
      spyk(
        RegisterRepository(
          fhirEngine = fhirEngine,
          dispatcherProvider = DefaultDispatcherProvider(),
          sharedPreferencesHelper = mockk(),
          configurationRegistry = configurationRegistry,
          configService = mockk(),
          fhirPathDataExtractor = fhirPathDataExtractor
        )
      )
    coEvery { fhirEngine.search<Immunization>(Search(type = ResourceType.Immunization)) } returns
      listOf(Immunization())
  }

  @Test
  fun loadRegisterDataGivenRelatedResourceHasNoFhirPathExpression() {
    coEvery {
      fhirEngine.search<Patient>(Search(type = ResourceType.Patient, count = 10, from = 10))
    } returns listOf(patient)

    runBlocking {
      val listResourceData = registerRepository.loadRegisterData(1, "patientRegister")
      val resourceData = listResourceData.first()

      Assert.assertEquals(1, listResourceData.size)

      Assert.assertEquals(ResourceType.Patient, resourceData.resource.resourceType)
    }

    verify { registerRepository.retrieveRegisterConfiguration("patientRegister", emptyMap()) }

    coVerify {
      fhirEngine.search<Patient>(Search(type = ResourceType.Patient, count = 10, from = 10))
    }

    coVerify { fhirEngine.search<Immunization>(Search(type = ResourceType.Immunization)) }
  }

  @Test
  fun loadRegisterDataGivenRelatedResourceHasFhirPathExpression() {
    val group =
      Group().apply {
        id = "12345"
        name = "Snow"
        active = true
        addMember().apply { entity = Reference("Patient/${patient.logicalId}") }
      }

    coEvery {
      fhirEngine.search<Group>(Search(type = ResourceType.Group, count = 10, from = 10))
    } returns listOf(group)

    every { fhirPathDataExtractor.extractData(group, "Group.member.entity") } returns
      listOf(Reference("Patient/12345"))

    coEvery { fhirEngine.get(type = ResourceType.Patient, "12345") } returns patient

    coEvery { fhirEngine.search<Condition>(Search(type = ResourceType.Condition)) } returns
      listOf(Condition())

    coEvery { fhirEngine.search<CarePlan>(Search(type = ResourceType.CarePlan)) } returns
      listOf(CarePlan())

    runBlocking {
      configurationRegistry.loadConfigurations("app/debug", context) { Assert.assertTrue(it) }
      val listResourceData = registerRepository.loadRegisterData(1, "householdRegister")
      val resourceData = listResourceData.first()

      Assert.assertEquals(1, listResourceData.size)

      Assert.assertEquals(ResourceType.Group, resourceData.resource.resourceType)
    }

    verify { registerRepository.retrieveRegisterConfiguration("householdRegister", emptyMap()) }

    coVerify { fhirEngine.search<Group>(Search(type = ResourceType.Group, count = 10, from = 10)) }

    verify { fhirPathDataExtractor.extractData(group, "Group.member.entity") }

    coVerify { fhirEngine.get(type = ResourceType.Patient, "12345") }

    coVerify { fhirEngine.search<Condition>(Search(type = ResourceType.Condition)) }

    coVerify { fhirEngine.search<CarePlan>(Search(type = ResourceType.CarePlan)) }
  }

  @Test
  fun loadProfileDataIsNotSupportedYet() {
    coEvery { fhirEngine.get(ResourceType.Patient, patient.id) } returns patient
    runBlocking {
      val profileData =
        registerRepository.loadProfileData(profileId = "patientProfile", resourceId = "12345")
      Assert.assertNotNull(profileData)
      Assert.assertEquals(ResourceType.Patient, profileData.resource?.resourceType)
    }
  }

  @Test
  fun loadRegisterDataGivenSecondaryResourcesAreConfigured() {
    val group =
      Group().apply {
        id = "1234567"
        name = "Paracetamol"
        active = true
      }

    coEvery { fhirEngine.search<Group>(Search(type = ResourceType.Group)) } returns listOf(group)

    coEvery { fhirEngine.search<Observation>(Search(type = ResourceType.Observation)) } returns
      listOf(Observation())

    coEvery {
      fhirEngine.search<Patient>(Search(type = ResourceType.Patient, count = 10, from = 10))
    } returns listOf(patient)

    runBlocking { registerRepository.loadRegisterData(1, "patientRegisterSecondary") }

    coVerify { fhirEngine.search<Group>(Search(type = ResourceType.Group)) }

    coVerify { fhirEngine.search<Observation>(Search(type = ResourceType.Observation)) }
  }

  @Test
  fun loadProfileDataGivenSecondaryResourcesAreConfigured() {
    val group =
      Group().apply {
        id = "1234567"
        name = "Paracetamol"
        active = true
      }

    coEvery { fhirEngine.search<Group>(Search(type = ResourceType.Group)) } returns listOf(group)

    coEvery { fhirEngine.get(ResourceType.Patient, patient.id) } returns patient

    coEvery { fhirEngine.search<Observation>(Search(type = ResourceType.Observation)) } returns
      listOf(Observation())

    runBlocking {
      val profileData =
        registerRepository.loadProfileData(
          profileId = "patientProfileSecondary",
          resourceId = "12345",
          paramsMap = emptyMap()
        )
      Assert.assertNotNull(profileData)
    }

    coVerify { fhirEngine.search<Group>(Search(type = ResourceType.Group)) }

    coVerify { fhirEngine.search<Observation>(Search(type = ResourceType.Observation)) }
  }

  @Test
  fun countRegisterDataReturnsCorrectCount() {
    coEvery { fhirEngine.count(Search(type = ResourceType.Patient)) } returns 20

    runBlocking {
      val recordsCount = registerRepository.countRegisterData("patientRegister")

      Assert.assertEquals(20, recordsCount)
    }
  }

  @Test
  @kotlinx.coroutines.ExperimentalCoroutinesApi
  fun loadRegisterDataWithParamsReturnsFilteredResources() = runTest {
    val group =
      Group().apply {
        id = "1234567"
        name = "Paracetamol"
        active = true
      }
    val paramsList =
      arrayListOf(
        ActionParameter(
          key = "paramsName",
          paramType = ActionParameterType.PARAMDATA,
          value = "testing1",
          dataType = DataType.STRING,
          linkId = null
        ),
        ActionParameter(
          key = "paramName2",
          paramType = ActionParameterType.PARAMDATA,
          value = "testing2",
          dataType = DataType.INTEGER,
          linkId = null
        ),
        ActionParameter(
          key = "paramName3",
          paramType = ActionParameterType.PREPOPULATE,
          value = "testing3",
          dataType = DataType.INTEGER,
          linkId = null
        ),
      )
    val paramsMap =
      paramsList
        .filter { it.paramType == ActionParameterType.PARAMDATA && !it.value.isNullOrEmpty() }
        .associate { it.key to it.value }

    coEvery { fhirEngine.search<Group>(Search(type = ResourceType.Group)) } returns listOf(group)

    coEvery { fhirEngine.search<Observation>(Search(type = ResourceType.Observation)) } returns
      listOf(Observation())

    coEvery {
      fhirEngine.search<Patient>(Search(type = ResourceType.Patient, count = 10, from = 10))
    } returns listOf(patient)

    val result =
      registerRepository.loadRegisterData(1, "patientRegisterSecondary", paramsMap = paramsMap)

    coVerify { fhirEngine.search<Group>(Search(type = ResourceType.Group)) }

    coVerify { fhirEngine.search<Observation>(Search(type = ResourceType.Observation)) }
    Assert.assertNotNull(result.size)
  }

  @Test
  fun countRegisterDataWithParams() {
    val paramsList =
      arrayListOf(
        ActionParameter(
          key = "paramsName",
          paramType = ActionParameterType.PARAMDATA,
          value = "testing1",
          dataType = DataType.STRING,
          linkId = null
        ),
        ActionParameter(
          key = "paramName2",
          paramType = ActionParameterType.PARAMDATA,
          value = "testing2",
          dataType = DataType.INTEGER,
          linkId = null
        ),
      )
    paramsList
      .filter { it.paramType == ActionParameterType.PARAMDATA && !it.value.isNullOrEmpty() }
      .associate { it.key to it.value }
    val paramsMap = emptyMap<String, String>()
    coEvery { fhirEngine.count(Search(type = ResourceType.Patient)) } returns 20

    runBlocking {
      val recordsCount = registerRepository.countRegisterData("patientRegister", paramsMap)

      Assert.assertEquals(20, recordsCount)
    }
  }
}
