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

package org.smartregister.fhircore.engine.ui.components.register

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.ui.theme.GreyTextColor
import org.smartregister.fhircore.engine.ui.theme.SearchHeaderColor
import org.smartregister.fhircore.engine.util.annotation.PreviewWithBackgroundExcludeGenerated

const val SEARCH_HEADER_TEXT_TAG = "searchHeaderTestTag"

@Composable
fun RegisterHeader(modifier: Modifier = Modifier, resultCount: Int) {
  Text(
    text = stringResource(id = R.string.search_result, resultCount),
    color = GreyTextColor,
    modifier =
      modifier
        .testTag(SEARCH_HEADER_TEXT_TAG)
        .background(SearchHeaderColor)
        .padding(horizontal = 16.dp, vertical = 16.dp)
        .fillMaxWidth()
  )
}

@PreviewWithBackgroundExcludeGenerated
@Composable
fun SearchHeaderPreview() {
  RegisterHeader(resultCount = 2)
}
