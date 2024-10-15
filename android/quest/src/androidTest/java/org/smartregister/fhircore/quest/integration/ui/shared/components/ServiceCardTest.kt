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

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.testing.TestNavHostController
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.view.ButtonProperties
import org.smartregister.fhircore.engine.configuration.view.CompoundTextProperties
import org.smartregister.fhircore.engine.configuration.view.ServiceCardProperties
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.domain.model.ServiceStatus
import org.smartregister.fhircore.engine.domain.model.ViewType
import org.smartregister.fhircore.quest.ui.shared.components.DIVIDER_TEST_TAG
import org.smartregister.fhircore.quest.ui.shared.components.ServiceCard

class ServiceCardTest {
  private val resourceData = ResourceData("id", ResourceType.Patient, emptyMap())

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun serviceActionButtonIsDisplayedCorrectly() {
    composeRule.setContent {
      ServiceCard(
        serviceCardProperties = initTestServiceCardProperties(),
        resourceData = resourceData,
        navController = TestNavHostController(LocalContext.current),
        decodeImage = null,
      )
    }
    composeRule
      .onNodeWithText("Next visit 09-10-2022", useUnmergedTree = true)
      .assertExists()
      .assertIsDisplayed()
  }

  @Test
  fun serviceActionButtonIsDisplayedCorrectlyWithButtonParams() {
    composeRule.setContent {
      ServiceCard(
        serviceCardProperties =
          initTestServiceCardProperties(serviceStatus = ServiceStatus.OVERDUE.name, text = "1"),
        resourceData = resourceData,
        navController = TestNavHostController(LocalContext.current),
        decodeImage = null,
      )
    }
    composeRule.onNodeWithText("1", useUnmergedTree = true).assertExists().assertIsDisplayed()
  }

  @Test
  fun serviceActionButtonNotDisplayedWhenVisibleIsFalse() {
    composeRule.setContent {
      ServiceCard(
        serviceCardProperties = initTestServiceCardProperties(visible = "false"),
        resourceData = resourceData,
        navController = TestNavHostController(LocalContext.current),
        decodeImage = null,
      )
    }
    composeRule.onNodeWithText("Next visit 09-10-2022", useUnmergedTree = true).assertDoesNotExist()
  }

  @Test
  fun serviceMemberIconsDisplayedWhenDividerIsNotShown() {
    composeRule.setContent {
      ServiceCard(
        serviceCardProperties = initTestServiceCardProperties(showVerticalDivider = false),
        resourceData = resourceData,
        navController = TestNavHostController(LocalContext.current),
        decodeImage = null,
      )
    }
    composeRule.onNodeWithTag(DIVIDER_TEST_TAG).assertDoesNotExist()
    // icons are displayed
    composeRule.onNodeWithText("+2").assertExists().assertIsDisplayed()
  }

  private fun initTestServiceCardProperties(
    showVerticalDivider: Boolean = false,
    serviceStatus: String = ServiceStatus.UPCOMING.name,
    text: String = "Next visit 09-10-2022",
    visible: String = "true",
  ): ServiceCardProperties {
    return ServiceCardProperties(
      viewType = ViewType.SERVICE_CARD,
      details =
        listOf(
          CompoundTextProperties(
            viewType = ViewType.COMPOUND_TEXT,
            primaryText = "Upcoming household service",
            primaryTextColor = "#000000",
          ),
          CompoundTextProperties(
            viewType = ViewType.COMPOUND_TEXT,
            primaryText = "Town/Village",
            primaryTextColor = "#5A5A5A",
            secondaryText = "HH No.",
            secondaryTextColor = "#555AAA",
          ),
          CompoundTextProperties(
            viewType = ViewType.COMPOUND_TEXT,
            primaryText = "Last visited yesterday",
            primaryTextColor = "#5A5A5A",
          ),
        ),
      serviceMemberIcons = "CHILD,CHILD,CHILD,CHILD",
      showVerticalDivider = showVerticalDivider,
      serviceButton =
        ButtonProperties(
          visible = visible,
          status = serviceStatus,
          text = text,
        ),
    )
  }
}
