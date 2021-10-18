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

package org.smartregister.fhirecore.quest.ui.patient.register

import android.app.Activity
import android.app.Application
import android.content.Intent
import androidx.core.view.size
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.fakes.RoboMenuItem
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.engine.configuration.view.loadRegisterViewConfiguration
import org.smartregister.fhircore.engine.databinding.BaseRegisterActivityBinding
import org.smartregister.fhircore.engine.ui.questionnaire.QuestionnaireActivity
import org.smartregister.fhircore.quest.QuestApplication
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.ui.patient.register.PatientRegisterActivity
import org.smartregister.fhircore.quest.ui.patient.register.PatientRegisterFragment
import org.smartregister.fhirecore.quest.robolectric.ActivityRobolectricTest
import org.smartregister.fhirecore.quest.shadow.FakeKeyStore
import org.smartregister.fhirecore.quest.shadow.QuestApplicationShadow

@Config(shadows = [QuestApplicationShadow::class])
class PatientRegisterActivityTest : ActivityRobolectricTest() {

  private lateinit var patientRegisterActivity: PatientRegisterActivity

  @Before
  fun setUp() {
    patientRegisterActivity =
      Robolectric.buildActivity(PatientRegisterActivity::class.java).create().resume().get()
  }

  @Test
  fun testSideMenuOptionsShouldReturnZeroOptions() {
    val menu = patientRegisterActivity.sideMenuOptions()

    Assert.assertEquals(0, menu.size)
  }

  @Test
  fun testOnSideMenuOptionSelectedShouldReturnTrue() {
    Assert.assertTrue(patientRegisterActivity.onMenuOptionSelected(RoboMenuItem()))
  }

  @Test
  fun testOnClientMenuOptionSelectedShouldCallRegisterActivity() {
    patientRegisterActivity.onMenuOptionSelected(
      RoboMenuItem().apply { itemId = R.id.menu_item_clients }
    )
    val expectedIntent = Intent(patientRegisterActivity, PatientRegisterActivity::class.java)
    val actualIntent =
      Shadows.shadowOf(ApplicationProvider.getApplicationContext<QuestApplication>())
        .nextStartedActivity

    Assert.assertEquals(expectedIntent.component, actualIntent.component)
  }

  @Test
  fun testRegisterClientShouldStartFamilyQuestionnaireActivity() {
    ReflectionHelpers.callInstanceMethod<PatientRegisterActivity>(
      patientRegisterActivity,
      "registerClient"
    )

    val expectedIntent = Intent(patientRegisterActivity, QuestionnaireActivity::class.java)
    val actualIntent =
      Shadows.shadowOf(ApplicationProvider.getApplicationContext<Application>()).nextStartedActivity

    Assert.assertEquals(expectedIntent.component, actualIntent.component)
    Assert.assertEquals(
      "3435",
      actualIntent.getStringExtra(QuestionnaireActivity.QUESTIONNAIRE_ARG_FORM)
    )
  }

  @Test
  fun testSetupConfigurableViewsShouldUpdateViews() {
    val config =
      ApplicationProvider.getApplicationContext<Application>()
        .loadRegisterViewConfiguration("quest-app-patient-register")

    val activityBinding =
      ReflectionHelpers.getField<BaseRegisterActivityBinding>(
        patientRegisterActivity,
        "registerActivityBinding"
      )
    with(activityBinding) {
      this.btnRegisterNewClient.text = ""
      this.toolbarLayout.tvClientsListTitle.text
      this.bottomNavView.menu.clear()
    }

    patientRegisterActivity.setupConfigurableViews(config)

    with(activityBinding) {
      Assert.assertEquals("Add new client", this.btnRegisterNewClient.text)
      Assert.assertEquals("Clients", this.toolbarLayout.tvClientsListTitle.text)
      Assert.assertEquals(2, this.bottomNavView.menu.size)
    }
  }

  @Test
  fun testBottomMenuOptionsShouldReturnNonZeroOptions() {
    val menu = patientRegisterActivity.bottomNavigationMenuOptions()

    Assert.assertEquals(2, menu.size)
    Assert.assertEquals(R.id.menu_item_clients, menu[0].id)
    Assert.assertEquals(getString(R.string.menu_clients), menu[0].title)
    Assert.assertEquals(R.id.menu_item_settings, menu[1].id)
    Assert.assertEquals(getString(R.string.menu_settings), menu[1].title)
  }

  @Test
  fun testSupportedFragmentsShouldReturnPatientRegisterFragmentList() {
    val fragments = patientRegisterActivity.supportedFragments()

    Assert.assertEquals(1, fragments.size)
    Assert.assertEquals(
      PatientRegisterFragment::class.java.simpleName,
      fragments.first().javaClass.simpleName
    )
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
