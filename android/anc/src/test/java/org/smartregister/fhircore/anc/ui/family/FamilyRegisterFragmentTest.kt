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

package org.smartregister.fhircore.anc.ui.family

import androidx.fragment.app.commitNow
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.smartregister.fhircore.anc.data.family.model.FamilyItem
import org.smartregister.fhircore.anc.robolectric.RobolectricTest
import org.smartregister.fhircore.anc.ui.family.register.FamilyRegisterActivity
import org.smartregister.fhircore.anc.ui.family.register.FamilyRegisterFragment
import org.smartregister.fhircore.engine.auth.AccountAuthenticator
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.ui.register.model.RegisterFilterType

@HiltAndroidTest
class FamilyRegisterFragmentTest : RobolectricTest() {

  @Inject lateinit var configurationRegistry: ConfigurationRegistry

  @Inject lateinit var accountAuthenticator: AccountAuthenticator

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  private val activityController = Robolectric.buildActivity(FamilyRegisterActivity::class.java)

  private lateinit var registerFragment: FamilyRegisterFragment

  @Before
  fun setUp() {
    hiltRule.inject()
    configurationRegistry.loadAppConfigurations(
      appId = "anc",
      accountAuthenticator = accountAuthenticator
    ) {}
    val registerActivity = activityController.create().resume().get()
    registerFragment = FamilyRegisterFragment()
    registerActivity.supportFragmentManager.commitNow {
      add(registerFragment, FamilyRegisterFragment.TAG)
    }
  }

  @After
  fun tearDown() {
    activityController.destroy()
  }

  @Test
  fun testPerformSearchFilterShouldReturnTrue() {
    val familyItem =
      FamilyItem(
        id = "fid",
        identifier = "1111",
        name = "Name ",
        gender = "M",
        age = "27",
        address = "Nairobi",
        isPregnant = true,
        members = emptyList(),
        servicesDue = 0,
        servicesOverdue = 0
      )

    val result =
      registerFragment.performFilter(RegisterFilterType.SEARCH_FILTER, familyItem, "1111")
    assertTrue(result)
  }

  @Test
  fun testPerformOverdueFilterShouldReturnTrue() {
    val familyItem =
      FamilyItem(
        id = "fid",
        identifier = "1111",
        name = "Name ",
        gender = "M",
        age = "27",
        address = "Nairobi",
        isPregnant = true,
        members = emptyList(),
        servicesDue = 4,
        servicesOverdue = 5
      )

    val result =
      registerFragment.performFilter(RegisterFilterType.OVERDUE_FILTER, familyItem, "1111")
    assertTrue(result)
  }
}
