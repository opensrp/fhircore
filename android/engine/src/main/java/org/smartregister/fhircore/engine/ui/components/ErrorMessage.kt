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

package org.smartregister.fhircore.engine.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.util.annotation.PreviewWithBackgroundExcludeGenerated

const val ERROR_MESSAGE_TAG = "errorMessageTag"
const val TRY_BUTTON_TAG = "tryButtonTag"

@Composable
fun ErrorMessage(message: String, modifier: Modifier = Modifier, onClickRetry: () -> Unit) {
  Column(
    modifier = modifier.padding(16.dp).fillMaxWidth(),
    verticalArrangement = Arrangement.Center
  ) {
    Text(
      text = message,
      fontSize = 14.sp,
      style = MaterialTheme.typography.h6,
      color = MaterialTheme.colors.error,
      modifier =
        modifier
          .padding(vertical = 8.dp)
          .align(Alignment.CenterHorizontally)
          .testTag(ERROR_MESSAGE_TAG)
    )
    OutlinedButton(
      onClick = onClickRetry,
      modifier = modifier.align(Alignment.CenterHorizontally).testTag(TRY_BUTTON_TAG)
    ) { Text(text = stringResource(R.string.try_again)) }
  }
}

@PreviewWithBackgroundExcludeGenerated
@Composable
fun ErrorMessagePreview() {
  ErrorMessage(
    message =
      "Received result from worker com.google.android.fhir.sync.Result@6e1206f and sending output Data",
    onClickRetry = {}
  )
}
