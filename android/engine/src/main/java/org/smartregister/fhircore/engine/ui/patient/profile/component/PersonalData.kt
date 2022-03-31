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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.domain.model.PatientProfileData
import org.smartregister.fhircore.engine.ui.theme.PatientProfileBackgroundColor
import org.smartregister.fhircore.engine.ui.theme.StatusTextColor

@Composable
fun PersonalData(
  patientProfileData: PatientProfileData,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxWidth().padding(16.dp)) {
    Text(
      text = patientProfileData.name,
      fontWeight = FontWeight.Bold,
    )
    Text(
      text = patientProfileData.status,
      color = StatusTextColor,
      modifier = modifier.padding(vertical = 10.dp)
    )
    Text(text = stringResource(R.string.id, patientProfileData.id), color = StatusTextColor)
    Spacer(modifier = modifier.height(16.dp))
    Row(
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier =
        modifier.clip(RoundedCornerShape(size = 8.dp)).background(PatientProfileBackgroundColor)
    ) {
      OtherDetailsItem(title = stringResource(R.string.sex), value = patientProfileData.sex)
      OtherDetailsItem(title = stringResource(R.string.age), value = patientProfileData.age)
      OtherDetailsItem(title = stringResource(R.string.dob), value = patientProfileData.dob)
    }
  }
}

@Composable
private fun OtherDetailsItem(title: String, value: String, modifier: Modifier = Modifier) {
  Column(modifier = modifier.padding(16.dp)) {
    Text(text = title, modifier.padding(bottom = 4.dp), color = StatusTextColor)
    Text(text = value)
  }
}

@Composable
@Preview(showBackground = true)
fun PersonalDataPreview() {
  val patientProfileData =
    PatientProfileData(
      name = "Kim Panny",
      status = "Family Head",
      id = "99358357",
      sex = "Female",
      age = "48y",
      dob = "08 Dec"
    )
  PersonalData(patientProfileData = patientProfileData)
}
