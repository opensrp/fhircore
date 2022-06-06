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

package org.smartregister.fhircore.quest.ui.patient.details

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import javax.inject.Inject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.data.patient.PatientRepository
import org.smartregister.fhircore.quest.data.patient.model.DetailsViewItem
import org.smartregister.fhircore.quest.robolectric.RobolectricTest
import org.smartregister.fhircore.quest.ui.patient.register.PatientItemMapper

@HiltAndroidTest
class SimpleDetailsScreenTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
  @get:Rule(order = 30) val composeRule = createComposeRule()

  @Inject lateinit var patientItemMapper: PatientItemMapper

  val patientRepository: PatientRepository = mockk()
  val defaultRepository: DefaultRepository = mockk()

  private lateinit var viewModel: SimpleDetailsViewModel

  @Before
  fun setUp() {
    hiltRule.inject()
    Faker.initPatientRepositoryMocks(patientRepository)

    coEvery { patientRepository.configurationRegistry } returns
      ConfigurationRegistry(
        ApplicationProvider.getApplicationContext(),
        mockk(),
        mockk(),
        mockk(),
        defaultRepository
      )

    viewModel = spyk(SimpleDetailsViewModel(patientRepository = patientRepository))
  }

  @Test
  fun testToolbarComponent() {
    val item =
      ReflectionHelpers.getField<MutableLiveData<DetailsViewItem>>(viewModel, "_detailsViewItem")
    item.postValue(DetailsViewItem("Test Results"))

    composeRule.setContent { SimpleDetailsScreen(viewModel) }

    composeRule
      .onNodeWithTag(DETAILS_TOOLBAR_TITLE)
      .assertTextEquals(
        ApplicationProvider.getApplicationContext<HiltTestApplication>()
          .getString(R.string.test_results)
      )
    composeRule.onNodeWithTag(DETAILS_TOOLBAR_BACK_ARROW).assertHasClickAction()
  }

  @Test
  fun testScreen2ShouldHaveDynamicData() {
    composeRule.setContent { simpleDetailsScreenView2() }

    val rows = composeRule.onAllNodesWithTag(DETAILS_DATA_ROW, true)
    rows.assertCountEquals(4)
    rows[0].onChildAt(0).assert(hasText("Sample Label 1"))
    rows[0].onChildAt(1).assert(hasText("Val 1"))
    rows[0].onChildAt(2).assert(hasText("Sample Label Two"))
    rows[0].onChildAt(3).assert(hasText("Val 2"))
    rows[1].onChildren().assertCountEquals(0)
    rows[2].onChildAt(0).assert(hasText("Label 1"))
    rows[2].onChildAt(1).assert(hasText("Value of Magenta"))
  }

  @Test
  fun testScreen1ShouldHaveDynamicData() {
    composeRule.setContent { simpleDetailsScreenView1() }

    val rows = composeRule.onAllNodesWithTag(DETAILS_DATA_ROW, true)
    rows.assertCountEquals(6)
    rows[0].onChildAt(0).assert(hasText("Sample Label 1"))
    rows[0].onChildAt(1).assert(hasText("Val 1"))
    rows[0].onChildAt(2).assert(hasText("Sample Label Two"))
    rows[0].onChildAt(3).assert(hasText("Val 2"))
    rows[1].onChildren().assertCountEquals(0)
    rows[2].onChildAt(0).assert(hasText("Label 1"))
    rows[2].onChildAt(1).assert(hasText("Value of Yellow"))
    rows[3].onChildAt(0).assert(hasText("What is the value of Label"))
    rows[4].onChildAt(0).assert(hasText("My test label with long text"))
    rows[5].onChildAt(1).assert(hasText("Another Dynamic value 1 having a different sample text"))
  }

  @Test
  fun testScreenWithEmptyDataShouldHaveEmptyViews() {
    composeRule.setContent { emptyView() }

    val rows = composeRule.onAllNodesWithTag(DETAILS_DATA_ROW, true)
    rows.assertCountEquals(0)
  }

  @Test
  fun testToolbarBackPressedButtonShouldCallBackPressedClickListener() {
    composeRule.setContent { SimpleDetailsScreen(viewModel) }

    composeRule.onNodeWithTag(DETAILS_TOOLBAR_BACK_ARROW).performClick()
    verify { viewModel.onBackPressed(true) }
  }
}
