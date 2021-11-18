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

import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
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
import org.junit.Test
import org.smartregister.fhircore.anc.data.family.FamilyDetailRepository
import org.smartregister.fhircore.anc.data.family.model.FamilyMemberItem
import org.smartregister.fhircore.anc.robolectric.RobolectricTest

class FamilyDetailViewModelTest : RobolectricTest() {

  private lateinit var viewModel: FamilyDetailViewModel
  private lateinit var repository: FamilyDetailRepository

  @Before
  fun setUp() {

    repository = mockk()
    viewModel = FamilyDetailViewModel(ApplicationProvider.getApplicationContext(), repository)
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

    every { repository.fetchDemographics() } returns MutableLiveData(patient)

    val demographic = viewModel.getDemographics().value
    Assert.assertEquals("john", demographic?.name?.first()?.given?.first()?.value)
    Assert.assertEquals("doe", demographic?.name?.first()?.family)
  }

  @Test
  fun testGetFamilyMembersShouldReturnDummyList() {

    val itemList =
      listOf(
        FamilyMemberItem("salina", "1", "20", "F", true, false),
        FamilyMemberItem("kevin", "2", "25", "F", false, false)
      )

    every { repository.fetchFamilyMembers() } returns MutableLiveData(itemList)

    val items = viewModel.getFamilyMembers().value

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

    every { repository.fetchEncounters() } returns MutableLiveData(listOf(encounter))

    val items = viewModel.getEncounters().value

    Assert.assertEquals(1, items?.size)
    Assert.assertEquals("first encounter", items?.get(0)?.class_?.display)
  }

  @Test
  fun testShouldVerifyAllClickListener() {

    var count = 0

    viewModel.setAppBackClickListener { ++count }
    viewModel.setMemberItemClickListener { ++count }
    viewModel.setAddMemberItemClickListener { ++count }
    viewModel.setSeeAllEncounterClickListener { ++count }
    viewModel.setEncounterItemClickListener { ++count }
    viewModel.setSeeAllUpcomingServiceClickListener { ++count }
    viewModel.setUpcomingServiceItemClickListener {
      ++count
      Assert.assertEquals(7, count)
    }

    viewModel.getAppBackClickListener().invoke()
    viewModel.getMemberItemClickListener().invoke(mockk())
    viewModel.getAddMemberItemClickListener().invoke()
    viewModel.getSeeAllEncounterClickListener().invoke()
    viewModel.getEncounterItemClickListener().invoke(mockk())
    viewModel.getSeeAllUpcomingServiceClickListener().invoke()
    viewModel.getUpcomingServiceItemClickListener().invoke(mockk())
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

    every { repository.fetchFamilyCarePlans() } returns MutableLiveData(listOf(carePlan))

    val items = viewModel.getFamilyCarePlans().value

    Assert.assertEquals(1, items?.size)
    Assert.assertEquals(cpTitle, carePlan.title)
    Assert.assertEquals(cpPeriodStartDate, carePlan.period.start)
  }
}
