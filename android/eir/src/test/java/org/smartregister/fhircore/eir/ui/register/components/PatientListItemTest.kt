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

package org.smartregister.fhircore.eir.ui.register.components

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import org.junit.Rule
import org.junit.Test
import org.robolectric.annotation.Config
import org.smartregister.fhircore.eir.R
import org.smartregister.fhircore.eir.robolectric.RobolectricTest
import org.smartregister.fhircore.eir.shadow.EirApplicationShadow
import org.smartregister.fhircore.eir.ui.patient.register.components.PatientRowDuePreview
import org.smartregister.fhircore.eir.ui.patient.register.components.PatientRowOverduePreview
import org.smartregister.fhircore.eir.ui.patient.register.components.PatientRowPartialPreview
import org.smartregister.fhircore.eir.ui.patient.register.components.PatientRowVaccinatedPreview

@Config(shadows = [EirApplicationShadow::class])
class PatientListItemTest : RobolectricTest() {

  @get:Rule val composeTestRule = createComposeRule()
  private val app = ApplicationProvider.getApplicationContext<Application>()

  @Test
  fun testPatientRowDuePreview() {
    composeTestRule.setContent { PatientRowDuePreview() }

    composeTestRule.onNodeWithText("Donald Dump, M, 78").assertExists()
    composeTestRule.onNodeWithText("Donald Dump, M, 78").assertIsDisplayed()

    composeTestRule.onNodeWithText("Last seen 2022-02-09").assertExists()
    composeTestRule.onNodeWithText("Last seen 2022-02-09").assertIsDisplayed()

    composeTestRule.onNodeWithText(app.getString(R.string.record_vaccine_nl)).assertExists()
    composeTestRule.onNodeWithText(app.getString(R.string.record_vaccine_nl)).assertIsDisplayed()
  }

  @Test
  fun testPatientRowPartialPreview() {
    composeTestRule.setContent { PatientRowPartialPreview() }

    composeTestRule.onNodeWithText("Donald Dump, M, 78").assertExists()
    composeTestRule.onNodeWithText("Donald Dump, M, 78").assertIsDisplayed()

    composeTestRule.onNodeWithText("Last seen 2022-02-09").assertExists()
    composeTestRule.onNodeWithText("Last seen 2022-02-09").assertIsDisplayed()

    composeTestRule.onNodeWithText("at risk").assertExists()
    composeTestRule.onNodeWithText("at risk").assertIsDisplayed()

    composeTestRule
      .onNodeWithText(app.getString(R.string.status_received_vaccine, 1, "2021-09-21"))
      .assertExists()
    composeTestRule
      .onNodeWithText(app.getString(R.string.status_received_vaccine, 1, "2021-09-21"))
      .assertIsDisplayed()
  }

  @Test
  fun testPatientRowOverduePreview() {
    composeTestRule.setContent { PatientRowOverduePreview() }

    composeTestRule.onNodeWithText("Donald Dump, M, 78").assertExists()
    composeTestRule.onNodeWithText("Donald Dump, M, 78").assertIsDisplayed()

    composeTestRule.onNodeWithText("Last seen 2022-02-09").assertExists()
    composeTestRule.onNodeWithText("Last seen 2022-02-09").assertIsDisplayed()

    composeTestRule.onNodeWithText(app.getString(R.string.status_overdue)).assertExists()
    composeTestRule.onNodeWithText(app.getString(R.string.status_overdue)).assertIsDisplayed()
  }

  @Test
  fun testPatientRowVaccinatedPreview() {
    composeTestRule.setContent { PatientRowVaccinatedPreview() }

    composeTestRule.onNodeWithText("Donald Dump, M, 78").assertExists()
    composeTestRule.onNodeWithText("Donald Dump, M, 78").assertIsDisplayed()

    composeTestRule.onNodeWithText("Last seen 2022-02-09").assertExists()
    composeTestRule.onNodeWithText("Last seen 2022-02-09").assertIsDisplayed()

    composeTestRule.onNodeWithText(app.getString(R.string.status_vaccinated)).assertExists()
    composeTestRule.onNodeWithText(app.getString(R.string.status_vaccinated)).assertIsDisplayed()
  }
}
