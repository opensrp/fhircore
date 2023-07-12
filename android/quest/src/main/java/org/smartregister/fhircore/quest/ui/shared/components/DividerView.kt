/*
 * Copyright 2021-2023 Ona Systems, Inc
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

import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.smartregister.fhircore.engine.configuration.view.DividerProperties
import org.smartregister.fhircore.engine.ui.theme.DividerColor
import org.smartregister.fhircore.engine.util.annotation.PreviewWithBackgroundExcludeGenerated

const val HORIZONTAL_DIVIDER_TEST_TAG = "horizontalDividerTestTag"

@Composable
fun DividerView(modifier: Modifier = Modifier, dividerProperties: DividerProperties) {
  Divider(
    color = DividerColor,
    thickness = dividerProperties.thickness.dp,
    modifier = modifier.testTag(HORIZONTAL_DIVIDER_TEST_TAG),
  )
}

@PreviewWithBackgroundExcludeGenerated
@Composable
private fun DividerPreview() {
  DividerView(dividerProperties = DividerProperties(thickness = 16f))
}
