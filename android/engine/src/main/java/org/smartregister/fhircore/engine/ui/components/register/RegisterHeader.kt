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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Chip
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.ui.filter.FilterOption
import org.smartregister.fhircore.engine.ui.theme.GreyTextColor
import org.smartregister.fhircore.engine.ui.theme.PersonalDataBackgroundColor
import org.smartregister.fhircore.engine.util.annotation.ExcludeFromJacocoGeneratedReport

const val SEARCH_HEADER_TEXT_TAG = "searchHeaderTestTag"

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun RegisterHeader(
  modifier: Modifier = Modifier,
  resultCount: Int,
  activeFilters: List<FilterOption> = listOf(),
) {
  val scrollState = rememberScrollState()
  Row(
    horizontalArrangement = Arrangement.spacedBy(2.dp),
    verticalAlignment = Alignment.CenterVertically,
    modifier =
      modifier
        .background(color = PersonalDataBackgroundColor)
        .fillMaxWidth()
        .horizontalScroll(scrollState),
  ) {
    if (resultCount != -1) {
      Text(
        text = stringResource(id = R.string.search_result, resultCount),
        color = GreyTextColor,
        modifier =
          modifier.testTag(SEARCH_HEADER_TEXT_TAG).padding(horizontal = 16.dp, vertical = 8.dp),
      )
    }
    repeat(activeFilters.size) {
      Chip(onClick = { /*TODO*/}) {
        Text(
          text = activeFilters[it].text(),
          color = GreyTextColor,
          modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
      }
    }
  }
}

@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
fun SearchHeaderPreview() {
  RegisterHeader(resultCount = 2)
}
