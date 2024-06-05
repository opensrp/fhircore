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

package org.smartregister.fhircore.quest.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.ui.theme.GreyTextColor
import org.smartregister.fhircore.engine.util.annotation.ExcludeFromJacocoGeneratedReport
import org.smartregister.fhircore.quest.ui.shared.models.RegisterViewData

const val DEFAULT_MAX_PAGE_COUNT = 20
const val TOTAL_PAGES_UNKNOWN = -1
const val DEFAULT_PAGE_NAV_KEY = -34
const val DEFAULT_PAGE_NAV_CONTENT_TYPE = -34
const val SEARCH_FOOTER_TAG = "searchFooterTag"
const val SEARCH_FOOTER_PREVIOUS_BUTTON_TAG = "searchFooterPreviousButtonTag"
const val SEARCH_FOOTER_NEXT_BUTTON_TAG = "searchFooterNextButtonTag"
const val SEARCH_FOOTER_PAGINATION_TAG = "searchFooterPaginationTag"

@Composable
fun RegisterFooter(
  previousButtonClickListener: () -> Unit,
  nextButtonClickListener: () -> Unit,
  pageNavigationPagingItems: LazyPagingItems<RegisterViewData.PageNavigationItemView>,
  modifier: Modifier = Modifier,
) {
  LazyColumn(modifier = modifier) {
    items(
      pageNavigationPagingItems.itemCount,
      key = pageNavigationPagingItems.itemKey { DEFAULT_PAGE_NAV_KEY },
      contentType = pageNavigationPagingItems.itemContentType { DEFAULT_PAGE_NAV_CONTENT_TYPE },
    ) {
      val pagingItem = pageNavigationPagingItems[it]!!
      RegisterFooterPageView(
        currentPage = pagingItem.currentPage,
        hasPreviousPage = pagingItem.hasPrev,
        hasNextPage = pagingItem.hasNext,
        previousButtonClickListener = previousButtonClickListener,
        nextButtonClickListener = nextButtonClickListener,
      )
    }
  }
}

@Composable
fun RegisterFooterPageView(
  currentPage: Int,
  hasPreviousPage: Boolean,
  hasNextPage: Boolean,
  previousButtonClickListener: () -> Unit,
  nextButtonClickListener: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth().testTag(SEARCH_FOOTER_TAG),
  ) {
    Box(
      modifier = modifier.weight(1f).padding(4.dp).wrapContentWidth(Alignment.Start),
    ) {
      if (hasPreviousPage) {
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
          id = R.string.str_page,
          currentPage,
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
      if (hasNextPage) {
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
  RegisterFooterPageView(
    1,
    hasPreviousPage = true,
    hasNextPage = true,
    previousButtonClickListener = {},
    nextButtonClickListener = {},
  )
}

@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
fun SearchFooterPreviewNoNextButton() {
  RegisterFooterPageView(
    20,
    true,
    hasNextPage = false,
    previousButtonClickListener = {},
    nextButtonClickListener = {},
  )
}

@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
fun SearchFooterPreviewWithBothPreviousAndNextButtons() {
  RegisterFooterPageView(
    6,
    true,
    hasNextPage = true,
    previousButtonClickListener = {},
    nextButtonClickListener = {},
  )
}

@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
fun SearchFooterPreviewWithZeroResults() {
  RegisterFooterPageView(
    1,
    false,
    hasNextPage = true,
    previousButtonClickListener = {},
    nextButtonClickListener = {},
  )
}
