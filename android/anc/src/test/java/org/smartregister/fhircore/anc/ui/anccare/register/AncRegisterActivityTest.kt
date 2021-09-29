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

import android.app.Activity
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.sync.Sync
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import java.time.OffsetDateTime
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.fakes.RoboMenuItem
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.anc.AncApplication
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.activity.ActivityRobolectricTest
import org.smartregister.fhircore.anc.data.anc.AncPatientRepository
import org.smartregister.fhircore.anc.data.family.FamilyRepository
import org.smartregister.fhircore.anc.shadow.AncApplicationShadow
import org.smartregister.fhircore.anc.shadow.FakeKeyStore
import org.smartregister.fhircore.anc.ui.family.register.FamilyRegisterActivity

@Config(shadows = [AncApplicationShadow::class])
internal class AncRegisterActivityTest : ActivityRobolectricTest() {

  private lateinit var ancRegisterActivity: AncRegisterActivity
  private lateinit var ancPatientRepository: AncPatientRepository
  private lateinit var familyRepository: FamilyRepository

  @Before
  fun setUp() {
    mockkObject(Sync)
    every { Sync.basicSyncJob(any()).stateFlow() } returns flowOf()
    every { Sync.basicSyncJob(any()).lastSyncTimestamp() } returns OffsetDateTime.now()

    ancPatientRepository = mockk()
    familyRepository = mockk()

    ancRegisterActivity =
      Robolectric.buildActivity(AncRegisterActivity::class.java, null).create().get()

    ReflectionHelpers.setField(ancRegisterActivity, "ancPatientRepository", ancPatientRepository)
    ReflectionHelpers.setField(ancRegisterActivity, "familyRepository", familyRepository)
  }

  @After
  fun cleanup() {
    unmockkObject(Sync)
  }

  @Test
  fun testActivityShouldNotNull() {
    assertNotNull(ancRegisterActivity)
  }

  @Test
  fun testActivityHasCorrectSideMenuItem() {

    coEvery { ancPatientRepository.countAll() } returns 1
    coEvery { familyRepository.countAll() } returns 1

    val sideMenu = ancRegisterActivity.sideMenuOptions()

    // verify anc menu
    assertEquals(R.id.menu_item_anc, sideMenu[0].itemId)
    assertEquals(R.string.anc_register_title, sideMenu[0].titleResource)
    assertEquals(
      shadowOf(ContextCompat.getDrawable(ancRegisterActivity, R.drawable.ic_baby_mother))
        .createdFromResId,
      shadowOf(sideMenu[0].iconResource).createdFromResId
    )
    assertTrue(sideMenu[0].opensMainRegister)
    assertEquals(1, sideMenu[0].countMethod.invoke())

    // verify family menu
    assertEquals(R.id.menu_item_family, sideMenu[1].itemId)
    assertEquals(R.string.family_register_title, sideMenu[1].titleResource)
    assertEquals(
      shadowOf(ContextCompat.getDrawable(ancRegisterActivity, R.drawable.ic_calender))
        .createdFromResId,
      shadowOf(sideMenu[1].iconResource).createdFromResId
    )
    assertFalse(sideMenu[1].opensMainRegister)
    assertEquals(1, sideMenu[1].countMethod.invoke())
  }

  @Test
  fun testOnSideMenuOptionSelectedShouldVerifyActivityStarting() {

    val menuItemFamily = RoboMenuItem(R.id.menu_item_family)
    ancRegisterActivity.onSideMenuOptionSelected(menuItemFamily)

    var expectedIntent = Intent(ancRegisterActivity, FamilyRegisterActivity::class.java)
    var actualIntent =
      shadowOf(ApplicationProvider.getApplicationContext<AncApplication>()).nextStartedActivity

    assertEquals(expectedIntent.component, actualIntent.component)

    val menuItemAnc = RoboMenuItem(R.id.menu_item_anc)
    ancRegisterActivity.onSideMenuOptionSelected(menuItemAnc)

    expectedIntent = Intent(ancRegisterActivity, AncRegisterActivity::class.java)
    actualIntent =
      shadowOf(ApplicationProvider.getApplicationContext<AncApplication>()).nextStartedActivity

    assertEquals(expectedIntent.component, actualIntent.component)
  }

  @Test
  fun testSupportedFragmentsShouldReturnAncRegisterFragment() {
    val fragments = ancRegisterActivity.supportedFragments()

    assertEquals(1, fragments.size)
    assertEquals(AncRegisterFragment::class.java.simpleName, fragments.first().javaClass.simpleName)
  }

  override fun getActivity(): Activity {
    return ancRegisterActivity
  }

  companion object {
    @JvmStatic
    @BeforeClass
    fun beforeClass() {
      FakeKeyStore.setup
    }
  }
}
