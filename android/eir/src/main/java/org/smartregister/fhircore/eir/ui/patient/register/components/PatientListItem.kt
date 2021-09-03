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

package org.smartregister.fhircore.eir.ui.patient.register.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.smartregister.fhircore.eir.R
import org.smartregister.fhircore.eir.data.model.PatientItem
import org.smartregister.fhircore.eir.data.model.PatientVaccineStatus
import org.smartregister.fhircore.eir.data.model.VaccineStatus
import org.smartregister.fhircore.eir.ui.patient.register.OpenPatientProfile
import org.smartregister.fhircore.eir.ui.patient.register.PatientRowClickListenerIntent
import org.smartregister.fhircore.eir.ui.patient.register.RecordPatientVaccine
import org.smartregister.fhircore.engine.ui.theme.DividerColor
import org.smartregister.fhircore.engine.ui.theme.OverdueColor
import org.smartregister.fhircore.engine.ui.theme.SubtitleTextColor
import org.smartregister.fhircore.engine.ui.theme.SuccessColor
import org.smartregister.fhircore.engine.ui.theme.WarningColor

@Composable
fun PatientRow(
  patientItem: PatientItem,
  clickListener: (PatientRowClickListenerIntent, PatientItem) -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min)
  ) {
    Column(
      modifier =
        modifier
          .clickable { clickListener(OpenPatientProfile, patientItem) }
          .padding(24.dp)
          .weight(0.65f)
    ) {
      Text(
        text = patientItem.demographics,
        fontSize = 16.sp,
        modifier = modifier.wrapContentWidth()
      )
      Spacer(modifier = modifier.height(8.dp))
      Row {
        Text(
          color = SubtitleTextColor,
          text = stringResource(id = R.string.date_last_seen, patientItem.lastSeen),
          fontSize = 12.sp,
          modifier = modifier.wrapContentWidth()
        )
        Text(
          color = WarningColor,
          text = patientItem.atRisk,
          fontSize = 12.sp,
          modifier = modifier.wrapContentWidth().padding(horizontal = 8.dp)
        )
      }
    }
    Box(
      modifier =
        modifier
          .fillMaxHeight()
          .padding(horizontal = 16.dp)
          .width(1.dp)
          .background(color = DividerColor)
    )
    VaccineStatusItem(
      patientItem = patientItem,
      clickListener = clickListener,
      modifier = modifier.weight(0.35f)
    )
  }
}

@Composable
fun VaccineStatusItem(
  patientItem: PatientItem,
  clickListener: (PatientRowClickListenerIntent, PatientItem) -> Unit,
  modifier: Modifier
) {
  Column(
    modifier = modifier.padding(vertical = 16.dp, horizontal = 16.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    if (patientItem.vaccineStatus.status == VaccineStatus.VACCINATED) {
      Image(
        painter = painterResource(id = R.drawable.ic_green_tick),
        contentDescription = stringResource(id = R.string.check)
      )
    }
    when (patientItem.vaccineStatus.status) {
      VaccineStatus.VACCINATED ->
        Text(text = stringResource(id = R.string.status_vaccinated), color = SuccessColor)
      VaccineStatus.OVERDUE ->
        Column(
          verticalArrangement = Arrangement.Center,
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = modifier.background(color = OverdueColor).fillMaxHeight().fillMaxWidth()
        ) {
          Text(
            text = stringResource(id = R.string.status_overdue),
            color = Color.White,
            modifier = modifier.wrapContentHeight().wrapContentWidth()
          )
        }
      VaccineStatus.PARTIAL ->
        Text(
          text =
            stringResource(
              id = R.string.status_received_vaccine,
              1,
              patientItem.vaccineStatus.date
            ),
          color = SubtitleTextColor,
          modifier = modifier.clickable { clickListener(RecordPatientVaccine, patientItem) }
        )
      VaccineStatus.DUE ->
        Text(
          text = stringResource(id = R.string.record_vaccine_nl),
          color = MaterialTheme.colors.primary,
          modifier = modifier.clickable { clickListener(RecordPatientVaccine, patientItem) }
        )
      else -> return
    }
  }
}

@Composable
@Preview(showBackground = true)
fun PatientRowDuePreview() {
  val patientItem =
    PatientItem(
      demographics = "Donald Dump, M, 78",
      lastSeen = "2022-02-09",
      vaccineStatus = PatientVaccineStatus(VaccineStatus.DUE, "")
    )
  PatientRow(patientItem = patientItem, { _, _ -> })
}

@Composable
@Preview(showBackground = true)
fun PatientRowPartialPreview() {
  val patientItem =
    PatientItem(
      demographics = "Donald Dump, M, 78",
      lastSeen = "2022-02-09",
      atRisk = "at risk",
      vaccineStatus = PatientVaccineStatus(VaccineStatus.PARTIAL, "2021-09-21")
    )
  PatientRow(patientItem = patientItem, { _, _ -> })
}

@Composable
@Preview(showBackground = true)
fun PatientRowOverduePreview() {
  val patientItem =
    PatientItem(
      demographics = "Donald Dump, M, 78",
      lastSeen = "2022-02-09",
      vaccineStatus = PatientVaccineStatus(VaccineStatus.OVERDUE, "")
    )
  PatientRow(patientItem = patientItem, { _, _ -> })
}

@Composable
@Preview(showBackground = true)
fun PatientRowVaccinatedPreview() {
  val patientItem =
    PatientItem(
      demographics = "Donald Dump, M, 78",
      lastSeen = "2022-02-09",
      vaccineStatus = PatientVaccineStatus(VaccineStatus.VACCINATED, "")
    )
  PatientRow(patientItem = patientItem, { _, _ -> })
}
