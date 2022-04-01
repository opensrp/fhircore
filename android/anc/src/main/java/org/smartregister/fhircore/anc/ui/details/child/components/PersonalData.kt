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

package org.smartregister.fhircore.anc.ui.details.child.components

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.ui.details.child.model.ChildProfileViewData

@Composable
fun PersonalData(
  childProfileViewData: ChildProfileViewData,
  modifier: Modifier = Modifier,
) {
  val statusTextColor = Color.Gray
  Card(elevation = 3.dp) {
    Column(modifier = modifier.fillMaxWidth().padding(16.dp)) {
      Text(text = childProfileViewData.name, fontWeight = FontWeight.Bold, fontSize = 20.sp)
      if (childProfileViewData.status.isNotEmpty()) {
        Text(
          text = childProfileViewData.status,
          color = Color.Gray,
          modifier = modifier.padding(vertical = 14.dp),
          fontSize = 20.sp
        )
      }
      Text(
        text = stringResource(R.string.id, childProfileViewData.identifier),
        color = statusTextColor,
        fontSize = 20.sp
      )
      Spacer(modifier = modifier.height(16.dp))
      Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.clip(RoundedCornerShape(size = 8.dp)).background(Color(0xFFF5F5F5))
      ) {
        OtherDetailsItem(
          title = stringResource(R.string.sex),
          value = childProfileViewData.sex,
          statusTextColor = statusTextColor
        )
        OtherDetailsItem(
          title = stringResource(R.string.age),
          value = childProfileViewData.age,
          statusTextColor = statusTextColor
        )
        OtherDetailsItem(
          title = stringResource(R.string.dob),
          value = childProfileViewData.dob,
          statusTextColor = statusTextColor
        )
      }
    }
  }
}

@Composable
private fun OtherDetailsItem(
  title: String,
  value: String,
  modifier: Modifier = Modifier,
  statusTextColor: Color
) {
  Column(modifier = modifier.padding(16.dp)) {
    Text(text = title, modifier.padding(bottom = 4.dp), color = statusTextColor, fontSize = 20.sp)
    Text(text = value, fontSize = 20.sp)
  }
}

@Composable
@Preview(showBackground = true)
fun PersonalDataPreview() {
  val patientProfileData =
    ChildProfileViewData(
      name = "Kim Panny",
      status = "Family Head",
      id = "99358357",
      sex = "Female",
      age = "48y",
      dob = "08 Dec"
    )
  PersonalData(childProfileViewData = patientProfileData)
}
