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

package org.smartregister.fhircore.anc.ui.anccare.register.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.data.model.AncPatientItem
import org.smartregister.fhircore.anc.data.model.VisitStatus
import org.smartregister.fhircore.anc.ui.anccare.register.AncRowClickListenerIntent
import org.smartregister.fhircore.anc.ui.anccare.register.OpenPatientProfile
import org.smartregister.fhircore.anc.ui.anccare.register.RecordAncVisit
import org.smartregister.fhircore.engine.ui.theme.DueColor
import org.smartregister.fhircore.engine.ui.theme.DueLightColor
import org.smartregister.fhircore.engine.ui.theme.OverdueDarkRedColor
import org.smartregister.fhircore.engine.ui.theme.OverdueLightColor
import org.smartregister.fhircore.engine.ui.theme.SubtitleTextColor

@Composable
fun AncRow(
  ancPatientItem: AncPatientItem,
  clickListener: (AncRowClickListenerIntent, AncPatientItem) -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
    modifier =
      modifier.fillMaxWidth().height(IntrinsicSize.Min).clickable {
        clickListener(OpenPatientProfile, ancPatientItem)
      }
  ) {
    Column(
      modifier =
        modifier.wrapContentWidth(Alignment.Start).padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
      Text(
        text = ancPatientItem.demographics,
        fontSize = 18.sp,
        modifier = modifier.wrapContentWidth()
      )
      Spacer(modifier = modifier.height(8.dp))
      Row {
        Text(
          color = SubtitleTextColor,
          text = ancPatientItem.address.capitalize(Locale.current),
          fontSize = 14.sp,
          modifier = modifier.wrapContentWidth()
        )
      }
    }
    Column(modifier = modifier.padding(20.dp).weight(0.45f)) {
      when (ancPatientItem.visitStatus) {
        VisitStatus.DUE -> ancVisitButton(DueColor, DueLightColor, ancPatientItem, clickListener)
        VisitStatus.OVERDUE ->
          ancVisitButton(OverdueColor, OverdueLightColor, ancPatientItem, clickListener)
      }
    }
    AncVisitButton(
      modifier = modifier.wrapContentWidth(Alignment.End).padding(horizontal = 16.dp),
      ancPatientItem = ancPatientItem,
      clickListener = clickListener
    )
  }
}

@Composable
fun AncVisitButton(
  ancPatientItem: AncPatientItem,
  clickListener: (AncRowClickListenerIntent, AncPatientItem) -> Unit,
  modifier: Modifier = Modifier
) {

  val textColor =
    when (ancPatientItem.visitStatus) {
      AncVisitStatus.DUE -> DueColor
      AncVisitStatus.OVERDUE -> OverdueDarkRedColor
      AncVisitStatus.PLANNED -> Color.Transparent
    }

  val bgColor =
    when (ancPatientItem.visitStatus) {
      AncVisitStatus.DUE -> DueLightColor
      AncVisitStatus.OVERDUE -> OverdueLightColor
      AncVisitStatus.PLANNED -> Color.Transparent
    }

  Text(
    text = stringResource(R.string.anc_record_visit_button_title),
    color = textColor,
    fontSize = 16.sp,
    fontWeight = FontWeight.Bold,
    modifier =
      modifier
        .clip(RoundedCornerShape(2.8.dp))
        .wrapContentWidth()
        .background(color = bgColor)
        .padding(4.8.dp)
        .clickable { clickListener(RecordAncVisit, ancPatientItem) },
  )
}

@Composable
@Preview(showBackground = true)
fun PreviewAncItemDue() {
  AncRow(
    ancPatientItem =
      AncPatientItem(
        patientIdentifier = "1213231",
        gender = "F",
        age = "27y",
        demographics = "Anna Bell, 27",
        name = "Anna Bell",
        atRisk = "yes risky",
        address = "Nairobi",
        visitStatus = VisitStatus.DUE
      ),
    clickListener = { _, _ -> }
  )
}

@Composable
@Preview(showBackground = true)
fun PreviewAncItemOverDue() {
  AncRow(
    ancPatientItem =
      AncPatientItem(
        patientIdentifier = "1213231",
        gender = "F",
        age = "27y",
        demographics = "Anna Bell, 27",
        name = "Anna Bell",
        atRisk = "yes risky",
        address = "Nairobi",
        visitStatus = VisitStatus.OVERDUE
      ),
    clickListener = { _, _ -> }
  )
}
