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

package org.smartregister.fhircore.anc.ui.family.details

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.coEvery
import io.mockk.mockk
import java.text.SimpleDateFormat
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.HumanName
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Period
import org.hl7.fhir.r4.model.StringType
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.anc.coroutine.CoroutineTestRule
import org.smartregister.fhircore.anc.data.family.FamilyDetailRepository
import org.smartregister.fhircore.anc.data.family.model.FamilyMemberItem
import org.smartregister.fhircore.anc.robolectric.RobolectricTest
import java.util.Date

class FamilyDetailViewModelTest : RobolectricTest() {

  @get:Rule(order = 1) val instantTaskExecutorRule = InstantTaskExecutorRule()

  @get:Rule(order = 2) val coroutineTestRule = CoroutineTestRule()

  private val familyDetailRepository: FamilyDetailRepository = mockk()

  private lateinit var familyDetailViewModel: FamilyDetailViewModel

  @Before
  fun setUp() {
    familyDetailViewModel = FamilyDetailViewModel(familyDetailRepository)
  }

  @Test
  fun testGetDemographicsShouldReturnTestPatient() {
    val patient =
      Patient().apply {
        name =
          listOf(
            HumanName().apply {
              given = listOf(StringType("john"))
              family = "doe"
            }
          )
      }

    coEvery { familyDetailRepository.fetchDemographics("1") } returns patient
    familyDetailViewModel.fetchDemographics("1")
    val demographic = familyDetailViewModel.demographics.value
    Assert.assertEquals("john", demographic?.name?.first()?.given?.first()?.value)
    Assert.assertEquals("doe", demographic?.name?.first()?.family)
  }

  @Test
  fun testGetFamilyMembersShouldReturnDummyList() {
    val itemList =
      listOf(
        FamilyMemberItem(
          name = "salina",
          id = "1",
          age = "20",
          gender = "F",
          pregnant = true,
          houseHoldHead = false,
          deathDate = Date(),
          servicesDue = 2,
          servicesOverdue = 3
        ),
        FamilyMemberItem(
          name = "kevin",
          id = "2",
          age = "25",
          gender = "F",
          pregnant = false,
          houseHoldHead = false,
          deathDate = null,
          servicesDue = 2,
          servicesOverdue = 3
        )
      )

    coEvery { familyDetailRepository.fetchFamilyMembers("1") } returns itemList
    familyDetailViewModel.fetchFamilyMembers("1")

    val items = familyDetailViewModel.familyMembers.value

    Assert.assertEquals(2, items?.count())

    (0..1).forEach {
      Assert.assertEquals(itemList[it].name, items?.get(it)?.name)
      Assert.assertEquals(itemList[it].id, items?.get(it)?.id)
      Assert.assertEquals(itemList[it].age, items?.get(it)?.age)
      Assert.assertEquals(itemList[it].gender, items?.get(it)?.gender)
      Assert.assertEquals(itemList[it].pregnant, items?.get(it)?.pregnant)
    }
  }

  @Test
  fun testGetEncountersShouldReturnSingleEncounterList() {
    val encounter = Encounter().apply { class_ = Coding("", "", "first encounter") }

    coEvery { familyDetailRepository.fetchEncounters("1") } returns listOf(encounter)
    familyDetailViewModel.fetchEncounters("1")

    val items = familyDetailViewModel.encounters.value

    Assert.assertEquals(1, items?.size)
    Assert.assertEquals("first encounter", items?.get(0)?.class_?.display)
  }

  @Test
  fun testGetFamilyCarePlansShouldReturnTestCarePlan() {

    val cpTitle = "First Care Plan"
    val cpPeriodStartDate = SimpleDateFormat("yyyy-MM-dd").parse("2021-01-01")
    val carePlan =
      CarePlan().apply {
        title = cpTitle
        period = Period().apply { start = cpPeriodStartDate }
      }

    coEvery { familyDetailRepository.fetchFamilyCarePlans("1") } returns listOf(carePlan)
    familyDetailViewModel.fetchCarePlans("1")

    val items = familyDetailViewModel.familyCarePlans.value

    Assert.assertEquals(1, items?.size)
    Assert.assertEquals(cpTitle, carePlan.title)
    Assert.assertEquals(cpPeriodStartDate, carePlan.period.start)
  }
}
