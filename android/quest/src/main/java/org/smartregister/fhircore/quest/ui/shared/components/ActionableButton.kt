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

package org.smartregister.fhircore.quest.ui.shared.components

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.navigation.ICON_TYPE_LOCAL
import org.smartregister.fhircore.engine.configuration.navigation.ImageConfig
import org.smartregister.fhircore.engine.configuration.view.ButtonProperties
import org.smartregister.fhircore.engine.configuration.view.ButtonType
import org.smartregister.fhircore.engine.configuration.view.ImageProperties
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.domain.model.ServiceStatus
import org.smartregister.fhircore.engine.ui.theme.DangerColor
import org.smartregister.fhircore.engine.ui.theme.DefaultColor
import org.smartregister.fhircore.engine.ui.theme.SuccessColor
import org.smartregister.fhircore.engine.util.annotation.PreviewWithBackgroundExcludeGenerated
import org.smartregister.fhircore.engine.util.extension.parseColor
import org.smartregister.fhircore.quest.util.extensions.conditional
import org.smartregister.fhircore.quest.util.extensions.handleClickEvent

const val ACTIONABLE_BUTTON_TEST_TAG = "actionableButtonTestTag"

@Composable
fun ActionableButton(
  modifier: Modifier = Modifier,
  buttonProperties: ButtonProperties,
  resourceData: ResourceData,
  navController: NavController,
  decodeImage: ((String) -> Bitmap?)?,
) {
  if (buttonProperties.visible.toBoolean()) {
    val status = buttonProperties.status
    val configuredContentColor = buttonProperties.contentColor.parseColor()
    val statusColor =
      if (configuredContentColor == Color.Unspecified) {
        buttonProperties.statusColor(resourceData.computedValuesMap)
      } else if (status == ServiceStatus.COMPLETED.name) DefaultColor else configuredContentColor
    val backgroundColor = buttonProperties.backgroundColor.parseColor()
    val isButtonEnabled = buttonProperties.enabled.toBoolean()
    val clickable = buttonProperties.clickable.toBoolean()
    val backgroundOpacity = buttonProperties.backgroundOpacity
    val colorOpacity = buttonProperties.colorOpacity
    val context = LocalContext.current
    OutlinedButton(
      onClick = {
        if (
          buttonProperties.enabled.toBoolean() &&
            (status == ServiceStatus.DUE.name || status == ServiceStatus.OVERDUE.name || clickable)
        ) {
          buttonProperties.actions.handleClickEvent(
            navController = navController,
            resourceData = resourceData,
            context = context,
          )
        }
      },
      colors =
        ButtonDefaults.buttonColors(
          backgroundColor =
            if (backgroundColor != Color.Unspecified) {
              backgroundColor
            } else {
              statusColor.copy(alpha = 0.08f)
            },
          contentColor = statusColor,
          disabledBackgroundColor =
            if (backgroundOpacity == 0f) {
              DefaultColor.copy(alpha = 0.08f)
            } else {
              backgroundColor.copy(alpha = backgroundOpacity)
            },
          disabledContentColor =
            if (colorOpacity == 0f) DefaultColor else statusColor.copy(alpha = colorOpacity),
        ),
      modifier =
        modifier
          .conditional(
            buttonProperties.fillMaxWidth,
            { fillMaxWidth() },
            { wrapContentWidth() },
          )
          .conditional(
            buttonProperties.buttonType == ButtonType.TINY,
            { defaultMinSize(minWidth = ButtonDefaults.MinWidth, minHeight = 10.dp) },
            {
              defaultMinSize(
                minWidth = ButtonDefaults.MinWidth,
                minHeight = ButtonDefaults.MinHeight,
              )
            },
          )
          .testTag(ACTIONABLE_BUTTON_TEST_TAG),
      enabled = buttonProperties.enabled.toBoolean(),
      border = BorderStroke(width = 0.8.dp, color = statusColor.copy(alpha = 0.1f)),
      elevation = null,
      contentPadding =
        if (buttonProperties.buttonType == ButtonType.TINY) {
          PaddingValues(vertical = 2.4.dp, horizontal = 4.dp)
        } else {
          PaddingValues(vertical = 4.8.dp, horizontal = 8.dp)
        },
      shape = RoundedCornerShape(buttonProperties.borderRadius),
    ) {
      // Each component here uses a new modifier to avoid inheriting the properties of the
      // parent
      val iconTintColor =
        if (isButtonEnabled) {
          when (status) {
            ServiceStatus.COMPLETED.name -> SuccessColor
            ServiceStatus.FAILED.name -> DangerColor
            else -> statusColor
          }
        } else {
          if (colorOpacity == 0f) DefaultColor else statusColor.copy(alpha = colorOpacity)
        }
      if (buttonProperties.startIcon != null) {
        Image(
          imageProperties = ImageProperties(imageConfig = buttonProperties.startIcon, size = 16),
          tint = iconTintColor,
          resourceData = resourceData,
          navController = navController,
          decodeImage = decodeImage,
        )
      } else {
        Icon(
          imageVector =
            when (status) {
              ServiceStatus.COMPLETED.name -> {
                Icons.Filled.Check
              }
              ServiceStatus.FAILED.name -> {
                Icons.Filled.Clear
              }
              else -> Icons.Filled.Add
            },
          contentDescription = null,
          tint = iconTintColor,
          modifier = Modifier.size(16.dp),
        )
      }
      Text(
        text = buttonProperties.text ?: "",
        fontWeight = FontWeight.W400,
        color =
          if (isButtonEnabled) {
            when (status) {
              ServiceStatus.COMPLETED.name -> DefaultColor.copy(0.9f)
              else -> statusColor
            }
          } else {
            if (colorOpacity == 0.0f) {
              DefaultColor.copy(alpha = 0.9f)
            } else {
              statusColor.copy(alpha = colorOpacity)
            }
          },
        textAlign = TextAlign.Start,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        modifier = Modifier.padding(2.dp),
        fontSize = buttonProperties.fontSize.sp,
        style = TextStyle(letterSpacing = buttonProperties.letterSpacing.sp),
      )
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
      ),
    resourceData = ResourceData("id", ResourceType.Patient, emptyMap()),
    navController = rememberNavController(),
    decodeImage = null,
  )
}

@PreviewWithBackgroundExcludeGenerated
@Composable
fun ActionableButtonTinyButtonPreview() {
  ActionableButton(
    buttonProperties =
      ButtonProperties(
        visible = "true",
        status = ServiceStatus.COMPLETED.name,
        text = "ANC Visit",
        buttonType = ButtonType.TINY,
      ),
    resourceData = ResourceData("id", ResourceType.Patient, emptyMap()),
    navController = rememberNavController(),
    decodeImage = null,
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
          status = ServiceStatus.UPCOMING.name,
          text = "Issue household bed-nets",
          contentColor = "#700f2b",
          enabled = "false",
          backgroundOpacity = 0.06f,
          colorOpacity = 0.6f,
          buttonType = ButtonType.MEDIUM,
          startIcon = ImageConfig(reference = "ic_walk", type = ICON_TYPE_LOCAL),
        ),
      resourceData = ResourceData("id", ResourceType.Patient, emptyMap()),
      navController = rememberNavController(),
      decodeImage = null,
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
          buttonType = ButtonType.MEDIUM,
        ),
      resourceData = ResourceData("id", ResourceType.Patient, emptyMap()),
      navController = rememberNavController(),
      decodeImage = null,
    )
    ActionableButton(
      modifier = Modifier.weight(1.0f),
      buttonProperties =
        ButtonProperties(
          status = "COMPLETED",
          text = "Completed Task",
          fillMaxWidth = true,
          buttonType = ButtonType.TINY,
        ),
      resourceData = ResourceData("id", ResourceType.Patient, emptyMap()),
      navController = rememberNavController(),
      decodeImage = null,
    )
  }
}
