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
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.smartregister.fhircore.anc.AncApplication
import org.smartregister.fhircore.anc.data.model.PatientItem
import org.smartregister.fhircore.anc.data.model.VisitStatus
import org.smartregister.fhircore.anc.robolectric.RobolectricTest
import org.smartregister.fhircore.anc.ui.details.PatientDetailsActivity
import org.smartregister.fhircore.anc.ui.family.register.FamilyRegisterActivity
import org.smartregister.fhircore.engine.auth.AccountAuthenticator
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.ui.register.model.RegisterFilterType

@HiltAndroidTest
class AncRegisterFragmentTest : RobolectricTest() {

  @Inject lateinit var configurationRegistry: ConfigurationRegistry

  @Inject lateinit var accountAuthenticator: AccountAuthenticator

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  private val activityController = Robolectric.buildActivity(FamilyRegisterActivity::class.java)

  private lateinit var registerFragment: AncRegisterFragment

  @Before
  fun setUp() {
    hiltRule.inject()
    configurationRegistry.loadAppConfigurations(
      appId = "anc",
      accountAuthenticator = accountAuthenticator
    ) {}
    val familyRegisterActivity = activityController.create().resume().get()
    familyRegisterActivity.supportFragmentManager.commitNow {
      registerFragment = AncRegisterFragment()
      add(registerFragment, AncRegisterFragment.TAG)
    }
  }

  @After
  fun cleanup() {
    activityController.destroy()
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
