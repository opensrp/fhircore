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

package org.smartregister.fhircore.engine.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.smartregister.fhircore.engine.domain.model.ActionableButtonData
import org.smartregister.fhircore.engine.ui.theme.DangerColor
import org.smartregister.fhircore.engine.ui.theme.DefaultColor
import org.smartregister.fhircore.engine.ui.theme.SuccessColor

@Composable
fun ActionableButton(
  actionableButtonData: ActionableButtonData,
  modifier: Modifier = Modifier,
  onAction: (String, String?) -> Unit
) {
  OutlinedButton(
    onClick = {
      if (actionableButtonData.questionnaireId != null)
        onAction(
          actionableButtonData.questionnaireId,
          actionableButtonData.backReference?.reference
        )
    },
    colors =
      ButtonDefaults.buttonColors(
        backgroundColor = actionableButtonData.contentColor.copy(alpha = 0.1f),
        contentColor = actionableButtonData.contentColor.copy(alpha = 0.9f)
      ),
    modifier = modifier.fillMaxWidth().padding(top = 0.dp, start = 16.dp, end = 16.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(
        imageVector = actionableButtonData.iconStart,
        contentDescription = null,
        tint = actionableButtonData.iconColor.copy(alpha = 0.9f)
      )
      Text(text = actionableButtonData.questionnaire, fontWeight = FontWeight.Medium)
    }
  }
}

@Composable
@Preview(showBackground = true)
fun PatientFormPreview() {
  Column {
    ActionableButton(
      actionableButtonData = ActionableButtonData("Household survey", "182912"),
      onAction = { _, _ -> }
    )
    ActionableButton(
      actionableButtonData = ActionableButtonData("Bednet distribution", "182212"),
      onAction = { _, _ -> }
    )
    ActionableButton(
      actionableButtonData = ActionableButtonData("Malaria diagnosis", "181212"),
      onAction = { _, _ -> }
    )
    ActionableButton(
      actionableButtonData = ActionableButtonData("Medicine treatment", "171212"),
      onAction = { _, _ -> }
    )
    ActionableButton(
      actionableButtonData =
        ActionableButtonData(
          "Sick child follow-up overdue 3 days ago",
          "171219",
          contentColor = DangerColor,
          iconColor = DangerColor
        ),
      onAction = { _, _ -> }
    )
    ActionableButton(
      actionableButtonData =
        ActionableButtonData(
          "Completed task",
          "171219",
          contentColor = DefaultColor,
          iconColor = SuccessColor,
          iconStart = Icons.Filled.Check
        ),
      onAction = { _, _ -> }
    )
  }
}
