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

package org.smartregister.fhircore.mwcore.ui.patient.register.components.list_items

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.smartregister.fhircore.engine.ui.theme.SubtitleTextColor
import org.smartregister.fhircore.engine.util.annotation.ExcludeFromJacocoGeneratedReport
import org.smartregister.fhircore.mwcore.data.patient.model.PatientItem
import org.smartregister.fhircore.mwcore.data.patient.model.genderFull
import org.smartregister.fhircore.mwcore.ui.patient.register.OpenPatientProfile
import org.smartregister.fhircore.mwcore.ui.patient.register.PatientRowClickListenerIntent

@Composable
fun ClientListItem(
  patientItem: PatientItem,
  clickListener: (PatientRowClickListenerIntent, PatientItem) -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier
      .fillMaxWidth()
      .height(IntrinsicSize.Min)
  ) {
    Spacer(modifier = modifier.width(8.dp))

    // TODO: update ART number to use actual identifier
    Text(
      text = "ART #",
      fontSize = 18.sp,
      fontWeight = FontWeight.Bold
    )
    Spacer(modifier = modifier.width(8.dp))
    Column(
      modifier =
      modifier
        .clickable { clickListener(OpenPatientProfile, patientItem) }
        .padding(15.dp)
        .weight(0.65f)
    ) {
      Text(
        text = patientItem.name,
        fontSize = 18.sp,
        modifier = modifier.wrapContentWidth()
      )
      Spacer(modifier = modifier.height(8.dp))
      Row {
        Text(
          color = SubtitleTextColor,
          text = "${patientItem.age}, ${patientItem.genderFull()}",
          fontSize = 16.sp,
          modifier = modifier.wrapContentWidth()
        )
      }
    }

    val backgroundColor = if (patientItem.genderFull() == "Male") Color.Blue else Color.Magenta

    // TODO: Use the Man and Woman icons instead of Male and Female
    val icon = if (patientItem.genderFull() == "Male") Icons.Filled.Male else Icons.Default.Female

    Box(contentAlignment = Alignment.Center,
      modifier = modifier
      .size(64.dp)
      .background(backgroundColor, RoundedCornerShape(8.dp))
    ) {
      Icon(
        imageVector = icon,
        contentDescription = "image",
        tint = Color.White,
        modifier = modifier.size(32.dp)
      )
    }

    IconButton(onClick = { /*TODO*/ }) {
      Icon(
        imageVector = Icons.Default.MoreVert,
        contentDescription = "image",
        tint = SubtitleTextColor,
      )
    }
  }
}

@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
fun PatientRowPreview() {
  val patientItem =
    PatientItem(
      id = "my-test-id",
      identifier = "10001",
      name = "John Doe",
      gender = "M",
      age = "27",
      address = "Nairobi"
    )
  ClientListItem(patientItem = patientItem, { _, _ -> })
}
