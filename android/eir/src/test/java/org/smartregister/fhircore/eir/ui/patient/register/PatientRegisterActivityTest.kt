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

import android.app.Activity
import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.sync.Sync
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.fakes.RoboMenuItem
import org.smartregister.fhircore.eir.R
import org.smartregister.fhircore.eir.activity.ActivityRobolectricTest
import org.smartregister.fhircore.eir.coroutine.CoroutineTestRule
import org.smartregister.fhircore.eir.fake.Faker
import org.smartregister.fhircore.eir.shadow.FakeKeyStore
import org.smartregister.fhircore.eir.ui.patient.details.PatientDetailsActivity
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.ui.register.model.SideMenuOption

@HiltAndroidTest
class PatientRegisterActivityTest : ActivityRobolectricTest() {

  @get:Rule(order = 0) val hiltAndroidRule = HiltAndroidRule(this)

  @get:Rule var coroutinesTestRule = CoroutineTestRule()

  private lateinit var patientRegisterActivity: PatientRegisterActivity

  @BindValue
  var configurationRegistry: ConfigurationRegistry =
    Faker.buildTestConfigurationRegistry("covax", mockk())
  @Before
  fun setUp() {
    mockkObject(Sync)

    hiltAndroidRule.inject()

    patientRegisterActivity =
      Robolectric.buildActivity(PatientRegisterActivity::class.java).create().get()
  }

  @After
  fun cleanup() {
    unmockkObject(Sync)
  }

  @Test
  fun testSideMenuOptionsShouldReturnCovaxMenuOptions() {
    val menu = patientRegisterActivity.sideMenuOptions()

    Assert.assertEquals(1, menu.size)
    with(menu.first()) {
      Assert.assertEquals(R.id.menu_item_covax, itemId)
      Assert.assertEquals(R.string.client_list_title_covax, titleResource)
      Assert.assertEquals(
        shadowOf(ContextCompat.getDrawable(patientRegisterActivity, R.drawable.ic_baby_mother))
          .createdFromResId,
        shadowOf(iconResource).createdFromResId
      )
    }
  }

  @Test
  fun testOnSideMenuOptionSelectedShouldReturnTrue() {
    Assert.assertTrue(patientRegisterActivity.onNavigationOptionItemSelected(RoboMenuItem()))
  }

  @Test
  fun testUpdateCountShouldSetRightValue() = runBlockingTest {
    val spy = spyk(patientRegisterActivity)
    every { spy.findSideMenuItem(any()) } returns
      RoboMenuItem().apply { itemId = R.id.menu_item_covax }

    val sideMenuOption =
      SideMenuOption(R.id.menu_item_covax, R.string.covax_app, mockk(), countMethod = { 123 })
    sideMenuOption.count = 0

    spy.updateCount(sideMenuOption)

    Assert.assertEquals(123, sideMenuOption.count)
  }

  @Test
  fun testSupportedFragmentsShouldReturnPatientRegisterFragmentList() {
    val fragments = patientRegisterActivity.supportedFragments()

    Assert.assertEquals(1, fragments.size)
    Assert.assertTrue(fragments.containsKey(PatientRegisterFragment.TAG))
  }

  @Test
  fun testMainFragmentTagShouldReturnPatientRegisterFragmentTag() {
    Assert.assertEquals(PatientRegisterFragment.TAG, patientRegisterActivity.mainFragmentTag())
  }

  @Test
  fun testOnBarcodeResultShouldNavigateToDetailScreenAndRegisterClientScreen() {
    val activity = spyk(patientRegisterActivity)
    val data = mockk<MutableLiveData<Result<Boolean>>>()
    val observer = slot<Observer<Result<Boolean>>>()

    every { activity.registerViewModel.patientExists(any()) } returns data
    every { data.observe(any(), capture(observer)) } returns Unit
    every { activity.navigateToDetails(any()) } returns Unit
    every { activity.registerClient(any()) } returns Unit

    activity.onBarcodeResult("12345", mockk())

    observer.captured.onChanged(Result.success(true))
    verify(exactly = 1) { activity.navigateToDetails("12345") }

    observer.captured.onChanged(Result.failure(mockk()))
    verify(exactly = 1) { activity.registerClient("12345") }
  }

  @Test
  fun testNavigateToDetailsShouldNavigateToPatientDetailActivity() {
    patientRegisterActivity.navigateToDetails("12345")

    val actualComponent = Intent(patientRegisterActivity, PatientDetailsActivity::class.java)
    val expectedIntent =
      shadowOf(ApplicationProvider.getApplicationContext<Application>()).nextStartedActivity

    Assert.assertEquals(actualComponent.component, expectedIntent.component)
  }

  override fun getActivity(): Activity {
    return patientRegisterActivity
  }

  companion object {
    @JvmStatic
    @BeforeClass
    fun beforeClass() {
      FakeKeyStore.setup
    }
  }
}
