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

package org.smartregister.fhircore.mwcore.ui.patient.register.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.smartregister.fhircore.engine.ui.theme.SubtitleTextColor
import org.smartregister.fhircore.engine.util.annotation.ExcludeFromJacocoGeneratedReport
import org.smartregister.fhircore.mwcore.R
import org.smartregister.fhircore.mwcore.configuration.view.Properties
import org.smartregister.fhircore.mwcore.configuration.view.Property
import org.smartregister.fhircore.mwcore.data.patient.model.AdditionalData
import org.smartregister.fhircore.mwcore.data.patient.model.PatientItem
import org.smartregister.fhircore.mwcore.data.patient.model.genderFull
import org.smartregister.fhircore.mwcore.ui.patient.register.OpenPatientProfile
import org.smartregister.fhircore.mwcore.ui.patient.register.PatientRowClickListenerIntent

const val PATIENT_BIO = "patientBio"

@Composable
fun PatientRow(
  patientItem: PatientItem,
  clickListener: (PatientRowClickListenerIntent, PatientItem) -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min),
  ) {
    Column(
      modifier =
        modifier
          .clickable { clickListener(OpenPatientProfile, patientItem) }
          .padding(15.dp)
          .weight(0.65f)
          .testTag(PATIENT_BIO)
    ) {
      Text(
        text = "${patientItem.name}, ${patientItem.age}",
        fontSize = 18.sp,
        modifier = modifier.wrapContentWidth()
      )
      Spacer(modifier = modifier.height(8.dp))
      Row {
        Text(
          color = SubtitleTextColor,
          text = patientItem.genderFull(),
          fontSize = 16.sp,
          modifier = modifier.wrapContentWidth()
        )
        Column {
          patientItem.additionalData?.forEach {
            Row {
              it.label?.let { label ->
                Text(
                  text = label,
                  color =
                    Color(
                      android.graphics.Color.parseColor(it.properties?.label?.color ?: "#000000")
                    ),
                  fontSize = it.properties?.label?.textSize?.sp ?: 16.sp,
                  modifier = modifier.wrapContentWidth(),
                  fontWeight =
                    FontWeight(it.properties?.label?.fontWeight?.weight ?: FontWeight.Normal.weight)
                )
              }

              Text(
                text = " " + stringResource(id = R.string.last_test, "${it.lastDateAdded}"),
                fontSize = it.properties?.value?.textSize?.sp ?: 16.sp,
                modifier = modifier.wrapContentWidth(),
                fontWeight =
                  FontWeight(it.properties?.value?.fontWeight?.weight ?: FontWeight.Normal.weight)
              )
            }
          }
        }
      }
    }
  }
}

@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
fun PreviewPatientRow() {
  MaterialTheme {
    PatientRow(
      patientItem = PatientItem("1", "1", "Rickey Ron", "M", "32y", ""),
      clickListener = { listenerIntent, data -> },
      modifier = Modifier.background(Color.White)
    )
  }
}

@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
fun PreviewPatientRowWithG6PDNormalStatus() {
  MaterialTheme {
    PatientRow(
      patientItem =
        PatientItem(
          "1",
          "1",
          "Rickey Ron",
          "M",
          "32y",
          "",
          listOf(
            AdditionalData(
              label = " Label 1",
              value = "Normal",
              valuePrefix = " G6PD Status - ",
              properties =
                Properties(
                  label = Property(color = "#FF0000", textSize = 16),
                  value = Property(color = "#00a000", textSize = 16)
                )
            )
          )
        ),
      clickListener = { listenerIntent, data -> },
      modifier = Modifier.background(Color.White)
    )
  }
}
