/*
 * Copyright 2021-2024 Ona Systems, Inc
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

package org.smartregister.fhircore.quest.integration.ui.shared.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.navigation.compose.rememberNavController
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.view.StackViewProperties
import org.smartregister.fhircore.engine.configuration.view.ViewAlignment
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.quest.ui.shared.components.STACK_VIEW_TEST_TAG
import org.smartregister.fhircore.quest.ui.shared.components.StackView

class StackViewTest {
  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun testStackViewIsRendered() {
    val stackViewProperties = StackViewProperties(alignment = ViewAlignment.CENTER, size = 250)
    composeTestRule.setContent {
      StackView(
        modifier = Modifier,
        stackViewProperties = stackViewProperties,
        resourceData = ResourceData("", ResourceType.Patient, emptyMap()),
        navController = rememberNavController(),
        decodeImage = null,
      )
    }
    composeTestRule.onNodeWithTag(STACK_VIEW_TEST_TAG).assertExists()
  }
}
