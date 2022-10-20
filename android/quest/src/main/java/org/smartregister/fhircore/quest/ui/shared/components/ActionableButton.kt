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

package org.smartregister.fhircore.quest.ui.shared.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import org.hl7.fhir.r4.model.Patient
import org.smartregister.fhircore.engine.configuration.view.ButtonProperties
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.domain.model.ServiceStatus
import org.smartregister.fhircore.engine.ui.theme.DangerColor
import org.smartregister.fhircore.engine.ui.theme.DefaultColor
import org.smartregister.fhircore.engine.ui.theme.InfoColor
import org.smartregister.fhircore.engine.ui.theme.SuccessColor
import org.smartregister.fhircore.engine.util.extension.interpolate
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
    val status = buttonProperties.interpolateStatus(resourceData.computedValuesMap)
    OutlinedButton(
      onClick = {
        if (status != ServiceStatus.UPCOMING && status != ServiceStatus.COMPLETED) {
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
          contentColor =
            buttonProperties.statusColor(resourceData.computedValuesMap).copy(alpha = 0.9f)
        ),
      modifier =
        modifier
          .fillMaxWidth()
          .padding(top = 0.dp, start = 12.dp, end = 12.dp)
          .wrapContentHeight()
          .testTag(ACTIONABLE_BUTTON_TEST_TAG)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier.wrapContentHeight().fillMaxWidth()
      ) {
        Spacer(modifier = modifier.weight(0.5f).wrapContentHeight())
        Icon(
          modifier = modifier.size(16.dp),
          imageVector =
            if (status == ServiceStatus.COMPLETED) Icons.Filled.Check else Icons.Filled.Add,
          contentDescription = null,
          tint =
            when (status) {
              ServiceStatus.COMPLETED -> SuccessColor.copy(alpha = 0.9f)
              else ->
                buttonProperties.statusColor(resourceData.computedValuesMap).copy(alpha = 0.9f)
            }
        )
        Spacer(modifier = modifier.width(6.dp))
        Text(
          text = buttonProperties.text?.interpolate(resourceData.computedValuesMap).toString(),
          fontWeight = FontWeight.Medium,
          color =
            if (status == ServiceStatus.COMPLETED) DefaultColor.copy(0.9f)
            else buttonProperties.statusColor(resourceData.computedValuesMap).copy(alpha = 0.9f)
        )
        Spacer(modifier = modifier.weight(0.5f))
        if (status == ServiceStatus.COMPLETED) {
          Icon(
            imageVector = Icons.Filled.ArrowDropDown,
            contentDescription = null,
            tint = DefaultColor.copy(alpha = 0.9f)
          )
        }
      }
    }
  }
}

/**
 * This function determines the status color to display depending on the value of the service status
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
  }
}

@Composable
fun ButtonProperties.interpolateStatus(computedValuesMap: Map<String, Any>): ServiceStatus {
  val interpolated = this.status.interpolate(computedValuesMap)
  return if (ServiceStatus.values().map { it.name }.contains(interpolated))
    ServiceStatus.valueOf(interpolated)
  else ServiceStatus.UPCOMING
}

@Composable
@Preview(showBackground = true)
fun ActionableButtonPreview() {
  Column(modifier = Modifier.height(50.dp)) {
    ActionableButton(
      buttonProperties = ButtonProperties(status = "OVERDUE", text = "Button Text"),
      resourceData = ResourceData(Patient()),
      navController = rememberNavController()
    )
  }
}

@Composable
@Preview(showBackground = true)
fun SmallActionableButtonPreview() {
  Column(modifier = Modifier.height(50.dp)) {
    ActionableButton(
      buttonProperties = ButtonProperties(status = "DUE", text = "Due Task"),
      resourceData = ResourceData(Patient()),
      navController = rememberNavController()
    )
  }
}
