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

package org.smartregister.fhircore.anc.data.family

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.FhirEngine
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.DateType
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.StringType
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.anc.data.patient.PatientRepository
import org.smartregister.fhircore.anc.robolectric.RobolectricTest
import org.smartregister.fhircore.anc.sdk.ResourceMapperExtended
import org.smartregister.fhircore.anc.ui.family.register.FamilyItemMapper
import org.smartregister.fhircore.anc.util.RegisterConfiguration
import org.smartregister.fhircore.anc.util.SearchFilter
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.DispatcherProvider

@HiltAndroidTest
class FamilyRepositoryTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  private lateinit var repository: FamilyRepository

  private val fhirEngine: FhirEngine = spyk()

  private val ancPatientRepository: PatientRepository = mockk()

  @Inject lateinit var familyItemMapper: FamilyItemMapper

  @Inject lateinit var dispatcherProvider: DispatcherProvider

  @get:Rule var instantTaskExecutorRule = InstantTaskExecutorRule()

  @Before
  fun setUp() {
    hiltRule.inject()
    repository =
      FamilyRepository(
        context = ApplicationProvider.getApplicationContext(),
        fhirEngine = fhirEngine,
        domainMapper = familyItemMapper,
        dispatcherProvider = dispatcherProvider,
        ancPatientRepository = ancPatientRepository
      )
  }

  @Test
  fun testLoadAllShouldReturnListOfFamilyItem() {
    val patients =
      listOf(buildPatient("1111", "Family1", "Given1"), buildPatient("2222", "Family2", "Given2"))

    coEvery { ancPatientRepository.searchCarePlan(any()) } returns emptyList()
    coEvery { fhirEngine.search<Patient>(any()) } returns patients
    coEvery { fhirEngine.count(any()) } returns 10

    runBlocking {
      val families = repository.loadData("", 0, true)

      Assert.assertEquals("Given1 Family1", families[0].name)
      Assert.assertEquals("1111", families[0].id)

      Assert.assertEquals("Given2 Family2", families[1].name)
      Assert.assertEquals("2222", families[1].id)
    }
  }

  @Test
  fun postProcessFamilyMemberShouldExtractEntities() = runBlockingTest {
    val resourceMapperExtended = mockk<ResourceMapperExtended>()

    coEvery { resourceMapperExtended.saveParsedResource(any(), any(), any(), "1111") } just runs

    ReflectionHelpers.setField(repository, "resourceMapperExtended", resourceMapperExtended)

    repository.postProcessFamilyMember(Questionnaire(), QuestionnaireResponse(), "1111")

    coVerify { resourceMapperExtended.saveParsedResource(any(), any(), any(), "1111") }
  }

  @Test
  fun postProcessFamilyHeadShouldExtractEntities() = runBlockingTest {
    val resourceMapperExtended = mockk<ResourceMapperExtended>()

    coEvery { resourceMapperExtended.saveParsedResource(any(), any(), any(), null) } just runs

    ReflectionHelpers.setField(repository, "resourceMapperExtended", resourceMapperExtended)

    repository.postProcessFamilyHead(Questionnaire(), QuestionnaireResponse())

    coVerify { resourceMapperExtended.saveParsedResource(any(), any(), any(), null) }
  }

  @Test
  fun updateProcessFamilyMemberShouldCallSaveParsedResourceWithEditBooleanAsTrue() =
      runBlockingTest {
    val defaultRepository = spyk(DefaultRepository(fhirEngine, DefaultDispatcherProvider()))

    val resourceMapperExtended = spyk(ResourceMapperExtended(defaultRepository))

    coEvery { resourceMapperExtended.saveParsedResource(any(), any(), any(), "1111") } just runs

    ReflectionHelpers.setField(repository, "resourceMapperExtended", resourceMapperExtended)

    val questionnaire = Questionnaire()
    val questionnaireResponse = QuestionnaireResponse()

    repository.updateProcessFamilyMember(
      "1111",
      questionnaire = questionnaire,
      questionnaireResponse = questionnaireResponse,
      "1111"
    )

    coVerify {
      resourceMapperExtended.saveParsedResource(
        questionnaireResponse = questionnaireResponse,
        questionnaire = questionnaire,
        any(),
        "1111",
        eq(true)
      )
    }
  }

  @Test
  fun updateProcessFamilyHeadShouldCallSaveParsedResourceWithEditBooleanAsTrue() = runBlockingTest {
    val defaultRepository = spyk(DefaultRepository(fhirEngine, DefaultDispatcherProvider()))
    val resourceMapperExtended = spyk(ResourceMapperExtended(defaultRepository))

    coEvery { resourceMapperExtended.saveParsedResource(any(), any(), any(), null) } just runs

    ReflectionHelpers.setField(repository, "resourceMapperExtended", resourceMapperExtended)

    val questionnaire = Questionnaire()
    val questionnaireResponse = QuestionnaireResponse()

    repository.updateProcessFamilyHead(
      "1111",
      questionnaire = questionnaire,
      questionnaireResponse = questionnaireResponse
    )

    coVerify {
      resourceMapperExtended.saveParsedResource(
        questionnaireResponse = questionnaireResponse,
        questionnaire = questionnaire,
        any(),
        null,
        eq(true)
      )
    }
  }

  @Test
  fun postEnrollIntoAncShouldExtractEntitiesAndCallAncRepository() = runBlockingTest {
    val resourceMapperExtended = mockk<ResourceMapperExtended>()

    coEvery { ancPatientRepository.enrollIntoAnc("1111", any()) } just runs
    coEvery { resourceMapperExtended.saveParsedResource(any(), any(), "1111", null) } just runs

    ReflectionHelpers.setField(repository, "resourceMapperExtended", resourceMapperExtended)

    val questionnaireResponse =
      QuestionnaireResponse().apply {
        addItem().apply {
          linkId = "lmp"
          addAnswer().apply { this.value = DateType() }
        }
      }

    repository.enrollIntoAnc(Questionnaire(), questionnaireResponse, "1111")

    coVerify { resourceMapperExtended.saveParsedResource(any(), any(), "1111", null) }

    coVerify { ancPatientRepository.enrollIntoAnc("1111", any()) }
  }

  @Test
  fun repositoryShouldLoadCorrectRegisterConfig() = runBlockingTest {
    val expected =
      RegisterConfiguration(
        "family",
        SearchFilter(
          "_tag",
          Enumerations.SearchParamType.TOKEN,
          Enumerations.DataType.CODING,
          valueCoding =
            Coding().apply {
              code = "35359004"
              system = "https://www.snomed.org"
            }
        ),
        null
      )

    val actual = ReflectionHelpers.getField<RegisterConfiguration>(repository, "registerConfig")

    Assert.assertEquals(expected.id, actual.id)
    Assert.assertEquals(expected.primaryFilter?.filterType, actual.primaryFilter?.filterType)
    Assert.assertEquals(expected.primaryFilter?.valueType, actual.primaryFilter?.valueType)
  }

  @Test
  fun testCountAllShouldReturnMoreThanOnePatientCount() {
    coEvery { fhirEngine.count(any()) } returns 5
    val count = runBlocking { repository.countAll() }
    Assert.assertEquals(5, count)
  }

  private fun buildPatient(id: String, family: String, given: String): Patient {
    return Patient().apply {
      this.id = id
      this.addName().apply {
        this.family = family
        this.given.add(StringType(given))
      }
      this.addAddress().apply {
        district = "Dist 1"
        city = "City 1"
      }
    }
  }
}
