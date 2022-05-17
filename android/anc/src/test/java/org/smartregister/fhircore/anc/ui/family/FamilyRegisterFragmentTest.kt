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
import com.google.android.fhir.sync.Sync
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import java.util.Date
import javax.inject.Inject
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Patient
import org.junit.After
import org.junit.Assert
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows
import org.smartregister.fhircore.anc.app.fakes.Faker
import org.smartregister.fhircore.anc.data.family.FamilyRepository
import org.smartregister.fhircore.anc.data.family.model.FamilyItem
import org.smartregister.fhircore.anc.robolectric.RobolectricTest
import org.smartregister.fhircore.anc.ui.family.details.FamilyDetailsActivity
import org.smartregister.fhircore.anc.ui.family.register.Family
import org.smartregister.fhircore.anc.ui.family.register.FamilyItemMapper
import org.smartregister.fhircore.anc.ui.family.register.FamilyRegisterActivity
import org.smartregister.fhircore.anc.ui.family.register.FamilyRegisterFragment
import org.smartregister.fhircore.anc.ui.family.register.OpenFamilyProfile
import org.smartregister.fhircore.anc.util.AncJsonSpecificationProvider
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.ui.register.model.RegisterFilterType
import org.smartregister.fhircore.engine.util.SecureSharedPreference
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.plusYears

@HiltAndroidTest
class FamilyRegisterFragmentTest : RobolectricTest() {
  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @BindValue
  var configurationRegistry: ConfigurationRegistry =
    Faker.buildTestConfigurationRegistry("anc", mockk())
  @Inject lateinit var jsonSpecificationProvider: AncJsonSpecificationProvider

  @BindValue val sharedPreferencesHelper: SharedPreferencesHelper = mockk()
  @BindValue val secureSharedPreference: SecureSharedPreference = mockk()

  private lateinit var registerFragment: FamilyRegisterFragment

  @Before
  fun setUp() {
    mockkObject(Sync)

    hiltRule.inject()

    every { sharedPreferencesHelper.read(any(), any<String>()) } returns ""

    registerFragment = FamilyRegisterFragment()
    val registerActivity =
      Robolectric.buildActivity(FamilyRegisterActivity::class.java).create().get()
    registerActivity.supportFragmentManager.commitNow {
      add(registerFragment, FamilyRegisterFragment.TAG)
    }
  }

  @After
  fun cleanup() {
    unmockkObject(Sync)
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

    val familyItem = mapper.transformInputToOutputModel(Family(head, members, emptyList()))

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
          this.scheduledPeriod.start = Date().plusYears(-1)
          this.scheduledPeriod.end = Date().plusYears(-1)
          this.status = CarePlan.CarePlanActivityStatus.SCHEDULED
        }
      }

    val mapper = FamilyItemMapper(mockk())
    val members = listOf(mapper.toFamilyMemberItem(head, listOf(), listOf(careplan)))

    val familyItem =
      FamilyItemMapper(mockk()).transformInputToOutputModel(Family(head, members, listOf(careplan)))

    val result =
      registerFragment.performFilter(RegisterFilterType.OVERDUE_FILTER, familyItem, "1111")
    assertTrue(result)
  }

  @Test
  fun testNavigateToDetailsShouldOpenFamilyDetailsActivity() {

    registerFragment.onItemClicked(
      OpenFamilyProfile,
      FamilyItem("1", "", "", "", mockk(), listOf(), 0, 0)
    )

    val expectedIntent =
      Intent(registerFragment.requireActivity(), FamilyDetailsActivity::class.java)
    val actualIntent =
      Shadows.shadowOf(ApplicationProvider.getApplicationContext<Application>()).nextStartedActivity

    Assert.assertEquals(expectedIntent.component, actualIntent.component)
  }

  @Test
  @Ignore
  fun testInitializeRegisterDataViewModelShouldInitializeViewModel() {

    var registerDataViewModel = registerFragment.initializeRegisterDataViewModel()
    assertNotNull(registerDataViewModel)
    Assert.assertEquals(
      FamilyRepository::class.simpleName,
      registerDataViewModel.registerRepository::class.simpleName
    )
  }
}
