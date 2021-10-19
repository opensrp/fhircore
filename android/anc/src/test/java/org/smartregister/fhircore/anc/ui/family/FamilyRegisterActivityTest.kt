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

import android.app.Activity
import android.app.Application
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
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.fakes.RoboMenuItem
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.anc.AncApplication
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.activity.ActivityRobolectricTest
import org.smartregister.fhircore.anc.data.family.FamilyRepository
import org.smartregister.fhircore.anc.data.patient.PatientRepository
import org.smartregister.fhircore.anc.shadow.AncApplicationShadow
import org.smartregister.fhircore.anc.shadow.FakeKeyStore
import org.smartregister.fhircore.anc.ui.anccare.register.AncRegisterActivity
import org.smartregister.fhircore.anc.ui.family.form.FamilyFormConstants
import org.smartregister.fhircore.anc.ui.family.form.FamilyQuestionnaireActivity
import org.smartregister.fhircore.anc.ui.family.register.FamilyRegisterActivity
import org.smartregister.fhircore.anc.ui.family.register.FamilyRegisterFragment
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity.Companion.QUESTIONNAIRE_ARG_FORM

@Config(shadows = [AncApplicationShadow::class])
internal class FamilyRegisterActivityTest : ActivityRobolectricTest() {

  private lateinit var familyRegisterActivity: FamilyRegisterActivity
  private lateinit var patientRepository: PatientRepository
  private lateinit var familyRepository: FamilyRepository

  @Before
  fun setUp() {
    mockkObject(Sync)
    every { Sync.basicSyncJob(any()).stateFlow() } returns flowOf()
    every { Sync.basicSyncJob(any()).lastSyncTimestamp() } returns OffsetDateTime.now()

    patientRepository = mockk()
    familyRepository = mockk()

    familyRegisterActivity =
      Robolectric.buildActivity(FamilyRegisterActivity::class.java, null).create().get()

    ReflectionHelpers.setField(familyRegisterActivity, "patientRepository", patientRepository)
    ReflectionHelpers.setField(familyRegisterActivity, "familyRepository", familyRepository)
  }

  @After
  fun cleanup() {
    unmockkObject(Sync)
  }

  @Test
  fun testActivityShouldNotNull() {
    assertNotNull(familyRegisterActivity)
  }

  @Test
  fun testActivityHasCorrectSideMenuItem() {

    coEvery { familyRepository.countAll() } returns 1
    coEvery { patientRepository.countAll() } returns 1

    val sideMenu = familyRegisterActivity.sideMenuOptions()

    // verify family menu
    assertEquals(R.id.menu_item_family, sideMenu[0].itemId)
    assertEquals(R.string.family_register_title, sideMenu[0].titleResource)
    assertEquals(
      Shadows.shadowOf(ContextCompat.getDrawable(familyRegisterActivity, R.drawable.ic_calender))
        .createdFromResId,
      Shadows.shadowOf(sideMenu[0].iconResource).createdFromResId
    )
    assertTrue(sideMenu[0].opensMainRegister)
    assertEquals(1, sideMenu[0].countMethod.invoke())

    // verify anc menu
    assertEquals(R.id.menu_item_anc, sideMenu[1].itemId)
    assertEquals(R.string.anc_register_title, sideMenu[1].titleResource)
    assertEquals(
      Shadows.shadowOf(ContextCompat.getDrawable(familyRegisterActivity, R.drawable.ic_baby_mother))
        .createdFromResId,
      Shadows.shadowOf(sideMenu[1].iconResource).createdFromResId
    )
    assertFalse(sideMenu[1].opensMainRegister)
    assertEquals(1, sideMenu[1].countMethod.invoke())
  }

  @Test
  fun testOnSideMenuOptionSelectedShouldVerifyActivityStarting() {

    val menuItemFamily = RoboMenuItem(R.id.menu_item_family)
    familyRegisterActivity.onMenuOptionSelected(menuItemFamily)

    var expectedIntent = Intent(familyRegisterActivity, FamilyRegisterActivity::class.java)
    var actualIntent =
      Shadows.shadowOf(ApplicationProvider.getApplicationContext<AncApplication>())
        .nextStartedActivity

    assertEquals(expectedIntent.component, actualIntent.component)

    val menuItemAnc = RoboMenuItem(R.id.menu_item_anc)
    familyRegisterActivity.onMenuOptionSelected(menuItemAnc)

    expectedIntent = Intent(familyRegisterActivity, AncRegisterActivity::class.java)
    actualIntent =
      Shadows.shadowOf(ApplicationProvider.getApplicationContext<AncApplication>())
        .nextStartedActivity

    assertEquals(expectedIntent.component, actualIntent.component)
  }

  @Test
  fun testRegisterClientShouldStartFamilyQuestionnaireActivity() {
    ReflectionHelpers.callInstanceMethod<FamilyRegisterActivity>(
      familyRegisterActivity,
      "registerClient"
    )

    val expectedIntent = Intent(familyRegisterActivity, FamilyQuestionnaireActivity::class.java)
    val actualIntent =
      Shadows.shadowOf(ApplicationProvider.getApplicationContext<Application>()).nextStartedActivity

    assertEquals(expectedIntent.component, actualIntent.component)
    assertEquals(
      FamilyFormConstants.FAMILY_REGISTER_FORM,
      actualIntent.getStringExtra(QUESTIONNAIRE_ARG_FORM)
    )
  }

  @Test
  fun testSupportedFragmentsShouldReturnAncRegisterFragment() {
    val fragments = familyRegisterActivity.supportedFragments()

    assertEquals(1, fragments.size)
    assertEquals(
      FamilyRegisterFragment::class.java.simpleName,
      fragments.first().javaClass.simpleName
    )
  }

  override fun getActivity(): Activity {
    return familyRegisterActivity
  }

  companion object {
    @JvmStatic
    @BeforeClass
    fun beforeClass() {
      FakeKeyStore.setup
    }
  }
}
