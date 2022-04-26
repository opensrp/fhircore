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
import io.mockk.mockk
import io.mockk.spyk
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Flag
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.StringType
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.anc.data.patient.PatientRepository
import org.smartregister.fhircore.anc.robolectric.RobolectricTest
import org.smartregister.fhircore.anc.ui.family.register.FamilyItemMapper
import org.smartregister.fhircore.anc.util.RegisterConfiguration
import org.smartregister.fhircore.anc.util.SearchFilter
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.extension.asReference
import org.smartregister.fhircore.engine.util.extension.makeItReadable

@HiltAndroidTest
class FamilyRepositoryTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
  @get:Rule(order = 1) var instantTaskExecutorRule = InstantTaskExecutorRule()

  private lateinit var repository: FamilyRepository

  private val fhirEngine: FhirEngine = spyk()

  private val ancPatientRepository: PatientRepository = mockk()

  @Inject lateinit var familyItemMapper: FamilyItemMapper

  @Inject lateinit var dispatcherProvider: DispatcherProvider

  @Before
  fun setUp() {
    hiltRule.inject()
    repository =
      FamilyRepository(
        context = ApplicationProvider.getApplicationContext(),
        fhirEngine = fhirEngine,
        dataMapper = familyItemMapper,
        dispatcherProvider = dispatcherProvider,
        ancPatientRepository = ancPatientRepository
      )
  }

  @Test
  fun testLoadAllShouldReturnListOfFamilyItem() {
    val patients =
      listOf(
        buildPatient("1111", "Family1", "Given1", true),
        buildPatient("2222", "Family2", "Given2")
      )

    coEvery { fhirEngine.search<Patient>(any()) } returns patients
    coEvery { ancPatientRepository.searchCarePlan(any(), any()) } returns emptyList()
    coEvery { ancPatientRepository.searchCondition(any()) } returns emptyList()
    coEvery { ancPatientRepository.searchPatientByLink(any()) } returns patients
    coEvery { fhirEngine.load(Patient::class.java, "1111") } returns patients[0]
    coEvery { fhirEngine.load(Patient::class.java, "2222") } returns patients[1]
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
  fun testChangeFamilyHeadShouldPerformNecessaryUpdates() {
    val familyTag = Coding().apply { display = "family" }
    val current =
      Patient().apply {
        id = "current"
        addressFirstRep.city = "Karachi"
        meta.addTag(familyTag)
        active = true
      }
    val next =
      Patient().apply {
        id = "next"
        addLink().other = current.asReference()
        active = true
      }

    val member =
      Patient().apply {
        id = "member"
        addLink().other = current.asReference()
      }
    val currentFlag = Flag()

    coEvery { fhirEngine.load(Patient::class.java, "current") } returns current
    coEvery { fhirEngine.load(Patient::class.java, "next") } returns next
    coEvery { fhirEngine.load(Patient::class.java, "member") } returns member
    coEvery { repository.ancPatientRepository.searchPatientByLink("current") } returns
      listOf(member)
    coEvery { repository.ancPatientRepository.searchCarePlan("current", familyTag) } returns
      listOf()
    coEvery { repository.ancPatientRepository.searchCarePlan(any(), null) } returns listOf()
    coEvery { repository.ancPatientRepository.searchCondition(any()) } returns listOf()
    coEvery { repository.ancPatientRepository.fetchActiveFlag("current", familyTag) } returns
      currentFlag

    runBlocking { repository.changeFamilyHead("current", "next") }

    coVerify { fhirEngine.save(current) }
    coVerify { fhirEngine.save(next) }
    coVerify { fhirEngine.save(currentFlag) }
    coVerify { repository.ancPatientRepository.searchPatientByLink("current") }
    coVerify { repository.ancPatientRepository.searchCarePlan("current", familyTag) }
    coVerify { repository.ancPatientRepository.fetchActiveFlag("current", familyTag) }

    Assert.assertEquals(0, current.meta.tag.size)
    Assert.assertEquals("family", next.meta.tagFirstRep.display)

    Assert.assertEquals("Patient/next", current.linkFirstRep.other.reference)
    Assert.assertEquals("Patient/next", member.linkFirstRep.other.reference)
    Assert.assertEquals(0, next.link.size)

    Assert.assertEquals("Karachi", next.addressFirstRep.city)

    Assert.assertEquals(Flag.FlagStatus.INACTIVE, currentFlag.status)
    Assert.assertEquals(Date().makeItReadable(), currentFlag.period.end.makeItReadable())
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

  private fun buildPatient(
    id: String,
    family: String,
    given: String,
    isHead: Boolean = false
  ): Patient {
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
      if (isHead) meta.addTag().display = "family"
    }
  }
}
