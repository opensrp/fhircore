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

import android.app.Application
import android.content.Intent
import androidx.fragment.app.commitNow
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.mockk
import java.util.Date
import javax.inject.Inject
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Patient
import org.junit.After
import org.junit.Assert
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.smartregister.fhircore.anc.robolectric.RobolectricTest
import org.smartregister.fhircore.anc.ui.family.register.Family
import org.smartregister.fhircore.anc.ui.family.register.FamilyItemMapper
import org.robolectric.Shadows
import org.smartregister.fhircore.anc.data.family.model.FamilyItem
import org.smartregister.fhircore.anc.ui.family.details.FamilyDetailsActivity
import org.smartregister.fhircore.anc.ui.family.register.FamilyRegisterActivity
import org.smartregister.fhircore.anc.ui.family.register.FamilyRegisterFragment
import org.smartregister.fhircore.anc.ui.family.register.OpenFamilyProfile
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
    val head =
      Patient().apply {
        id = "fid"
        identifierFirstRep.value = "1111"
        nameFirstRep.family = "Name"
        addressFirstRep.city = "Nairobi"
        meta.addTag().display = "family"
      }
    val mapper = FamilyItemMapper(registerFragment.requireContext())
    val members = listOf(mapper.toFamilyMemberItem(head, listOf(), listOf()))

    val familyItem = mapper.mapToDomainModel(Family(head, members, emptyList()))

    val result =
      registerFragment.performFilter(RegisterFilterType.SEARCH_FILTER, familyItem, "1111")
    assertTrue(result)
  }

  @Test
  fun testPerformOverdueFilterShouldReturnTrue() {
    val head =
      Patient().apply {
        id = "fid"
        identifierFirstRep.value = "1111"
        nameFirstRep.family = "Name"
        addressFirstRep.city = "Nairobi"
        meta.addTag().display = "family"
      }

    val careplan =
      CarePlan().apply {
        this.status = CarePlan.CarePlanStatus.ACTIVE
        this.activityFirstRep.detail.apply {
          this.scheduledPeriod.start = Date()
          this.scheduledPeriod.end = Date()
          this.status = CarePlan.CarePlanActivityStatus.SCHEDULED
        }
      }

    val mapper = FamilyItemMapper(mockk())
    val members = listOf(mapper.toFamilyMemberItem(head, listOf(), listOf(careplan)))

    val familyItem =
      FamilyItemMapper(mockk()).mapToDomainModel(Family(head, members, listOf(careplan)))

    val result =
      registerFragment.performFilter(RegisterFilterType.OVERDUE_FILTER, familyItem, "1111")
    assertTrue(result)
  }

  @Test
  fun testNavigateToDetailsShouldOpenFamilyDetailsActivity() {

    registerFragment.onItemClicked(
      OpenFamilyProfile,
      FamilyItem("1", "", "", "", "", "", false, listOf(), 0, 0)
    )

    val expectedIntent =
      Intent(registerFragment.requireActivity(), FamilyDetailsActivity::class.java)
    val actualIntent =
      Shadows.shadowOf(ApplicationProvider.getApplicationContext<Application>()).nextStartedActivity

    Assert.assertEquals(expectedIntent.component, actualIntent.component)
  }
}
