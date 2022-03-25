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
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.fragment.app.commitNow
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.mockk
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import org.robolectric.Robolectric
import org.robolectric.Shadows
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.ui.register.model.RegisterFilterType
import org.smartregister.fhircore.engine.util.ListenerIntent
import org.smartregister.fhircore.quest.QuestApplication
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.data.patient.model.PatientItem
import org.smartregister.fhircore.quest.robolectric.RobolectricTest
import org.smartregister.fhircore.quest.ui.patient.details.QuestPatientDetailActivity
import org.smartregister.fhircore.quest.ui.patient.register.components.PATIENT_BIO
import org.smartregister.fhircore.quest.ui.patient.register.components.dummyPatientPagingList

@HiltAndroidTest
class PatientRegisterFragmentTest : RobolectricTest() {

  @get:Rule val hiltRule = HiltAndroidRule(this)

  @get:Rule val composeRule = createComposeRule()

  @BindValue
  var configurationRegistry: ConfigurationRegistry =
    Faker.buildTestConfigurationRegistry("g6pd", mockk())

  private lateinit var registerFragment: PatientRegisterFragment

  @Before
  fun setUp() {
    hiltRule.inject()

    registerFragment = PatientRegisterFragment()
    val registerActivity =
      Robolectric.buildActivity(PatientRegisterActivity::class.java).create().resume().get()
    registerActivity.supportFragmentManager.commitNow { add(registerFragment, "") }
  }

  @Test
  fun testConstructRegisterListShouldEnabled() {
    composeRule.setContent {
      registerFragment.ConstructRegisterList(
        pagingItems = dummyPatientPagingList(),
        modifier = Modifier
      )
    }

    composeRule.onAllNodesWithTag(PATIENT_BIO).assertAll(isEnabled())
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
  fun testPerformFilterShouldReturnTrueForEmptyFilter() {
    Assert.assertTrue(
      registerFragment.performFilter(RegisterFilterType.SEARCH_FILTER, PatientItem(), "")
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

  @Test
  fun testOnItemClickedWithUnrecognizedIntentShouldThrowUnsupportedException() {
    val result =
      assertThrows<UnsupportedOperationException> {
        registerFragment.onItemClicked(object : ListenerIntent {}, PatientItem())
      }

    Assert.assertEquals(UnsupportedOperationException::class.java.name, result::class.java.name)
    Assert.assertEquals("Given ListenerIntent is not supported", result.message)
  }
}
