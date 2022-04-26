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

package org.smartregister.fhircore.anc.ui.anccare.register

import android.content.Intent
import androidx.fragment.app.commitNow
import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.sync.Sync
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import javax.inject.Inject
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.smartregister.fhircore.anc.AncApplication
import org.smartregister.fhircore.anc.app.fakes.Faker
import org.smartregister.fhircore.anc.data.model.PatientItem
import org.smartregister.fhircore.anc.data.model.VisitStatus
import org.smartregister.fhircore.anc.robolectric.RobolectricTest
import org.smartregister.fhircore.anc.ui.details.PatientDetailsActivity
import org.smartregister.fhircore.anc.ui.family.register.FamilyRegisterActivity
import org.smartregister.fhircore.anc.util.AncJsonSpecificationProvider
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.ui.register.model.RegisterFilterType
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper

@HiltAndroidTest
class AncRegisterFragmentTest : RobolectricTest() {
  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @BindValue
  var configurationRegistry: ConfigurationRegistry =
    Faker.buildTestConfigurationRegistry("anc", mockk())
  @Inject lateinit var jsonSpecificationProvider: AncJsonSpecificationProvider

  @BindValue val sharedPreferencesHelper: SharedPreferencesHelper = mockk()
  @BindValue val secureSharedPreference: SecureSharedPreference = mockk()

  private lateinit var registerFragment: AncRegisterFragment

  @Before
  fun setUp() {
    mockkObject(Sync)

    hiltRule.inject()

    every { sharedPreferencesHelper.read(any(), any<String>()) } returns ""
    registerFragment = AncRegisterFragment()

    val registerActivity =
      Robolectric.buildActivity(FamilyRegisterActivity::class.java).create().get()
    registerActivity.supportFragmentManager.commitNow {
      add(registerFragment, AncRegisterFragment.TAG)
    }
  }

  @After
  fun cleanup() {
    unmockkObject(Sync)
  }

  @Test
  fun testNavigateToDetailsShouldGotoToAncDetailsActivity() {
    val patientItem = PatientItem(patientIdentifier = "test_patient")
    registerFragment.onItemClicked(OpenPatientProfile, patientItem)

    val expectedIntent = Intent(registerFragment.context, PatientDetailsActivity::class.java)
    val actualIntent =
      shadowOf(ApplicationProvider.getApplicationContext<AncApplication>()).nextStartedActivity

    Assert.assertEquals(expectedIntent.component, actualIntent.component)
  }

  @Test
  fun testPerformFilterShouldReturnTrue() {

    Assert.assertTrue(
      registerFragment.performFilter(RegisterFilterType.SEARCH_FILTER, PatientItem(), "")
    )
    Assert.assertTrue(
      registerFragment.performFilter(
        registerFilterType = RegisterFilterType.SEARCH_FILTER,
        data = PatientItem(patientIdentifier = "12345"),
        value = "12345"
      )
    )
  }

  @Test
  fun testPerformOverdueFilterShouldReturnTrue() {
    val result =
      registerFragment.performFilter(
        registerFilterType = RegisterFilterType.OVERDUE_FILTER,
        data = PatientItem(patientIdentifier = "12345", visitStatus = VisitStatus.OVERDUE),
        value = "12345"
      )
    Assert.assertTrue(result)
  }
}
