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

package org.smartregister.fhircore.quest.ui.shared.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.fhir.sync.CurrentSyncJobStatus
import com.google.android.fhir.sync.SyncJobStatus
import com.google.android.fhir.sync.SyncOperation
import java.time.OffsetDateTime
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import org.smartregister.fhircore.engine.ui.theme.DangerColor
import org.smartregister.fhircore.engine.ui.theme.DefaultColor
import org.smartregister.fhircore.engine.ui.theme.SubtitleTextColor
import org.smartregister.fhircore.engine.ui.theme.SuccessColor
import org.smartregister.fhircore.engine.ui.theme.SyncBarBackgroundColor
import org.smartregister.fhircore.engine.util.annotation.PreviewWithBackgroundExcludeGenerated
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.util.extensions.conditional

const val TRANSPARENCY = 0.2f
const val SYNC_PROGRESS_INDICATOR_TEST_TAG = "syncProgressIndicatorTestTag"

@Composable
fun SyncStatusView(
  currentSyncJobStatus: CurrentSyncJobStatus?,
  progressPercentage: Int? = null,
  minimized: Boolean = false,
  onRetry: () -> Unit = {},
  onCancel: () -> Unit = {},
) {
  val height =
    if (minimized) {
      36.dp
    } else if (currentSyncJobStatus is CurrentSyncJobStatus.Running) 92.dp else 56.dp
  Row(
    modifier =
      Modifier.height(height)
        .animateContentSize()
        .background(Color.Transparent) // Inherits the color from the parent
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .conditional(minimized, { padding(vertical = 4.dp) }, { padding(vertical = 16.dp) }),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (
      (currentSyncJobStatus is CurrentSyncJobStatus.Failed ||
        currentSyncJobStatus is CurrentSyncJobStatus.Succeeded)
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        if (!minimized) {
          Icon(
            imageVector =
              if (currentSyncJobStatus is CurrentSyncJobStatus.Succeeded) {
                Icons.Default.CheckCircle
              } else Icons.Default.Error,
            contentDescription = null,
            tint =
              when (currentSyncJobStatus) {
                is CurrentSyncJobStatus.Failed -> DangerColor
                is CurrentSyncJobStatus.Succeeded -> SuccessColor
                else -> DefaultColor
              },
          )
        }
        SyncStatusTitle(
          text =
            if (currentSyncJobStatus is CurrentSyncJobStatus.Succeeded) {
              stringResource(org.smartregister.fhircore.engine.R.string.sync_complete)
            } else {
              stringResource(org.smartregister.fhircore.engine.R.string.sync_error)
            },
          minimized = minimized,
          startPadding = if (minimized) 0 else 16,
        )
      }
    }

    if (currentSyncJobStatus is CurrentSyncJobStatus.Running) {
      Column(modifier = Modifier.weight(1f)) {
        if (!minimized) {
          SyncStatusTitle(
            text =
              stringResource(
                org.smartregister.fhircore.engine.R.string.sync_inprogress,
                progressPercentage ?: 0,
              ),
            minimized = false,
            color = Color.White,
            startPadding = 0,
          )
        }
        LinearProgressIndicator(
          progress = (progressPercentage?.toFloat()?.div(100)) ?: 0f,
          color = MaterialTheme.colors.primary,
          backgroundColor = Color.White,
          modifier =
            Modifier.testTag(SYNC_PROGRESS_INDICATOR_TEST_TAG)
              .padding(vertical = 6.dp)
              .fillMaxWidth(),
        )
        if (!minimized) {
          Text(
            text =
              stringResource(id = org.smartregister.fhircore.engine.R.string.minutes_remaining),
            color = SubtitleTextColor,
            fontSize = 14.sp,
            textAlign = TextAlign.Start,
            modifier = Modifier.align(Alignment.Start),
          )
        }
      }
    }

    if (
      (currentSyncJobStatus is CurrentSyncJobStatus.Failed ||
        currentSyncJobStatus is CurrentSyncJobStatus.Running) && !minimized
    ) {
      Text(
        text =
          stringResource(
            if (currentSyncJobStatus is CurrentSyncJobStatus.Failed) {
              org.smartregister.fhircore.engine.R.string.retry
            } else org.smartregister.fhircore.engine.R.string.cancel,
          ),
        modifier =
          Modifier.padding(start = 16.dp).clickable {
            if (currentSyncJobStatus is CurrentSyncJobStatus.Failed) {
              onRetry()
            } else {
              onCancel()
            }
          },
        color = MaterialTheme.colors.primary,
        fontWeight = FontWeight.SemiBold,
      )
    }
  }
}

@Composable
private fun SyncStatusTitle(
  text: String,
  color: Color = Color.Unspecified,
  minimized: Boolean,
  startPadding: Int,
) {
  Text(
    text = text,
    modifier = Modifier.padding(start = startPadding.dp),
    fontWeight = FontWeight.SemiBold,
    fontSize = if (minimized) 14.sp else 16.sp,
    color = color,
  )
}

@Composable
@PreviewWithBackgroundExcludeGenerated
fun SyncStatusSucceededPreview() {
  AppTheme {
    Column(modifier = Modifier.background(SuccessColor.copy(alpha = TRANSPARENCY))) {
      SyncStatusView(currentSyncJobStatus = CurrentSyncJobStatus.Succeeded(OffsetDateTime.now()))
    }
  }
}

@Composable
@PreviewWithBackgroundExcludeGenerated
fun SyncStatusFailedPreview() {
  AppTheme {
    Column(modifier = Modifier.background(DangerColor.copy(alpha = TRANSPARENCY))) {
      SyncStatusView(currentSyncJobStatus = CurrentSyncJobStatus.Failed(OffsetDateTime.now()))
    }
  }
}

@Composable
@PreviewWithBackgroundExcludeGenerated
fun SyncStatusRunningPreview() {
  AppTheme {
    Column(modifier = Modifier.background(SyncBarBackgroundColor)) {
      SyncStatusView(
        currentSyncJobStatus =
          CurrentSyncJobStatus.Running(
            inProgressSyncJob =
              SyncJobStatus.InProgress(
                SyncOperation.DOWNLOAD,
                187,
                34,
              ),
          ),
      )
    }
  }
}

@Composable
@PreviewWithBackgroundExcludeGenerated
fun SyncStatusSucceededMinimizedPreview() {
  AppTheme {
    Column(modifier = Modifier.background(SuccessColor.copy(alpha = TRANSPARENCY))) {
      SyncStatusView(
        currentSyncJobStatus = CurrentSyncJobStatus.Succeeded(OffsetDateTime.now()),
        minimized = true,
      )
    }
  }
}

@Composable
@PreviewWithBackgroundExcludeGenerated
fun SyncStatusFailedMinimizedPreview() {
  AppTheme {
    Column(modifier = Modifier.background(DangerColor.copy(alpha = TRANSPARENCY))) {
      SyncStatusView(
        currentSyncJobStatus = CurrentSyncJobStatus.Failed(OffsetDateTime.now()),
        minimized = true,
      )
    }
  }
}

@Composable
@PreviewWithBackgroundExcludeGenerated
fun SyncStatusRunningMinimizedPreview() {
  AppTheme {
    Column(modifier = Modifier.background(SyncBarBackgroundColor)) {
      SyncStatusView(
        currentSyncJobStatus =
          CurrentSyncJobStatus.Running(
            inProgressSyncJob =
              SyncJobStatus.InProgress(
                SyncOperation.DOWNLOAD,
                187,
                34,
              ),
          ),
        minimized = true,
      )
    }
  }
}
