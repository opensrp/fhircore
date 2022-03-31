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

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.domain.model.PatientProfileRowItem
import org.smartregister.fhircore.engine.domain.model.PatientProfileSection
import org.smartregister.fhircore.engine.ui.components.Separator
import org.smartregister.fhircore.engine.ui.theme.DangerColor
import org.smartregister.fhircore.engine.ui.theme.DefaultColor
import org.smartregister.fhircore.engine.ui.theme.InfoColor
import org.smartregister.fhircore.engine.ui.theme.OverdueColor
import org.smartregister.fhircore.engine.ui.theme.StatusTextColor
import org.smartregister.fhircore.engine.ui.theme.WarningColor

@Composable
fun ProfileActionableItem(
  patientProfileRowItem: PatientProfileRowItem,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier.fillMaxWidth().padding(16.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      if (patientProfileRowItem.profileSection == PatientProfileSection.UPCOMING_SERVICES &&
          patientProfileRowItem.startIcon != null
      ) {
        Box(
          contentAlignment = Alignment.Center,
          modifier =
            modifier
              .padding(end = 8.dp)
              .clip(RoundedCornerShape(6.dp))
              .background(
                patientProfileRowItem.startIconBackgroundColor ?: DefaultColor.copy(alpha = 0.3f),
              )
              .padding(8.dp)
        ) {
          Image(
            painter = painterResource(patientProfileRowItem.startIcon),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            colorFilter =
              ColorFilter.tint(
                if (patientProfileRowItem.startIconBackgroundColor != null) Color.White
                else Color.Black.copy(alpha = 0.5f)
              )
          )
        }
      }
      Column {
        TitleRow(patientProfileRowItem = patientProfileRowItem, modifier = modifier)
        Spacer(modifier = modifier.height(8.dp))
        SubtitleRow(patientProfileRowItem = patientProfileRowItem, modifier = modifier)
      }
    }
    ActionButton(patientProfileRowItem, modifier)
  }
}

@Composable
private fun ActionButton(patientProfileRowItem: PatientProfileRowItem, modifier: Modifier) {
  if (patientProfileRowItem.profileSection == PatientProfileSection.TASKS &&
      patientProfileRowItem.actionButtonColor != null &&
      patientProfileRowItem.actionButtonText != null
  ) {
    OutlinedButton(
      onClick = { /*TODO perform click action */},
      colors =
        ButtonDefaults.buttonColors(
          backgroundColor = patientProfileRowItem.actionButtonColor.copy(alpha = 0.2f),
          contentColor = patientProfileRowItem.actionButtonColor,
        )
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = Icons.Filled.Add, contentDescription = null)
        Text(text = patientProfileRowItem.actionButtonText)
      }
    }
  } else if (patientProfileRowItem.showAngleRightIcon) {
    Icon(
      imageVector = Icons.Outlined.ChevronRight,
      contentDescription = null,
      tint = DefaultColor.copy(0.7f)
    )
  }
}

@Composable
private fun TitleRow(patientProfileRowItem: PatientProfileRowItem, modifier: Modifier) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Text(
      text = patientProfileRowItem.title,
      fontWeight = FontWeight.SemiBold,
      modifier = modifier.padding(end = 8.dp)
    )
    if (patientProfileRowItem.titleIcon != null)
      Image(painter = painterResource(patientProfileRowItem.titleIcon), contentDescription = null)
  }
}

@Composable
private fun SubtitleRow(patientProfileRowItem: PatientProfileRowItem, modifier: Modifier) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Text(
      text = patientProfileRowItem.subtitle,
      color = StatusTextColor,
      fontSize = 12.sp,
      modifier = modifier.padding(end = 8.dp)
    )
    if (patientProfileRowItem.showDot) Separator()
    if (patientProfileRowItem.subtitleStatus != null)
      Text(
        text = patientProfileRowItem.subtitleStatus,
        color = patientProfileRowItem.subtitleStatusColor ?: StatusTextColor,
        modifier =
          modifier
            .clip(RoundedCornerShape(6.dp))
            .background(
              if (patientProfileRowItem.showDot) Color.Transparent
              else
                patientProfileRowItem.subtitleStatusColor?.copy(alpha = 0.2f)
                  ?: StatusTextColor.copy(alpha = 0.2f)
            )
            .padding(4.dp)
      )
  }
}

@Composable
@Preview(showBackground = true)
fun ProfileActionableItemForTasksPreview() {
  Column {
    ProfileActionableItem(
      PatientProfileRowItem(
        title = "ANC",
        titleIcon = R.drawable.ic_pregnant,
        subtitle = "due date",
        profileSection = PatientProfileSection.TASKS,
        actionButtonColor = InfoColor,
        actionButtonText = "ANC visit"
      )
    )
    Divider()
    ProfileActionableItem(
      PatientProfileRowItem(
        title = "Sick",
        titleIcon = R.drawable.ic_pregnant,
        subtitle = "due date",
        profileSection = PatientProfileSection.TASKS,
        actionButtonColor = OverdueColor,
        actionButtonText = "Malaria medicine"
      )
    )
  }
}

@Composable
@Preview(showBackground = true)
fun ProfileActionableItemForAncCardPreview() {
  Column {
    ProfileActionableItem(
      PatientProfileRowItem(
        title = "Granuloma Annulare",
        subtitle = "23 weeks (EDD: 20-Jun-2021)",
        profileSection = PatientProfileSection.SERVICE_CARD
      )
    )
    Divider()
    ProfileActionableItem(
      PatientProfileRowItem(
        title = "Blood pressure",
        subtitle = "111/80",
        subtitleStatus = "at risk",
        subtitleStatusColor = WarningColor,
        profileSection = PatientProfileSection.SERVICE_CARD
      )
    )
    Divider()
    ProfileActionableItem(
      PatientProfileRowItem(
        title = "Heart rate",
        subtitle = "186",
        subtitleStatus = "danger",
        subtitleStatusColor = DangerColor,
        profileSection = PatientProfileSection.SERVICE_CARD
      )
    )
    Divider()
    ProfileActionableItem(
      PatientProfileRowItem(
        title = "Weight gain",
        subtitle = "+ 6.7kg",
        subtitleStatus = "good",
        subtitleStatusColor = DefaultColor,
        profileSection = PatientProfileSection.SERVICE_CARD
      )
    )
  }
}

@Composable
@Preview(showBackground = true)
fun ProfileActionableItemForMedicalHistoryPreview() {
  Column {
    ProfileActionableItem(
      PatientProfileRowItem(
        title = "Diarrhoea",
        subtitle = "Stomach ache, with painful running stomach",
        profileSection = PatientProfileSection.MEDICAL_HISTORY
      )
    )
    Divider()
    ProfileActionableItem(
      PatientProfileRowItem(
        title = "Malaria",
        subtitle = "High temperatures and loss of appetite, long sleepless nights",
        profileSection = PatientProfileSection.MEDICAL_HISTORY
      )
    )
    Divider()
    ProfileActionableItem(
      PatientProfileRowItem(
        title = "Health issue",
        subtitle = "Description of symptoms",
        profileSection = PatientProfileSection.MEDICAL_HISTORY
      )
    )
  }
}

@Composable
@Preview(showBackground = true)
fun ProfileActionableItemForTestResultsPreview() {
  Column {
    ProfileActionableItem(
      PatientProfileRowItem(
        title = "Deficient (5-Oct-2021)",
        subtitle = "G6PD: 4.4",
        subtitleStatus = "Hb: 2.2",
        profileSection = PatientProfileSection.TEST_RESULTS,
        showDot = true,
        showAngleRightIcon = true
      )
    )
    Divider()
    ProfileActionableItem(
      PatientProfileRowItem(
        title = "Normal (30-Aug-2020)",
        subtitle = "G6PD: 6.0",
        subtitleStatus = "Hb: 9.0",
        profileSection = PatientProfileSection.TEST_RESULTS,
        showDot = true,
        showAngleRightIcon = true
      )
    )
    Divider()
    ProfileActionableItem(
      PatientProfileRowItem(
        title = "Deficient (11-Mar-2020)",
        subtitle = "G6PD: 4.3",
        subtitleStatus = "Hb: 2.0",
        profileSection = PatientProfileSection.TEST_RESULTS,
        showDot = true,
        showAngleRightIcon = true
      )
    )
  }
}

@Composable
@Preview(showBackground = true)
fun ProfileActionableItemForUpcomingServicesPreview() {
  Column {
    ProfileActionableItem(
      PatientProfileRowItem(
        title = "ANC facility visit",
        subtitle = "22-May-2021",
        profileSection = PatientProfileSection.UPCOMING_SERVICES,
        startIcon = R.drawable.gm_calendar_today_24,
        startIconBackgroundColor = WarningColor.copy(alpha = 0.7f)
      )
    )
    Divider()
    ProfileActionableItem(
      PatientProfileRowItem(
        title = "Sick check in",
        subtitle = "25-Aug-2021",
        profileSection = PatientProfileSection.UPCOMING_SERVICES,
        startIcon = R.drawable.ic_households,
        startIconBackgroundColor = DangerColor.copy(alpha = 0.6f)
      )
    )
    Divider()
    ProfileActionableItem(
      PatientProfileRowItem(
        title = "Vaccination",
        subtitle = "03-Sept-2021",
        profileSection = PatientProfileSection.UPCOMING_SERVICES,
        startIcon = R.drawable.ic_needle,
        startIconBackgroundColor = InfoColor.copy(alpha = 0.5f)
      )
    )
  }
}
