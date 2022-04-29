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

package org.smartregister.fhircore.mwcore.ui.task.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.smartregister.fhircore.engine.ui.components.Dot
import org.smartregister.fhircore.engine.ui.theme.BlueTextColor
import org.smartregister.fhircore.engine.ui.theme.DueLightColor
import org.smartregister.fhircore.engine.ui.theme.OverdueDarkRedColor
import org.smartregister.fhircore.engine.ui.theme.OverdueLightColor
import org.smartregister.fhircore.engine.ui.theme.SubtitleTextColor
import org.smartregister.fhircore.mwcore.R
import org.smartregister.fhircore.mwcore.data.task.model.PatientTaskItem
import org.smartregister.fhircore.mwcore.ui.task.PatientTaskListenerIntent

const val ROW_PATIENT_TASK = "rowPatientTask"
const val TEXT_TITLE = "textTitle"
const val TEXT_SUBTITLE_ADDRESS = "textSubtitleAddress"
const val TEXT_SUBTITLE_ID = "textSubtitleId"
const val TEXT_SUBTITLE_DISTANCE = "textSubtitleDistance"
const val TEXT_SUBTITLE_DESCRIPTION = "textSubtitleDescription"
const val ICON_SUBTITLE = "iconSubtitle"
const val LABEL_MAIN = "labelMain"
const val ICON_MAIN = "iconMain"

@Composable
fun PatientTaskRow(
  patientItem: PatientTaskItem,
  useLabel: Boolean,
  clickListener: (PatientTaskListenerIntent, PatientTaskItem) -> Unit,
  displaySelectContentOnly: Boolean = false,
  modifier: Modifier,
) {
  val titleText =
    if (!displaySelectContentOnly) {
      patientItem.demographics()
    } else {
      patientItem.name
    }
  val subTitleText = patientItem.address
  Row(
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier.testTag(ROW_PATIENT_TASK).fillMaxWidth().height(IntrinsicSize.Min)
  ) {
    Column(
      modifier =
        modifier.wrapContentWidth(Alignment.Start).padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
      Text(
        text = titleText,
        fontSize = 18.sp,
        modifier = modifier.testTag(TEXT_TITLE).wrapContentWidth()
      )
      Spacer(modifier = modifier.height(8.dp))
      Row(
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (useLabel) {
          Text(
            color = SubtitleTextColor,
            text = subTitleText,
            fontSize = 14.sp,
            modifier = modifier.testTag(TEXT_SUBTITLE_ADDRESS).wrapContentWidth()
          )
          Dot(modifier = modifier, showDot = patientItem.id.isNotEmpty())
          if (patientItem.id.isNotEmpty()) {
            Text(
              color = SubtitleTextColor,
              text = stringResource(R.string.hash_sign) + patientItem.id,
              fontSize = 14.sp,
              modifier = modifier.testTag(TEXT_SUBTITLE_ID).wrapContentWidth()
            )
          }
        } else {
          Image(
            painter = painterResource(id = R.drawable.ic_walk),
            contentDescription = null,
            modifier = modifier.testTag(ICON_SUBTITLE).padding(end = 4.dp)
          )
          Text(
            color = SubtitleTextColor,
            text = stringResource(R.string.sample_distance),
            fontSize = 14.sp,
            modifier = modifier.testTag(TEXT_SUBTITLE_DISTANCE).wrapContentWidth()
          )
          Dot(modifier = modifier, showDot = patientItem.description.isNotEmpty())
          Text(
            color = SubtitleTextColor,
            text = patientItem.description,
            fontSize = 14.sp,
            modifier = modifier.testTag(TEXT_SUBTITLE_DESCRIPTION).wrapContentWidth()
          )
        }
      }
    }
    if (useLabel) {
      TaskLabel(
        modifier =
          modifier.testTag(LABEL_MAIN).wrapContentWidth(Alignment.End).padding(horizontal = 16.dp),
        patientItem = patientItem,
        clickListener = clickListener
      )
    } else {
      TaskIcon(
        modifier =
          modifier.testTag(ICON_MAIN).wrapContentWidth(Alignment.End).padding(horizontal = 16.dp),
        patientItem = patientItem,
        clickListener = clickListener
      )
    }
  }
}

@Composable
fun TaskIcon(
  patientItem: PatientTaskItem,
  clickListener: (PatientTaskListenerIntent, PatientTaskItem) -> Unit,
  modifier: Modifier
) {
  val iconId = if (patientItem.overdue) R.drawable.ic_overdue else R.drawable.ic_due
  Image(painter = painterResource(id = iconId), contentDescription = null, modifier = modifier)
}

@Composable
fun TaskLabel(
  patientItem: PatientTaskItem,
  clickListener: (PatientTaskListenerIntent, PatientTaskItem) -> Unit,
  modifier: Modifier
) {
  val textColor = if (patientItem.overdue) OverdueDarkRedColor else BlueTextColor
  val bgColor = if (patientItem.overdue) OverdueLightColor else DueLightColor

  Text(
    text = "+ ${patientItem.description}",
    color = textColor,
    fontSize = 16.sp,
    fontWeight = FontWeight.Bold,
    textAlign = TextAlign.Center,
    modifier =
      modifier
        .clip(RoundedCornerShape(2.8.dp))
        .wrapContentWidth()
        .background(color = bgColor)
        .padding(4.8.dp)
  )
}
