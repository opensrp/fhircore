/*
 * Copyright 2021-2023 Ona Systems, Inc
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

package org.smartregister.fhircore.quest.ui.shared.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.NavController
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.view.CompoundTextProperties
import org.smartregister.fhircore.engine.configuration.view.TextOverFlow
import org.smartregister.fhircore.engine.domain.model.ResourceData

class CompoundTextTest {

  private val navController = mockk<NavController>(relaxed = true, relaxUnitFun = true)
  private val resourceData = mockk<ResourceData>(relaxed = true, relaxUnitFun = true)

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun testWhenMaxLinesIsExceededThenTextIsEllipsized() {
    val temp =
      """
            Lorem Ipsum is simply dummy text of the printing and typesetting industry. 
            Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, 
            when an unknown printer took a galley of type and scrambled it to make a type specimen book. 
            It has survived not only five centuries, but also the leap into electronic typesetting, 
            remaining essentially unchanged. 
            It was popularised in the 1960s with the release of Letraset sheets containing Lorem Ipsum passages, 
            and more recently with desktop publishing software like Aldus PageMaker including versions of Lorem Ipsum.
      """.trimIndent()
    val compoundTextProperties =
      CompoundTextProperties(maxLines = 1, overflow = TextOverFlow.ELLIPSIS, primaryText = temp)
    composeTestRule.setContent {
      CompoundText(
        compoundTextProperties = compoundTextProperties,
        resourceData = resourceData,
        navController = navController
      )
    }

    composeTestRule.onNodeWithText(temp, useUnmergedTree = true).assertDoesNotExist()
  }
  @Test
  fun testWhenMaxLinesIsNotExceededThenTextIsNotEllipsized() {
    val temp = """
            Lorem Ipsum
    """.trimIndent()
    val compoundTextProperties =
      CompoundTextProperties(maxLines = 1, overflow = TextOverFlow.ELLIPSIS, primaryText = temp)
    composeTestRule.setContent {
      CompoundText(
        compoundTextProperties = compoundTextProperties,
        resourceData = resourceData,
        navController = navController
      )
    }
    composeTestRule.onNodeWithText(temp, useUnmergedTree = true).assertExists()
  }
}
