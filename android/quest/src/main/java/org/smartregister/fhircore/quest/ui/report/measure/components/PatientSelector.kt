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

package org.smartregister.fhircore.quest.ui.report.measure.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.smartregister.fhircore.engine.ui.theme.InfoColor
import org.smartregister.fhircore.engine.util.annotation.ExcludeFromJacocoGeneratedReport
import org.smartregister.fhircore.quest.R

const val PATIENT_NAME_TEST_TAG = "patientNameTestTag"
const val CLOSE_ICON_TEST_TAG = "closeIconTestTag"
const val CLOSE_ICON_BACKGROUND_TEST_TAG = "closeIconBackgroundTestTag"
const val CHANGE_TEXT_TEST_TAG = "changeTextTestTag"
const val CHANGE_ROW_TEST_TAG = "changeRowTestTag"

@Composable
fun PatientSelector(
  patientName: String,
  onChangePatient: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier.wrapContentWidth().padding(vertical = 8.dp, horizontal = 8.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier =
        modifier
          .clip(RoundedCornerShape(15.dp))
          .background(color = Color.LightGray.copy(alpha = 0.4f))
          .wrapContentWidth()
          .padding(8.dp),
      contentAlignment = Alignment.Center
    ) {
      Row(
        modifier = Modifier.align(Alignment.Center),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = patientName,
          textAlign = TextAlign.Center,
          fontSize = 16.sp,
          modifier = modifier.testTag(PATIENT_NAME_TEST_TAG)
        )
        Spacer(modifier = Modifier.size(8.dp))
        Row(
          modifier =
            modifier
              .clip(RoundedCornerShape(8.dp))
              .background(color = Color.LightGray)
              .wrapContentWidth()
              .clickable { onChangePatient() }
        ) {
          Box(
            modifier =
              modifier
                .clip(RoundedCornerShape(25.dp))
                .size(24.dp)
                .background(color = Color.DarkGray.copy(alpha = 0.4f))
                .wrapContentWidth()
                .padding(4.dp)
                .testTag(CLOSE_ICON_BACKGROUND_TEST_TAG),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              Icons.Filled.Close,
              contentDescription = "Back arrow",
              modifier = Modifier.size(20.dp).testTag(CLOSE_ICON_TEST_TAG)
            )
          }
        }
      }
    }
    Row(
      modifier =
        modifier
          .wrapContentWidth()
          .clickable { onChangePatient() }
          .padding(vertical = 8.dp, horizontal = 12.dp)
          .testTag(CHANGE_ROW_TEST_TAG),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = stringResource(id = R.string.change),
        textAlign = TextAlign.Center,
        color = InfoColor,
        fontSize = 16.sp,
        modifier = modifier.testTag(CHANGE_TEXT_TEST_TAG)
      )
    }
  }
}

@Preview(showBackground = true)
@Composable
@ExcludeFromJacocoGeneratedReport
fun SelectedPatientPreview() {
  PatientSelector(patientName = "Mary Magdalene", onChangePatient = {})
}
