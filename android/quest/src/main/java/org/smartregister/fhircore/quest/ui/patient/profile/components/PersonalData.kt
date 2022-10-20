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

package org.smartregister.fhircore.quest.ui.patient.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.ui.theme.PersonalDataBackgroundColor
import org.smartregister.fhircore.engine.ui.theme.StatusTextColor
import org.smartregister.fhircore.quest.ui.shared.models.ProfileViewData

@Composable
fun PersonalData(
  patientProfileViewData: ProfileViewData.PatientProfileViewData,
  modifier: Modifier = Modifier,
) {
  Card(elevation = 3.dp, modifier = modifier.fillMaxWidth()) {
    Column(modifier = modifier.padding(16.dp)) {
      Text(
        text = patientProfileViewData.name,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      if (patientProfileViewData.status != null) {
        Text(
          text = patientProfileViewData.status,
          color = StatusTextColor,
          fontSize = 18.sp,
          modifier = modifier.padding(vertical = 10.dp)
        )
      }
      if (patientProfileViewData.identifier != null) {
        var idKeyValue =
          stringResource(
            R.string.id,
            patientProfileViewData.identifier.ifEmpty {
              stringResource(R.string.identifier_unassigned)
            }
          )
        if (patientProfileViewData.showIdentifierInProfile) {
          idKeyValue =
            stringResource(
              R.string.idKeyValue,
              patientProfileViewData.identifierKey,
              patientProfileViewData.identifier.ifEmpty {
                stringResource(R.string.identifier_unassigned)
              }
            )
          Text(
            text = idKeyValue,
            color = StatusTextColor,
            fontSize = 18.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }
      Spacer(modifier = modifier.height(16.dp))
      Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier =
          modifier.clip(RoundedCornerShape(size = 8.dp)).background(PersonalDataBackgroundColor)
      ) {
        OtherDetailsItem(title = stringResource(R.string.sex), value = patientProfileViewData.sex)
        OtherDetailsItem(title = stringResource(R.string.age), value = patientProfileViewData.age)
      }
    }
  }
}

@Composable
private fun OtherDetailsItem(title: String, value: String, modifier: Modifier = Modifier) {
  Column(modifier = modifier.padding(16.dp)) {
    Text(text = title, modifier.padding(bottom = 4.dp), color = StatusTextColor, fontSize = 18.sp)
    Text(text = value, fontSize = 18.sp)
  }
}

@Composable
@Preview(showBackground = true)
fun PersonalDataPreview() {
  val patientProfileData =
    ProfileViewData.PatientProfileViewData(
      logicalId = "99358357",
      name = "Kim Panny",
      status = "Family Head",
      sex = "Female",
      age = "48y",
      dob = "08 Dec",
      identifier = "123455"
    )
  PersonalData(patientProfileViewData = patientProfileData)
}

@Composable
@Preview(showBackground = true)
fun PersonalDataPreviewWithARTNumber() {
  val patientProfileData =
    ProfileViewData.PatientProfileViewData(
      logicalId = "99358357",
      name = "Kim Panny",
      status = "Family Head",
      sex = "Female",
      age = "48y",
      dob = "08 Dec",
      identifier = "123455",
      identifierKey = "HCC Number",
      showIdentifierInProfile = true
    )
  PersonalData(patientProfileViewData = patientProfileData)
}
