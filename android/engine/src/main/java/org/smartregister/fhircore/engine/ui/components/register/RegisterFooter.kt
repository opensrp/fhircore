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

package org.smartregister.fhircore.engine.ui.components.register

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.ui.theme.GreyTextColor
import org.smartregister.fhircore.engine.util.annotation.PreviewWithBackgroundExcludeGenerated

const val DEFAULT_MAX_PAGE_COUNT = 20
const val SEARCH_FOOTER_TAG = "searchFooterTag"
const val SEARCH_FOOTER_PREVIOUS_BUTTON_TAG = "searchFooterPreviousButtonTag"
const val SEARCH_FOOTER_NEXT_BUTTON_TAG = "searchFooterNextButtonTag"
const val SEARCH_FOOTER_PAGINATION_TAG = "searchFooterPaginationTag"

@Composable
fun RegisterFooter(
  resultCount: Int,
  currentPage: Int,
  pagesCount: Int,
  previousButtonClickListener: () -> Unit,
  nextButtonClickListener: () -> Unit,
  modifier: Modifier = Modifier,
) {
  if (resultCount > 0) {
    Row(modifier = modifier.fillMaxWidth().testTag(SEARCH_FOOTER_TAG)) {
      Box(
        modifier = modifier.weight(1f).padding(4.dp).wrapContentWidth(Alignment.Start),
      ) {
        if (currentPage > 1) {
          TextButton(
            onClick = previousButtonClickListener,
            modifier = modifier.testTag(SEARCH_FOOTER_PREVIOUS_BUTTON_TAG),
          ) {
            Icon(
              painter = painterResource(id = R.drawable.ic_chevron_left),
              contentDescription = stringResource(R.string.str_next),
            )
            Text(
              fontSize = 14.sp,
              color = MaterialTheme.colors.primary,
              text = stringResource(id = R.string.str_previous),
            )
          }
        }
      }
      Text(
        fontSize = 14.sp,
        color = GreyTextColor,
        text = stringResource(id = R.string.str_page_info, currentPage, pagesCount),
        modifier =
          modifier
            .testTag(SEARCH_FOOTER_PAGINATION_TAG)
            .padding(4.dp)
            .align(Alignment.CenterVertically),
      )
      Box(
        modifier = modifier.weight(1f).padding(4.dp).wrapContentWidth(Alignment.End),
      ) {
        if (currentPage < pagesCount) {
          TextButton(
            onClick = nextButtonClickListener,
            modifier = modifier.testTag(SEARCH_FOOTER_NEXT_BUTTON_TAG),
          ) {
            Text(
              fontSize = 14.sp,
              color = MaterialTheme.colors.primary,
              text = stringResource(id = R.string.str_next),
            )
            Icon(
              painter = painterResource(id = R.drawable.ic_chevron_right),
              contentDescription = stringResource(R.string.str_next),
            )
          }
        }
      }
    }
  }
}

@PreviewWithBackgroundExcludeGenerated
@Composable
fun SearchFooterPreviewNoPreviousButton() {
  RegisterFooter(10, 1, DEFAULT_MAX_PAGE_COUNT, {}, {})
}

@PreviewWithBackgroundExcludeGenerated
@Composable
fun SearchFooterPreviewNoNextButton() {
  RegisterFooter(10, 20, DEFAULT_MAX_PAGE_COUNT, {}, {})
}

@PreviewWithBackgroundExcludeGenerated
@Composable
fun SearchFooterPreviewWithBothPreviousAndNextButtons() {
  RegisterFooter(10, 6, DEFAULT_MAX_PAGE_COUNT, {}, {})
}

@PreviewWithBackgroundExcludeGenerated
@Composable
fun SearchFooterPreviewWithZeroResults() {
  RegisterFooter(0, 6, DEFAULT_MAX_PAGE_COUNT, {}, {})
}
