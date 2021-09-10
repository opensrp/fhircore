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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.LoadState
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.ui.theme.GreyTextColor

@Composable
fun SearchHeader(resultCount: Int, modifier: Modifier = Modifier) {
  Text(
    text = stringResource(id = R.string.search_result, resultCount),
    color = GreyTextColor,
    modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()
  )
}

@Composable
@Preview(showBackground = true)
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
    Row(modifier = modifier.fillMaxWidth()) {
      Box(
        modifier = modifier.weight(1f).padding(4.dp).wrapContentWidth(Alignment.Start),
      ) {
        if (currentPage > 1) {
          TextButton(
            onClick = previousButtonClickListener,
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
        modifier = modifier.padding(4.dp).align(Alignment.CenterVertically)
      )
      Box(
        modifier = modifier.weight(1f).padding(4.dp).wrapContentWidth(Alignment.End),
      ) {
        if (currentPage < pageNumbers) {
          TextButton(
            onClick = nextButtonClickListener,
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
fun SearchFooterPreviewNoPreviousButton() {
  SearchFooter(10, 1, 20, {}, {})
}

@Composable
@Preview(showBackground = true)
fun SearchFooterPreviewNoNextButton() {
  SearchFooter(10, 20, 20, {}, {})
}

@Composable
@Preview(showBackground = true)
fun SearchFooterPreviewWithBothPreviousAndNextButtons() {
  SearchFooter(10, 6, 20, {}, {})
}

@Composable
@Preview(showBackground = true)
fun SearchFooterPreviewWithZeroResults() {
  SearchFooter(0, 6, 20, {}, {})
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
  currentPage: Int,
  pagesCount: Int,
  previousButtonClickListener: () -> Unit,
  nextButtonClickListener: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(modifier = modifier.fillMaxSize()) {
    if (showResultsCount) {
      SearchHeader(resultCount = resultCount)
    }
    Box(
      contentAlignment = Alignment.TopCenter,
      modifier = modifier.weight(1f).padding(4.dp).fillMaxSize()
    ) {
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
    if (!showResultsCount) {
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

@Composable
fun NoResults(modifier: Modifier) {
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
fun NoResultsPreview() {
  NoResults(modifier = Modifier)
}

@Composable
@Preview(showBackground = true)
fun PaginatedRegisterPreviewWithResults() {
  PaginatedRegister(
    loadState = LoadState.Loading,
    showResultsCount = true,
    resultCount = 0,
    body = { Text(text = "Something cool") },
    currentPage = 0,
    pagesCount = 20,
    previousButtonClickListener = {},
    nextButtonClickListener = {}
  )
}

@Composable
@Preview(showBackground = true)
fun PaginatedRegisterPreviewWithoutResults() {
  PaginatedRegister(
    loadState = LoadState.Loading,
    showResultsCount = false,
    resultCount = 0,
    body = { Text(text = "Something cool") },
    currentPage = 0,
    pagesCount = 20,
    previousButtonClickListener = {},
    nextButtonClickListener = {}
  )
}
