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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.smartregister.fhircore.engine.util.annotation.ExcludeFromJacocoGeneratedReport

const val PROGRESS_MSG_TAG = "progressMsg"
const val CIRCULAR_PROGRESS_BAR = "circularProgressBar"

@Composable
fun CircularProgressBar(modifier: Modifier = Modifier, text: String? = null) {
  Column(
    modifier = modifier.testTag(CIRCULAR_PROGRESS_BAR).padding(8.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    CircularProgressIndicator(
      modifier = modifier.align(Alignment.CenterHorizontally).size(28.dp),
      strokeWidth = 2.4.dp
    )
    if (text != null) {
      Text(text = text, modifier = modifier.testTag(PROGRESS_MSG_TAG))
    }
  }
}

@Preview(showBackground = true)
@Composable
@ExcludeFromJacocoGeneratedReport
fun CircularProgressBarPreview() {
  CircularProgressBar()
}
