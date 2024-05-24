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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.ui.theme.GreyTextColor
import org.smartregister.fhircore.engine.util.annotation.ExcludeFromJacocoGeneratedReport

const val DEFAULT_MAX_PAGE_COUNT = 20
const val TOTAL_PAGES_UNKNOWN = -1
const val SEARCH_FOOTER_TAG = "searchFooterTag"
const val SEARCH_FOOTER_PREVIOUS_BUTTON_TAG = "searchFooterPreviousButtonTag"
const val SEARCH_FOOTER_NEXT_BUTTON_TAG = "searchFooterNextButtonTag"
const val SEARCH_FOOTER_PAGINATION_TAG = "searchFooterPaginationTag"

@Composable
fun RegisterFooter(
  currentPageStateFlow: StateFlow<Int>,
  pagesCountStateFlow: StateFlow<Int>,
  previousButtonClickListener: () -> Unit,
  nextButtonClickListener: () -> Unit,
  modifier: Modifier = Modifier,
  onCountLoaded: () -> Unit = {},
) {
  val currentPageState = currentPageStateFlow.collectAsState()
  val currentPage by remember { currentPageState }
  val pagesCountState = pagesCountStateFlow.collectAsState()
  val pagesCount by remember { pagesCountState }
  val customScope = rememberCoroutineScope()

  LaunchedEffect(key1 = pagesCount) {
    if (pagesCount != TOTAL_PAGES_UNKNOWN) {
      customScope.launch { onCountLoaded.invoke() }
    }
  }

  Row(
    modifier = modifier.fillMaxWidth().testTag(SEARCH_FOOTER_TAG),
  ) {
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
      text =
        stringResource(
          id = R.string.str_page_info,
          currentPage,
          if (pagesCount == TOTAL_PAGES_UNKNOWN) "_" else "$pagesCount",
        ),
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

@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
fun SearchFooterPreviewNoPreviousButton() {
  RegisterFooter(MutableStateFlow(1), MutableStateFlow(DEFAULT_MAX_PAGE_COUNT), {}, {})
}

@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
fun SearchFooterPreviewNoNextButton() {
  RegisterFooter(MutableStateFlow(20), MutableStateFlow(DEFAULT_MAX_PAGE_COUNT), {}, {})
}

@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
fun SearchFooterPreviewWithBothPreviousAndNextButtons() {
  RegisterFooter(MutableStateFlow(6), MutableStateFlow(DEFAULT_MAX_PAGE_COUNT), {}, {})
}

@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
fun SearchFooterPreviewWithZeroResults() {
  RegisterFooter(MutableStateFlow(6), MutableStateFlow(DEFAULT_MAX_PAGE_COUNT), {}, {})
}
