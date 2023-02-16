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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.view.ButtonProperties
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.domain.model.ServiceStatus
import org.smartregister.fhircore.engine.ui.theme.DangerColor
import org.smartregister.fhircore.engine.ui.theme.DefaultColor
import org.smartregister.fhircore.engine.ui.theme.InfoColor
import org.smartregister.fhircore.engine.ui.theme.SuccessColor
import org.smartregister.fhircore.engine.ui.theme.WarningColor
import org.smartregister.fhircore.engine.util.annotation.PreviewWithBackgroundExcludeGenerated
import org.smartregister.fhircore.engine.util.extension.interpolate
import org.smartregister.fhircore.quest.util.extensions.clickable
import org.smartregister.fhircore.quest.util.extensions.conditional
import org.smartregister.fhircore.quest.util.extensions.handleClickEvent

const val ACTIONABLE_BUTTON_TEST_TAG = "actionableButtonTestTag"
const val MAX_CHARS = 16

@Composable
fun ActionableButton(
  modifier: Modifier = Modifier,
  buttonProperties: ButtonProperties,
  resourceData: ResourceData,
  navController: NavController,
  shouldTruncateLargeText: Boolean = false
) {
  if (buttonProperties.visible.interpolate(resourceData.computedValuesMap).toBoolean()) {
    val status = buttonProperties.interpolateStatus(resourceData.computedValuesMap)
    val statusColor = buttonProperties.statusColor(resourceData.computedValuesMap)
    val buttonEnabled =
      buttonProperties.enabled.interpolate(resourceData.computedValuesMap).toBoolean()
    val clickable = buttonProperties.clickable(resourceData)
    OutlinedButton(
      onClick = {
        if (buttonEnabled && (status == ServiceStatus.DUE || clickable)) {
          buttonProperties.actions.handleClickEvent(
            navController = navController,
            resourceData = resourceData
          )
        }
      },
      colors =
        ButtonDefaults.buttonColors(
          backgroundColor =
            buttonProperties.statusColor(resourceData.computedValuesMap).copy(alpha = 0.1f),
          contentColor = buttonProperties.statusColor(resourceData.computedValuesMap),
          disabledBackgroundColor = DefaultColor.copy(alpha = 0.1f),
          disabledContentColor = DefaultColor,
        ),
      modifier =
        modifier
          .conditional(buttonProperties.fillMaxWidth, { fillMaxWidth() }, { wrapContentWidth() })
          .padding(horizontal = 12.dp, vertical = 4.dp)
          .wrapContentHeight()
          .testTag(ACTIONABLE_BUTTON_TEST_TAG),
      enabled = buttonEnabled,
      border =
        BorderStroke(
          width = 0.5.dp,
          color = buttonProperties.statusColor(resourceData.computedValuesMap).copy(alpha = 0.1f)
        ),
      elevation = null
    ) {
      // Each component here uses a new modifier to avoid inheriting the properties of the parent
      Icon(
        imageVector =
          if (status == ServiceStatus.COMPLETED) Icons.Filled.Check else Icons.Filled.Add,
        contentDescription = null,
        tint =
          if (buttonEnabled)
            when (status) {
              ServiceStatus.COMPLETED -> SuccessColor
              else -> statusColor
            }
          else DefaultColor,
        modifier = Modifier.size(14.dp)
      )
      val buttonText = buttonProperties.text?.interpolate(resourceData.computedValuesMap).toString()
      val truncatedText =
        if (buttonText.length > MAX_CHARS && shouldTruncateLargeText)
          "${buttonText.substring(0, MAX_CHARS)}..."
        else buttonText

      Text(
        text = truncatedText,
        fontWeight = FontWeight.Medium,
        color =
          if (buttonEnabled)
            when (status) {
              ServiceStatus.COMPLETED -> DefaultColor.copy(0.9f)
              else -> statusColor
            }
          else DefaultColor.copy(0.9f),
        textAlign = TextAlign.Start,
        overflow = TextOverflow.Ellipsis,
        maxLines = 2,
        modifier =
          Modifier.padding(horizontal = 4.dp)
            .conditional(status == ServiceStatus.COMPLETED, { weight(1f) }),
        fontSize = buttonProperties.fontSize.sp
      )
      if (status == ServiceStatus.COMPLETED) {
        Icon(
          imageVector = Icons.Filled.ArrowDropDown,
          contentDescription = null,
          tint = DefaultColor,
          modifier = Modifier.size(18.dp),
        )
      }
    }
  }
}

/**
 * This function determines the status color to display depending on the value of the service status
 *
 * @property computedValuesMap Contains data extracted from the resources to be used on the UI
 */
@Composable
fun ButtonProperties.statusColor(computedValuesMap: Map<String, Any>): Color {
  val interpolated = this.status.interpolate(computedValuesMap)
  val status =
    if (ServiceStatus.values().map { it.name }.contains(interpolated))
      ServiceStatus.valueOf(interpolated)
    else ServiceStatus.UPCOMING

  return when (status) {
    ServiceStatus.DUE -> InfoColor
    ServiceStatus.OVERDUE -> DangerColor
    ServiceStatus.UPCOMING -> DefaultColor
    ServiceStatus.COMPLETED -> DefaultColor
    ServiceStatus.IN_PROGRESS -> WarningColor
  }
}

@Composable
fun ButtonProperties.interpolateStatus(computedValuesMap: Map<String, Any>): ServiceStatus {
  val interpolated = this.status.interpolate(computedValuesMap)
  return if (ServiceStatus.values().map { it.name }.contains(interpolated))
    ServiceStatus.valueOf(interpolated)
  else ServiceStatus.UPCOMING
}

@PreviewWithBackgroundExcludeGenerated
@Composable
fun ActionableButtonPreview() {
  ActionableButton(
    buttonProperties =
      ButtonProperties(
        visible = "true",
        status = ServiceStatus.IN_PROGRESS.name,
        text = "ANC Visit ANC Visit ANC Visit ANC Visit ANC Visit ANC Visit ANC Visit",
        smallSized = true,
      ),
    resourceData = ResourceData("id", ResourceType.Patient, emptyMap(), emptyMap()),
    navController = rememberNavController(),
    shouldTruncateLargeText = true
  )
}

@PreviewWithBackgroundExcludeGenerated
@Composable
fun DisabledActionableButtonPreview() {
  Row(modifier = Modifier.fillMaxWidth()) {
    ActionableButton(
      buttonProperties =
        ButtonProperties(
          visible = "true",
          status = ServiceStatus.COMPLETED.name,
          text = "Issuing of teenage pads and household due on 23-01-2023",
          smallSized = true,
          enabled = "true"
        ),
      resourceData = ResourceData("id", ResourceType.Patient, emptyMap(), emptyMap()),
      navController = rememberNavController()
    )
  }
}

@PreviewWithBackgroundExcludeGenerated
@Composable
fun SmallActionableButtonPreview() {
  Row(modifier = Modifier.fillMaxWidth()) {
    ActionableButton(
      modifier = Modifier.weight(1.0f),
      buttonProperties = ButtonProperties(status = "DUE", text = "Due Task", fillMaxWidth = true),
      resourceData = ResourceData("id", ResourceType.Patient, emptyMap(), emptyMap()),
      navController = rememberNavController()
    )
    ActionableButton(
      modifier = Modifier.weight(1.0f),
      buttonProperties =
        ButtonProperties(status = "COMPLETED", text = "Completed Task", fillMaxWidth = true),
      resourceData = ResourceData("id", ResourceType.Patient, emptyMap(), emptyMap()),
      navController = rememberNavController()
    )
  }
}
