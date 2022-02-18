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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.paging.LoadState
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.ui.theme.GreyTextColor
import org.smartregister.fhircore.engine.util.annotation.ExcludeFromJacocoGeneratedReport

const val MAX_PAGE_COUNT = 20
const val SEARCH_HEADER_TEXT_TAG = "searchHeaderTestTag"
const val SEARCH_FOOTER_TAG = "searchFooterTag"
const val SEARCH_FOOTER_PREVIOUS_BUTTON_TAG = "searchFooterPreviousButtonTag"
const val SEARCH_FOOTER_NEXT_BUTTON_TAG = "searchFooterNextButtonTag"
const val SEARCH_FOOTER_PAGINATION_TAG = "searchFooterPaginationTag"

@Composable
fun SearchHeader(resultCount: Int, modifier: Modifier = Modifier) {
  Text(
    text = stringResource(id = R.string.search_result, resultCount),
    color = GreyTextColor,
    modifier =
      modifier
        .testTag(SEARCH_HEADER_TEXT_TAG)
        .padding(horizontal = 16.dp, vertical = 8.dp)
        .fillMaxWidth()
  )
}

@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
fun SearchHeaderPreview() {
  SearchHeader(resultCount = 2)
}

@Composable
fun SearchFooter(
  resultCount: Int,
  currentPage: Int,
  pageNumbers: Int,
  previousButtonClickListener: () -> Unit,
  nextButtonClickListener: () -> Unit,
  modifier: Modifier = Modifier
) {
  if (resultCount > 0)
    Row(modifier = modifier.fillMaxWidth().testTag(SEARCH_FOOTER_TAG)) {
      Box(
        modifier = modifier.weight(1f).padding(4.dp).wrapContentWidth(Alignment.Start),
      ) {
        if (currentPage > 1) {
          TextButton(
            onClick = previousButtonClickListener,
            modifier = modifier.testTag(SEARCH_FOOTER_PREVIOUS_BUTTON_TAG)
          ) {
            Icon(
              painter = painterResource(id = R.drawable.ic_chevron_left),
              contentDescription = stringResource(R.string.str_next)
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
        text = stringResource(id = R.string.str_page_info, currentPage, pageNumbers),
        modifier =
          modifier
            .testTag(SEARCH_FOOTER_PAGINATION_TAG)
            .padding(4.dp)
            .align(Alignment.CenterVertically)
      )
      Box(
        modifier = modifier.weight(1f).padding(4.dp).wrapContentWidth(Alignment.End),
      ) {
        if (currentPage < pageNumbers) {
          TextButton(
            onClick = nextButtonClickListener,
            modifier = modifier.testTag(SEARCH_FOOTER_NEXT_BUTTON_TAG)
          ) {
            Text(
              fontSize = 14.sp,
              color = MaterialTheme.colors.primary,
              text = stringResource(id = R.string.str_next),
            )
            Icon(
              painter = painterResource(id = R.drawable.ic_chevron_right),
              contentDescription = stringResource(R.string.str_next)
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
  SearchFooter(10, 1, MAX_PAGE_COUNT, {}, {})
}

@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
fun SearchFooterPreviewNoNextButton() {
  SearchFooter(10, 20, MAX_PAGE_COUNT, {}, {})
}

@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
fun SearchFooterPreviewWithBothPreviousAndNextButtons() {
  SearchFooter(10, 6, MAX_PAGE_COUNT, {}, {})
}

@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
fun SearchFooterPreviewWithZeroResults() {
  SearchFooter(0, 6, MAX_PAGE_COUNT, {}, {})
}

/**
 * TODO fix issue with ktfmt formatting annotated high order functions. Current workaround below:
 * lambda in this format content: (@Composable() () -> Unit) to allow spotlessApply
 */
@Composable
fun PaginatedRegister(
  loadState: LoadState,
  showResultsCount: Boolean,
  resultCount: Int,
  body: (@Composable() () -> Unit),
  showPageCount: Boolean = true,
  currentPage: Int,
  pagesCount: Int,
  previousButtonClickListener: () -> Unit,
  nextButtonClickListener: () -> Unit,
  modifier: Modifier = Modifier
) {
  val bottomPadding =
    when {
      showResultsCount -> {
        4.dp
      }
      showPageCount -> {
        40.dp
      }
      else -> 0.dp
    }
  Column(modifier = modifier.fillMaxWidth().height(200.dp)) {
    ConstraintLayout(modifier = Modifier.fillMaxSize()) {
      val (topRef, bodyRef, bottomRef, searchFooterRef) = createRefs()
      Column(
        modifier =
          modifier.constrainAs(topRef) {
            width = Dimension.wrapContent
            height = Dimension.value(4.dp)
            start.linkTo(parent.start)
            top.linkTo(parent.top)
            end.linkTo(parent.end)
          }
      ) { Text(text = "hidden") }
      Column(
        modifier =
          modifier.constrainAs(bottomRef) {
            width = Dimension.fillToConstraints
            height = Dimension.value(4.dp)
            start.linkTo(parent.start)
            bottom.linkTo(parent.bottom)
            end.linkTo(parent.end)
          }
      ) { Text(text = "hidden", color = MaterialTheme.colors.primary) }
      Column(
        modifier =
          modifier.padding(bottom = bottomPadding).fillMaxSize().constrainAs(bodyRef) {
            height = Dimension.fillToConstraints
            start.linkTo(parent.start)
            top.linkTo(topRef.bottom)
            end.linkTo(parent.end)
            bottom.linkTo(bottomRef.top)
          }
      ) {
        if (showResultsCount) {
          SearchHeader(resultCount = resultCount)
        }
        Box(contentAlignment = Alignment.TopCenter, modifier = modifier.fillMaxSize()) {
          if (loadState == LoadState.Loading) {
            CircularProgressBar()
          } else {
            if (resultCount == 0 && showResultsCount) {
              NoResults(modifier = modifier)
            } else {
              body()
            }
          }
        }
      }
      if (showPageCount) {
        if (!showResultsCount) {
          Box(modifier = Modifier.constrainAs(searchFooterRef) { bottom.linkTo(parent.bottom) }) {
            SearchFooter(
              resultCount = resultCount,
              currentPage = currentPage,
              pageNumbers = pagesCount,
              previousButtonClickListener = previousButtonClickListener,
              nextButtonClickListener = nextButtonClickListener
            )
          }
        }
      }
    }
  }
}

@Composable
fun NoResults(modifier: Modifier = Modifier) {
  Column(
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text(
      fontWeight = FontWeight.Bold,
      text = stringResource(R.string.no_results),
      modifier = modifier.padding(8.dp),
      textAlign = TextAlign.Center
    )
    Text(
      color = GreyTextColor,
      text = stringResource(id = R.string.no_results_message),
      modifier = modifier.padding(8.dp),
      textAlign = TextAlign.Center
    )
  }
}

@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
fun NoResultsPreview() {
  NoResults(modifier = Modifier)
}

@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
fun PaginatedRegisterPreviewWithResults() {
  PaginatedRegister(
    loadState = LoadState.Loading,
    showResultsCount = true,
    resultCount = 0,
    body = { Text(text = "Something cool") },
    showPageCount = true,
    currentPage = 0,
    pagesCount = MAX_PAGE_COUNT,
    previousButtonClickListener = {},
    nextButtonClickListener = {}
  )
}

@Composable
@Preview(showBackground = true)
@ExcludeFromJacocoGeneratedReport
fun PaginatedRegisterPreviewWithoutResults() {
  PaginatedRegister(
    loadState = LoadState.Loading,
    showResultsCount = false,
    resultCount = 0,
    body = { Text(text = "Something cool") },
    showPageCount = false,
    currentPage = 0,
    pagesCount = MAX_PAGE_COUNT,
    previousButtonClickListener = {},
    nextButtonClickListener = {}
  )
}
