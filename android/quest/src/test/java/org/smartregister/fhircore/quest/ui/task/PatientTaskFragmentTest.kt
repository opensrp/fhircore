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

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.fragment.app.commitNow
import com.google.android.fhir.sync.Sync
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Assert
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.view.RegisterViewConfiguration
import org.smartregister.fhircore.engine.ui.register.model.RegisterFilterType
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.data.task.PatientTaskRepository
import org.smartregister.fhircore.quest.data.task.model.PatientTaskItem
import org.smartregister.fhircore.quest.robolectric.RobolectricTest
import org.smartregister.fhircore.quest.ui.patient.register.PatientRegisterActivity
import org.smartregister.fhircore.quest.ui.task.components.ROW_PATIENT_TASK
import org.smartregister.fhircore.quest.ui.task.components.dummyPatientTaskPagingList

@HiltAndroidTest
class PatientTaskFragmentTest : RobolectricTest() {
  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule val composeRule = createComposeRule()

  @BindValue
  var configurationRegistry: ConfigurationRegistry =
    Faker.buildTestConfigurationRegistry("g6pd", mockk())

  private lateinit var patientTaskFragment: PatientTaskFragment

  @Before
  fun setUp() {
    mockkObject(Sync)

    hiltRule.inject()

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
  fun testConstructRegisterListShouldEnabled() {
    composeRule.setContent {
      patientTaskFragment.registerViewConfiguration =
        RegisterViewConfiguration(
          appId = "quest",
          classification = "classification",
          useLabel = true
        )
      patientTaskFragment.ConstructRegisterList(
        pagingItems = dummyPatientTaskPagingList(),
        modifier = Modifier
      )
    }

    composeRule.onAllNodesWithTag(ROW_PATIENT_TASK).assertAll(isEnabled())
  }

  @Test
  fun testPerformFilterShouldReturnFalseWithUnknownFilterType() {
    Assert.assertFalse(
      patientTaskFragment.performFilter(
        RegisterFilterType.SEARCH_FILTER,
        PatientTaskItem(name = "Samia"),
        ""
      )
    )
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
