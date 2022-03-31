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

package org.smartregister.fhircore.engine.ui.patient.profile.component

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
import org.smartregister.fhircore.engine.domain.model.PatientFormData
import org.smartregister.fhircore.engine.ui.theme.InfoColor

@Composable
fun PatientForm(
  patientProfileData: PatientFormData,
  onFormClick: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  OutlinedButton(
    onClick = { onFormClick(patientProfileData.questionnaireId) },
    colors =
      ButtonDefaults.buttonColors(
        backgroundColor = InfoColor.copy(alpha = 0.2f),
        contentColor = InfoColor.copy(alpha = 0.8f)
      ),
    modifier = modifier.fillMaxWidth().padding(top = 16.dp, start = 16.dp, end = 16.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(imageVector = Icons.Filled.Add, contentDescription = null)
      Text(text = patientProfileData.questionnaire, fontWeight = FontWeight.Light)
    }
  }
}

@Composable
@Preview(showBackground = true)
fun PatientFormPreview() {
  Column {
    PatientForm(
      patientProfileData = PatientFormData("Household survey", "182912"),
      onFormClick = {}
    )
    PatientForm(
      patientProfileData = PatientFormData("Bednet distribution", "182212"),
      onFormClick = {}
    )
    PatientForm(
      patientProfileData = PatientFormData("Malaria diagnosis", "181212"),
      onFormClick = {}
    )
    PatientForm(
      patientProfileData = PatientFormData("Medicine treatment", "171212"),
      onFormClick = {}
    )
    PatientForm(
      patientProfileData = PatientFormData("G6PD test result", "171219"),
      onFormClick = {}
    )
  }
}
