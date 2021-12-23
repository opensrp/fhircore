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

package org.smartregister.fhircore.quest.ui.patient.register.components

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.smartregister.fhircore.engine.ui.theme.SubtitleTextColor
import org.smartregister.fhircore.engine.util.annotation.ExcludeFromJacocoGeneratedReport
import org.smartregister.fhircore.quest.configuration.view.PatientRegisterRowViewConfiguration
import org.smartregister.fhircore.quest.configuration.view.patientRegisterRowViewConfigurationOf
import org.smartregister.fhircore.quest.data.patient.model.G6PDStatus
import org.smartregister.fhircore.quest.data.patient.model.PatientItem
import org.smartregister.fhircore.quest.data.patient.model.genderFull
import org.smartregister.fhircore.quest.data.patient.model.makeItLabel
import org.smartregister.fhircore.quest.ui.patient.register.OpenPatientProfile
import org.smartregister.fhircore.quest.ui.patient.register.PatientRowClickListenerIntent

const val PATIENT_BIO = "patientBio"

@Composable
fun PatientRow(
  patientItem: PatientItem,
  clickListener: (PatientRowClickListenerIntent, PatientItem) -> Unit,
  modifier: Modifier = Modifier,
  patientRegisterRowViewConfiguration: PatientRegisterRowViewConfiguration =
    patientRegisterRowViewConfigurationOf()
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

        if (patientRegisterRowViewConfiguration.showG6pdStatus) {
          patientItem.g6pdStatus?.run {
            Text(color = SubtitleTextColor, text = " - ", fontSize = 16.sp)
            Text(
              text = makeItLabel(LocalContext.current),
              color = color,
              fontSize = 16.sp,
              modifier = modifier.wrapContentWidth()
            )
          }
        }
      }
    }
  }
}

@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
fun PatientRow() {
  MaterialTheme {
    PatientRow(
      patientItem = PatientItem("1", "1", "Rickey Ron", "M", "32y", "", G6PDStatus.Deficient),
      clickListener = { listenerIntent, data -> },
      modifier = Modifier.background(Color.White)
    )
  }
}

@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
fun PatientRowWithG6PDDeficientStatus() {
  MaterialTheme {
    PatientRow(
      patientItem = PatientItem("1", "1", "Rickey Ron", "M", "32y", "", G6PDStatus.Deficient),
      clickListener = { listenerIntent, data -> },
      modifier = Modifier.background(Color.White),
      patientRegisterRowViewConfiguration =
        patientRegisterRowViewConfigurationOf(appId = "g6pd", showG6pdStatus = true)
    )
  }
}

@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
fun PatientRowWithG6PDIntermediateStatus() {
  MaterialTheme {
    PatientRow(
      patientItem = PatientItem("1", "1", "Rickey Ron", "M", "32y", "", G6PDStatus.Intermediate),
      clickListener = { listenerIntent, data -> },
      modifier = Modifier.background(Color.White),
      patientRegisterRowViewConfiguration =
        patientRegisterRowViewConfigurationOf(appId = "g6pd", showG6pdStatus = true)
    )
  }
}

@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
fun PatientRowWithG6PDNormalStatus() {
  MaterialTheme {
    PatientRow(
      patientItem = PatientItem("1", "1", "Rickey Ron", "M", "32y", "", G6PDStatus.Normal),
      clickListener = { listenerIntent, data -> },
      modifier = Modifier.background(Color.White),
      patientRegisterRowViewConfiguration =
        patientRegisterRowViewConfigurationOf(appId = "g6pd", showG6pdStatus = true)
    )
  }
}
