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
import io.mockk.spyk
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
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.app.fakes.Faker
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.rulesengine.RulesFactory
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.fhirpath.FhirPathDataExtractor

@HiltAndroidTest
class RegisterRepositoryTest : RobolectricTest() {

  @get:Rule(order = 0) var hiltRule = HiltAndroidRule(this)

  var context: Context = ApplicationProvider.getApplicationContext()

  private lateinit var fhirEngine: FhirEngine

  private lateinit var dispatcherProvider: DefaultDispatcherProvider

  @Inject lateinit var configurationRegistry: ConfigurationRegistry

  @Inject lateinit var rulesFactory: RulesFactory

  private lateinit var registerRepository: RegisterRepository

  private val patient = Faker.buildPatient("12345")

  private val fhirPathDataExtractor: FhirPathDataExtractor = mockk()

  @Before
  fun setUp() {
    hiltRule.inject()
    fhirEngine = mockk()
    dispatcherProvider = DefaultDispatcherProvider()
    registerRepository =
      spyk(
        RegisterRepository(
          fhirEngine = fhirEngine,
          dispatcherProvider = dispatcherProvider,
          configurationRegistry = configurationRegistry,
          rulesFactory = rulesFactory,
          fhirPathDataExtractor = fhirPathDataExtractor
        )
      )
    runBlocking {
      configurationRegistry.loadConfigurations("app/debug", context) { Assert.assertTrue(it) }
    }
    coEvery { fhirEngine.search<Immunization>(Search(type = ResourceType.Immunization)) } returns
      listOf(Immunization())
  }

  @Test
  fun loadRegisterDataGivenRelatedResourceHasNoFhirPathExpression() {
    coEvery {
      fhirEngine.search<Patient>(Search(type = ResourceType.Patient, count = 20, from = 20))
    } returns listOf(patient)

    runBlocking {
      val listResourceData = registerRepository.loadRegisterData(1, "patientRegister")
      val resourceData = listResourceData.first()

      Assert.assertEquals(1, listResourceData.size)
      Assert.assertEquals(ResourceType.Patient, resourceData.baseResource.resourceType)

      Assert.assertEquals(1, resourceData.relatedResourcesMap.values.first().size)
      Assert.assertEquals(
        ResourceType.Immunization,
        resourceData.relatedResourcesMap.values.first().first().resourceType
      )

      Assert.assertEquals("Nelson Mandela", resourceData.computedValuesMap["patientName"])
      Assert.assertEquals(
        Enumerations.AdministrativeGender.MALE.name.lowercase(),
        (resourceData.computedValuesMap["patientGender"] as String).lowercase()
      )
    }

    verify { registerRepository.retrieveRegisterConfiguration("patientRegister") }

    coVerify {
      fhirEngine.search<Patient>(Search(type = ResourceType.Patient, count = 20, from = 20))
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
      fhirEngine.search<Group>(Search(type = ResourceType.Group, count = 20, from = 20))
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
      Assert.assertTrue(profileData.computedValuesMap.containsKey(PATIENT_NAME))
      Assert.assertEquals("Nelson Mandela", profileData.computedValuesMap[PATIENT_NAME])
      Assert.assertTrue(profileData.computedValuesMap.containsKey(PATIENT_ID))
      Assert.assertEquals("12345", profileData.computedValuesMap[PATIENT_ID])
    }
  }

  @Test
  fun filterActiveGroupsReturnsOnlyActiveGroups() {
    val activeGroup =
      Group().apply {
        id = "12345"
        name = "Snow"
        active = true
        addMember().apply { entity = Reference("Patient/${patient.logicalId}") }
      }

    val inActiveGroup =
      Group().apply {
        id = "22222"
        name = "Mordor"
        active = false
        addMember().apply { entity = Reference("Patient/${patient.logicalId}") }
      }

    val search = Search(type = ResourceType.Group, count = 20, from = 20)
    coEvery { fhirEngine.search<Group>(search) } returns listOf(activeGroup, inActiveGroup)

    val actualGroups: List<Resource> = runBlocking {
      registerRepository.filterActiveGroups(search = search)
    }

    Assert.assertEquals(1, actualGroups.size)
    Assert.assertEquals("12345", actualGroups[0].id)
  }

  companion object {
    private const val PATIENT_NAME = "patientName"
    private const val PATIENT_ID = "patientId"
  }
}
