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

package org.smartregister.fhircore.engine.configuration

import org.junit.Assert
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.view.ButtonProperties
import org.smartregister.fhircore.engine.configuration.view.ButtonType
import org.smartregister.fhircore.engine.configuration.view.ViewAlignment
import org.smartregister.fhircore.engine.domain.model.ViewType
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.ui.theme.DangerColor
import org.smartregister.fhircore.engine.ui.theme.DefaultColor
import org.smartregister.fhircore.engine.ui.theme.InfoColor
import org.smartregister.fhircore.engine.ui.theme.WarningColor

class ButtonPropertiesTest : RobolectricTest() {
  private val buttonProperties =
    ButtonProperties(
      viewType = ViewType.BUTTON,
      weight = 0f,
      backgroundColor = "@{backgroundColor}",
      padding = 0,
      borderRadius = 2,
      alignment = ViewAlignment.NONE,
      fillMaxWidth = true,
      fillMaxHeight = false,
      clickable = "false",
      visible = "true",
      enabled = "@{enabled}",
      text = "@{text}",
      status = "@{status}",
      smallSized = false,
      fontSize = 14.0f,
      actions = emptyList(),
      buttonType = ButtonType.MEDIUM,
    )

  @Test
  fun testInterpolateInButtonProperties() {
    val map = mutableMapOf<String, String>()
    map["status"] = "DUE"
    map["backgroundColor"] = "#FFA500"
    map["enabled"] = "true"
    map["text"] = "ANC Visit"
    val interpolatedButton = buttonProperties.interpolate(map)
    Assert.assertEquals("DUE", interpolatedButton.status)
    Assert.assertEquals("#FFA500", interpolatedButton.backgroundColor)
    Assert.assertEquals("true", interpolatedButton.enabled)
    Assert.assertEquals("ANC Visit", interpolatedButton.text)
  }

  @Test
  fun testStatusColorIsPopulatedCorrectly() {
    val map = mutableMapOf<String, String>()
    map["status"] = "DUE"
    val statusColorDue = buttonProperties.statusColor(map)
    Assert.assertEquals(statusColorDue, InfoColor)
    map["status"] = "OVERDUE"
    val statusColorOverdue = buttonProperties.statusColor(map)
    Assert.assertEquals(statusColorOverdue, DangerColor)
    map["status"] = "COMPLETED"
    val statusColorCompleted = buttonProperties.statusColor(map)
    Assert.assertEquals(statusColorCompleted, DefaultColor)
    map["status"] = "IN_PROGRESS"
    val statusColorInProgress = buttonProperties.statusColor(map)
    Assert.assertEquals(statusColorInProgress, WarningColor)
    map["status"] = "UPCOMING"
    val statusColorUpcoming = buttonProperties.statusColor(map)
    Assert.assertEquals(statusColorUpcoming, DefaultColor)
  }
}
