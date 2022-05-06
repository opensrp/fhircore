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
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.smartregister.fhircore.engine.domain.model.FormButtonData

@Composable
fun FormButton(
  formButtonData: FormButtonData,
  modifier: Modifier = Modifier,
  onFormClick: (String) -> Unit
) {
  OutlinedButton(
    onClick = {
      if (formButtonData.questionnaireId != null) onFormClick(formButtonData.questionnaireId)
    },
    colors =
      ButtonDefaults.buttonColors(
        backgroundColor = formButtonData.color.copy(alpha = 0.2f),
        contentColor = formButtonData.color.copy(alpha = 0.6f)
      ),
    modifier = modifier.fillMaxWidth().padding(top = 0.dp, start = 16.dp, end = 16.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(imageVector = Icons.Filled.Add, contentDescription = null)
      Text(text = formButtonData.questionnaire, fontWeight = FontWeight.Medium)
    }
  }
}

@Composable
@Preview(showBackground = true)
fun PatientFormPreview() {
  Column {
    FormButton(formButtonData = FormButtonData("Household survey", "182912"), onFormClick = {})
    FormButton(formButtonData = FormButtonData("Bednet distribution", "182212"), onFormClick = {})
    FormButton(formButtonData = FormButtonData("Malaria diagnosis", "181212"), onFormClick = {})
    FormButton(formButtonData = FormButtonData("Medicine treatment", "171212"), onFormClick = {})
    FormButton(formButtonData = FormButtonData("G6PD test result", "171219"), onFormClick = {})
  }
}
