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

package org.smartregister.fhircore.anc.ui.reports

import android.app.Application
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import org.junit.Rule
import org.junit.Test
import org.robolectric.annotation.Config
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.robolectric.RobolectricTest
import org.smartregister.fhircore.anc.shadow.AncApplicationShadow

@Config(shadows = [AncApplicationShadow::class])
class ReportsHomeScreenTest : RobolectricTest() {

  @get:Rule val composeRule = createComposeRule()
  private val app = ApplicationProvider.getApplicationContext<Application>()

  @Test
  fun testQuestPatientDetailScreenComponents() {
    composeRule.setContent { ReportsHomeScreen(dummyReportData()) }
    // toolbar should have valid title and icon
    composeRule.onNodeWithTag(TOOLBAR_TITLE).assertTextEquals(app.getString(R.string.reports))
    composeRule.onNodeWithTag(TOOLBAR_BACK_ARROW).assertHasClickAction()
  }
}
