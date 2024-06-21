/*
 * Copyright 2021-2024 Ona Systems, Inc
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.ui.theme.InfoColor
import org.smartregister.fhircore.engine.util.annotation.PreviewWithBackgroundExcludeGenerated
import org.smartregister.fhircore.quest.ui.shared.models.MeasureReportSubjectViewData

const val SUBJECT_NAME_TEST_TAG = "subjectNameTestTag"
const val CLOSE_ICON_TEST_TAG = "closeIconTestTag"
const val CHANGE_TEXT_TEST_TAG = "changeTextTestTag"

@Composable
fun SubjectSelector(
  subjects: Set<MeasureReportSubjectViewData>,
  onAddSubject: () -> Unit,
  onRemoveSubject: (MeasureReportSubjectViewData) -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyVerticalGrid(columns = GridCells.Adaptive(130.dp), modifier = modifier.fillMaxWidth()) {
    subjects.forEach { subject ->
      item {
        Row(
          modifier =
            modifier
              .clip(RoundedCornerShape(15.dp))
              .background(color = Color.LightGray.copy(alpha = 0.4f))
              .wrapContentWidth()
              .padding(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = subject.display,
            textAlign = TextAlign.Center,
            fontSize = 16.sp,
            modifier = modifier.testTag(SUBJECT_NAME_TEST_TAG).width(85.dp),
            maxLines = 1,
            softWrap = false,
          )

          Spacer(modifier = Modifier.size(8.dp))
          Row(
            modifier =
              modifier
                .clip(RoundedCornerShape(8.dp))
                .background(color = Color.LightGray)
                .wrapContentWidth()
                .clickable { onRemoveSubject(subject) },
          ) {
            Box(
              modifier =
                modifier
                  .clip(RoundedCornerShape(25.dp))
                  .size(24.dp)
                  .background(color = Color.DarkGray.copy(alpha = 0.4f))
                  .wrapContentWidth()
                  .padding(4.dp),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                Icons.Filled.Close,
                contentDescription = "Back arrow",
                modifier = modifier.size(20.dp).testTag(CLOSE_ICON_TEST_TAG),
              )
            }
          }
        }
      }
    }
    item {
      Text(
        text = stringResource(id = R.string.add),
        textAlign = TextAlign.Center,
        color = InfoColor,
        fontSize = 16.sp,
        modifier = modifier.testTag(CHANGE_TEXT_TEST_TAG).clickable { onAddSubject() },
      )
    }
  }
}

@PreviewWithBackgroundExcludeGenerated
@Composable
fun SelectedSubjectPreview() {
  SubjectSelector(
    subjects =
      setOf(
        MeasureReportSubjectViewData(ResourceType.Patient, "1", "John Jared"),
        MeasureReportSubjectViewData(ResourceType.Patient, "2", "Jane Doe"),
        MeasureReportSubjectViewData(ResourceType.Patient, "3", "John Doe"),
        MeasureReportSubjectViewData(ResourceType.Patient, "4", "Lorem Ipsm"),
        MeasureReportSubjectViewData(ResourceType.Patient, "5", "Mary Magdalene"),
      ),
    onAddSubject = {},
    onRemoveSubject = {},
  )
}
