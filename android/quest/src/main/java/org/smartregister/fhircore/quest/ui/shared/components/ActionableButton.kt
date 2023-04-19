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
import org.smartregister.fhircore.engine.configuration.view.ButtonType
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.domain.model.ServiceStatus
import org.smartregister.fhircore.engine.ui.theme.DefaultColor
import org.smartregister.fhircore.engine.ui.theme.SuccessColor
import org.smartregister.fhircore.engine.util.annotation.PreviewWithBackgroundExcludeGenerated
import org.smartregister.fhircore.engine.util.extension.interpolate
import org.smartregister.fhircore.engine.util.extension.parseColor
import org.smartregister.fhircore.quest.util.extensions.clickable
import org.smartregister.fhircore.quest.util.extensions.conditional
import org.smartregister.fhircore.quest.util.extensions.handleClickEvent

const val ACTIONABLE_BUTTON_TEST_TAG = "actionableButtonTestTag"

@Composable
fun ActionableButton(
  modifier: Modifier = Modifier,
  buttonProperties: ButtonProperties,
  resourceData: ResourceData,
  navController: NavController
) {
  if (buttonProperties.visible.interpolate(resourceData.computedValuesMap).toBoolean()) {
    val interpolatedButtonProperties = buttonProperties.interpolate(resourceData.computedValuesMap)
    val statusColor = buttonProperties.statusColor(resourceData.computedValuesMap)
    val status = interpolatedButtonProperties.status
    val backgroundColor = interpolatedButtonProperties.backgroundColor
    val isButtonEnabled = interpolatedButtonProperties.enabled.toBoolean()
    val clickable = buttonProperties.clickable(resourceData)
    OutlinedButton(
      onClick = {
        if (interpolatedButtonProperties.enabled.toBoolean() && (status == ServiceStatus.DUE.name || clickable)) {
          buttonProperties.actions.handleClickEvent(
            navController = navController,
            resourceData = resourceData
          )
        }
      },
      colors =
        ButtonDefaults.buttonColors(
          backgroundColor =
            if (backgroundColor.parseColor() != Color.Unspecified) {
              backgroundColor.parseColor()
            } else statusColor.copy(alpha = 0.1f),
          contentColor = statusColor,
          disabledBackgroundColor = DefaultColor.copy(alpha = 0.1f),
          disabledContentColor = DefaultColor,
        ),
      modifier =
        modifier
          .conditional(buttonProperties.fillMaxWidth, { fillMaxWidth() }, { wrapContentWidth() })
          .padding(horizontal = 12.dp, vertical = 4.dp)
          .wrapContentHeight()
          .testTag(ACTIONABLE_BUTTON_TEST_TAG),
      enabled = interpolatedButtonProperties.enabled.toBoolean(),
      border =
        BorderStroke(
          width = 0.5.dp,
          color = statusColor.copy(alpha = 0.1f)
        ),
      elevation = null
    ) {
      // Each component here uses a new modifier to avoid inheriting the properties of the parent
      Icon(
        imageVector =
          if (status == ServiceStatus.COMPLETED.name) Icons.Filled.Check else Icons.Filled.Add,
        contentDescription = null,
        tint =
        if (isButtonEnabled)
          when (status) {
            ServiceStatus.COMPLETED.name -> SuccessColor
            else -> statusColor
          }
        else DefaultColor,
        modifier = Modifier.size(16.dp)
      )
      Text(
        text = interpolatedButtonProperties.text ?: "",
        fontWeight = FontWeight.Medium,
        color =
          if (isButtonEnabled)
            when (status) {
              ServiceStatus.COMPLETED.name -> DefaultColor.copy(0.9f)
              else -> statusColor
            }
          else DefaultColor.copy(0.9f),
        textAlign = TextAlign.Start,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        modifier =
          Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
            .conditional(status == ServiceStatus.COMPLETED.name, { weight(1f) }),
        fontSize = buttonProperties.fontSize.sp
      )
      if (status == ServiceStatus.COMPLETED.name) {
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

@PreviewWithBackgroundExcludeGenerated
@Composable
fun ActionableButtonPreview() {
  ActionableButton(
    buttonProperties =
      ButtonProperties(
        visible = "true",
        status = ServiceStatus.IN_PROGRESS.name,
        text = "ANC Visit",
        buttonType = ButtonType.TINY
      ),
    resourceData = ResourceData("id", ResourceType.Patient, emptyMap()),
    navController = rememberNavController()
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
          enabled = "true",
          buttonType = ButtonType.BIG
        ),
      resourceData = ResourceData("id", ResourceType.Patient, emptyMap()),
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
      buttonProperties =
        ButtonProperties(
          status = "DUE",
          text = "Due Task",
          fillMaxWidth = true,
          buttonType = ButtonType.TINY
        ),
      resourceData = ResourceData("id", ResourceType.Patient, emptyMap()),
      navController = rememberNavController()
    )
    ActionableButton(
      modifier = Modifier.weight(1.0f),
      buttonProperties =
        ButtonProperties(
          status = "COMPLETED",
          text = "Completed Task",
          fillMaxWidth = true,
          buttonType = ButtonType.TINY
        ),
      resourceData = ResourceData("id", ResourceType.Patient, emptyMap()),
      navController = rememberNavController()
    )
  }
}
