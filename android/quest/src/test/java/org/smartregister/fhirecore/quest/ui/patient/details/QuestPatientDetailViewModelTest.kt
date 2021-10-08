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

package org.smartregister.fhirecore.quest.ui.patient.details

import androidx.lifecycle.MutableLiveData
import io.mockk.every
import io.mockk.mockk
import org.hl7.fhir.r4.model.HumanName
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.StringType
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.annotation.Config
import org.smartregister.fhircore.quest.data.patient.PatientRepository
import org.smartregister.fhircore.quest.ui.patient.details.QuestPatientDetailActivity
import org.smartregister.fhircore.quest.ui.patient.details.QuestPatientDetailViewModel
import org.smartregister.fhirecore.quest.robolectric.RobolectricTest
import org.smartregister.fhirecore.quest.shadow.QuestApplicationShadow

@Config(shadows = [QuestApplicationShadow::class])
class QuestPatientDetailViewModelTest : RobolectricTest() {

  private lateinit var viewModel: QuestPatientDetailViewModel
  private lateinit var repository: PatientRepository

  @Before
  fun setUp() {
    repository = mockk()
    viewModel =
      QuestPatientDetailViewModel.get(
        Robolectric.buildActivity(QuestPatientDetailActivity::class.java).get(),
        mockk(),
        repository,
        ""
      )
  }

  @Test
  fun testVerifyBackPressListener() {
    var count = 0

    viewModel.setOnBackPressListener { ++count }

    viewModel.onBackPressListener().invoke()
    Assert.assertEquals(1, count)
  }

  @Test
  fun testVerifyMenuItemClickListener() {
    var menuItem = ""

    viewModel.setOnMenuItemClickListener { menuItem = it }

    viewModel.onMenuItemClickListener().invoke("ONE")
    Assert.assertEquals("ONE", menuItem)
  }

  @Test
  fun testGetDemographicsShouldReturnDummyPatient() {

    every { repository.fetchDemographics(any()) } returns
      MutableLiveData(
        Patient().apply {
          name =
            listOf(
              HumanName().apply {
                given = listOf(StringType("john"))
                family = "doe"
              }
            )
        }
      )

    val patient = viewModel.getDemographics().value

    Assert.assertEquals("john", patient?.name?.first()?.given?.first()?.value)
    Assert.assertEquals("doe", patient?.name?.first()?.family)
  }
}
