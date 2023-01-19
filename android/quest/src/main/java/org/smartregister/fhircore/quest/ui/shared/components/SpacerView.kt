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

package org.smartregister.fhircore.quest.ui.shared.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.smartregister.fhircore.engine.configuration.view.SpacerProperties
import org.smartregister.fhircore.engine.util.annotation.ExcludeFromJacocoGeneratedReport
import org.smartregister.fhircore.engine.util.annotation.PreviewWithBackgroundExcludeGenerated

const val VERTICAL_SPACER_TEST_TAG = "verticalSpacerTestTag"
const val HORIZONTAL_SPACER_TEST_TAG = "verticalSpacerTestTag"

@Composable
fun SpacerView(
  modifier: Modifier = Modifier,
  spacerProperties: SpacerProperties,
) {
  if (spacerProperties.height != null) {
    Spacer(
      modifier = modifier.height(spacerProperties.height!!.dp).testTag(VERTICAL_SPACER_TEST_TAG)
    )
  } else if (spacerProperties.width != null) {
    Spacer(
      modifier = modifier.width(spacerProperties.width!!.dp).testTag(HORIZONTAL_SPACER_TEST_TAG)
    )
  }
}

@PreviewWithBackgroundExcludeGenerated
@Composable
private fun VerticalSpacerPreview() {
  SpacerView(spacerProperties = SpacerProperties(height = 16f, width = null))
}

@PreviewWithBackgroundExcludeGenerated
@Composable
private fun HorizontalSpacerPreview() {
  SpacerView(spacerProperties = SpacerProperties(height = null, width = 16f))
}
