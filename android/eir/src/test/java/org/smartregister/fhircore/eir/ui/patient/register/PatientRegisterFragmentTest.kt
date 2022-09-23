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

package org.smartregister.fhircore.eir.ui.patient.register

import android.content.Intent
import androidx.fragment.app.commitNow
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.mockk
import org.junit.Assert
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.smartregister.fhircore.eir.EirApplication
import org.smartregister.fhircore.eir.data.model.PatientItem
import org.smartregister.fhircore.eir.data.model.PatientVaccineStatus
import org.smartregister.fhircore.eir.data.model.VaccineStatus
//import org.smartregister.fhircore.eir.fake.Faker
import org.smartregister.fhircore.eir.robolectric.RobolectricTest
import org.smartregister.fhircore.eir.shadow.FakeKeyStore
import org.smartregister.fhircore.eir.ui.patient.details.PatientDetailsActivity
import org.smartregister.fhircore.eir.ui.vaccine.RecordVaccineActivity
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.ui.register.model.RegisterFilterType
/*
@HiltAndroidTest
class PatientRegisterFragmentTest : RobolectricTest() {

  private lateinit var registerFragment: PatientRegisterFragment

  @get:Rule val hiltAndroidRule = HiltAndroidRule(this)

  @BindValue
  var configurationRegistry: ConfigurationRegistry =
    Faker.buildTestConfigurationRegistry("covax", mockk())
  @Before
  fun setUp() {
    hiltAndroidRule.inject()

    registerFragment = PatientRegisterFragment()
    val registerActivity =
      Robolectric.buildActivity(PatientRegisterActivity::class.java).create().resume().get()
    registerActivity.supportFragmentManager.commitNow { add(registerFragment, "") }
  }

  @Test
  fun testNavigateToDetailsShouldGotoToPatientDetailActivity() {
    registerFragment.navigateToDetails("")

    val expectedIntent = Intent(registerFragment.context, PatientDetailsActivity::class.java)
    val actualIntent =
      shadowOf(ApplicationProvider.getApplicationContext<EirApplication>()).nextStartedActivity

    Assert.assertEquals(expectedIntent.component, actualIntent.component)
  }

  @Test
  fun testNavigateToDetailsShouldGotoToRecordVaccineActivity() {

    val patientItem = PatientItem(patientIdentifier = "test_patient")
    registerFragment.onItemClicked(RecordPatientVaccine, patientItem)

    val expectedIntent = Intent(registerFragment.context, RecordVaccineActivity::class.java)
    val actualIntent =
      shadowOf(ApplicationProvider.getApplicationContext<EirApplication>()).nextStartedActivity

    Assert.assertEquals(expectedIntent.component, actualIntent.component)
  }

  @Test
  fun testPerformFilterShouldReturnTrue() {

    Assert.assertTrue(
      registerFragment.performFilter(RegisterFilterType.SEARCH_FILTER, PatientItem(), "")
    )
    Assert.assertTrue(
      registerFragment.performFilter(
        RegisterFilterType.SEARCH_FILTER,
        PatientItem(patientIdentifier = "12345"),
        "12345"
      )
    )
    Assert.assertTrue(
      registerFragment.performFilter(
        RegisterFilterType.OVERDUE_FILTER,
        PatientItem(vaccineStatus = PatientVaccineStatus(VaccineStatus.OVERDUE, "")),
        true
      )
    )
    Assert.assertTrue(
      registerFragment.performFilter(RegisterFilterType.OVERDUE_FILTER, PatientItem(), false)
    )
  }

  companion object {
    @JvmStatic
    @BeforeClass
    fun beforeClass() {
      FakeKeyStore.setup
    }
  }
}*/
