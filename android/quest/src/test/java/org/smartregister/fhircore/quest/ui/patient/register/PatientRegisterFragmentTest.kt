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

package org.smartregister.fhircore.quest.ui.patient.register

import android.content.Intent
import androidx.fragment.app.commitNow
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.smartregister.fhircore.engine.ui.register.model.RegisterFilterType
import org.smartregister.fhircore.quest.QuestApplication
import org.smartregister.fhircore.quest.data.patient.model.PatientItem
import org.smartregister.fhircore.quest.robolectric.RobolectricTest
import org.smartregister.fhircore.quest.shadow.FakeKeyStore
import org.smartregister.fhircore.quest.shadow.QuestApplicationShadow
import org.smartregister.fhircore.quest.ui.patient.details.QuestPatientDetailActivity

@Config(shadows = [QuestApplicationShadow::class])
class PatientRegisterFragmentTest : RobolectricTest() {

  private lateinit var registerFragment: PatientRegisterFragment

  @Before
  fun setUp() {
    registerFragment = PatientRegisterFragment()
    val registerActivity =
      Robolectric.buildActivity(PatientRegisterActivity::class.java).create().resume().get()
    registerActivity.supportFragmentManager.commitNow { add(registerFragment, "") }
  }

  @Test
  fun testPerformFilterShouldReturnTrueWithMatchingDataAndSearchFilter() {
    Assert.assertTrue(
      registerFragment.performFilter(
        RegisterFilterType.SEARCH_FILTER,
        PatientItem(name = "Samia"),
        ""
      )
    )
    Assert.assertTrue(
      registerFragment.performFilter(
        RegisterFilterType.SEARCH_FILTER,
        PatientItem(identifier = "12345"),
        "12345"
      )
    )

    Assert.assertTrue(
      registerFragment.performFilter(
        RegisterFilterType.SEARCH_FILTER,
        PatientItem(name = "Razi"),
        "Razi"
      )
    )

    Assert.assertTrue(
      registerFragment.performFilter(
        RegisterFilterType.SEARCH_FILTER,
        PatientItem(id = "1234"),
        "1234"
      )
    )
  }

  @Test
  fun testPerformFilterShouldReturnFalseForUnhandledFilterType() {
    Assert.assertFalse(
      registerFragment.performFilter(RegisterFilterType.OVERDUE_FILTER, PatientItem(), "222")
    )
  }

  @Test
  fun testNavigateToDetailsShouldGotoToPatientDetailActivity() {
    registerFragment.navigateToDetails("")

    val expectedIntent = Intent(registerFragment.context, QuestPatientDetailActivity::class.java)
    val actualIntent =
      Shadows.shadowOf(ApplicationProvider.getApplicationContext<QuestApplication>())
        .nextStartedActivity

    Assert.assertEquals(expectedIntent.component, actualIntent.component)
  }

  @Test
  fun testOnItemClickedWithOpenPatientProfileShouldReturnNavigateToDetails() {
    registerFragment.onItemClicked(OpenPatientProfile, PatientItem())

    val expectedIntent = Intent(registerFragment.context, QuestPatientDetailActivity::class.java)
    val actualIntent =
      Shadows.shadowOf(ApplicationProvider.getApplicationContext<QuestApplication>())
        .nextStartedActivity

    Assert.assertEquals(expectedIntent.component, actualIntent.component)
  }

  companion object {
    @JvmStatic
    @BeforeClass
    fun beforeClass() {
      FakeKeyStore.setup
    }
  }
}
