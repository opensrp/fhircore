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
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.verify
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Group
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.rulesengine.RulesFactory
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.fhirpath.FhirPathDataExtractor

@HiltAndroidTest
class RegisterRepositoryTest : RobolectricTest() {

  @get:Rule var hiltRule = HiltAndroidRule(this)

  var context: Context = ApplicationProvider.getApplicationContext()

  private lateinit var fhirEngine: FhirEngine
  private lateinit var dispatcherProvider: DefaultDispatcherProvider

  @Inject lateinit var configurationRegistry: ConfigurationRegistry

  private lateinit var rulesFactory: RulesFactory
  private lateinit var registerRepository: RegisterRepository

  @Before
  fun setUp() {
    hiltRule.inject()

    fhirEngine = mockk()
    dispatcherProvider = DefaultDispatcherProvider()
    rulesFactory = mockk()
    registerRepository =
      spyk(
        RegisterRepository(
          fhirEngine = fhirEngine,
          dispatcherProvider = dispatcherProvider,
          configurationRegistry = configurationRegistry,
          rulesFactory = rulesFactory
        )
      )
  }

  @Test
  fun loadRegisterDataGivenRelatedResourceHasNoFhirPathExpression() {
    val patient =
      Patient().apply {
        id = "12345"
        addName().apply {
          addGiven("Jon")
          family = "Snow"
        }
        gender = Enumerations.AdministrativeGender.MALE
      }
    coEvery {
      fhirEngine.search<Patient>(Search(type = ResourceType.Patient, count = 20, from = 20))
    } returns listOf(patient)

    val immunization = Immunization()
    coEvery { fhirEngine.search<Immunization>(Search(type = ResourceType.Immunization)) } returns
      listOf(immunization)

    every {
      rulesFactory.fireRule(ruleConfigs = any(), baseResource = any(), relatedResourcesMap = any())
    } returns
      mapOf(
        Pair("patientName", "Jon Snow"),
        Pair("patientGender", Enumerations.AdministrativeGender.MALE)
      )

    runBlocking {
      configurationRegistry.loadConfigurations("app/debug", context) { Assert.assertTrue(it) }
      val listResourceData = registerRepository.loadRegisterData(1, "patientRegister")
      val resourceData = listResourceData.first()

      Assert.assertEquals(1, listResourceData.size)
      Assert.assertEquals(ResourceType.Patient, resourceData.baseResource.resourceType)

      Assert.assertEquals(1, resourceData.relatedResourcesMap.values.first().size)
      Assert.assertEquals(
        ResourceType.Immunization,
        resourceData.relatedResourcesMap.values.first().first().resourceType
      )

      Assert.assertEquals("Jon Snow", resourceData.computedValuesMap["patientName"])
      Assert.assertEquals(
        Enumerations.AdministrativeGender.MALE,
        resourceData.computedValuesMap["patientGender"]
      )
    }

    verify { registerRepository.retrieveRegisterConfiguration("patientRegister") }

    coVerify {
      fhirEngine.search<Patient>(Search(type = ResourceType.Patient, count = 20, from = 20))
    }

    coVerify { fhirEngine.search<Immunization>(Search(type = ResourceType.Immunization)) }

    verify {
      rulesFactory.fireRule(ruleConfigs = any(), baseResource = any(), relatedResourcesMap = any())
    }
  }

  @Test
  fun loadRegisterDataGivenRelatedResourceHasFhirPathExpression() {
    val patient =
      Patient().apply {
        id = "12345"
        addName().apply {
          addGiven("Jon")
          family = "Snow"
        }
        gender = Enumerations.AdministrativeGender.MALE
      }

    val group =
      Group().apply {
        id = "12345"
        name = "Snow"
        addMember().apply { entity = Reference("Patient/${patient.logicalId}") }
      }

    coEvery {
      fhirEngine.search<Group>(Search(type = ResourceType.Group, count = 20, from = 20))
    } returns listOf(group)

    mockkObject(FhirPathDataExtractor)

    every { FhirPathDataExtractor.extractData(group, "Group.member.entity") } returns
      listOf(Reference("Patient/12345"))

    coEvery { fhirEngine.get(type = ResourceType.Patient, "12345") } returns patient

    coEvery { fhirEngine.search<Condition>(Search(type = ResourceType.Condition)) } returns
      listOf(Condition())

    coEvery { fhirEngine.search<CarePlan>(Search(type = ResourceType.CarePlan)) } returns
      listOf(CarePlan())

    every {
      rulesFactory.fireRule(ruleConfigs = any(), baseResource = any(), relatedResourcesMap = any())
    } returns mapOf(Pair("familyName", "Snow"))

    runBlocking {
      configurationRegistry.loadConfigurations("app/debug", context) { Assert.assertTrue(it) }
      val listResourceData = registerRepository.loadRegisterData(1, "householdRegister")
      val resourceData = listResourceData.first()

      Assert.assertEquals(1, listResourceData.size)
      Assert.assertEquals(ResourceType.Group, resourceData.baseResource.resourceType)

      Assert.assertEquals(1, resourceData.relatedResourcesMap.values.first().size)
      Assert.assertEquals(
        ResourceType.Patient,
        resourceData.relatedResourcesMap.values.first().first().resourceType
      )

      Assert.assertEquals("Snow", resourceData.computedValuesMap["familyName"])
    }

    verify { registerRepository.retrieveRegisterConfiguration("householdRegister") }

    coVerify { fhirEngine.search<Group>(Search(type = ResourceType.Group, count = 20, from = 20)) }

    verify { FhirPathDataExtractor.extractData(group, "Group.member.entity") }

    coVerify { fhirEngine.get(type = ResourceType.Patient, "12345") }

    coVerify { fhirEngine.search<Condition>(Search(type = ResourceType.Condition)) }

    coVerify { fhirEngine.search<CarePlan>(Search(type = ResourceType.CarePlan)) }

    verify {
      rulesFactory.fireRule(ruleConfigs = any(), baseResource = any(), relatedResourcesMap = any())
    }

    unmockkObject(FhirPathDataExtractor)
  }

  @Test
  fun loadProfileDataIsNotSupportedYet() {
    runBlocking {
      Assert.assertNotNull(
        registerRepository.loadProfileData(profileId = "12345", resourceId = "485738")
      )
    }
  }
}
