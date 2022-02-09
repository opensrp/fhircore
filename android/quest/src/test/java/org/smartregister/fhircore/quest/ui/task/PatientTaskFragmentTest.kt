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

package org.smartregister.fhircore.quest.ui.task

import androidx.fragment.app.commitNow
import com.google.android.fhir.sync.Sync
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkObject
import javax.inject.Inject
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Period
import org.hl7.fhir.r4.model.StringType
import org.hl7.fhir.r4.model.Task
import org.junit.After
import org.junit.Assert
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.smartregister.fhircore.engine.auth.AccountAuthenticator
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.ui.register.model.RegisterFilterType
import org.smartregister.fhircore.engine.util.DateUtils.getDate
import org.smartregister.fhircore.quest.data.task.PatientTaskRepository
import org.smartregister.fhircore.quest.robolectric.RobolectricTest
import org.smartregister.fhircore.quest.ui.patient.register.PatientRegisterActivity

@HiltAndroidTest
class PatientTaskFragmentTest : RobolectricTest() {
  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @Inject lateinit var configurationRegistry: ConfigurationRegistry

  private lateinit var patientTaskFragment: PatientTaskFragment

  @Before
  fun setUp() {
    mockkObject(Sync)

    val accountAuthenticator = mockk<AccountAuthenticator>()
    every { accountAuthenticator.launchLoginScreen() } just runs

    hiltRule.inject()

    configurationRegistry.loadAppConfigurations(
      appId = "quest",
      accountAuthenticator = accountAuthenticator
    ) {}
    patientTaskFragment = PatientTaskFragment()
    val patientRegisterActivity =
      Robolectric.buildActivity(PatientRegisterActivity::class.java).create().get()
    patientRegisterActivity.supportFragmentManager.commitNow {
      add(patientTaskFragment, PatientTaskFragment.TAG)
    }
  }

  @After
  fun cleanup() {
    unmockkObject(Sync)
  }

  @Test
  fun testPerformSearchFilterShouldReturnTrue() {
    val patientTask =
      PatientTask(
        patient =
          Patient().apply {
            id = "2"
            nameFirstRep.given = listOf(StringType("Name"))
            nameFirstRep.family = "Surname"
            gender = Enumerations.AdministrativeGender.MALE
            addressFirstRep.city = "Nairobi"
          },
        task =
          Task().apply {
            id = "1"
            description = "Sick Visit"
            executionPeriod =
              Period().apply {
                start = "2020-03-10".getDate("yyyy-MM-dd")
                end = "2020-03-12".getDate("yyyy-MM-dd")
              }
          }
      )
    val mapper = PatientTaskItemMapper(patientTaskFragment.requireContext())

    val patientTaskItem = mapper.mapToDomainModel(patientTask)

    val result =
      patientTaskFragment.performFilter(RegisterFilterType.SEARCH_FILTER, patientTaskItem, "1")
    assertTrue(result)
  }

  @Test
  fun testPerformOverdueFilterShouldReturnTrue() {
    val patientTask =
      PatientTask(
        patient =
          Patient().apply {
            id = "2"
            nameFirstRep.given = listOf(StringType("Name"))
            nameFirstRep.family = "Surname"
            gender = Enumerations.AdministrativeGender.MALE
            addressFirstRep.city = "Nairobi"
          },
        task =
          Task().apply {
            id = "1"
            description = "Sick Visit"
            executionPeriod =
              Period().apply {
                start = "2020-03-10".getDate("yyyy-MM-dd")
                end = "2020-03-12".getDate("yyyy-MM-dd")
              }
          }
      )
    val mapper = PatientTaskItemMapper(patientTaskFragment.requireContext())

    val patientTaskItem = mapper.mapToDomainModel(patientTask)

    val result =
      patientTaskFragment.performFilter(RegisterFilterType.OVERDUE_FILTER, patientTaskItem, "1")
    assertTrue(result)
  }

  @Test
  @Ignore
  fun testInitializeRegisterDataViewModelShouldInitializeViewModel() {
    val registerDataViewModel = patientTaskFragment.initializeRegisterDataViewModel()
    assertNotNull(registerDataViewModel)
    Assert.assertEquals(
      PatientTaskRepository::class.simpleName,
      registerDataViewModel.registerRepository::class.simpleName
    )
  }
}
